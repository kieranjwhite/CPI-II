package com.hourglassapps.persist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Promiser;
import com.hourglassapps.util.Typed;

public abstract class AbstractFilesJournal<K,C> implements Journal<K, C>, Promiser<Void,Void,Path> {
	private final static String TAG=AbstractFilesJournal.class.getName();
	private final static String PARTIAL_DIR_NAME="partial";
	public final static String COMPLETED_DIR_NAME="completed";
	protected final static int FIRST_FILENAME=0;
	public final static char META_PREFIX='_';
	protected final static String CUSTOM_PREFIX=META_PREFIX+"_";
	
	private final Path mDirectory;
	private final Path mCompletedDir;
	private final Converter<K,String> mFilenameGenerator;
	private final int mFirstFilename;
	private int mFilename;
	protected final Path mPartialDir;
	protected final Trail<C> mTrail;
	private final Deferred<Void,Void,Path> mDeferred=new DeferredObject<>();
	private final Converter<C,Typed<C>> mToTyped;
	
	protected interface PreDeleteAction {
		public void run() throws IOException;
	}
	
	public AbstractFilesJournal(Path pDirectory, Converter<K,String> pFilenameGenerator, Converter<C,Typed<C>> pToTyped) throws IOException {
		this(pDirectory, pFilenameGenerator, pToTyped, FIRST_FILENAME);
	}

	public AbstractFilesJournal(Path pDirectory, Converter<K,String> pFilenameGenerator, 
			Converter<C,Typed<C>> pToTyped, 
			int pFirstFilename) throws IOException {
		mToTyped=pToTyped;
		mFirstFilename=pFirstFilename;
		mDirectory=pDirectory;
		mkdir(mDirectory);
		mPartialDir=partialDir(mDirectory);
		mCompletedDir=mDirectory.resolve(COMPLETED_DIR_NAME);
		mkdir(mCompletedDir);
		mFilenameGenerator=pFilenameGenerator;
		mTrail=new Trail<C>(mPartialDir);
		setupPartial(mPartialDir);
		mFilename=mFirstFilename;
		mTrail.clear();
	}

	public Path path(String pCompletedName) {
		return mCompletedDir.resolve(pCompletedName);
	}
	
	protected static Path partialDir(Path pParent) {
		return pParent.resolve(PARTIAL_DIR_NAME);
	}
		
	private static void setupPartial(Path pPartialDir) throws IOException {
		if(Files.exists(pPartialDir)) {
			deleteFlatDir(pPartialDir);
		}
		mkdir(pPartialDir);
	}
	
	public static void mkdir(Path pDir) throws IOException {
		if(!Files.exists(pDir)) {
			Files.createDirectory(pDir);
		}
	}
	
	public static void deleteFlatDir(Path pDir) throws IOException {
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
	public boolean addedAlready(K pKey) throws IOException {
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
	public final void addNew(C pContent) throws IOException {
		addNew(mToTyped.convert(pContent));
	}
	
	protected abstract void addNew(Typed<C> pContent) throws IOException;

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
		mTrail.save(pDest);
		assert !Files.exists(pDest);
		Files.move(mPartialDir, pDest, StandardCopyOption.ATOMIC_MOVE);
		mDeferred.notify(pDest);
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
		setupPartial(mPartialDir);
		mFilename=mFirstFilename;
		mTrail.clear();
	}

	@Override
	public void close() throws Exception {
		mDeferred.resolve(null);
	}

	@Override
	public Promise<Void, Void, Path> promise() {
		return mDeferred;
	}

}
