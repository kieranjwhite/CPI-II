package com.hourglassapps.util;

import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.map.MultiValueMap;

public class MultiDistinctValueMap<K,V> extends MultiValueMap<K, V> {
	private static final long serialVersionUID = 1L;

	public MultiDistinctValueMap() {
		super((new HashMap<K, HashSet<V>>()),
				new Factory<HashSet<V>>(){

			@Override
			public HashSet<V> create() {
				return new HashSet<V>();
			}

		});
	}
}