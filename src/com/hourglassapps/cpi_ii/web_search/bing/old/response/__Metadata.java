package com.hourglassapps.cpi_ii.web_search.bing.old.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hourglassapps.cpi_ii.web_search.bing.old.response.Response.Type;

public class __Metadata {
	private String mUri;
	private Type mType;
	
	public void setUri(String pUri) {
		mUri=pUri;
	}
	
	public void setType(Type pType) {
		mType=pType;
	}
}
