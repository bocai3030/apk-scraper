package com.bocai.ac.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bocai.ac.SnarferInterface;
import com.bocai.ac.Utility;
import com.google.common.collect.Maps;

public class ContentGroup {
    private static final Logger DATA_LOGGER = LoggerFactory.getLogger("DATA_LOGGER");

    private final Map<String, String> _namedValues = new HashMap<String, String>();
    private final List<ContentRegExMultiImage> _images = new ArrayList<ContentRegExMultiImage>();

    public ContentGroup(final SnarferInterface parent, final Node xml, final String sourceUrl, final String pageContent) throws TransformerException, IOException {
        this(parent, xml, sourceUrl, pageContent, null);
    }

    public ContentGroup(final SnarferInterface parent, final Node xml, final String sourceUrl, final String pageContent, final Map<String, String> versionInfo) throws TransformerException,
    IOException {

        // Parse the package name from the source URL (version will come from
        // page content)
        final String packageName = Utility.getAndroidPackageNameFromURL(sourceUrl);
        String versionName = "";

        // Parse the content from the page
        final NodeList children = xml.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node curNode = children.item(i);
            if ("content".equalsIgnoreCase(curNode.getNodeName())) {
                final ContentBase content = ContentBase.getInstance(curNode);
                if (content instanceof ContentRegExMultiImage) {
                    this._images.add((ContentRegExMultiImage) content);
                } else {
                    String contentText = content.getContent(pageContent);

                    if (content.getName().compareTo("price") == 0) {
                        if (contentText.compareTo("0") == 0) {
                            contentText = "Free";
                        }
                    }

                    this._namedValues.put(content.getName(), contentText);

                    if ("versionName".equalsIgnoreCase(content.getName())) {
                        versionName = contentText;
                    }
                }
            }
        }

        // Log results of our parsing for the group and prep for persistence
        final StringBuilder logMsg = new StringBuilder(String.format("Parsed content for package:%s version:%s\r\n", packageName, versionName));
        for (final String name : this._namedValues.keySet()) {
            logMsg.append(String.format("\t\t%s: %s\r\n", name, this._namedValues.get(name)));
        }
        ContentGroup.DATA_LOGGER.info(logMsg.toString());

        // The whole "versionInfo" thing is an Android Market scraping specific
        // optimization *sigh*
        // Pick the best value for versionCode, if available
        String versionCode = null;
        if (versionInfo != null) {
            versionCode = Utility.pickVersionCode(versionName, versionInfo);
            versionInfo.put("versionCodeSelected", versionCode);
        }

        this.insertOrUpdateDb(packageName, Maps.<String, String> newHashMap()); // figure out otherFields

        this.handleImages(parent, pageContent, packageName, versionName);
    }

    private void insertOrUpdateDb(final String pkgName, final Map<String, String> otherFields) {
        // TODO here to implement the logic to insert or update db records about this package meta info
    }

    private boolean handleImages(final SnarferInterface parent, final String pageContent, final String packageName, final String versionName) {
        // TODO here to implement the logic of handling images store and db records saving. the icon, preview images, and etc.
        return true;
    }

}
