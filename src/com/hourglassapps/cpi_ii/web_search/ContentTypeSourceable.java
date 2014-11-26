package com.hourglassapps.cpi_ii.web_search;

public class ContentTypeSourceable implements Sourceable {
	private final int mFilename;
	private final String mContentType;
	
	public ContentTypeSourceable(int pFilename, String pContentType) {
		mFilename=pFilename;
		mContentType=pContentType;
	}
	
	@Override
	public int dstKey() {
		return mFilename;
	}

	public String contentType() {
		return mContentType;
	}

	@Override
	public String src() {
		return mContentType;
	}
	
	@Override
	public String toString() {
		return "src: "+mFilename; 
	}
}
