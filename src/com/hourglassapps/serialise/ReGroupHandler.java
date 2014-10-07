package com.hourglassapps.serialise;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.Segment;

/**
 * Recognises strings matching a particular regular expression and 
 * invokes a specified {@link com.hourglassapps.serialise.SearchAndReplacer} instance to 
 * generate an alternative String. This String is typically pushed to an output 
 * {@link com.hourglassapps.serialise.Buffer}.
 * @author kieran
 *
 */
public class ReGroupHandler {
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
	
	/**
	 * Pushes an unchanged subarray of chars to the output {@link com.hourglassapps.serialise.Buffer}.
	 * This subarray begins (inclusively) at a specified index of pSrc and ends (inclusively) 
	 * at the character index preceding the <code>mPat</code> instance. 
	 * @param pSrc
	 * @param pStartIdx start idx within pSrc
	 * @param pGroupIdx 
	 * @param pGroupLen
	 * @return character index after <code>mPat</code> match.
	 */
	private int pushCharsBetween(char pSrc[], int pStartIdx, 
			int pGroupIdx, int pGroupLen) {
		mOutBuf.push(pSrc, pStartIdx, numCharsToStartOfGroup(pStartIdx, pGroupIdx));
		return pGroupIdx+pGroupLen+mSuffixLen;
	}
	
	/**
	 * Preprocesses a subarray of <code>chars</code> invoking <code>mReplacer</code> to deal with any
	 * sequence of characters requiring special attention.
	 * @param pSrc
	 * @param pLen number of characters in pSrc that must be prepreprocessed in this method invocation. 
	 * Typically pLen is the number of characters from the start of <code>pSrc</code> to the beginning 
	 * of a line we do not yet wish to preprocess, but it can extend to include multiple lines across 
	 * the entire populated length of <code>pSrc</code>.
	 * @return number of characters pushed to output {@link com.hourglassapps.serialise.Buffer}.
	 */
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
				pushedCnt+=mReplacer.run(toTweak);
			}
		}
		int remainderLen=pLen-startIdx;
		pushedCnt+=remainderLen;
		mOutBuf.push(pSrc, startIdx, remainderLen);
		return pushedCnt;
	}
}