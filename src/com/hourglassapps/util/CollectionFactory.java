package com.hourglassapps.util;

import java.util.Collection;

public interface CollectionFactory<C extends Collection<T>, T> {
	public C inst();
	public C inst(Collection<? extends T> els);
	public T removeOne(C pColl);
}

