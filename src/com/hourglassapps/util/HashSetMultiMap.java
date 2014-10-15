package com.hourglassapps.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class HashSetMultiMap<K,E> extends AbstractMultiMap<K, Set<E>, E> {
	private static final long serialVersionUID = 1L;
	private Map<K, Set<E>> mDelegateMap=new HashMap<K, Set<E>>();
	
	public HashSetMultiMap() {
		super(HashSetMultiMap.<E>genFact());
	}

	public HashSetMultiMap(MultiMap<? extends K, ? extends Set<E>, ? extends E> pOrig) {
		this();
		mergeAll(pOrig);
	}
	
	private static <E> CollectionFactory<Set<E>, E> genFact() {
		return new CollectionFactory<Set<E>, E>() {
			@Override
			public Set<E> inst() {
				return new HashSet<E>();
			}

			@Override
			public Set<E> inst(Collection<? extends E> els) {
				return new HashSet<E>(els);
			}

			@Override
			public E removeOne(Set<E> pColl) {
				E removed;
				Iterator<E> it=pColl.iterator();
				while(it.hasNext()) {
					removed=it.next();
					it.remove();
					return removed;
				}
				return null;
			}
			
		};
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
	public Set<java.util.Map.Entry<K, Set<E>>> entrySet() {
		return mDelegateMap.entrySet();
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public Set<E> get(Object key) {
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
	public Set<E> put(K key, Set<E> value) {
		return mDelegateMap.put(key, value);
	}

	/**
	 * @param arg0
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map<? extends K, ? extends Set<E>> arg0) {
		mDelegateMap.putAll(arg0);
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Set<E> remove(Object key) {
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
	public Collection<Set<E>> values() {
		return mDelegateMap.values();
	}

}

