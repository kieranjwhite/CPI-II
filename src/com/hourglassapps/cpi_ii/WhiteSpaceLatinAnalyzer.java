package com.hourglassapps.cpi_ii;

import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

import com.hourglassapps.cpi_ii.latin.LatinAnalyzer;
import com.hourglassapps.util.Ii;

public final class WhiteSpaceLatinAnalyzer extends LatinAnalyzer {

	@Override
	protected Ii<Tokenizer, TokenStream> underlyingTokeniser(Reader pReader) {
		final Tokenizer source = new WhitespaceTokenizer(getVersion(), pReader);
		return new Ii<Tokenizer, TokenStream>(source, source);
	}
	
}