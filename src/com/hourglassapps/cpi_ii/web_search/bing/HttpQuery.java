package com.hourglassapps.cpi_ii.web_search.bing;

import java.net.URI;
import java.util.List;

import com.hourglassapps.cpi_ii.web_search.Query;
import com.hourglassapps.util.Ii;

public class HttpQuery<K> implements Query<K,URI> {

	private final K mName;
	private final URI mUri;
	
	HttpQuery(K pName) {
		mName=pName;
		mUri=null;
	}
	
	HttpQuery(K pName, Ii<URI, List<String>> pFormat) {
		mName=pName;
		mUri=pFormat.fst();
	}

	@Override
	public boolean done() {
		return mUri==null;
	}

	@Override
	public URI raw() {
		return mUri;
	}
	
	@Override
	public K uniqueName() {
		return mName;
	}
}