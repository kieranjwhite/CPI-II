package com.hourglassapps.cpi_ii.stem;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

import com.hourglassapps.cpi_ii.stem.snowball.lucene.SnowballFilter;
import com.hourglassapps.cpi_ii.stem.snowball.tartarus.SnowballProgram;
import com.hourglassapps.util.MultiMap;

public final class IdentityRecorderFilter extends StemRecorderFilter {
	public IdentityRecorderFilter(TokenStream pInput) throws IOException {
		super(pInput);
	}


	public <C extends Set<String>> IdentityRecorderFilter(TokenStream pInput,
			MultiMap<String, C, String> stem2Expansions, SnowballProgram pStemmer) throws IOException {
		super(pInput, stem2Expansions);
	}

	@Override
	protected TokenFilter stemmer(TokenFilter pInput) throws IOException {
		return new IdentityFilter(pInput);
	}
	
}