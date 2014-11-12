package com.hourglassapps.cpi_ii.web_search;

import java.net.URL;
import java.util.List;

import com.hourglassapps.util.Ii;

public class HttpQuery<K> implements Query<K,URL> {

	private final K mName;
	private final URL mUrl;
	
	public HttpQuery(K pName) {
		mName=pName;
		mUrl=null;
	}
	
	public HttpQuery(K pName, URL pUrl) {
		mName=pName;
		mUrl=pUrl;
	}

	@Override
	public boolean empty() {
		return mUrl==null;
	}

	@Override
	public URL raw() {
		return mUrl;
	}
	
	@Override
	public K uniqueName() {
		return mName;
	}
}