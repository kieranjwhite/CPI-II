package com.hourglassapps.threading;

import java.util.Random;

import com.hourglassapps.util.Filter;

public class RandomTemplate<I> implements FilterTemplate<I> {
	private final Random mRng;
	private final double mP;
	private final int mBaseSeed;
	
	public RandomTemplate(int pSeed, double pP) {
		mRng=new Random();
		mP=pP;
		mBaseSeed=pSeed<<32;
	}

	@Override
	public ThreadFunction convert(I pIn) {
		final int hash=pIn.hashCode();
		return new ThreadFunction() {

			@Override
			public boolean accept(int pTid, int pNumThreads) {
				mRng.setSeed(hash+mBaseSeed);
				boolean res=mRng.nextDouble()<mP;
				return res;
			}
			
		};
	}

}
