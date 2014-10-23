package com.hourglassapps.cpi_ii;

public class IdentityConverter<I> implements Converter<I, I> {
	@Override
	public I convert(I pIn) {
		return pIn;
	}

}
