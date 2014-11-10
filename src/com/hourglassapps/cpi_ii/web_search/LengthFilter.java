package com.hourglassapps.cpi_ii.web_search;

import java.net.URL;

import com.hourglassapps.util.Filter;

public class LengthFilter implements Filter<URL> {
	private int mLongest;
	
	public LengthFilter(int pMinLen) {
		mLongest=pMinLen;
	}
		
	@Override
	public boolean accept(URL pQuery) {
		if(pQuery.toString().length()>mLongest) {
			mLongest=pQuery.toString().length();
			return true;
		}
		return false;
	}

}
