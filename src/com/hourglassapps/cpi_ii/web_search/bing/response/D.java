package com.hourglassapps.cpi_ii.web_search.bing.response;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class D implements Iterable<Result> {
	private Result[] mResults;
	private String mNext;
	
	public void setResults(Result[] pResults) {
		mResults=pResults;
	}
	
	public int size() {
		return mResults.length;
	}
	
	public Result result(int pIdx) {
		return mResults[pIdx];
	}
	
	public URI next() throws URISyntaxException {
		return new URI(mNext);
	}

	@Override
	public Iterator<Result> iterator() {
		return new Iterator<Result>(){
			private int mIdx=0;
			
			@Override
			public boolean hasNext() {
				return mIdx<size();
			}

			@Override
			public Result next() {
				if(hasNext()) {
					return result(mIdx);
				} else {
					throw new NoSuchElementException();
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}

	public List<URI> urls() throws URISyntaxException {
		List<URI> urls=new ArrayList<URI>();
		for(int i=0; i<mResults.length; i++) {
			urls.add(mResults[i].url());
		}
		return null;
	}
}