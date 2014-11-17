package com.hourglassapps.cpi_ii.web_search;

public class ContentTypeSourceable implements Sourceable {
	private final long mFilename;
	private final String mContentType;
	
	public ContentTypeSourceable(long pFilename, String pContentType) {
		mFilename=pFilename;
		mContentType=pContentType;
	}
	
	@Override
	public long dstKey() {
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
