package com.hourglassapps.util;


public class IdentityConverter<I> implements Converter<I, I> {
	@Override
	public I convert(I pIn) {
		return pIn;
	}

}
