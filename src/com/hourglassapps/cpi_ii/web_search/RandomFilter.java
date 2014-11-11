package com.hourglassapps.cpi_ii.web_search;

import java.util.Random;

import com.hourglassapps.util.Filter;

public class RandomFilter<T> implements Filter<T> {
	private final Random mRng;
	private final double mP;
	
	public RandomFilter(double pP) {
		mRng=new Random();
		mP=pP;
	}

	@Override
	public boolean accept(T pArg) {
		int hash=pArg.hashCode();
		mRng.setSeed(hash);
		boolean res=mRng.nextDouble()<mP;
		return res;
	}

}
