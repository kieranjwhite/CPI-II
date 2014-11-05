package com.hourglassapps.util;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import com.hourglassapps.cpi_ii.Journal;

public class FileJournal<K,C> implements Journal<K, C> {
	private final static String TAG=FileJournal.class.getName();
	private final static String PARTIAL_DIR_NAME="partial";
	private final static String COMPLETED_DIR_NAME="completed";
	private final static long FIRST_FILENAME=1;
	
	private final File mDirectory;
	private final File mPartialDir;
	private final File mCompletedDir;
	
	private final Converter<K,String> mFilenameGenerator;
	private final Converter<C,ReadableByteChannel> mContentGenerator;
	
	private long mFilename=FIRST_FILENAME;
	private final List<C> mTrail=new ArrayList<C>();
	
	public FileJournal(File pDirectory, Converter<K,String> pFilenameGenerator, Converter<C,ReadableByteChannel> pContentGenerator) throws IOException {
		mDirectory=pDirectory;
		mkdir(mDirectory);
		mPartialDir=new File(mDirectory, PARTIAL_DIR_NAME);
		setupPartial(mPartialDir);
		mCompletedDir=new File(mDirectory, COMPLETED_DIR_NAME);
		mkdir(mCompletedDir);
		mFilenameGenerator=pFilenameGenerator;
		mContentGenerator=pContentGenerator;
	}

	private static void setupPartial(File pPartialDir) throws IOException {
		if(pPartialDir.exists()) {
			deleteFlatDir(pPartialDir);
		}
		mkdir(pPartialDir);
	}
	
	private static void mkdir(File pDir) throws IOException {
		if(!pDir.exists()) {
			if(!pDir.mkdir()) {
				throw new IOException("failed to create directory: "+pDir.getCanonicalPath());
			}
		}
	}
	
	private static void deleteFlatDir(File pDir) {
	    File[] files = pDir.listFiles();
	    if(files!=null) {
	        for(File f: files) {
	        	assert !f.isDirectory();
	        	f.delete();
	        }
	    }
	    pDir.delete();

	}
	
	private File destDir(K pKey) {
		String displayedKey=mFilenameGenerator.convert(pKey);
		return new File(mCompletedDir, displayedKey);
	}
	
	@Override
	public boolean has(K pKey) {
		return destDir(pKey).exists();
	}

	@Override
	public void add(Typed<C> pContent) throws IOException {
		C source=pContent.get();
		mTrail.add(source);
		ReadableByteChannel channel=mContentGenerator.convert(source);
		if(channel==null) {
			mFilename++;
			return;
		}
		File dest=new File(mPartialDir, Long.toString(mFilename++)+pContent.extension());
		assert !dest.exists();
		try(FileChannel out=FileChannel.open(dest.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
			out.transferFrom(channel, 0, Long.MAX_VALUE);
		}
	}

	private void saveTrail(File pDest) throws IOException {
		try(PrintWriter out=new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(mPartialDir, '_'+pDest.getName())))))) {
			for(C source: mTrail) {
				out.println(source);
			}
		}
	}
	
	@Override
	public void commitEntry(K pKey) throws IOException {
		File dest=destDir(pKey);
		saveTrail(dest);
		assert !dest.exists();
		Files.move(mPartialDir.toPath(), dest.toPath(), StandardCopyOption.ATOMIC_MOVE);
		startEntry();
	}

	@Override
	public void reset() throws IOException {
		startEntry();
		assert mCompletedDir.exists();
		File[] files = mCompletedDir.listFiles();
		if(files!=null) {
			for(File f: files) {
				assert f.isDirectory();
				deleteFlatDir(f);
			}
		}
	}

	@Override
	public void startEntry() throws IOException {
		setupPartial(mPartialDir);
		mFilename=FIRST_FILENAME;
		mTrail.clear();
	}

}
