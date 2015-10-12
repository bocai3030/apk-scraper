package com.bocai.ac.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;

/** A RegEx content handler that concatenates the values of all matches */
public class ContentRegExMulti extends ContentRegEx {

    private List<String> _contentList = null;

    protected ContentRegExMulti(final String name, final String type, final String op, final Node xml) {
        super(name, type, op, xml);
    }

    @Override
    public String getContent(final String pageContent) throws IOException, TransformerException {
        if (this._content == null) {
            this._content = "";
            this.getContentList(pageContent); // This call populates the private member we then use
            for (final String curContent : this._contentList) {
                this._content += curContent.trim() + " ";
            }
            this._content = this._content.trim();
        }
        return (this._content);
    }

    public List<String> getContentList(final String pageContent) throws IOException, TransformerException {
        if (this._contentList == null) {
            this._contentList = new ArrayList<String>();
            final Matcher matcher = this._regExPattern.matcher(pageContent);
            String curContent = "";
            while (matcher.find()) {
                if (this._regExGroup == -1) {
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        curContent += matcher.group(i) + " ";
                    }
                } else {
                    curContent = matcher.group(this._regExGroup);
                }
                if ((curContent != null) && (curContent.trim().length() > 0)) {
                    this._contentList.add(curContent.trim());
                }
            }
        }
        return (this._contentList);
    }

}
