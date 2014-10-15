package com.hourglassapps.serialise;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class AbstractSerialiser<T> implements Serialiser<T> {

	@Override
	public void save(File pSaveTo) throws IOException {
		try(OutputStream out=new BufferedOutputStream(new FileOutputStream(pSaveTo))) {
			save(out);
		}
	}

	@Override
	public T restore(File pLoadFrom) throws IOException {
		try(InputStream in=new BufferedInputStream(new FileInputStream(pLoadFrom))) {
			return restore(in);			
		}
	}

}
