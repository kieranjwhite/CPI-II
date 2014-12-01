package com.hourglassapps.cpi_ii.lucene;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.TopDocs;

public class FoundGenerator implements ResultGenerator<Boolean> {
	public boolean mFound=false;
	
	@Override
	public void run(IndexReader pReader, TopDocs pResults)
			throws IOException {
		if(pResults.scoreDocs.length>=1) {
			mFound=true;
		}
	}
	
	@Override
	public Boolean result() {
		return mFound;
	}
}