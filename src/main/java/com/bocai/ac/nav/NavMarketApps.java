package com.bocai.ac.nav;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import javax.xml.transform.TransformerException;

import org.apache.http.HttpException;
import org.w3c.dom.Node;

import com.bocai.ac.ConfigHelper;
import com.bocai.ac.SnarferInterface;
import com.bocai.ac.Utility;

public class NavMarketApps extends NavRegEx {

    private boolean _suppressVersionUpdating = false;

    protected NavMarketApps(final String name, final String handler, final String target, final String targetType, final Node xml) {
        super(name, handler, target, targetType, xml);
        this._suppressVersionUpdating = Boolean.parseBoolean(ConfigHelper.getConfigValue("snarf.profile.suppress_version_updating"));
    }

    @Override
    public void runNav(final SnarferInterface parent, final String pageContent) throws IOException, TransformerException, InterruptedException, HttpException {

        // Build our URL list
        final boolean urlIsRelative = "relative".equalsIgnoreCase(this.getNavType());
        final Map<String, String> urlToKey = new HashMap<String, String>();
        final Matcher matcher = this._regExPattern.matcher(pageContent);
        while (matcher.find()) {
            String url = matcher.group(this._regExGroup);
            if (urlIsRelative) {
                url = parent.getSiteRoot() + url;
            }
            if (this._suppressVersionUpdating) {

                // TODO: Version updating is turned off, so get the package name from the URL and use that to skip packages already in the DB
                final String packageName = Utility.getAndroidPackageNameFromURL(url);
                System.out.println("********* PACKAGE NAME:  " + packageName);

            } else {
                urlToKey.put(url, null);
            }
        }

        // Navigate to the URL(s)
        super.runNav(parent, urlToKey, "get");

    }

}
