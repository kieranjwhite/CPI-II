package com.hourglassapps.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public abstract class AbstractMultiMap<K, C extends Collection<E>, E> implements MultiMap<K, C, E> {
	private static final long serialVersionUID = 1L;
	private CollectionFactory<C,E> mFact;
	
	public AbstractMultiMap(CollectionFactory<C,E> pFact) {
		mFact=pFact;
	}
	
	public boolean addOne(K key, E value) {
		if(containsKey(key)) {
			Collection<E> orig=get(key);
			orig.add(value);
		} else {
			C list=mFact.inst();
			put(key, list);
			list.add(value);
		}
		return true;
	}

	public E removeOne(Object key) {
		if(containsKey(key)) {
			C orig=get(key);
			int size=orig.size();
			if(size>0) {
				return mFact.removeOne(orig);
			}
		}
		return null;
	}

	@Override
	public boolean containsOne(Object key) {
		if(containsKey(key)) {
			Collection<E> orig=get(key);
			return orig.size()>0;
		}
		return false;
	}

	@Override
	public void mergeAll(Map<? extends K, ? extends E> m) {
		for(K k: m.keySet()) {
			E e = m.get(k);
			if(containsKey(k)) {
				Collection<E> orig=get(k);
				orig.add(e);
			} else {
				put(k, mFact.inst(Collections.singletonList(e)));
			}		
		}
	}

	@Override
	public void mergeAll(MultiMap<? extends K, ? extends C, ? extends E> m) {
		for(K k: m.keySet()) {
			Collection<? extends E> els = m.get(k);
			if(containsKey(k)) {
				Collection<E> orig=get(k);
				orig.addAll(els);
			} else {
				put(k, mFact.inst(els));
			}		
		}
	}
}

