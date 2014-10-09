package com.hourglassapps.cpi_ii;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.hourglassapps.util.Log;

public class ConductusIndex {
	private final static String TAG=ConductusIndex.class.getName();

	private final static String ID_KEY="eprintid";
	public final static String CONTENT_KEY="content";
	
	private final static int NGRAM_SIZE=3;
	

	private File mIndexDir;
	private Directory mDir;

	private Analyzer mAnalyzer;

	public ConductusIndex(File pIndexDir) throws IOException {
		mIndexDir=pIndexDir;
	}

	public Directory dir() throws IOException {
		if(mDir==null) {
			mDir=FSDirectory.open(mIndexDir);
		}
		return mDir;
	}

	public Analyzer analyzer() {
		 if(mAnalyzer==null) {
			 mAnalyzer=new ShingleAnalyzerWrapper(new StandardAnalyzer(CharArraySet.EMPTY_SET),
						NGRAM_SIZE, NGRAM_SIZE, ShingleFilter.DEFAULT_TOKEN_SEPARATOR, false, true, 
						ShingleFilter.DEFAULT_FILLER_TOKEN
					);
			 //mAnalyzer=new StandardAnalyzer(CharArraySet.EMPTY_SET);	
		 }
		 return mAnalyzer;
	}
	
	public Field eprintIdField(long pEprintId) {
		return new TextField(ID_KEY, Long.toString(pEprintId), Field.Store.YES);
	}
	
	private Term eprintIdTerm(long pEprintId) {
		return new Term(ID_KEY, Long.toString(pEprintId));
	}
	
	public Field contentField(String pContent) {
		FieldType type = new FieldType();
		type.setIndexed(true);
		type.setStored(true);
		type.setStoreTermVectors(true);
		
		return new Field(CONTENT_KEY, pContent, type);
	}

	public TermQuery eprintIdQuery(long pEprintId) {
		return new TermQuery(eprintIdTerm(pEprintId));
	}

	public Term term(String termText) {
		return new Term(CONTENT_KEY, termText);
	}
}
