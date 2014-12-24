package com.hourglassapps.persist;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.InputStreamFactory;
import com.hourglassapps.util.Promiser;
import com.hourglassapps.util.Rtu;

public class FileCopyJournal implements Journal<Path, Void>, Promiser<Void,Void,Path> {
	private final static String COMPLETED="completed";
	private final static String PARTIAL="partial";
	protected final Path mCompleted;
	protected final Path mPartialFile;
	private final Path mDir;
	protected final Converter<Path,String> mSourceToDestFilename;
	private final Deferred<Void,Void,Path> mDeferred=new DeferredObject<>();
	private final InputStreamFactory mStreamFact;
	
	public FileCopyJournal(Path pDir, Converter<Path,String> pAddedToString, InputStreamFactory pStreamFact) throws IOException {
		mStreamFact=pStreamFact;
		mDir=pDir;
		AbstractFilesJournal.mkdir(pDir);
		mCompleted=mDir.resolve(COMPLETED);
		AbstractFilesJournal.mkdir(mCompleted);
		mPartialFile=mDir.resolve(PARTIAL);
		mSourceToDestFilename=pAddedToString;
	}

	@Override
	public boolean addedAlready(Path pKey) throws IOException {
		String basename=mSourceToDestFilename.convert(pKey);
		if(basename==null) {
			return true; //shouldn't happen but if it does reporting it is someone else's responsibility
		}
		return Files.exists(mCompleted.resolve(basename));
	}

	@Override
	public void addNew(Void pContent) {}

	@Override
	public void close() {
		mDeferred.resolve(null);
	}

	protected Path partial() {
		return mPartialFile;
	}
	
	@Override
	public void commit(Path pKey) throws IOException {
		String basename=mSourceToDestFilename.convert(pKey);
		if(basename==null) {
			return; //shouldn't happen but if it does reporting it is someone else's responsibility
		}
		Path dest=mCompleted.resolve(basename);
		try(InputStream in=mStreamFact.wrap(new FileInputStream(pKey.toFile()))) {
			Rtu.copyFile(in, mPartialFile);
		}
		Files.move(mPartialFile, dest, StandardCopyOption.ATOMIC_MOVE);
		mDeferred.notify(dest);		
	}
	
	@Override
	public Promise<Void, Void, Path> promise() {
		return mDeferred;
	}

	protected PrintWriter writer() throws IOException {
		return new PrintWriter(new BufferedWriter(new FileWriter(mPartialFile.toFile())));
	}
	
	@Override
	public void reset() throws IOException {
		AbstractFilesJournal.deleteFlatDir(mCompleted);
	}

}
