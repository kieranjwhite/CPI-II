package com.hourglassapps.threading;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hourglassapps.util.Filter;

public class RandomTemplate<I> implements FilterTemplate<I> {
	private final List<Random> mRngs=new ArrayList<Random>();
	private final double mP;
	private final int mBaseSeed;
	
	public RandomTemplate(int pNumThreads, int pSeed, double pP) {
		for(int t=0; t<pNumThreads; t++) {
			mRngs.add(new Random());
		}
		mP=pP;
		mBaseSeed=pSeed<<32;
	}

	@Override
	public ThreadFunction convert(I pIn) {
		final int hash=pIn.hashCode();
		return new ThreadFunction() {

			@Override
			public boolean accept(int pTid, int pNumThreads) {
				if(pTid>=mRngs.size()) {
					throw new IllegalArgumentException("out of range tid: "+pTid+" max: "+mRngs.size());
				}
				Random rng=mRngs.get(pTid);
				rng.setSeed(hash+mBaseSeed);
				boolean res=rng.nextDouble()<mP;
				return res;
			}
			
		};
	}

}
