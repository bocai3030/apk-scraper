package com.bocai.ac.nav;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.apache.http.HttpException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bocai.ac.SnarferInterface;

public class NavRegEx extends NavBase {

    protected int _regExGroup = 0;
    protected Pattern _regExPattern = null;

    protected NavRegEx(final String name, final String handler, final String target, final String targetType, final Node xml) {
        super(name, handler, target, targetType, xml);
        this._regExPattern = Pattern.compile(target, Pattern.DOTALL | Pattern.MULTILINE);
        final NodeList children = xml.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node curNode = children.item(i);
            if ("target".equalsIgnoreCase(curNode.getNodeName())) {
                this._regExGroup = Integer.parseInt(curNode.getAttributes().getNamedItem("group").getTextContent());
            }
        }
    }

    @Override
    public void runNav(final SnarferInterface parent, final String pageContent) throws HttpException, IOException, TransformerException, InterruptedException {

        // Build our URL list
        final boolean urlIsRelative = "relative".equalsIgnoreCase(this.getNavType());
        final Map<String, String> urlToKey = new HashMap<String, String>();
        final Matcher matcher = this._regExPattern.matcher(pageContent);
        while (matcher.find()) {
            final String url = matcher.group(this._regExGroup);
            if (urlIsRelative) {
                urlToKey.put(parent.getSiteRoot() + url, null);
            } else {
                urlToKey.put(url, null);
            }
        }

        // Navigate to the URL(s)
        super.runNav(parent, urlToKey, "get");
    }

}
