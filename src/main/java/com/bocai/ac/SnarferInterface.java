package com.bocai.ac;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.apache.http.HttpException;

public interface SnarferInterface {

    public String getSiteRoot();

    public void setCurrentDataKey(String _currentDataKey);

    public String getCurrentDataKey();

    public void runHandler(String handlerName, String sourceUrl, String pageContent) throws TransformerException, IOException, InterruptedException, HttpException;

}
