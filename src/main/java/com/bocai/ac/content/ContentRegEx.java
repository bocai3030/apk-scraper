package com.bocai.ac.content;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;

/** A RegEx content handler that returns only the value of the first match */
public class ContentRegEx extends ContentBase {

    protected int _regExGroup = 0;
    protected Pattern _regExPattern = null;
    protected String _content = null;
    protected boolean contentICareAbout = false;

    protected ContentRegEx(final String name, final String type, final String op, final Node xml) {
        super(name, type, op, xml);

        if (name.equalsIgnoreCase("fiveStars")) {
            System.out.println("Found the description");
            this.contentICareAbout = true;
        } else if (name.equalsIgnoreCase("rating2")) {
            System.out.println("Found Rating 2");
            this.contentICareAbout = true;
        }

        this._regExPattern = Pattern.compile(op, Pattern.DOTALL | Pattern.MULTILINE);
        final String groupText = xml.getAttributes().getNamedItem("group").getTextContent();
        if (groupText.equalsIgnoreCase("all")) {
            this._regExGroup = -1;
        } else {
            this._regExGroup = Integer.parseInt(groupText);
        }
    }

    @Override
    public String getContent(final String pageContent) throws IOException, TransformerException {
        if (this.contentICareAbout == true) {
            System.out.println("Content i care about");
        }

        if (this._content != null) {
            return (this._content);
        }
        final Matcher matcher = this._regExPattern.matcher(pageContent);
        while (matcher.find()) {
            if (this._regExGroup == -1) {
                this._content = "";
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    this._content += matcher.group(i) + " ";
                }
            } else {
                this._content = matcher.group(this._regExGroup);
            }
            if ((this._content != null) && (this._content.trim().length() > 0)) {
                this._content = this._content.trim();
                break;
            }
        }
        return (this._content);
    }

}
