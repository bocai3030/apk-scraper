package com.bocai.ac;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.bocai.ac.content.ContentGroup;
import com.bocai.ac.image.ImageServiceConnector;
import com.bocai.ac.nav.NavBase;

public class Main implements SnarferInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private final HashMap<String, Node> handlerMap = new HashMap<String, Node>();
    private String _siteRoot = null;
    private String _currentDataKey = null;

    private Long _scraperPauseTime = null;
    private int _scraperDelayMaxTime;
    private int _scraperDelayMinTime;

    public static void main(final String[] args) {
        // Ensure we have the needed command-line argument
        final String usageText = "USAGE:\r\n\t-package {package_name}\t(scrape data for the given package only)"
                + "\r\n\t-cat\t\t\t(scrape data based on the categories found on the Android Market home page)";
        if (args.length <= 0) {
            System.out.println(usageText);
            System.exit(-1);
        }
        if (("-package".equalsIgnoreCase(args[0])) && (args.length <= 1)) {
            System.out.println(usageText);
            System.exit(-1);
        }
        if ((!"-package".equalsIgnoreCase(args[0])) && (!"-cat".equalsIgnoreCase(args[0]))) {
            System.out.println(usageText);
            System.exit(-1);
        }

        System.exit((new Main()).run(args));
    }

    /** Returns a process exit code suitable for POSIX style systems */
    private int run(final String[] args) {
        int exitCode = 0;

        try {
            this._scraperDelayMinTime = Integer.parseInt(ConfigHelper.getConfigValue("scraping_deley_min_time"));
            this._scraperDelayMaxTime = Integer.parseInt(ConfigHelper.getConfigValue("scraping_deley_max_time"));
            this._scraperPauseTime = Long.parseLong(ConfigHelper.getConfigValue("scraping_pause_time"));
            System.out.println(String.format("DelayTime: max %d, min %d, PauseTime: %d", this._scraperDelayMaxTime, this._scraperDelayMinTime, this._scraperPauseTime));
        } catch (final NumberFormatException nfe) {
            System.out.println("NumberFormatException: " + nfe.getMessage());
            return (-1);
        }

        final String nThreadsStr = ConfigHelper.getConfigValue("image.processing.thread.number");
        final int nThreads = Integer.parseInt(nThreadsStr);
        ImageServiceConnector.initialize();
        // ImageServiceConnector.start(Executors.newFixedThreadPool(nThreads));
        Main.LOGGER.info(String.format("started images processor with %s threads", nThreads));
        try {
            // Initialize our handlers, database, etc.
            this.init();

            // Run the health check
            // this.runHealthCheck(transactionId); // disable this for google play has put the com.getjar.test.webview app off the shelve.

            if ("-cat".equalsIgnoreCase(args[0])) {

                // Start a full scraping run beginning at the home page
                exitCode = this.startScrapingCategories();
            } else if ("-package".equalsIgnoreCase(args[0])) {

                // Just scrape data for the given package name
                final String packageName = args[1];
                exitCode = this.startScrapingPackage(packageName, null, new int[1]);
            }
        } catch (final Exception e) {
            Main.LOGGER.error(null, e);
            exitCode = -1;
        }
        return (exitCode);
    }

    /**
     * Scrapes data from Android Marketplace starting from the home page and navigating the categories
     *
     * @throws HttpException
     */
    private int startScrapingCategories() throws TransformerException, IOException, InterruptedException, HttpException {

        // Run the "main" handler (scraping based on categories)
        if (!this.handlerMap.containsKey("main")) {
            Main.LOGGER.error("No 'main' handler found. Please ensure that your profile XML defines a handler called 'main'");
            return (-1);
        }
        this.runHandler("main", null, null);
        return (0);
    }

    /**
     * Scrapes data from Android Marketplace for the given package name
     *
     * @throws HttpException
     */
    public int startScrapingPackage(final String packageName, final Map<String, String> versionInfo, final int[] statusCode) throws TransformerException, IOException, InterruptedException,
    HttpException {

        // Check our state
        if ((packageName == null) || (packageName.length() <= 0)) {
            Main.LOGGER.error("Can not call startScrapingPackage() with no packageName");
            return (-1);
        }
        if (!this.handlerMap.containsKey("productPage")) {
            Main.LOGGER.error("No 'productPage' handler found. Please ensure that your profile XML defines a handler called 'productPage'");
            return (-1);
        }

        final long start = System.currentTimeMillis();
        boolean retry = false;

        do {
            // Scrape data for a specific package name
            final String url = String.format("https://play.google.com/store/apps/details?id=%s&hl=en", packageName);
            final String pageContent = NavBase.runUrlNav(null, url, "GET", null, statusCode);
            if ((statusCode[0] != HttpStatus.SC_OK) || (pageContent == null) || (pageContent.length() <= 0)) {

                // A 404 most likely means that Android Market does not have the given package (don't count that as a failure)
                if (statusCode[0] != 404) {
                    if (statusCode[0] == HttpStatus.SC_SERVICE_UNAVAILABLE) {
                        // We got a 503 responses with CAPTCHA, pause for a while and retry later.
                        retry = true;
                        Main.LOGGER.warn(String.format("503 Response received for package '%s'. Pause for %s seconds and retry.", packageName, this._scraperPauseTime));
                        try {
                            Thread.sleep(this._scraperPauseTime);
                        } catch (final InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    } else {
                        Main.LOGGER.warn(String.format("Failed to load data for package '%s'", packageName));
                        return (-1);
                    }
                } else {
                    retry = false;
                }

            } else {
                retry = false;
                this.runHandler("productPage", url, pageContent, versionInfo);
            }

            final long end = System.currentTimeMillis();
            final long scrapeTime = end - start;
            System.out.println(packageName + " scrape time: " + scrapeTime);

            // Generate a random delay between the range
            final Random r = new Random();
            final int scraperDelay = r.nextInt(this._scraperDelayMaxTime - this._scraperDelayMinTime + 1) + this._scraperDelayMinTime;

            // Slow down the faster requests (<_scraperDelayTime)
            if (scrapeTime < scraperDelay) {
                System.out.println(packageName + " Delay: " + (scraperDelay - scrapeTime));
                try {
                    Thread.sleep(scraperDelay - scrapeTime);
                } catch (final InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        } while (retry);

        return (0);
    }

    public void init() throws SQLException, SAXException, IOException, ParserConfigurationException {
        final String profilePath = ConfigHelper.getConfigValue("snarf.profile.path");
        final InputStream profileFileInputStream = this.getClass().getClassLoader().getResourceAsStream(profilePath);

        // Parse the profile XML
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document dom = builder.parse(profileFileInputStream);

        // Hash the handlers into memory
        final Element root = dom.getDocumentElement();
        this._siteRoot = root.getAttribute("siteRoot");
        final NodeList handlerNodes = root.getChildNodes();
        for (int i = 0; i < handlerNodes.getLength(); i++) {
            final Node curNode = handlerNodes.item(i);
            if (!"handler".equalsIgnoreCase(curNode.getNodeName())) {
                continue;
            }

            // This is a handler node
            final String handlerName = curNode.getAttributes().getNamedItem("name").getTextContent();
            Main.LOGGER.info(String.format("Adding handler '%s'", handlerName));
            this.handlerMap.put(handlerName, curNode);
        }
    }

    @Override
    public String getSiteRoot() {
        return (this._siteRoot);
    }

    @Override
    public void setCurrentDataKey(final String _currentDataKey) {
        this._currentDataKey = _currentDataKey;
    }

    @Override
    public String getCurrentDataKey() {
        return this._currentDataKey;
    }

    @Override
    public void runHandler(final String handlerName, final String sourceUrl, final String pageContent) throws TransformerException, IOException, InterruptedException, HttpException {
        this.runHandler(handlerName, sourceUrl, pageContent, null);
    }

    public void runHandler(final String handlerName, final String sourceUrl, final String pageContent, final Map<String, String> versionInfo) throws TransformerException, IOException,
            InterruptedException, HttpException {
        Main.LOGGER.info(String.format("Handler '%s' started...", handlerName));
        final Node xml = this.handlerMap.get(handlerName);
        final NodeList children = xml.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node curNode = children.item(i);
            if ("contentGroup".equalsIgnoreCase(curNode.getNodeName())) {
                new ContentGroup(this, curNode, sourceUrl, pageContent, versionInfo);
            } else if ("nav".equalsIgnoreCase(curNode.getNodeName())) {
                this.handleNav(curNode, pageContent);
            }
        }
    }

    private void handleNav(final Node xml, final String pageContent) throws TransformerException, IOException, InterruptedException, HttpException {
        // Handle 'nav' nodes
        final NavBase nav = NavBase.getInstance(xml);
        Main.LOGGER.info(String.format("Handling nav '%s'", nav.getName()));
        nav.runNav(this, pageContent);
    }
}
