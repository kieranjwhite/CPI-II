package com.hourglassapps.cpi_ii;

public interface Record<I,C> {
	public I id();
	public C content();
	public boolean ignore();
}
