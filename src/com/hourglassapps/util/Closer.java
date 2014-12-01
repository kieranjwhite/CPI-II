package com.hourglassapps.util;

import java.io.Closeable;
import java.util.Deque;
import java.util.LinkedList;

public class Closer implements AutoCloseable {
	private Deque<AutoCloseable> mCloseables=new LinkedList<>();
	
	/**
	 * Adds an AutoCloseable instance to the end of our list of AutoCloseables.
	 * @param pCloseable - if <code>null</code> nothing will be added.
	 * @return this to allow chaining
	 */
	public Closer before(AutoCloseable pCloseable) {
		if(pCloseable!=null) {
			mCloseables.addLast(pCloseable);
		}
		return this;
	}
	
	/**
	 * Adds an AutoCloseable instance to the end of our list of AutoCloseables.
	 * @param pCloseable - if <code>null</code> nothing will be added.
	 * @return this to allow chaining
	 */
	public Closer after(AutoCloseable pCloseable) {
		if(pCloseable!=null) {
			mCloseables.addFirst(pCloseable);
		}
		return this;
	}
	
	/**
	 * Closes all AutoCloseable instance in same order that they were before'ed and
	 */
	@Override
	public void close() throws Exception {
		try(ConcreteThrower<Exception> thrower=new ConcreteThrower<Exception>()) {
			AutoCloseable c;
			while((c=mCloseables.pollFirst())!=null) {
				try {
					c.close();
				} catch(Exception e) {
					thrower.ctch(e);
				}			
			}
		}
	}
}