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
	private File mModel;
	
	public StempelRecorderFilter(TokenStream pInput, boolean pRecord, File pModel) throws IOException {
		super(pInput, pRecord);
		mModel=pModel;
	}


	public <C extends Set<String>> StempelRecorderFilter(TokenStream pInput, boolean pRecord,
			MultiMap<String, C, String> stem2Expansions, File pModel) throws IOException {
		super(pInput, pRecord, stem2Expansions);
		mModel=pModel;
	}

	@Override
	protected TokenFilter stemmer(TokenFilter pInput) throws IOException {
		try(InputStream in=new FileInputStream(mModel)) {
			return new StempelFilter(pInput, new StempelStemmer(in));
		}
	}

}