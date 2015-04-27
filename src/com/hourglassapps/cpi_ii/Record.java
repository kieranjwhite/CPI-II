package com.hourglassapps.cpi_ii;

public interface Record<I,C> {
	public I id();
	public C content();

	/**
	 * Usually multiple PoemRecords will be return by a parser to some client class. It might be
	 * the case that some of these records should not be processed if e.g. they are in the wrong language.
	 * This method indicates which records the client should skip.
	 * @return true if this record should be skipped, false otherwise. 
	 */
	public boolean ignore();
}
