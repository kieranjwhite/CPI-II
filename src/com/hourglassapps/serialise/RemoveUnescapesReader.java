package com.hourglassapps.serialise;

import java.io.IOException;
import java.io.Reader;

import javax.swing.text.Segment;

public class RemoveUnescapesReader extends Reader {
	private Reader mInput;
	private boolean done=false;
	private Buffer mPending=new Buffer();
	/**
	 * Deals with identifying an modifying lines matching the 'unescape' regular expression.
	 */
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
	/**
	 * Deals with identifying an modifying a string matching a single HTMLified control character,
	 * (i.e. a string matching the re <code>%([a-fA-F0-9]{2})</code>.
	 * <p>
	 * Such a control character (i.e. any 2 digit hex number preceded by 
	 * a %) is converted to the number's corresponding ASCII character, with 
	 * one exception: <code>%09</code> is replaced by a space
	 */
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

	/**
	 * Reads json and passes along any line that doesn't contain a match for 
	 * the regular expression "unescape\\('([^)]*)'\\)".
	 * <p>
	 * Any line that does match that re is replaced by one where the matching 
	 * substring is replaced by a modified version of captured group 1 (i.e. 
	 * the matching string corresponding to the <code>([^)]*)</code> part of the re.
	 * <p>
	 * Captured group 1 is altered so that any 2 digit hex number preceded by 
	 * a % is converted to the number's corresponding ASCII character, with 
	 * one exception: <code>%09</code> is replaced by a space.
	 * <p>
	 * Examples:
	 * <p>
	 * <code>
	 * "eprintid": "4763", -> "eprintid": "4763",<br>
	 * "title": unescape('Amors%20m%27a%20au%20las%20pris'), -> "title": "Amors m'a au las pris",<br>
	 * "title": unescape('Amors%20m%27a%20au%09las%20pris'), -> "title": "Amors m'a au las pris",<br>
	 * </code>
	 */
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
		while(last_copied!=0) {
			last_copied=copyToNewLine();
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
