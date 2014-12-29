package com.hourglassapps.persist;

import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.URLUtils;

public class Shortener implements Converter<String,String> {
	private final static int MAX_SUFFIX_CHARS=1+(int)Math.floor(Math.log10(Integer.MAX_VALUE)+1);
	private Map<String,String> mLongNameToShorter=new HashMap<>();
	private int mCurSuffix=0;
	private final ConcreteThrower<? super UnsupportedEncodingException> mThrower;
	private final int mMaxLen;

	public Shortener(int pMaxLen, ConcreteThrower<? super UnsupportedEncodingException> pThrower) {
		mMaxLen=pMaxLen;
		mThrower=pThrower;
	}
	
	@Override
	public String convert(String cleaned) {
		try {
			String encoded=URLUtils.encode(cleaned);
			if(encoded.length()>mMaxLen) {
				/*
				 * Assumes convert is invoked in a fixed order. If that order changes
				 * (eg if the the conductus xml export is modified) a new report will 
				 * have to be generated from scratch (ie by deleting the directory poems/results).
				 */
				if(mLongNameToShorter.containsKey(cleaned)) {
					encoded=mLongNameToShorter.get(cleaned);
				} else {
					encoded=(encoded.substring(0,mMaxLen-MAX_SUFFIX_CHARS)+'_')+(mCurSuffix++);
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