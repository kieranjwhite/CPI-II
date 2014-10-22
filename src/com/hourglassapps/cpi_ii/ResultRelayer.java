package com.hourglassapps.cpi_ii;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.TopDocs;

public interface ResultRelayer {
	public void run(IndexReader pReader, TopDocs pResults) throws IOException;
}