package com.hourglassapps.util;

import java.io.IOException;

public interface IOIterator<R> extends ThrowableIterator<R> {
	@Override
	public void close() throws IOException;
}