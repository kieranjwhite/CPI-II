package com.hourglassapps.util;

public interface Converter<I, O> {
	public O convert(I pIn);
}
