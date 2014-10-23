package com.hourglassapps.cpi_ii.latin;

import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import com.hourglassapps.cpi_ii.NumeralFilter;
import com.hourglassapps.util.Ii;

public final class StandardLatinAnalyzer extends LatinAnalyzer {

	@Override
	protected Ii<Tokenizer, TokenStream> underlyingTokeniser(Reader pReader) {
		final Tokenizer source = new StandardTokenizer(getVersion(), pReader);
		TokenStream result = new StandardFilter(getVersion(), source);
		result=new NumeralFilter(result);
		result = new LatinLowerCaseFilter(result);
		result = new StopFilter(getVersion(), result, stopwords);
		return new Ii<Tokenizer, TokenStream>(source, result);
	}
	
}