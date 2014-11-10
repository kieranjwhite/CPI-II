package com.hourglassapps.cpi_ii.web_search;

import java.util.Random;

import com.hourglassapps.util.Filter;

public class RandomFilter<T> implements Filter<T> {
	private final Random mRng;
	private final double mP;
	
	public RandomFilter(long pSeed, double pP) {
		mRng=new Random(pSeed);
		mP=pP;
	}

	@Override
	public boolean accept(T pArg) {
		return mRng.nextDouble()<mP;
	}

}
