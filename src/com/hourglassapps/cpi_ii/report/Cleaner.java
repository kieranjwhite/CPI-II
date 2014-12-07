package com.hourglassapps.cpi_ii.report;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hourglassapps.util.Converter;

final class Cleaner implements Converter<String,String> {
	private final Pattern mNonLetters=Pattern.compile("[^a-z]+");
	private final static String JOINT=" ";
	
	@Override
	public String convert(String pIn) {
		Matcher m=mNonLetters.matcher(pIn);
		String cleaned=m.replaceAll(JOINT);
		return cleaned.trim();
	}
	
}