package com.hourglassapps.persist;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
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

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Typed;

public abstract class AbstractFileJournal<K,C,S> implements Journal<K, Typed<C>> {
	private final static String TAG=AbstractFileJournal.class.getName();
	private final static String PARTIAL_DIR_NAME="partial";
	private final static String COMPLETED_DIR_NAME="completed";
	protected final static int FIRST_FILENAME=1;
	private final static char META_PREFIX='_';
	protected final static String CUSTOM_PREFIX=META_PREFIX+"_";
	
	private final Path mDirectory;
	private final Path mCompletedDir;
	private final Converter<K,String> mFilenameGenerator;
	private final int mFirstFilename;
	private final List<C> mTrail;
	private int mFilename;
	private final PreDeleteAction mPreDelete;
	protected final Path mPartialDir;
	protected final S mContentGenerator;	
	
	protected interface PreDeleteAction {
		public void run() throws IOException;
	}
	
	public AbstractFileJournal(Path pDirectory, Converter<K,String> pFilenameGenerator, S pContentGenerator) throws IOException {
		this(pDirectory, pFilenameGenerator, pContentGenerator, FIRST_FILENAME);
	}

	public AbstractFileJournal(Path pDirectory, Converter<K,String> pFilenameGenerator, 
			S pContentGenerator, int pFirstFilename) throws IOException {
		this(pDirectory, pFilenameGenerator, pContentGenerator, pFirstFilename, new PreDeleteAction(
				){
					@Override
					public void run() {
					}});
	}
	
	public AbstractFileJournal(Path pDirectory, Converter<K,String> pFilenameGenerator, 
			S pContentGenerator, int pFirstFilename, PreDeleteAction pPreDelete) throws IOException {
		mFirstFilename=pFirstFilename;
		mDirectory=pDirectory;
		mkdir(mDirectory);
		mPartialDir=partialDir(mDirectory);
		mCompletedDir=mDirectory.resolve(COMPLETED_DIR_NAME);
		mkdir(mCompletedDir);
		mFilenameGenerator=pFilenameGenerator;
		mContentGenerator=pContentGenerator;
		mTrail=new ArrayList<C>();
		mPreDelete=pPreDelete;
		mPreDelete.run();
		setupPartial(mPartialDir);
		mFilename=mFirstFilename;
		mTrail.clear();
	}

	protected static Path partialDir(Path pParent) {
		return pParent.resolve(PARTIAL_DIR_NAME);
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
	
	protected static void deleteFlatDir(Path pDir) throws IOException {
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
	public boolean addExisting(K pKey) {
		return Files.exists(destDir(pKey));
	}

	protected int filename() {
		return mFilename;
	}
	
	public Path dest(Typed<C> pContent) throws IOException {
		return mPartialDir.resolve(Long.toString(filename())+pContent.extension());
	}
	
	protected void incFilename() {
		mFilename++;
	}
	
	@Override
	public abstract void addNew(Typed<C> pContent) throws IOException;

	protected void saveTrail(Path pDest) throws IOException {
		try(PrintWriter out=
				new PrintWriter(new BufferedWriter(
						new FileWriter(mPartialDir.resolve(META_PREFIX+pDest.getFileName().toString()).toString())))) {
			for(C source: mTrail) {
				out.println(source);
			}
		}
	}

	protected void tryTidy(Path pDest) throws IOException {
		if(Files.exists(mPartialDir)) {
			/*
			 * tidyUp will be invoked more than once for a query if a download times out and
			 * subsequent closing of the HttpAsyncClient results in any pending mPromises 
			 * being resolved. 
			 */
			tidyUp(pDest);
		}		
	}
	
	protected void tidyUp(Path pDest) throws IOException {
		saveTrail(pDest);
		assert !Files.exists(pDest);
		Files.move(mPartialDir, pDest, StandardCopyOption.ATOMIC_MOVE);
	}
	
	@Override
	public void commit(final K pKey) throws IOException {
		Path dest=destDir(pKey);
		tryTidy(dest);
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

	protected void startEntry() throws IOException {
		mPreDelete.run();
		setupPartial(mPartialDir);
		mFilename=mFirstFilename;
		mTrail.clear();
	}

}
