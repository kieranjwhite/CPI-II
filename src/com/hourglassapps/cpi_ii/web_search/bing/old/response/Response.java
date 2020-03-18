package com.hourglassapps.cpi_ii.web_search.bing.response;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class Response {
	public enum Type { WebResult };

	private D mD;
	
	public void setD(D pD) {
		mD=pD;
	}
	
	public D d() {
		return mD;
	}

	public List<URL> urls() throws URISyntaxException {
		return mD.urls();
	}
	
	public URL next() throws MalformedURLException {
		return mD.next();
	}
}