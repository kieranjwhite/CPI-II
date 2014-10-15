package com.hourglassapps.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListFactory<E> implements CollectionFactory<List<E>, E> {

	@Override
	public List<E> inst() {
		return new ArrayList<E>();
	}

	@Override
	public List<E> inst(Collection<? extends E> els) {
		return new ArrayList<E>(els);
	}

	@Override
	public E removeOne(List<E> pColl) {
		return pColl.remove(pColl.size()-1);
	}
	
}

