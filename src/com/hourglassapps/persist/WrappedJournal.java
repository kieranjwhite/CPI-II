package com.hourglassapps.persist;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.FileWrapper;

public class WrappedJournal extends FileJournal<Path> {
	private final Class<?> mTemplateClass;
	private final String mStart;
	private final String mEnd;
	
	private FileWrapper mWrapper;
	
	public WrappedJournal(Path pDir, final Converter<Path, String> pAddedToString, 
			Class<?> pTemplateClass, String pStart, String pEnd)
			throws IOException {
		super(pDir, pAddedToString);
		mTemplateClass=pTemplateClass;
		mStart=pStart;
		mEnd=pEnd;
	}

	@Override 
	protected PrintWriter writer() {
		return mWrapper.writer();
	}
	
	@Override
	public void commit(String pKey) throws IOException {
		try(FileWrapper wrapper=new FileWrapper(mTemplateClass, mStart, mEnd, partial())) {
			mWrapper=wrapper;
			write();
		}
		tidyUp(pKey);
	}
}
