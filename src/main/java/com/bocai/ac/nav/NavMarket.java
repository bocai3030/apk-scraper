package com.bocai.ac.nav;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import javax.xml.transform.TransformerException;

import org.apache.http.HttpException;
import org.w3c.dom.Node;

import com.bocai.ac.SnarferInterface;

public class NavMarket extends NavRegEx {
    protected NavMarket(final String name, final String handler, final String target, final String targetType, final Node xml) {
        super(name, handler, target, targetType, xml);
    }

    @Override
    public void runNav(final SnarferInterface parent, final String pageContent) throws IOException, TransformerException, InterruptedException, HttpException {

        // Build our URL list
        final boolean urlIsRelative = "relative".equalsIgnoreCase(this.getNavType());
        final Map<String, String> urlToKey = new HashMap<String, String>();
        final Matcher matcher = this._regExPattern.matcher(pageContent);
        while (matcher.find()) {

            // The Android Market PC web site seems to cap the category requests at 800 items.
            // In other words, we can only scrape the top 800 apps for each category.
            // URL Example:
            // https://market.android.com/details?id=apps_topselling_free&cat=BOOKS_AND_REFERENCE&start=776&num=24

            // 34 * 24 = 816 (see above)
            final String category = matcher.group(this._regExGroup);
            for (int start = 0; start < 34; start++) {
                final String url = String.format("/details?id=apps_topselling_free&cat=%s&start=%d&num=24", category, (start * 24));
                if (urlIsRelative) {
                    urlToKey.put(parent.getSiteRoot() + url, null);
                } else {
                    urlToKey.put(url, null);
                }
            }
        }

        // Navigate to the URL(s)
        super.runNav(parent, urlToKey, "get");
    }

}
