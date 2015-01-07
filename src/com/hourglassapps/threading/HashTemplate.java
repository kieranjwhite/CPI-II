package com.hourglassapps.threading;

import java.util.Random;

import com.hourglassapps.util.Log;

public class HashTemplate<I> implements FilterTemplate<I> {
	private final static String TAG=HashTemplate.class.getName();
	private final Random mRng=new Random();
	//private int mLines=0;
	
	@Override
	public ThreadFunction convert(final I pIn) {
		final int hash=pIn.hashCode(); //we can't use hash directly as it won't be randomly distributed so instead we seed a random number generator with this value.
		return new ThreadFunction() {
			@Override
			public boolean accept(int pTid, int pNumThreads) {
				mRng.setSeed(hash);
				int tidResponsible=mRng.nextInt(pNumThreads);

				boolean accepted=(tidResponsible==pTid);
				return accepted;
			}
			
		};
	}

}
