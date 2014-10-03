package com.hourglassapps.util;

import java.util.Iterator;

public interface ThrowableIterator<T> extends Iterator<T> {
	public void throwCaught() throws Throwable;
}
