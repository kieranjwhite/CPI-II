package com.hourglassapps.cpi_ii;

public interface Converter<I, O> {
	public O convert(I pIn);
}
