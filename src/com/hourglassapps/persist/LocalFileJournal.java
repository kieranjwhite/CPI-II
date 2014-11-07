package com.hourglassapps.persist;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

import com.hourglassapps.util.Converter;

public class LocalFileJournal<K,C> extends FileJournal<K,C,ReadableByteChannel> {

	public LocalFileJournal(Path pDirectory,
			Converter<K, String> pFilenameGenerator,
			Converter<C, ReadableByteChannel> pContentGenerator)
			throws IOException {
		super(pDirectory, pFilenameGenerator, pContentGenerator);
	}
}
