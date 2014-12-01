package com.hourglassapps.persist;

import java.io.IOException;

import com.hourglassapps.util.Typed;

public class NullJournal<K,C> implements Journal<K,C> {

	@Override
	public boolean addExisting(K pKey) {
		return false;
	}

	@Override
	public void addNew(C pContent) throws IOException {
	}

	@Override
	public void commit(K pKey) throws IOException {
	}

	@Override
	public void reset() throws IOException {
	}

	@Override
	public void close() {
	}
}