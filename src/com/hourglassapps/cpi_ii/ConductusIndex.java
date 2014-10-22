package com.hourglassapps.cpi_ii;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.hourglassapps.cpi_ii.latin.LatinAnalyzer;
import com.hourglassapps.util.Log;

public class ConductusIndex {
	private final static String TAG=ConductusIndex.class.getName();

	private final static int NGRAM_SIZE=2;

	private final File mIndexDir;
	private Directory mDir;

	private Analyzer mAnalyser;
	private LatinAnalyzer mTokeniser=new WhiteSpaceLatinAnalyzer();
	
	public ConductusIndex(File pIndexDir) throws IOException {
		mIndexDir=pIndexDir;
	}

	/* Suppressing resource warning (suggesting that LatinAnalyzer was never closed)
	 * as I suspect analyzer is closed by IndexWriter that uses it, when it is closed. */
	@SuppressWarnings({ "unused", "resource" })	
	public ConductusIndex enableStemmer(boolean stem) {
		if(mAnalyser!=null) {
			mAnalyser.close();
		}
		 Analyzer termAnalyser;
		 if(stem) {
			 mTokeniser=mTokeniser.setStemmer(LatinAnalyzer.STEMPEL_RECORDER_FACTORY);			 			 
		 } else {
			 mTokeniser=mTokeniser.setStemmer(LatinAnalyzer.IDENTITY_RECORDER_FACTORY);			 			 
		 }
		 termAnalyser=mTokeniser;
		 if(NGRAM_SIZE<2) {
			 mAnalyser=termAnalyser;				 
		 } else {
			 mAnalyser=new ShingleAnalyzerWrapper(
					 termAnalyser,
					 NGRAM_SIZE, NGRAM_SIZE, ShingleFilter.DEFAULT_TOKEN_SEPARATOR, false, true, 
					 ShingleFilter.DEFAULT_FILLER_TOKEN
					 );
		 }
		
		return this;
	}
	
	public Directory dir() throws IOException {
		if(mDir==null) {
			mDir=FSDirectory.open(mIndexDir);
		}
		return mDir;
	}

	public Analyzer analyser() {
		if(mAnalyser==null) {
			enableStemmer(false);
		}
		assert mAnalyser!=null;
		return mAnalyser;
	}

	public LatinAnalyzer tokeniser() {
		if(mTokeniser==null) {
			enableStemmer(false);
		}
		assert mTokeniser!=null;
		return mTokeniser;
	}
	
	public boolean storeStems(OutputStream pSave) throws IOException {
		return tokeniser().storeStems(pSave);
	}
	
	public Set<String> tokenExpansions(String pToken) {
		return tokeniser().tokenExpansions(pToken);
	}
	
	public boolean displayStemGroups() {
		return tokeniser().displayStemGroups();
	}
	
	public void visitTerms(TermHandler pConsumer) throws IOException {
		try(AtomicReader reader=AtomicReader.open(dir()).leaves().get(0).reader()) {
			//Terms terms=reader.terms(CONTENT.s());
			Fields fields=reader.fields();
			if(fields!=null) {
				Terms terms=fields.terms(FieldVal.CONTENT.s());
				if(terms!=null) {
					TermsEnum e=terms.iterator(null);
					pConsumer.run(e);
				}
			}
		}

	}

	public void interrogate(FieldVal pKeyField, String pKey, int pNumResults, ResultRelayer pRelayer) throws IOException {
		try(IndexReader reader=DirectoryReader.open(dir())) {
			interrogate(reader, pKeyField, pKey, pNumResults, pRelayer);
		}
	}

	public void interrogate(FieldVal pKeyField, String pKey, ResultRelayer pRelayer) throws IOException {
		try(IndexReader reader=DirectoryReader.open(dir())) {
			/*
			for (int i=0; i<reader.maxDoc(); i++) {
			    Document doc = reader.document(i);
			    System.out.println("doc: "+doc.get(pKeyField.s()));
			}
			*/
			interrogate(reader, pKeyField, pKey, reader.numDocs(), pRelayer);
		}
	}

	private void interrogate(IndexReader pReader, FieldVal pKeyField, String pKey, int pNumResults, ResultRelayer pRelayer) throws IOException {
		IndexSearcher searcher = new IndexSearcher(pReader);

		TopDocs results = searcher.search(pKeyField.query(pKey), pNumResults);
		if(results.totalHits<1) {
			Log.i(TAG, "unrecognised key "+pKey);
			return;
		}
		pRelayer.run(pReader, results);
		
	}

	public ConductusIndex setTokenizer(
			LatinAnalyzer pTokeniser) {
		mTokeniser=pTokeniser;
		return this;
	}
}
