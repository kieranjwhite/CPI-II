package com.hourglassapps.util;

import java.util.Collection;
import java.util.Map;

public interface MultiMap<K, C extends Collection<E>, E> extends Map<K, C> {
	public boolean addOne(K key, E value);
	public E removeOne(Object key);
	public boolean containsOne(Object key);
	public void mergeAll(Map<? extends K, ? extends E> m);
	public void mergeAll(MultiMap<? extends K, ? extends C, ? extends E> m);
	//public Iterable<Map.Entry<K, E>> each();
}
