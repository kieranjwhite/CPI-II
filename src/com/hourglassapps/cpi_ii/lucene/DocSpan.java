package com.hourglassapps.cpi_ii.lucene;

public class DocSpan implements Comparable<DocSpan> {
	private int mStartOffset;
	private int mEndOffset;
	
	public DocSpan(int pStartOffset, int pEndOffset) {
		if(pStartOffset>pEndOffset) {
			throw new IllegalArgumentException("start: "+pStartOffset+" end: "+pEndOffset);
		}
		mStartOffset=pStartOffset;
		mEndOffset=pEndOffset;
	}
	
	public int startOffset() {
		return mStartOffset;
	}
	
	public int endOffset() {
		return mEndOffset;
	}
	
	/**
	 * 
	 * @param pOther
	 * @return false if both DocSpans remain separate, true if pOther can be discarded in which case this inst spans both of the initial instances
	 */
	public boolean merged(DocSpan pOther) {
		if(mEndOffset<pOther.mStartOffset || mStartOffset>pOther.mEndOffset) {
			//separate DocSpans, not touching 
			return false;
		}
		
		if(mStartOffset<=pOther.mStartOffset && mEndOffset>=pOther.mEndOffset) {
			//this DocSpan entirely surrounds the other
			return true;
		}
		
		if(mStartOffset>=pOther.mStartOffset && mEndOffset<=pOther.mEndOffset) {
			//this DocSpan entirely surrounded by the other
			mStartOffset=pOther.mStartOffset;
			mEndOffset=pOther.mEndOffset;
			return true;
		}
		
		//overlap exists or the two instances are touching
		mStartOffset=Math.min(mStartOffset, pOther.mStartOffset);
		mEndOffset=Math.max(mEndOffset, pOther.mEndOffset);
		return true;
	}

	@Override
	public int compareTo(DocSpan arg0) {
		if(arg0==this) { 
			return 0;
		}
		
		if(this.mStartOffset==arg0.mStartOffset) {
			return this.mEndOffset-arg0.mEndOffset;
		} else {
			return this.mStartOffset-arg0.mStartOffset;
		}
	}

}
