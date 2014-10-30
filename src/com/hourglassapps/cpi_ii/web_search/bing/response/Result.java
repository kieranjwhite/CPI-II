package com.hourglassapps.cpi_ii.web_search.bing.response;

import java.net.URI;
import java.net.URISyntaxException;

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
	
	private URI mUri=null;
	
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
	
	public URI url() throws URISyntaxException {
		if(mUri==null) {
			try {
			mUri=new URI(mUrl);
			} catch(Exception e) {
				Log.e(TAG, e);
			}
		}
		return mUri;
	}
}