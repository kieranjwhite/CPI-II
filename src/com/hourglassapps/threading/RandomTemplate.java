package com.hourglassapps.threading;

import java.util.Random;

import com.hourglassapps.util.Filter;

public class RandomTemplate<T> extends AbstractFilterTemplate<T> {
	private final Random mRng;
	private final double mP;
	private final int mBaseSeed;
	
	public RandomTemplate(int pSeed, double pP) {
		mRng=new Random();
		mP=pP;
		mBaseSeed=pSeed<<32;
	}

	@Override
	public Filter<T> filter(int pFilterNumber, int pTotal) {
		return new Filter<T>() {

			@Override
			public boolean accept(T pArg) {
				int hash=pArg.hashCode();
				mRng.setSeed(hash+mBaseSeed);
				boolean res=mRng.nextDouble()<mP;
				return res;
			}
			
		};
	}

}
