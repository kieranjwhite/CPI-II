package com.hourglassapps.cpi_ii.web_search.bing.api_v7.response;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import com.hourglassapps.util.Log;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {
    private final static String TAG=Response.class.getName();

    public enum Type { WebResult };

    private WebPages mWebPages=new WebPages();
	
    public void setWebPages(WebPages pWebPages) {
	Log.i(TAG, "setWebPage "+pWebPages);
	mWebPages=pWebPages;
    }
	
    public WebPages webPages() {
	return mWebPages;
    }

    public long totalEstimatedMatches() {
	return mWebPages.totalEstimatedMatches();
    }
    
    public List<URL> urls() throws URISyntaxException {
	Log.i(TAG, "urls.");
	return mWebPages.urls();
    }
}
