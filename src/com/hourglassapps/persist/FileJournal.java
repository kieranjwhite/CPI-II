package com.hourglassapps.persist;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Typed;

public class FileJournal<K,C,S extends ReadableByteChannel> extends AbstractFileJournal<K,C,Converter<C,S>> {
	private final static String TAG=FileJournal.class.getName();
	public FileJournal(Path pDirectory, Converter<K,String> pFilenameGenerator, Converter<C,S> pContentGenerator) 
			throws IOException {
		super(pDirectory, pFilenameGenerator, pContentGenerator);
	}

	private S source(Typed<C> pContent) {
		C source=pContent.get();
		trailAdd(source);
		return mContentGenerator.convert(source);
	}
	
	@Override
	public void add(Typed<C> pContent) throws IOException {
		S channel=source(pContent);
		if(channel instanceof SelectableChannel && ((SelectableChannel)channel).isBlocking()) {
			throw new IOException("channel must be blocking");
		}
		if(channel==null) {
			incFilename();
			return;
		}
		Path dest=dest(pContent);
		incFilename();
		try(FileChannel out=FileChannel.open(dest, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
			out.transferFrom(channel, 0, Long.MAX_VALUE);
		}
	}
}
