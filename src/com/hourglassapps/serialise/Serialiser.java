package com.hourglassapps.serialise;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Serialiser<T> {
	public void save(File pSaveTo) throws IOException;
	public void save(OutputStream pOut) throws IOException;
	public T restore(File pLoadFrom) throws IOException;
	public T restore(InputStream pIn) throws IOException;
}
