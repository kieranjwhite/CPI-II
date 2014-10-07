package com.hourglassapps.serialise;

public class Buffer {
	private StringBuilder mBuf=new StringBuilder();
	
	public Buffer() {
	}
	
	public void push(char pSrc[], int pSrcOff, int pLen) {
		mBuf.append(pSrc, pSrcOff, pLen);
	}
	
	public int shift(char pDst[], int pDstOff, int pMaxLen) {
		int copyable=Math.min(mBuf.length(), pMaxLen);
		System.arraycopy(mBuf, 0, pDst, pDstOff, copyable);
		mBuf.delete(0, copyable);
		return copyable;
	}
}