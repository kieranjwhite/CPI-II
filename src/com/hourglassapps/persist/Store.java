package com.hourglassapps.persist;

import java.io.IOException;

public interface Store<K,A,C> extends AutoCloseable {
	/**
	 * Adds existing entry to Store
	 * @param pKey entry to add
	 * @return <code>true</code> if pKey was an existing entry, <code>false</code> otherwise
	 * @throws IOException
	 */
	public boolean addExisting(K pKey) throws IOException;
	/**
	 * Adds new entry to store
	 * @param pContent entry to add
	 * @throws IOException
	 */
	public void addNew(A pContent) throws IOException;
	/**
	 * Makes any additions since last commit, or since Store instantiation permanent
	 * @param pCommittment
	 * @throws IOException
	 */
	public void commit(C pCommittment) throws IOException;
	
	/**
	 * Deletes all content in journal
	 * @throws IOException
	 */
	public void reset() throws IOException;

}
