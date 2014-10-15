package com.hourglassapps.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class TreeArrayMultiMap<K extends Comparable<K>, E> extends AbstractMultiMap<K, List<E>, E> implements SortedMultiMap<K, List<E>,E> {
	private static final long serialVersionUID = 1L;
	private SortedMap<K, List<E>> mDelegateMap=new TreeMap<K, List<E>>();
	
	public TreeArrayMultiMap() {
		super(new ListFactory<E>());
	}

	/**
	 * 
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		mDelegateMap.clear();
	}

	/**
	 * @return
	 * @see java.util.SortedMap#comparator()
	 */
	public Comparator<? super K> comparator() {
		return mDelegateMap.comparator();
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		return mDelegateMap.containsKey(key);
	}

	/**
	 * @param value
	 * @return
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		return mDelegateMap.containsValue(value);
	}

	/**
	 * @return
	 * @see java.util.Map#entrySet()
	 */
	public Set<java.util.Map.Entry<K, List<E>>> entrySet() {
		return mDelegateMap.entrySet();
	}

	/**
	 * @return
	 * @see java.util.SortedMap#firstKey()
	 */
	public K firstKey() {
		return mDelegateMap.firstKey();
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public List<E> get(Object key) {
		return mDelegateMap.get(key);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.SortedMap#headMap(java.lang.Object)
	 */
	public SortedMap<K, List<E>> headMap(K arg0) {
		return mDelegateMap.headMap(arg0);
	}

	/**
	 * @return
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		return mDelegateMap.isEmpty();
	}

	/**
	 * @return
	 * @see java.util.Map#keySet()
	 */
	public Set<K> keySet() {
		return mDelegateMap.keySet();
	}

	/**
	 * @return
	 * @see java.util.SortedMap#lastKey()
	 */
	public K lastKey() {
		return mDelegateMap.lastKey();
	}

	/**
	 * @param key
	 * @param value
	 * @return
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public List<E> put(K key, List<E> value) {
		return mDelegateMap.put(key, value);
	}

	/**
	 * @param arg0
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map<? extends K, ? extends List<E>> arg0) {
		mDelegateMap.putAll(arg0);
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public List<E> remove(Object key) {
		return mDelegateMap.remove(key);
	}

	/**
	 * @return
	 * @see java.util.Map#size()
	 */
	public int size() {
		return mDelegateMap.size();
	}

	/**
	 * @param startKey
	 * @param endKey
	 * @return
	 * @see java.util.SortedMap#subMap(java.lang.Object, java.lang.Object)
	 */
	public SortedMap<K, List<E>> subMap(K startKey, K endKey) {
		return mDelegateMap.subMap(startKey, endKey);
	}

	/**
	 * @param startKey
	 * @return
	 * @see java.util.SortedMap#tailMap(java.lang.Object)
	 */
	public SortedMap<K, List<E>> tailMap(K startKey) {
		return mDelegateMap.tailMap(startKey);
	}

	/**
	 * @return
	 * @see java.util.Map#values()
	 */
	public Collection<List<E>> values() {
		return mDelegateMap.values();
	}
}

