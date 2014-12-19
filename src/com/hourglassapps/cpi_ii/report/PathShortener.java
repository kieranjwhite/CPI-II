package com.hourglassapps.cpi_ii.report;

import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.URLUtils;

public class PathShortener implements Converter<String,String> {
	private final static int MAX_LEN=196;
	private Map<String,String> mLongNameToShorter=new HashMap<>();
	private int mCurSuffix=0;
	private final ConcreteThrower<? super UnsupportedEncodingException> mThrower;
	
	public PathShortener(ConcreteThrower<? super UnsupportedEncodingException> pThrower) {
		mThrower=pThrower;
	}
	
	@Override
	public String convert(String cleaned) {
		try {
			String encoded=URLUtils.encode(cleaned);
			assert encoded.indexOf('_')==-1;
			if(encoded.length()>MAX_LEN) {
				/*
				 * Assumes convert is invoked in a fixed order. If that order changes
				 * (eg if the the conductus xml export is modified) a new report will 
				 * have to be generated from scratch (ie by deleting the directory poems/results).
				 */
				if(mLongNameToShorter.containsKey(cleaned)) {
					encoded=mLongNameToShorter.get(cleaned);
				} else {
					encoded=(encoded.substring(0,MAX_LEN)+'_')+(mCurSuffix++);
					mLongNameToShorter.put(cleaned, encoded);
				}
			}
			Paths.get(encoded); //This will trigger an InvalidPathException if encoded is still too long -- if that happens reduce MAX_LEN
			return encoded;
		} catch (UnsupportedEncodingException e) {
			mThrower.ctch(e);
		}
		return null;
	}
	
}