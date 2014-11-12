package com.hourglassapps.cpi_ii.web_search;

public interface Query<K,R> {
	public K uniqueName();
	public boolean empty();
	public R raw();
}
