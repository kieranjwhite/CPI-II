package com.hourglassapps.cpi_ii;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.shingle.ShingleFilter;

import com.hourglassapps.cpi_ii.latin.LatinAnalyzer;
import com.hourglassapps.cpi_ii.lucene.LoggingAnalyzer;

public class NGramAnalyzerBuilder {
	private final static String TAG=NGramAnalyzerBuilder.class.getName();

	private final LatinAnalyzer mUnigramAnalyser;
	private final int mN;
	
	public NGramAnalyzerBuilder(LatinAnalyzer pTermAnalyser, int pN) {
		mUnigramAnalyser=pTermAnalyser;
		mN=pN;
	}

	@SuppressWarnings({ "unused", "resource" })	
	public Analyzer build() {
		LatinAnalyzer unigramAnalyser;
		Analyzer nGramAnalyser;
		assert mN>0;
		if(mN<2) {
			nGramAnalyser=mUnigramAnalyser;				 
		} else {
			nGramAnalyser=new LoggingAnalyzer(new ShingleAnalyzerWrapper(
					mUnigramAnalyser,
					mN, mN, ShingleFilter.DEFAULT_TOKEN_SEPARATOR, false, false, 
					ShingleFilter.DEFAULT_FILLER_TOKEN
					));
		}		
		return nGramAnalyser;
	}
	
}
