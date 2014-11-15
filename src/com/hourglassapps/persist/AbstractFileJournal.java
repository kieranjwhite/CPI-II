package com.hourglassapps.persist;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hourglassapps.cpi_ii.Journal;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Typed;

public abstract class AbstractFileJournal<K,C,S> implements Journal<K, C> {
	private final static String TAG=AbstractFileJournal.class.getName();
	private final static String PARTIAL_DIR_NAME="partial";
	private final static String COMPLETED_DIR_NAME="completed";
	private final static long FIRST_FILENAME=1;
	
	private final Path mDirectory;
	private final Path mPartialDir;
	private final Path mCompletedDir;
	
	private final Converter<K,String> mFilenameGenerator;
	protected final S mContentGenerator;	
	private final long mFirstFilename;
	
	private long mFilename;
	private final List<C> mTrail;

	public AbstractFileJournal(Path pDirectory, Converter<K,String> pFilenameGenerator, S pContentGenerator) throws IOException {
		this(pDirectory, pFilenameGenerator, pContentGenerator, FIRST_FILENAME);
	}

	public AbstractFileJournal(Path pDirectory, Converter<K,String> pFilenameGenerator, 
			S pContentGenerator, long pFirstFilename) throws IOException {
		mFirstFilename=pFirstFilename;
		mDirectory=pDirectory;
		mkdir(mDirectory);
		mPartialDir=mDirectory.resolve(PARTIAL_DIR_NAME);
		mCompletedDir=mDirectory.resolve(COMPLETED_DIR_NAME);
		mkdir(mCompletedDir);
		mFilenameGenerator=pFilenameGenerator;
		mContentGenerator=pContentGenerator;
		mTrail=new ArrayList<C>();
		setupPartial(mPartialDir);
		mFilename=mFirstFilename;
		mTrail.clear();
	}

	protected void trailAdd(C pSource) {
		mTrail.add(pSource);
	}
	
	private static void setupPartial(Path pPartialDir) throws IOException {
		if(Files.exists(pPartialDir)) {
			deleteFlatDir(pPartialDir);
		}
		mkdir(pPartialDir);
	}
	
	private static void mkdir(Path pDir) throws IOException {
		if(!Files.exists(pDir)) {
			Files.createDirectory(pDir);
		}
	}
	
	private static void deleteFlatDir(Path pDir) throws IOException {
	    File[] files = pDir.toFile().listFiles();
	    if(files!=null) {
	        for(File f: files) {
	        	assert !f.isDirectory();
	        	f.delete();
	        }
	    }
	    Files.delete(pDir);

	}
	
	protected Path destDir(K pKey) {
		String displayedKey=mFilenameGenerator.convert(pKey);
		return mCompletedDir.resolve(displayedKey);
	}
	
	@Override
	public boolean has(K pKey) {
		return Files.exists(destDir(pKey));
	}

	protected void incFilename() {
		mFilename++;
	}
	
	protected Path dest(Typed<C> pContent) throws IOException {
		return mPartialDir.resolve(Long.toString(mFilename)+pContent.extension());
	}
	
	@Override
	public abstract void add(Typed<C> pContent) throws IOException;

	protected void saveTrail(Path pDest) throws IOException {
		try(PrintWriter out=
				new PrintWriter(new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(mPartialDir.resolve('_'+pDest.getFileName().toString()).toString()))))) {
			for(C source: mTrail) {
				out.println(source);
			}
		}
	}

	protected void tidyUp(Path pDest) throws IOException {
		if(Files.exists(mPartialDir)) { 
			/*
			 * tidyUp will be invoked more than once for a query if a download times out and
			 * subsequent closing of the HttpAsyncClient results in any pending mPromises 
			 * being resolved. 
			 */
			saveTrail(pDest);
			assert !Files.exists(pDest);
			Files.move(mPartialDir, pDest, StandardCopyOption.ATOMIC_MOVE);
		}
	}
	
	@Override
	public void commitEntry(final K pKey) throws IOException {
		Path dest=destDir(pKey);
		tidyUp(dest);
		startEntry(); //Note this is invoked even if there's an exception
	}

	@Override
	public void reset() throws IOException {
		startEntry();
		assert Files.exists(mCompletedDir);
		File[] files = mCompletedDir.toFile().listFiles();
		if(files!=null) {
			for(File f: files) {
				assert f.isDirectory();
				deleteFlatDir(f.toPath());
			}
		}
	}

	@Override
	public void startEntry() throws IOException {
		setupPartial(mPartialDir);
		mFilename=mFirstFilename;
		mTrail.clear();
	}

}