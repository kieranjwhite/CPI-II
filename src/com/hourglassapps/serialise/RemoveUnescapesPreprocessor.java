package com.hourglassapps.serialise;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import javax.swing.text.Segment;

public class RemoveUnescapesPreprocessor extends Preprocessor {
	private Reader mReader=new UnescapeReader();
	private Buffer mPending=new Buffer();
	private PatternGroup mUnescapeGroup=new PatternGroup("unescape\\(([^)]*)\\)", 9, 1, 
			new SearchAndReplacer() {

		@Override
		public void run(String pUnescapeArg) {
			int len=pUnescapeArg.length();
			char src[]=new char[len];
			pUnescapeArg.getChars(0, len, src, 0);
			mEscapedCharGroup.push(src, len);
		}
		
	}, mPending);
	private PatternGroup mEscapedCharGroup=new PatternGroup("%([a-fA-F0-9]{2})", 1, 0, 
			new SearchAndReplacer() {
		
		@Override
		public void run(String pCtrlArg) {
		}
		
	}, mPending);

	private char mBuf[];
	private int mLen=0;
	
	private class UnescapeReader extends Reader {

		@Override
		public void close() throws IOException {
			mInput.close();
		}

		@Override
		public int read(char[] pDst, int pDstOff, int pMaxLen) throws IOException {
			assert (new Segment(mBuf, 0, mLen)).toString().indexOf('\n')==-1;
			int new_max=mLen+pMaxLen;
			if(mBuf==null || mBuf.length<new_max) {
				mBuf=enlarge(mBuf, new_max);
			}
			assert mBuf!=null && mBuf.length>=new_max;
			int chars_read=mInput.read(mBuf, mLen, pMaxLen);
			mLen+=chars_read;
			if(chars_read==-1) {
				mUnescapeGroup.push(mBuf, mLen);
			}
			
			int last_copied=copyToNewLine(mBuf, mLen, pDst, pDstOff);
			int copied=last_copied;
			while(last_copied!=0) {
				
				last_copied=copyToNewLine(mBuf, mLen, pDst, pDstOff);
				copied+=last_copied;
			}
			return copied;
		}
		
		private char[] enlarge(char pBuf[], int pNewMax) {
			char buf[]=new char[pNewMax];
			System.arraycopy(pBuf, 0, buf, 0, mLen);
			return buf;
		}
		
		private int copyToNewLine(char pSrc[], int pSrcOff, char pDst[], int pDstOff) {
			int new_line_idx=indexOf('\n');
			if(new_line_idx!=-1) {
				return 0;
			} else {
				return 0;				
			}			
		}
		
		private int indexOf(char pSought) {
			for(int idx=mLen; idx<mBuf.length; idx++) {
				if(mBuf[idx]==pSought) {
					return idx;
				}
			}
			return -1;
		}
	}
	
	public RemoveUnescapesPreprocessor(File pFile) throws IOException {
		super(pFile);
	}

	@Override
	public Reader reader() {
		return mReader;
	}

	@Override
	public void close() throws IOException {
		mReader.close();
	}
	
	
}
