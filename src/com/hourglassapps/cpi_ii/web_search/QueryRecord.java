package com.hourglassapps.cpi_ii.web_search;

import java.nio.file.Path;

public class QueryRecord<K> {
	private final int mTid;
	private final K mKey;
	private final Path mDirToIndex;
	
	public QueryRecord(int pTid, K pKey, Path pDirToIndex) {
		mTid=pTid;
		mKey=pKey;
		mDirToIndex=pDirToIndex;
	}
	
	public int tid() {
		return mTid;
	}
	
	public K key() {
		return mKey;
	}
	
	public Path dir() {
		return mDirToIndex;
	}
}