package com.hourglassapps.cpi_ii.web_search.bing.old.response;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.hourglassapps.util.Log;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Result {
	private final static String TAG=Result.class.getName();
	
	private __Metadata mMetadata;
	private String mId;
	private String mTitle;
	private String mDescription;
	private String mDisplayUrl;
	private String mUrl;
	
	private URL mUri=null;
	
	public void set__metadata(__Metadata pMetadata) {
		mMetadata=pMetadata;
	}
	
	public void setID(String pId) {
		mId=pId;
	}
	
	public void setTitle(String pTitle) {
		mTitle=pTitle;
	}
	
	public void setDescription(String pDescription) {
		mDescription=pDescription;
	}
	
	public void setDisplayUrl(String pDisplayUrl) {
		mDisplayUrl=pDisplayUrl;
	}
	
	@JsonSetter("Url")
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
