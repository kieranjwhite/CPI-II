package com.hourglassapps.cpi_ii.stem;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

import com.hourglassapps.cpi_ii.stem.snowball.lucene.SnowballFilter;
import com.hourglassapps.cpi_ii.stem.snowball.tartarus.SnowballProgram;
import com.hourglassapps.util.MultiMap;

public final class SnowballRecorderFilter extends StemRecorderFilter {
	private SnowballProgram mStemmer;
	public SnowballRecorderFilter(TokenStream pInput, boolean pRecord, SnowballProgram pStemmer) throws IOException {
		super(pInput, pRecord);
		mStemmer=pStemmer;
	}


	public <C extends Set<String>> SnowballRecorderFilter(TokenStream pInput, boolean pRecord,
			MultiMap<String, C, String> stem2Expansions, SnowballProgram pStemmer) throws IOException {
		super(pInput, pRecord, stem2Expansions);
		mStemmer=pStemmer;
	}

	@Override
	protected TokenFilter stemmer(TokenFilter pInput) throws IOException {
		return new SnowballFilter(pInput, mStemmer);
	}

}