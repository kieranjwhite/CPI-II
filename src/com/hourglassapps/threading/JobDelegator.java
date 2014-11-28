package com.hourglassapps.threading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Filter;
import com.hourglassapps.util.Ii;

/***
 * Intended for the purpose of partitioning work between multiple threads a <code>ConverterReceiver<I></code> instance
 * can generate a number of <code>Filter<I></code> instances based on a template. Each filter determines whether
 * its respective thread should accept a particular job.  
 * @author kieran
 *
 * @param <I>
 */
public class JobDelegator<I> {
	private final Converter<Ii<Integer, Integer>, Converter<I, Set<Integer>>> mTemplate;
	private final int mNumThreads;
	private Set<Integer> mLastFilters=Collections.emptySet();
	private I mLastInput=null;
	
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

	public Filter<I> filter(int pFilterNum) {
		final Converter<I, Set<Integer>> inputMatcher=
				mTemplate.convert(new Ii<Integer,Integer>(pFilterNum, mNumThreads));

		final Integer fixed=Integer.valueOf(pFilterNum);
		return new Filter<I>(){

			@Override
			public boolean accept(I pArg) {
				if(pArg!=mLastInput && !pArg.equals(mLastInput)) {
					mLastFilters=inputMatcher.convert(pArg);
					mLastInput=pArg;
				}
				return mLastFilters.contains(fixed);
			}

		};
	}
}
