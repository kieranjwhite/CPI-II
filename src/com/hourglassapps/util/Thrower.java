package com.hourglassapps.util;

public interface Thrower extends AutoCloseable {
	/**
	 * If the implementation has already encountered and caught a Throwable that is subclass of the argument throw it now.
	 * @param pCatchable a type that the client of the implementation can handle if thrown.
	 * @throws Throwable
	 */
	public <E extends Exception> void throwCaught(Class<E> pCatchable) throws Throwable;
}
