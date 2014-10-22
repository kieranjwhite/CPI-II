package com.hourglassapps.cpi_ii;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

public class FieldVal {
	private final String mName;
	private final boolean mAnalyse;
	
	public final static FieldVal KEY=new FieldVal("eprintid", true);
	public final static FieldVal CONTENT=new FieldVal("content", true);
	
	FieldVal(String pName, boolean pAnalyse) {
		mName=pName;
		mAnalyse=pAnalyse;
	}
	
	public boolean analyse() {
		return mAnalyse;
	}

	public String s() {
		return mName;
	}
	
	public String toString() {
		return s();
	}

	public Field field(String pKey) {
		FieldType type = new FieldType();
	    type.setIndexed(true);
	    type.setStored(true);
	    
	    type.setTokenized(analyse());
		type.setStoreTermVectors(analyse());
	    
	    type.freeze();
		return new Field(s(), pKey, type);
	}
	
	public TermQuery query(String pTerm) {
		return new TermQuery(term(pTerm));
	}

	public Term term(String termText) {
		return new Term(s(), termText);
	}
	
}