package com.hourglassapps.threading;

import com.hourglassapps.util.Filter;

public class HashTemplate<I> extends AbstractFilterTemplate<I> {

	@Override
	public Filter<I> filter(final int pFilterNumber, final int pTotal) {
		return new Filter<I>() {

			@Override
			public boolean accept(I pArg) {
				int hash=pArg.hashCode();
				return (hash % pTotal)==pFilterNumber;
			}
			
		};
	}

}
