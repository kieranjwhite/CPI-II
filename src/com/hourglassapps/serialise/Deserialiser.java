package com.hourglassapps.serialise;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Deserialiser<T> {
	public T restore(File pLoadFrom) throws IOException;
	public T restore(InputStream pIn) throws IOException;
}
