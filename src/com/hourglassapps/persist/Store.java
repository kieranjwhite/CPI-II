package com.hourglassapps.persist;

import java.io.IOException;

public interface Store<K,A,C> {
	public boolean has(K pKey) throws IOException;
	public void add(A pContent) throws IOException;
	public void commit(C pCommittment) throws IOException;
	
	/**
	 * Deletes all content in journal
	 * @throws IOException
	 */
	public void reset() throws IOException;

}
