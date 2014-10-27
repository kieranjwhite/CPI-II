package com.hourglassapps.cpi_ii.web_search.bing.response;

import java.net.URI;
import java.net.URISyntaxException;

public class Result {
	private __Metadata mMetadata;
	private String mId;
	private String mTitle;
	private String mDescription;
	private String mDisplayUrl;
	private String mUrl;
	
	private URI mUri=null;
	
	public void set__Metadata(__Metadata pMetadata) {
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
	
	public void setUrl(String pUrl) {
		mUrl=pUrl;
	}
	
	public URI url() throws URISyntaxException {
		if(mUri==null) {
			mUri=new URI(mUrl);
		}
		return mUri;
	}
}