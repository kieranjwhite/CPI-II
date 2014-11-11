package com.hourglassapps.cpi_ii.web_search;

import java.util.Random;

import com.hourglassapps.util.Filter;

public class RandomFilter<T> implements Filter<T> {
	private final Random mRng;
	private final double mP;
	private final int mBaseSeed;
	
	public RandomFilter(int pSeed, double pP) {
		mRng=new Random();
		mP=pP;
		mBaseSeed=pSeed<<32;
	}

	@Override
	public boolean accept(T pArg) {
		int hash=pArg.hashCode();
		mRng.setSeed(hash+mBaseSeed);
		boolean res=mRng.nextDouble()<mP;
		return res;
	}

}
