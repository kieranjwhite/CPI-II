package com.hourglassapps.cpi_ii.lucene;

import java.io.Reader;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

public class FieldVal {
	private final String mName;
	private final boolean mTokenise;
	private final FieldType mType;
	private final boolean mStore;
	
	public FieldVal(String pName, boolean pTokenise) {
		this(pName, pTokenise, true);
	}
	
	public FieldVal(String pName, boolean pTokenise, boolean pStore) {
		mName=pName;
		mTokenise=pTokenise;
		mStore=pStore;
		mType=type(pTokenise, pStore);
	}
	
	public boolean tokenised() {
		return mTokenise;
	}

	public boolean stored() {
		return mStore;
	}
	
	public String s() {
		return mName;
	}
	
	public String toString() {
		return s();
	}

	private static FieldType type(boolean pTokenise, boolean pStored) {
		FieldType type = new FieldType();
	    type.setIndexed(true);
	    type.setStored(pStored);
	    
	    boolean tokenise=pTokenise;
	    type.setOmitNorms(!tokenise);
	    type.setTokenized(tokenise);
		type.setStoreTermVectors(tokenise);
		type.setIndexOptions(tokenise?IndexOptions.DOCS_AND_FREQS_AND_POSITIONS:IndexOptions.DOCS_ONLY);
	    
	    type.freeze();
	    return type;
	}
	
	public Field field(String pVal) {
		return new Field(s(), pVal, mType);
	}
	
	public Field field(Reader pVal) {
		return new Field(s(), pVal, mType);
	}
	
	public TermQuery query(String pTerm) {
		return new TermQuery(term(pTerm));
	}

	public Term term(String termText) {
		return new Term(s(), termText);
	}

}