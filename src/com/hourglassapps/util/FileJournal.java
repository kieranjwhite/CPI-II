package com.hourglassapps.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.hourglassapps.cpi_ii.Journal;

public class FileJournal<K,C> implements Journal<K, C> {
	private final static String TAG=FileJournal.class.getName();
	private final static String PARTIAL_DIR_NAME="partial";
	private final static String COMPLETED_DIR_NAME="completed";
	
	private final File mDirectory;
	private final File mPartialDir;
	private final File mCompletedDir;
	
	private final Converter<K,String> mFilenameGenerator;
	private final Converter<C,String> mContentGenerator;
	
	public FileJournal(File pDirectory, Converter<K,String> pFilenameGenerator, Converter<C,String> pContentGenerator) throws IOException {
		mDirectory=pDirectory;
		mkdir(mDirectory);
		mPartialDir=new File(mDirectory, PARTIAL_DIR_NAME);
		mkdir(mPartialDir);
		mCompletedDir=new File(mDirectory, COMPLETED_DIR_NAME);
		mkdir(mCompletedDir);
		mFilenameGenerator=pFilenameGenerator;
		mContentGenerator=pContentGenerator;
	}

	private static void mkdir(File pDir) throws IOException {
		if(!pDir.exists()) {
			if(!pDir.mkdir()) {
				throw new IOException("failed to create directory: "+pDir.getCanonicalPath());
			}
		}
	}
	
	private File destDir(K pKey) {
		return new File(mCompletedDir, mFilenameGenerator.convert(pKey));		
	}
	
	@Override
	public boolean has(K pKey) {
		return destDir(pKey).exists();
	}

	@Override
	public void add(C pContent) {
		String content=mContentGenerator.convert(pContent);
	}

	@Override
	public void commitEntry(K pKey) {
		File dest=destDir(pKey);
		
		startEntry();
	}

	@Override
	public void reset() {
	}

	@Override
	public void startEntry() {
		// TODO Auto-generated method stub
		
	}

}
