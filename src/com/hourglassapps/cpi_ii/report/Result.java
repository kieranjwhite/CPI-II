package com.hourglassapps.cpi_ii.report;

import java.nio.file.Path;
import java.util.Collections;
import java.util.SortedSet;

import com.hourglassapps.cpi_ii.lucene.DocSpan;

public class Result {
	private final String mTitle;
	private final Path mPath;
	private final SortedSet<DocSpan> mMatches;
	
	public Result(String pTitle, Path pPath, SortedSet<DocSpan> pMatches) {
		mTitle=pTitle;
		mPath=pPath;
		mMatches=Collections.unmodifiableSortedSet(pMatches);
	}
	
	public String title() {
		return mTitle;
	}
	
	public Path path() {
		return mPath;
	}
	
	public SortedSet<DocSpan> matches() {
		return mMatches;
	}
	
}
