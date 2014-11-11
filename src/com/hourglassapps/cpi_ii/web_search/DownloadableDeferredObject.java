package com.hourglassapps.cpi_ii.web_search;

import java.net.URL;
import java.nio.file.Path;

import org.jdeferred.impl.DeferredObject;

public class DownloadableDeferredObject<A,B,C> extends DeferredObject<A,B,C> {
	private final URL mSrc;
	private final Path mDst;
	
	public DownloadableDeferredObject(URL pSrc, Path pDst) {
		mSrc=pSrc;
		mDst=pDst;
	}

	@Override
	public String toString() {
		return "DeferredObject: "+mSrc.toString()+" to "+mDst.toString();
	}
}
