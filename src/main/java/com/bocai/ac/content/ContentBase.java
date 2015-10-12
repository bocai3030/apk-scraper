package com.bocai.ac.content;

import java.io.IOException;
import java.lang.reflect.Constructor;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;

import com.bocai.ac.Utility;

public abstract class ContentBase {

    /** Returns a Content instance of the correct type */
    public static ContentBase getInstance(final Node xml) throws TransformerException {

        String name = null;
        String type = null;
        String op = null;

        name = xml.getAttributes().getNamedItem("name").getTextContent();
        type = xml.getAttributes().getNamedItem("type").getTextContent();
        op = xml.getTextContent();

        if ((op == null) || (type == null) || (name == null)) {
            throw (new RuntimeException("Missing required XML elements in: " + Utility.xmlToString(xml)));
        }

        // Get an instance of the correct navigation class
        ContentBase contentInstance = null;
        final String className = String.format("%s.%s", ContentBase.class.getPackage().getName(), type);
        try {
            final Class<?> contentClass = Class.forName(className);
            final Constructor<?> contentConstructor = contentClass.getDeclaredConstructor(String.class, String.class, String.class, Node.class);
            contentConstructor.setAccessible(true);
            contentInstance = (ContentBase) contentConstructor.newInstance(name, type, op, xml);
        } catch (final Exception e) {
            throw (new RuntimeException("Unsupported content type found in the profile XML: " + className, e));
        }
        return (contentInstance);
    }

    private String _name = null;
    private String _type = null;
    private String _op = null;

    public String getName() {
        return this._name;
    }

    public String getType() {
        return this._type;
    }

    public String getOp() {
        return this._op;
    }

    protected ContentBase(final String name, final String type, final String op, final Node xml) {
        this._name = name;
        this._type = type;
        this._op = op;
    }

    public abstract String getContent(String pageContent) throws IOException, TransformerException;

}
