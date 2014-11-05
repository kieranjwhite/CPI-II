package com.hourglassapps.cpi_ii.web_search.bing;

import java.net.URL;
import java.util.List;

import com.hourglassapps.cpi_ii.web_search.Query;
import com.hourglassapps.util.Ii;

public class HttpQuery<K> implements Query<K,URL> {

	private final K mName;
	private final URL mUri;
	
	HttpQuery(K pName) {
		mName=pName;
		mUri=null;
	}
	
	HttpQuery(K pName, Ii<URL, List<String>> pFormat) {
		mName=pName;
		mUri=pFormat.fst();
	}

	@Override
	public boolean done() {
		return mUri==null;
	}

	@Override
	public URL raw() {
		return mUri;
	}
	
	@Override
	public K uniqueName() {
		return mName;
	}
}