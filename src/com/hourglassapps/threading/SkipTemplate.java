package com.hourglassapps.threading;

public class SkipTemplate<T> implements FilterTemplate<T> {
	private final int mNumToSkip;
	private final int mSkipCnt[];
	
	public SkipTemplate(int pNumThreads, int pNumToSkip) {
		mNumToSkip=pNumToSkip;
		mSkipCnt=new int[pNumThreads];
		for(int t=0; t<pNumThreads; t++) {
			mSkipCnt[t]=Integer.MAX_VALUE; //ensures that the first accept() call returns true
		}
	}

	@Override
	public ThreadFunction convert(T pIn) {
		return new ThreadFunction() {

			@Override
			public boolean accept(int pTid, int pNumThreads) {
				if(pTid>=mSkipCnt.length) {
					throw new IllegalArgumentException("out of range tid: "+pTid+" max: "+mSkipCnt.length);
				}
				boolean accept=mSkipCnt[pTid]++>=mNumToSkip;
				if(accept) {
					mSkipCnt[pTid]=0;
				}
				return accept;
			}

		};
	}

}
