package com.hourglassapps.cpi_ii.stem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.stempel.StempelFilter;
import org.apache.lucene.analysis.stempel.StempelStemmer;

import com.hourglassapps.util.MultiMap;

public final class StempelRecorderFilter extends StemRecorderFilter {
	public StempelRecorderFilter(TokenStream pInput) throws IOException {
		super(pInput);
	}


	public <C extends Set<String>> StempelRecorderFilter(TokenStream pInput,
			MultiMap<String, C, String> stem2Expansions) throws IOException {
		super(pInput, stem2Expansions);
	}

	@Override
	protected TokenFilter stemmer(TokenFilter pInput) throws IOException {
		try(InputStream in=new FileInputStream(new File("/tmp/stempel/model.out"))) {
			return new StempelFilter(pInput, new StempelStemmer(in));
		}
	}

}