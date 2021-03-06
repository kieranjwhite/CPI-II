package com.hourglassapps.cpi_ii.web_search.bing.old.response;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.hourglassapps.cpi_ii.web_search.bing.old.BingArgs;
import com.hourglassapps.util.Log;

public class D implements Iterable<Result> {
	private final static String TAG=D.class.getName();
	
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
	
	public void set__next(String pNext) {
		mNext=pNext;
	}
	
	public URL next() throws MalformedURLException {
		if(mNext==null) {
			return null;
		}
		return new URL(mNext+BingArgs.JSON_SPECIFIER);
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

	public List<URL> urls() {
		List<URL> urls=new ArrayList<URL>();
		for(int i=0; i<mResults.length; i++) {
			try {
				urls.add(mResults[i].url());
			} catch(MalformedURLException e) {
				Log.i(TAG, e);
			}
		}
		return urls;
	}
}
