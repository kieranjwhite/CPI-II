package com.hourglassapps.util;

import java.util.Iterator;

public class NullIterable<A> implements Iterable<A> {
	@Override
	public Iterator<A> iterator() {
		return new Iterator<A>(){

			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public A next() {
				return null;
			}

			@Override
			public void remove() {
			}
			
		};
	}
}
