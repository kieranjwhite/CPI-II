package com.hourglassapps.cpi_ii.stem;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

import com.hourglassapps.cpi_ii.snowball.lucene.SnowballFilter;
import com.hourglassapps.cpi_ii.snowball.tartarus.SnowballProgram;
import com.hourglassapps.util.MultiMap;

public final class SnowballRecorderFilter extends StemRecorderFilter {
	private SnowballProgram mStemmer;
	public SnowballRecorderFilter(TokenStream pInput, SnowballProgram pStemmer) throws IOException {
		super(pInput);
		mStemmer=pStemmer;
	}


	public <C extends Set<String>> SnowballRecorderFilter(TokenStream pInput,
			MultiMap<String, C, String> stem2Expansions, SnowballProgram pStemmer) throws IOException {
		super(pInput, stem2Expansions);
		mStemmer=pStemmer;
	}

	@Override
	protected TokenFilter stemmer(TokenFilter pInput) throws IOException {
		return new SnowballFilter(pInput, mStemmer);
	}

}