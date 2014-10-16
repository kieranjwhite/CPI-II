package com.hourglassapps.cpi_ii;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;
import org.apache.lucene.util.Version;

public class NumeralFilter extends FilteringTokenFilter {
	private final static Pattern NUMBER=Pattern.compile("^[0-9]+$");
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	public NumeralFilter(TokenStream input) {
		super(input);
	}

	@Override
	protected boolean accept() throws IOException {
		Matcher matcher=NUMBER.matcher(new String(termAtt.buffer(), 0, termAtt.length()));
		return !matcher.matches();
	}

}
