package com.hourglassapps.util;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Rtu;

public class FileWrapper implements AutoCloseable {
	private final Path mPath;
	private PrintWriter mOut;
	private final Class<?> mClient;
	private final String mEnd;
	
	public FileWrapper(Class<?> pClient, String pStart, String pEnd, Path pDest) throws IOException {
		mClient=pClient;
		mEnd=pEnd;
		
		mPath=pDest;
		if(Files.exists(mPath)) {
			Files.delete(mPath);
		}
		OutputStream outStream=new BufferedOutputStream(new FileOutputStream(mPath.toFile()));
		try(InputStream in=mClient.getResourceAsStream(pStart)) {
			Rtu.copyFile(in, outStream);
		}
		mOut=new PrintWriter(outStream);
	}

	public PrintWriter writer() {
		return mOut;
	}
	
	private OutputStream copyInto(String pSrc) throws IOException {
		OutputStream outStream=new BufferedOutputStream(new FileOutputStream(mPath.toFile(), true));
		try(InputStream in=mClient.getResourceAsStream(pSrc)) {
			Rtu.copyFile(in, outStream);
		}
		return outStream;
	}
	
	public PrintWriter insert(String pMid) throws IOException {
		mOut.close();
		OutputStream out=copyInto(pMid);
		mOut=new PrintWriter(out);
		return mOut;
	}
	
	@Override
	public void close() throws IOException {
		mOut.close();
		try(OutputStream out=copyInto(mEnd)) {}
	}
}
