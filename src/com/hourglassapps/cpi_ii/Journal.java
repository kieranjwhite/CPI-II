package com.hourglassapps.cpi_ii;

import java.io.IOException;

import org.jdeferred.Promise;

import com.hourglassapps.util.Typed;

public interface Journal<K,C> {
	public boolean has(K pKey);
	public void startEntry() throws IOException;
	public void add(Typed<C> pContent) throws IOException;
	public void commitEntry(K pKey) throws IOException;
	public void reset() throws IOException;
}
