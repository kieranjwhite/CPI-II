package com.hourglassapps.util;

import com.google.common.cache.CacheBuilder;

public class Cache<A extends Comparable<A>,B> implements Thrower {
	private final com.google.common.cache.Cache<A,B> mCache;
	private final Converter<A,B> mCachedCreator;
	
	public Cache(Converter<A,B> pCachedCreator, int pCacheSize) {
		mCache=CacheBuilder.newBuilder().maximumSize(pCacheSize).build();
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
		B cached=mCache.getIfPresent(pKey);
		if(cached==null) {
			cached=mCachedCreator.convert(pKey);
			mCache.put(pKey, cached);
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
