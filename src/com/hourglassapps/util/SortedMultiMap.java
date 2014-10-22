package com.hourglassapps.util;

import java.util.Collection;
import java.util.SortedMap;

public interface SortedMultiMap<K extends Comparable<K>, C extends Collection<E>, E> extends SortedMap<K, C>, MultiMap<K, C, E> {
}
