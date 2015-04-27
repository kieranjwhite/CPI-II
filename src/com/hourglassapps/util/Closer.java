package com.hourglassapps.util;

import java.io.Closeable;
import java.util.Deque;
import java.util.LinkedList;

public class Closer implements AutoCloseable {
	private boolean mClosed=false;
	private Deque<AutoCloseable> mCloseables=new LinkedList<>();
	
	/**
	 * Adds an AutoCloseable instance to the end of our list of AutoCloseables.
	 * @param pCloseable - if <code>null</code> nothing will be added.
	 * @return this to allow chaining
	 */
	public Closer before(AutoCloseable pCloseable) {
		assert !closed();
		if(pCloseable!=null) {
			mCloseables.addLast(pCloseable);
		}
		return this;
	}
	
	/**
	 * Adds an AutoCloseable instance to the start of our list of AutoCloseables.
	 * @param pCloseable - if <code>null</code> nothing will be added.
	 * @return this to allow chaining
	 */
	public Closer after(AutoCloseable pCloseable) {
		assert !closed();
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
		if(!closed()) {
			mClosed=true;
			AutoCloseable c;
			while((c=mCloseables.pollFirst())!=null) {
				c.close();
			}
		}
	}

	public boolean closed() {
		return mClosed;
	}
}