package com.hourglassapps.serialise;

import java.io.IOException;
import java.io.Reader;

import javax.swing.text.Segment;

public class RemoveUnescapesReader extends Reader {
	private Reader mInput;
	private boolean done=false;
	private Buffer mPending=new Buffer();
	private ReGroupHandler mUnescapeGroup=new ReGroupHandler("unescape\\('([^)]*)'\\)", 10, 2, 
			new SearchAndReplacer() {
		private char quoteArr[]=new char[]{'"'};
		
		@Override
		public int run(String pUnescapeArg) {
			int len=pUnescapeArg.length();
			char src[]=new char[len];
			pUnescapeArg.getChars(0, len, src, 0);
			int pushedCnt=2; //2 for opening and closing quotes
			mPending.push(quoteArr, 0, 1);
			pushedCnt+=mEscapedCharGroup.push(src, len);
			mPending.push(quoteArr, 0, 1);
			return pushedCnt;
		}

	}, mPending);
	private ReGroupHandler mEscapedCharGroup=new ReGroupHandler("%([a-fA-F0-9]{2})", 1, 0, 
			new SearchAndReplacer() {

		@Override
		public int run(String pCtrlArg) {
			int c=Integer.parseInt(pCtrlArg, 16);
			String s;
			switch(c) {
			case 9:
				s=" ";
				break;
			default:
				s=Character.toString((char)c);
			}
			char charArr[]=s.toCharArray();
			assert charArr.length==1;
			mPending.push(charArr, 0, 1);
			return 1;
		}

	}, mPending);

	private char mBuf[]=new char[0];
	private int mLen=0;
	private int mNewLen;
	
	public RemoveUnescapesReader(Reader pInput) throws IOException {
		super();
		mInput=pInput;
	}

	@Override
	public void close() throws IOException {
		mInput.close();
	}

	@Override
	public int read(char[] pDst, int pDstOff, int pMaxLen) throws IOException {
		if(done) {
			return -1;
		}
		assert (new Segment(mBuf, 0, mLen)).toString().indexOf('\n')==-1;
		int newMax=mLen+pMaxLen;
		if(mBuf==null || mBuf.length<newMax) {
			mBuf=enlarge(mBuf, newMax);
		}
		assert mBuf!=null && mBuf.length>=newMax;
		int charsRead=mInput.read(mBuf, mLen, pMaxLen);
		if(charsRead==-1) {
			done=true;
			mUnescapeGroup.push(mBuf, mLen);
			return mPending.shift(pDst, pDstOff, pMaxLen);
		}

		mNewLen=mLen+charsRead;
		int last_copied=copyToNewLine();
		int copied=last_copied;
		while(last_copied!=0) {

			last_copied=copyToNewLine();
			copied+=last_copied;
		}
		return mPending.shift(pDst, pDstOff, pMaxLen);
	}

	private char[] enlarge(char pBuf[], int pNewMax) {
		char buf[]=new char[pNewMax];
		System.arraycopy(pBuf, 0, buf, 0, mLen);
		return buf;
	}

	private int copyToNewLine() {
		int newLineIdx=indexOf('\n', mNewLen);
		if(newLineIdx!=-1) {
			assert newLineIdx>=mLen && newLineIdx<mNewLen;
			int nextLineStartIdx=newLineIdx+1;
			int pushedCnt=mUnescapeGroup.push(mBuf, nextLineStartIdx);
			mNewLen-=nextLineStartIdx;
			System.arraycopy(mBuf, nextLineStartIdx, mBuf, 0, mNewLen);
			mLen=0;
			return pushedCnt;
		} else {
			mLen=mNewLen;
			return 0;				
		}			
	}

	private int indexOf(char pSought, int pLen) {
		for(int idx=mLen; idx<pLen; idx++) {
			if(mBuf[idx]==pSought) {
				return idx;
			}
		}
		return -1;
	}

}
