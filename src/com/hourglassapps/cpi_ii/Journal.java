package com.hourglassapps.cpi_ii;

public interface Journal<K, C> {
	public boolean has(K pKey);
	public void startEntry();
	public void add(C pContent);
	public void commitEntry(K pKey);
	public void reset();
}
