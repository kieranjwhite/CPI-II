package com.hourglassapps.serialise;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.Segment;

class ReGroupHandler {
	private final Pattern mPat;
	private final int mPrefixLen;
	private final int mSuffixLen;
	private final SearchAndReplacer mReplacer;
	private final Buffer mOutBuf;
	
	public ReGroupHandler(String pPatText, int pPrefixLen, int pSuffixLen, SearchAndReplacer pReplacer, Buffer pBuffer) {
		mPat=Pattern.compile(pPatText);
		mPrefixLen=pPrefixLen;
		mSuffixLen=pSuffixLen;
		mReplacer=pReplacer;
		mOutBuf=pBuffer;
	}

	private int numCharsToStartOfGroup(int pStartIdx, int pGroupIdx) {
		return pGroupIdx-mPrefixLen-pStartIdx;
	}
	
	private int pushCharsBetween(char pSrc[], int pStartIdx, 
			int pGroupIdx, int pGroupLen) {
		mOutBuf.push(pSrc, pStartIdx, numCharsToStartOfGroup(pStartIdx, pGroupIdx));
		return pGroupIdx+pGroupLen+mSuffixLen;
	}
	
	public int push(char pSrc[], int pLen) {
		int pushedCnt=0;
		Segment seg=new Segment(pSrc, 0, pLen);
		Matcher arg=mPat.matcher(seg);
		int startIdx=0;
		while(arg.find()) {
			int numGroups=arg.groupCount();
			if(numGroups==0) {
				break;
			} else {
				String toTweak=arg.group(1);
				int groupIdx=arg.start(1);
				pushedCnt+=numCharsToStartOfGroup(startIdx, groupIdx);
				startIdx=pushCharsBetween(pSrc, startIdx, groupIdx, 
						toTweak.length());
				mReplacer.run(toTweak);
			}
		}
		int remainderLen=pLen-startIdx;
		pushedCnt+=remainderLen;
		mOutBuf.push(pSrc, startIdx, remainderLen);
		return pushedCnt;
	}
}