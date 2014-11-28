package com.hourglassapps.threading;

import java.util.HashSet;
import java.util.Set;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Filter;
import com.hourglassapps.util.Ii;

public abstract class AbstractFilterTemplate<I> implements FilterTemplate<I> {
	private Set<Integer> mResults=new HashSet<>();
	
	@Override
	public Converter<I, Set<Integer>> convert(final Ii<Integer,Integer> pNumTot) {
		return converter(pNumTot.fst(), pNumTot.snd(), mResults);
	}

	public Converter<I,Set<Integer>> converter(final int pFilterNumber, final int pTotal, final Set<Integer> pToFill) {
		if(pTotal<1) {
			throw new IllegalArgumentException("pTotal must be >=1: "+pTotal);
		}
		if(pFilterNumber<0 || pFilterNumber>=pTotal) {
			throw new IllegalArgumentException("pFilterNumber must be >=0 && < pTotal:"+pFilterNumber+" / "+pTotal);
		}

		return new Converter<I, Set<Integer>>() {
			Filter<I> mFilter=filter(pFilterNumber, pTotal);
			@Override
			public Set<Integer> convert(I pIn) {
				pToFill.clear();
				if(mFilter.accept(pIn)) {
					pToFill.add(pFilterNumber);
				}
				return pToFill;
			}
		};
	}
	
	abstract public Filter<I> filter(int pFilterNumber, int pTotal);
}
