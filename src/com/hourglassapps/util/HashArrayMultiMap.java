package com.hourglassapps.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HashArrayMultiMap<K,E> extends AbstractMultiMap<K, List<E>, E> {
	private static final long serialVersionUID = 1L;
	private Map<K, List<E>> mDelegateMap=new HashMap<K, List<E>>();
	
	public HashArrayMultiMap() {
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
	 * @param key
	 * @return
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public List<E> get(Object key) {
		return mDelegateMap.get(key);
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
	 * @return
	 * @see java.util.Map#values()
	 */
	public Collection<List<E>> values() {
		return mDelegateMap.values();
	}
}

