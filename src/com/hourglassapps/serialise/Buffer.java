package com.hourglassapps.serialise;

import com.hourglassapps.util.Log;

/**
 * Think of a Buffer instance as a queue of characters, allowing an 
 * arbitrary number of characters to be pushed to the end 
 * or shifted from the start in a single method call.
 */
public class Buffer {
	private final static String TAG=Buffer.class.getName();
	private StringBuilder mBuf=new StringBuilder();
	
	public Buffer() {
	}
	
	public void push(char pSrc[], int pSrcOff, int pLen) {
		mBuf.append(pSrc, pSrcOff, pLen);
	}
	
	public int shift(char pDst[], int pDstOff, int pMaxLen) {
		int copyable=Math.min(mBuf.length(), pMaxLen);
		mBuf.getChars(0, copyable, pDst, pDstOff);
		mBuf.delete(0, copyable);
		String toLog=new String(pDst, pDstOff, copyable);
		Log.i(TAG, toLog);
		return copyable;
	}
}