package com.hourglassapps.cpi_ii.web_search;

public interface Query<K,R> {
	public K uniqueName();
	public boolean done();
	public R raw();
}
