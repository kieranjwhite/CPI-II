package com.hourglassapps.cpi_ii.latin;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import com.hourglassapps.cpi_ii.NumeralFilter;
import com.hourglassapps.cpi_ii.stem.StemRecorderFilter;
import com.hourglassapps.cpi_ii.stem.StemRecorderFilter.Factory;
import com.hourglassapps.cpi_ii.stem.StempelRecorderFilter;
import com.hourglassapps.util.Ii;

public final class StandardLatinAnalyzer extends LatinAnalyzer {
	public StandardLatinAnalyzer() {
		super();
	}
	
	public StandardLatinAnalyzer(String pPackageFile) throws IOException {
		super(pPackageFile);
	}

	@Override
	protected Ii<Tokenizer, TokenStream> underlyingTokeniser(Reader pReader) {
		final Tokenizer source = new StandardTokenizer(getVersion(), pReader);
		//TokenStream result = new StandardFilter(getVersion(), source);
		TokenStream result=new NumeralFilter(source);
		result = new LatinLowerCaseFilter(result);
		result = new StopFilter(getVersion(), result, stopwords);
		return new Ii<Tokenizer, TokenStream>(source, result);
	}

	public static Analyzer searchAnalyzer() throws IOException {
		@SuppressWarnings("resource")
		Analyzer analyser=new StandardLatinAnalyzer(LatinAnalyzer.PERSEUS_STOPWORD_FILE).
		setStemmer(new StemRecorderFilter.Factory() {
	
			@Override
			public StemRecorderFilter inst(TokenStream pInput) throws IOException {
				return new StempelRecorderFilter(pInput, false, new File("data/com/hourglassapps/cpi_ii/latin/stem/stempel/model.out"));
			}
	
		});
		return analyser;
	}
	
}