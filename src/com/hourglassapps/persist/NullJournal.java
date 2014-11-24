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
	public void add(C pContent) throws IOException {
	}

	@Override
	public void commit(K pKey) throws IOException {
	}

	@Override
	public void reset() throws IOException {
	}
}