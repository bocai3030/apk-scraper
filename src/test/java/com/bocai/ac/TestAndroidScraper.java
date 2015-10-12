package com.bocai.ac;

import org.junit.Test;

/** A JUnit test class for testing the Android Market PC web site scraper */
public class TestAndroidScraper {
    @Test
    public void testSinglePackageScraping() throws Exception {
        try {
            final Main main = new Main();
            main.init();

            final int exitCode = main.startScrapingPackage("com.gto.zero.zboost", null, new int[1]);
            if (exitCode != 0) {
                throw (new RuntimeException(String.format("Test resulted in an exit code of %d", exitCode)));
            }
        } catch (final Exception e) {
            e.printStackTrace();
            throw (e);
        }
    }

}
