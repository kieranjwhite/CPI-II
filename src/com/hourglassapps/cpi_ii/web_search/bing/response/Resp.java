package com.hourglassapps.cpi_ii.web_search.bing.response;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class Resp {
	private D mD;
	
	public void setD(D pD) {
		mD=pD;
	}
	
	public D d() {
		return mD;
	}

	public List<URI> urls() throws URISyntaxException {
		return mD.urls();
	}
	
	public URI next() throws URISyntaxException {
		return mD.next();
	}
}