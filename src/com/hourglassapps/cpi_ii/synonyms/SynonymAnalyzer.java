package com.hourglassapps.cpi_ii.synonyms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.lucene.analysis.util.CharArraySet;

import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Thrower;

public class SynonymAnalyzer extends Analyzer implements AutoCloseable {
	private final static String TAG=SynonymAnalyzer.class.getName();
	private final ConcreteThrower<Exception> mThrower=new ConcreteThrower<>();
	
	@Override
	protected TokenStreamComponents createComponents(String fieldName,
			Reader pUnderlying) {
		Tokenizer filter=new WhitespaceTokenizer(pUnderlying);

		try {
			return new TokenStreamComponents(filter, new WordNetFilter(filter));
		} catch (IOException | ParseException e) {
			Log.e(TAG, e);
		}
		return new TokenStreamComponents(filter, filter);
	}

	public void closeThrower() throws Exception {
		mThrower.close();
	}
	
}
