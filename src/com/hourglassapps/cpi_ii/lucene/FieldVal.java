package com.hourglassapps.cpi_ii.lucene;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

public class FieldVal {
	private final String mName;
	private final boolean mTokenise;
	
	public FieldVal(String pName, boolean pTokenise) {
		mName=pName;
		mTokenise=pTokenise;
	}
	
	public boolean tokenised() {
		return mTokenise;
	}

	public String s() {
		return mName;
	}
	
	public String toString() {
		return s();
	}

	public Field field(String pVal) {
		FieldType type = new FieldType();
	    type.setIndexed(true);
	    type.setStored(true);
	    
	    type.setTokenized(tokenised());
		type.setStoreTermVectors(tokenised());
	    
	    type.freeze();
		return new Field(s(), pVal, type);
	}
	
	public TermQuery query(String pTerm) {
		return new TermQuery(term(pTerm));
	}

	public Term term(String termText) {
		return new Term(s(), termText);
	}

}