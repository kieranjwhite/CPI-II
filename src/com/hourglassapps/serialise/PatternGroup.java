package com.hourglassapps.serialise;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.Segment;

class PatternGroup {
	private Pattern mPat;
	private int mPrefixLen;
	private int mSuffixLen;
	private SearchAndReplacer mReplacer;
	private Buffer mOutBuf;
	
	public PatternGroup(String pPatText, int pPrefixLen, int pSuffixLen, SearchAndReplacer pReplacer, Buffer pBuffer) {
		Pattern.compile(pPatText);
		mPrefixLen=pPrefixLen;
		mSuffixLen=pSuffixLen;
		mReplacer=pReplacer;
		mOutBuf=pBuffer;
	}

	private int pushCharsBetween(char pSrc[], int pStartIdx, 
			int pGroupPrefixLen, int pGroupIdx, int pGroupLen, int pGroupSuffixLen) {
		mOutBuf.push(pSrc, pStartIdx, pGroupIdx-pGroupPrefixLen-pStartIdx);
		return pGroupIdx+pGroupLen+pGroupSuffixLen;
	}
	
	public void push(char pSrc[], int pLen) {
		Segment seg=new Segment(pSrc, 0, pLen);
		Matcher arg=mPat.matcher(seg);
		int startIdx=0;
		while(arg.find()) {
			int numGroups=arg.groupCount();
			if(numGroups==0) {
				break;
			} else {
				String toTweak=arg.group(1);
				startIdx=pushCharsBetween(pSrc, startIdx, mPrefixLen, arg.start(1), 
						toTweak.length(), mSuffixLen);
				mReplacer.run(toTweak);
			}
		}
		mOutBuf.push(pSrc, startIdx, pLen-startIdx);
	}
}