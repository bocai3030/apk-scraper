package com.bocai.ac.nav;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.net.SocketException;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bocai.ac.SnarferInterface;
import com.bocai.ac.Utility;

public abstract class NavBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(NavBase.class);

    /** Returns a Nav instance of the correct type */
    public static NavBase getInstance(final Node xml) throws TransformerException {

        String name = null;
        String handler = null;
        String target = null;
        String targetType = null;

        name = xml.getAttributes().getNamedItem("name").getTextContent();

        final NodeList children = xml.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node curNode = children.item(i);
            if ("handler".equalsIgnoreCase(curNode.getNodeName())) {
                handler = curNode.getTextContent();
            } else if ("target".equalsIgnoreCase(curNode.getNodeName())) {
                target = curNode.getTextContent();
                targetType = curNode.getAttributes().getNamedItem("type").getTextContent();
            }
        }

        if ((targetType == null) || (target == null) || (handler == null) || (name == null)) {
            throw (new RuntimeException("Missing required XML elements in: " + Utility.xmlToString(xml)));
        }

        // Get an instance of the correct navigation class
        NavBase navInstance = null;
        final String className = String.format("%s.%s", NavBase.class.getPackage().getName(), targetType);
        try {
            final Class<?> navClass = Class.forName(className);
            final Constructor<?> navConstructor = navClass.getDeclaredConstructor(String.class, String.class, String.class, String.class, Node.class);
            navConstructor.setAccessible(true);
            navInstance = (NavBase) navConstructor.newInstance(name, handler, target, targetType, xml);
        } catch (final Exception e) {
            throw (new RuntimeException("Unsupported navigation target type found in the profile XML: " + className, e));
        }
        return (navInstance);
    }

    private String _name = null;
    private String _handler = null;
    private String _target = null;
    private String _targetType = null;
    private String _input = null;
    private String _inputType = null;
    private String _navType = "relative"; // Can be 'relative' or 'absolute'
    protected boolean _isDataKey = false;

    public String getName() {
        return this._name;
    }

    public String getHandler() {
        return this._handler;
    }

    public String getTarget() {
        return this._target;
    }

    public String getTargetType() {
        return this._targetType;
    }

    public String getInput() {
        return this._input;
    }

    public String getInputType() {
        return this._inputType;
    }

    public String getNavType() {
        return this._navType;
    }

    protected NavBase(final String name, final String handler, final String target, final String targetType, final Node xml) {

        this._name = name;
        this._handler = handler;
        this._target = target;
        this._targetType = targetType;

        final NodeList children = xml.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node curNode = children.item(i);
            if ("input".equalsIgnoreCase(curNode.getNodeName())) {
                this._input = curNode.getTextContent();
                this._inputType = curNode.getAttributes().getNamedItem("type").getTextContent();
                final Node dataKeyNode = curNode.getAttributes().getNamedItem("dataKey");
                if (dataKeyNode != null) {
                    this._isDataKey = Boolean.parseBoolean(dataKeyNode.getTextContent());
                }
            } else if ("target".equalsIgnoreCase(curNode.getNodeName())) {
                final Node urlTypeNode = curNode.getAttributes().getNamedItem("ra");
                if (urlTypeNode != null) {
                    this._navType = urlTypeNode.getTextContent();
                }
            }
        }
    }

    /** Loads the given URL using the given method and returns the resulting content as text */
    public static String runUrlNav(final SnarferInterface parent, final String url, final String methodStr, final String dataKey, final int[] statusCode) throws HttpException, InterruptedException {
        String pageContent = null;
        boolean runAgain = false;
        do {
            try {
                NavBase.LOGGER.info("NAV: Loading " + url);
                runAgain = false;

                // Prepare the search GET request
                final HttpClient client = new DefaultHttpClient();
                HttpGet method = null;
                if ("post".equalsIgnoreCase(methodStr)) {
                    throw (new RuntimeException("POST method not yet supported"));
                } else {
                    method = new HttpGet(url);
                }
                // method.setRequestHeader("User-Agent", userAgent);

                // Make the HTTP request and check the results
                final long start = System.currentTimeMillis();
                final HttpResponse response = client.execute(method);

                statusCode[0] = response.getStatusLine().getStatusCode();

                if (statusCode[0] != HttpStatus.SC_OK) {
                    NavBase.LOGGER.error(String.format("NAV: URL failed [code:%d]", statusCode[0]));
                    return (null);
                }
                // TODO wow this is bad but so is everything else so just move forward for now
                // We will process this as a stream later

                // Read the search results
                final HttpEntity entity = response.getEntity();
                final StringWriter writer = new StringWriter();
                IOUtils.copy(entity.getContent(), writer);

                pageContent = writer.toString();
                final long requestTime = System.currentTimeMillis() - start;
                NavBase.LOGGER.info(String.format("NAV: Finished loading %s %d ms", url, requestTime));
            } catch (final IllegalArgumentException sockEx) {
                NavBase.LOGGER.warn("skipped invalid URL: " + url);
            } catch (final SocketException sockEx) {
                NavBase.LOGGER.warn("HTTP Request Failed", sockEx);
                Thread.sleep(60000); // Sleep for 1 minute before trying again
                runAgain = true;
            } catch (final IOException e) {
                NavBase.LOGGER.warn("IOException: " + url);
            }
        } while (runAgain);
        return (pageContent);
    }

    protected void runNav(final SnarferInterface parent, final Map<String, String> urlToKey, final String methodStr) throws HttpException, IOException, TransformerException, InterruptedException {

        // Run our URL based navigation operations
        for (final String url : urlToKey.keySet()) {

            // Look up the current data key (or set as needed)
            final String dataKey = urlToKey.get(url);
            if ((dataKey != null) && (dataKey.length() > 0)) {
                parent.setCurrentDataKey(dataKey);
            }

            // Request the content from the URL
            final String pageContent = NavBase.runUrlNav(parent, url, methodStr, dataKey, new int[1]);
            if ((pageContent == null) || (pageContent.length() <= 0)) {
                continue;
            }

            // Run the handler for this content
            parent.runHandler(this.getHandler(), url, pageContent);
        }
    }

    public abstract void runNav(SnarferInterface parent, String pageContent) throws HttpException, IOException, TransformerException, InterruptedException;

}
