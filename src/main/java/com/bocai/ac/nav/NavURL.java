package com.bocai.ac.nav;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.http.HttpException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bocai.ac.SnarferInterface;

public class NavURL extends NavBase {

    private String _urlMethod = "get";

    public String getUrlMethod() {
        return this._urlMethod;
    }

    protected NavURL(final String name, final String handler, final String target, final String targetType, final Node xml) {
        super(name, handler, target, targetType, xml);

        final NodeList children = xml.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node curNode = children.item(i);
            if ("target".equalsIgnoreCase(curNode.getNodeName())) {
                final Node methodNode = curNode.getAttributes().getNamedItem("method");
                if (methodNode != null) {
                    this._urlMethod = methodNode.getTextContent();
                }
            }
        }
    }

    @Override
    public void runNav(final SnarferInterface parent, final String pageContent) throws IOException, TransformerException, InterruptedException, HttpException {

        // Build our URL list
        String dataKey = null;
        final boolean urlIsRelative = "relative".equalsIgnoreCase(this.getNavType());
        final Map<String, String> urlToKey = new HashMap<String, String>();
        if (this.getInput() != null) {
            if ("file".equalsIgnoreCase(this.getInputType())) {

                // Read in an input file
                FileInputStream fileInStream = null;
                DataInputStream dataInStream = null;
                BufferedReader reader = null;
                try {
                    fileInStream = new FileInputStream(this.getInput());
                    dataInStream = new DataInputStream(fileInStream);
                    reader = new BufferedReader(new InputStreamReader(dataInStream));
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        if (this._isDataKey) {
                            dataKey = line;
                        }
                        if (urlIsRelative) {
                            urlToKey.put(parent.getSiteRoot() + String.format(this.getTarget(), URLEncoder.encode(line, "UTF-8")), dataKey);
                        } else {
                            urlToKey.put(String.format(this.getTarget(), URLEncoder.encode(line, "UTF-8")), dataKey);
                        }
                    }
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                            reader = null;
                        } catch (final Throwable t) {
                        }
                    }
                    if (dataInStream != null) {
                        try {
                            dataInStream.close();
                            dataInStream = null;
                        } catch (final Throwable t) {
                        }
                    }
                    if (fileInStream != null) {
                        try {
                            fileInStream.close();
                            fileInStream = null;
                        } catch (final Throwable t) {
                        }
                    }
                }

            } else {
                throw (new RuntimeException("Unsupported input type: " + this.getInputType()));
            }
        } else {
            if (urlIsRelative) {
                urlToKey.put(parent.getSiteRoot() + this.getTarget(), dataKey);
            } else {
                urlToKey.put(this.getTarget(), dataKey);
            }
        }

        // Navigate to the URL(s)
        super.runNav(parent, urlToKey, this.getUrlMethod());
    }

}
