package com.hourglassapps.util;

public interface ThrowingIterable<T> extends Iterable<T> {
	public ThrowableIterator<T> throwableIterator();
}
