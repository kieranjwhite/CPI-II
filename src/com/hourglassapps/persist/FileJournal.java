package com.hourglassapps.persist;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Promiser;

public class FileJournal<A> implements Journal<String, A>, Promiser<Void,Void,Path> {
	private final static String COMPLETED="completed";
	private final static String PARTIAL="partial";
	private final Path mCompleted;
	private final Path mPartialFile;
	private final Path mDir;
	private final List<A> mPartial=new ArrayList<>();
	private final Converter<A,String> mAddedToString;
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

	@Override
	public void reset() throws IOException {
		AbstractFilesJournal.deleteFlatDir(mCompleted);
		mPartial.clear();
	}

	@Override
	public void close() {
		mDeferred.resolve(null);
	}

	@Override
	public void commit(String pKey) throws IOException {
		Path dest=mCompleted.resolve(pKey);
		try(PrintWriter writer=new PrintWriter(new BufferedWriter(new FileWriter(mPartialFile.toFile())))) {
			for(A p: mPartial) {
				String path=mAddedToString.convert(p);
				writer.println(path);
			}
		}
		Files.move(mPartialFile, dest, StandardCopyOption.ATOMIC_MOVE);
		mPartial.clear();
		mDeferred.notify(dest);
	}

	@Override
	public Promise<Void, Void, Path> promise() {
		return mDeferred;
	}

}
