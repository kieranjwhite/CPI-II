package com.hourglassapps.cpi_ii.web_search.bing.api_v7.response;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.hourglassapps.util.Log;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Value {
	private final static String TAG=Value.class.getName();
	
	private String mName;
	private String mDisplayUrl;
	private String mUrl;
	
	private URL mUri=null;
	
	public void setName(String pName) {
		mName=pName;
	}
	
	public void setDisplayUrl(String pDisplayUrl) {
		mDisplayUrl=pDisplayUrl;
	}
	
	public void setUrl(String pUrl) {
		mUrl=pUrl;
	}
	
	public URL url() throws MalformedURLException {
		if(mUri==null) {
			mUri=new URL(mUrl);
		}
		return mUri;
	}
}
