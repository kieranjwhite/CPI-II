package com.hourglassapps.threading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Filter;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Rtu;

/***
 * Intended for the purpose of partitioning work between multiple threads. A <code>ConverterReceiver<I></code> instance
 * can generate a number of <code>Filter<I></code> instances based on a template. Each filter determines whether
 * its respective thread should accept a particular job.  
 * @author kieran
 *
 * @param <I>
 */
public class JobDelegator<I> {
	private final FilterTemplate<I> mTemplate;
	private final int mNumThreads;
	
	private I mLastI;
	private ThreadFunction mLastTF;
	
	public JobDelegator(int pNumThreads, FilterTemplate<I> pTemplate) {
		if(pNumThreads<=0) {
			throw new IllegalArgumentException("Num threads must be >0: "+pNumThreads);
		}
		mNumThreads=pNumThreads;
		mTemplate=pTemplate;
	}

	public Filter<I> filter() {
		return filter(0);
	}
	
	public List<Filter<I>> filters() {
		List<Filter<I>> output=new ArrayList<Filter<I>>();
		for(int i=0; i<mNumThreads; i++) {
			output.add(filter(i));
		}
		assert output.size()==mNumThreads;
		return output;
	}

	public Filter<I> filter(final int pFilterNum) {
		return new Filter<I>() {

			@Override
			public boolean accept(I pArg) {
				if(!Rtu.safeEq(mLastI, pArg)) {
					mLastTF=mTemplate.convert(pArg);
					mLastI=pArg;
				}
				return mLastTF.accept(pFilterNum, mNumThreads);
			}
			
		};
	}
}
