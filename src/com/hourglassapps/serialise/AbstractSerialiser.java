package com.hourglassapps.serialise;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractSerialiser<T> implements Serialiser<T> {

	@Override
	public void save(File pSaveTo) throws IOException {
		try(OutputStream out=new BufferedOutputStream(new FileOutputStream(pSaveTo))) {
			save(out);
		}
	}
}
