package com.hourglassapps.cpi_ii.report;

import java.util.ArrayList;
import java.util.List;

import com.hourglassapps.util.Converter;

public class LineGenerator {
	public enum LineType {
		TITLE, BODY;
	}

	private final Converter<String,String> mCleaner;
	private final long mEprintId;
	private boolean mBuilt=false;

	private List<Line> mLines=new ArrayList<>();

	public LineGenerator(long pEprintId, Converter<String,String> pCleaner) {
		mEprintId=pEprintId;
		mCleaner=pCleaner;
	}

	public void addLine(String pLine, LineType pType) {
		assert mBuilt==false;
		mLines.add(new Line(pLine, pType, mLines.size()));
	}

	public long eprintId() {
		return mEprintId;
	}

	public Line line(int pLineNum) {
		if(mBuilt==false) {
			mBuilt=true;
		}
		return mLines.get(pLineNum); 
	}

	public int numLines() {
		return mLines.size();
	}
	
	public class Line {
		private final int mLineNum;
		private final String mText;
		private final LineType mType;

		private Line(String pText, LineType pType, int pLineNum) {
			mLineNum=pLineNum;
			mText=pText;
			mType=pType;
		}

		public long eprintId() {
			return mEprintId;
		}

		public String text() {
			return mText;
		}

		public String cleaned() {
			return mCleaner.convert(mText);
		}
		
		public LineType type() {
			return mType;
		}

		public Line next() {
			int nextNum=mLineNum+1;
			if(nextNum==mLines.size()) {
				return null;
			}
			assert nextNum<mLines.size();
			return mLines.get(nextNum);
		}

		public Line prev() {
			int prevNum=mLineNum-1;
			if(prevNum==-1) {
				return null;
			}
			assert prevNum>=0;
			return mLines.get(prevNum);
		}
		
		public String toString() {
			return text();
		}
	}
}
