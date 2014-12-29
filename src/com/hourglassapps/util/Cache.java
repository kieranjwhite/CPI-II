package com.hourglassapps.util;

import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class Cache<A,B> implements Thrower {
	private final Set<SoftReference<A>> mKeys=new HashSet<>();
	private final Map<A,B> mKeyToCached=new WeakHashMap<>();
	private final Converter<A,B> mCachedCreator;
	
	public Cache(Converter<A,B> pCachedCreator) {
		mCachedCreator=pCachedCreator;
	}
	
	/*
	 */
	/**
	 * Returns a cached item or creates it if it's not already in the cache.
	 * Please ensure than references to pKey do not hang around. The cached item will remain in the cache at least as long of pKey (and references to it) exists.
	 * @param pKey
	 * @return item from cache, or null if that was returned by the Converter when creating the item
	 */
	public B get(A pKey) {
		B cached=mKeyToCached.get(pKey);
		if(cached==null) {
			cached=mCachedCreator.convert(pKey);
			mKeyToCached.put(pKey, cached);
			mKeys.add(new SoftReference<A>(pKey));
		}
		return cached;
	}

	@Override
	public void close() throws Exception {
		try {
			throwCaught(Exception.class);
		} catch(Throwable e) {
			throw (Exception)e;
		}
	}

	@Override
	public <E extends Exception> void throwCaught(Class<E> pCatchable)
			throws Throwable {
	}
}
