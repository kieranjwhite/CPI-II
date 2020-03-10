package com.hourglassapps.threading;

import com.hourglassapps.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FailingTemplate<I> implements FilterTemplate<I> {
    private final static String TAG=FailingTemplate.class.getName();

    private int mQueryCnt=0;
    
    public FailingTemplate() {
	}

	@Override
	public ThreadFunction convert(I pIn) {
		return new ThreadFunction() {

			@Override
			public boolean accept(int pTid, int pNumThreads) {
			    Log.i(TAG, "query number: "+mQueryCnt++);
			    return false;
			}
			
		};
	}
}
