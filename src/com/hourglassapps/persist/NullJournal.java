package com.hourglassapps.persist;

import java.io.IOException;

import com.hourglassapps.cpi_ii.Journal;
import com.hourglassapps.util.Typed;

public class NullJournal<K,C> implements Journal<K,C> {

	@Override
	public boolean has(K pKey) {
		return false;
	}

	@Override
	public void startEntry() throws IOException {
	}

	@Override
	public void add(Typed<C> pContent) throws IOException {
	}

	@Override
	public void commitEntry(K pKey) throws IOException {
	}

	@Override
	public void reset() throws IOException {
	}
	
}