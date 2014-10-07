package com.hourglassapps.serialise;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class Preprocessor {
	protected Reader mInput;

	public Preprocessor(File pFile) throws IOException {
		mInput=new BufferedReader(new FileReader(pFile));
	}
	
	public Reader reader() {
		return mInput;
	}
	
	public void close() throws IOException {
		mInput.close();
	}
		
}