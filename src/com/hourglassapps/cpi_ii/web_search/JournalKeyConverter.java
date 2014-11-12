package com.hourglassapps.cpi_ii.web_search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Log;

public class JournalKeyConverter implements Converter<String, String> {
	private final String TAG=JournalKeyConverter.class.getName();
	
	public final static Converter<String,String> SINGLETON=new JournalKeyConverter();
	
	private final static String PATH_ENCODING=StandardCharsets.UTF_8.toString();

	@Override
	public String convert(String pIn) {
		//Creates a filename from pIn, a query's first disjunction
		try {
			return URLEncoder.encode(pIn, PATH_ENCODING);
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, e);
			System.exit(-1);
			return null;
		}
	}
}
