package com.hourglassapps.cpi_ii.synonyms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.lucene.analysis.util.CharArraySet;

public class WordNetFilter extends TokenFilter {
	private final static String WORDNET_SYNONYMS="wn_s.pl";
	private final SynonymFilter mDelegate;
	
	public WordNetFilter(TokenStream pStream) throws IOException, ParseException {
		super(pStream);
		try(Reader reader=new BufferedReader(new InputStreamReader(
				SynonymAnalyzer.class.getResourceAsStream(WORDNET_SYNONYMS)))) {
			WordnetSynonymParser parser = new WordnetSynonymParser(true, true, 
					new StandardAnalyzer(CharArraySet.EMPTY_SET));
			parser.parse(reader);
			SynonymMap syns=parser.build();
			mDelegate=new SynonymFilter(pStream, syns, true);
		}
	}
	
	@Override
	public boolean incrementToken() throws IOException {
		return mDelegate.incrementToken();
	}

}
