package com.hourglassapps.persist;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
import com.hourglassapps.util.Promiser;

public class FileJournal<A> implements Journal<String, A>, Promiser<Void,Void,Path> {
	private final static String COMPLETED="completed";
	private final static String PARTIAL="partial";
	protected final Path mCompleted;
	protected final Path mPartialFile;
	private final Path mDir;
	private final List<A> mPartial=new ArrayList<>();
	protected final List<A> mProtectedPartial=Collections.unmodifiableList(mPartial);
	protected final Converter<A,String> mAddedToString;
	private final Deferred<Void,Void,Path> mDeferred=new DeferredObject<>();
	
	public FileJournal(Path pDir, Converter<A,String> pAddedToString) throws IOException {
		mDir=pDir;
		AbstractFilesJournal.mkdir(pDir);
		mCompleted=mDir.resolve(COMPLETED);
		AbstractFilesJournal.mkdir(mCompleted);
		mPartialFile=mDir.resolve(PARTIAL);
		mAddedToString=pAddedToString;
	}

	@Override
	public boolean addedAlready(String pKey) throws IOException {
		return Files.exists(mCompleted.resolve(pKey));
	}

	@Override
	public void addNew(A pContent) throws IOException {
		mPartial.add(pContent);
	}

	protected void clearPartial() {
		mPartial.clear();
	}
	
	@Override
	public void close() {
		mDeferred.resolve(null);
	}

	protected Path partial() {
		return mPartialFile;
	}
	
	protected void tidyUp(String pKey) throws IOException {
		Path dest=mCompleted.resolve(pKey);
		Files.move(mPartialFile, dest, StandardCopyOption.ATOMIC_MOVE);
		mPartial.clear();
		mDeferred.notify(dest);		
	}
	
	@Override
	public void commit(String pKey) throws IOException {
		write();
		tidyUp(pKey);
	}

	@Override
	public Promise<Void, Void, Path> promise() {
		return mDeferred;
	}

	protected PrintWriter writer() throws IOException {
		return new PrintWriter(new BufferedWriter(new FileWriter(mPartialFile.toFile())));
	}
	
	protected void write() throws IOException {
		try(PrintWriter writer=writer()) {
			for(A p: mProtectedPartial) {
				String path=mAddedToString.convert(p);
				writer.println(path);
			}
		}
	}

	@Override
	public void reset() throws IOException {
		AbstractFilesJournal.deleteFlatDir(mCompleted);
		clearPartial();
	}

}
