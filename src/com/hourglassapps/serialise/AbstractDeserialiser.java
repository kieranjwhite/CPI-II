package com.hourglassapps.serialise;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class AbstractDeserialiser<T> implements Deserialiser<T> {

	@Override
	public T restore(File pLoadFrom) throws IOException {
		try(InputStream in=new BufferedInputStream(new FileInputStream(pLoadFrom))) {
			return restore(in);			
		}
	}

}
