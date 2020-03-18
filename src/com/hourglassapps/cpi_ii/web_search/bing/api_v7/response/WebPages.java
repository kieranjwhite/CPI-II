package com.hourglassapps.cpi_ii.web_search.bing.api_v7.response;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.hourglassapps.cpi_ii.web_search.bing.api_v7.BingArgs;
import com.hourglassapps.util.Log;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebPages implements Iterable<Value> {
    private final static String TAG=WebPages.class.getName();

    private long mTotalEstimatedMatches=0;
    private Value[] mValue=new Value[]{};

    public void setTotalEstimatedMatches(long pTotalEstimatedMatches) {
	mTotalEstimatedMatches=pTotalEstimatedMatches;
    }

    public long totalEstimatedMatches() {
	return mTotalEstimatedMatches;
    }
    
    public void setValue(Value[] pValue) {
	mValue=pValue;
    }
	
    public int size() {
	return mValue.length;
    }
	
    public Value value(int pIdx) {
	return mValue[pIdx];
    }
	
    @Override
    public Iterator<Value> iterator() {
	return new Iterator<Value>(){
	    private int mIdx=0;
			
	    @Override
	    public boolean hasNext() {
		return mIdx<size();
	    }

	    @Override
	    public Value next() {
		if(hasNext()) {
		    return value(mIdx);
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
	//TODO remove this url
	try {
	    urls.add(new URL("http://www.catholictradition.org/Christ/nature-grace.htm"));
	} catch(MalformedURLException ex) {}
	for(int i=0; i<mValue.length; i++) {
	    try {
		urls.add(mValue[i].url());
	    } catch(MalformedURLException e) {
		Log.i(TAG, e);
	    }
	}
	return urls;
    }
}
