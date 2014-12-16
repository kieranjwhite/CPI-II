package com.hourglassapps.persist;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class IndexedVisitor implements FileVisitor<Path> {
	private final static String META_PREFIX=Character.toString(AbstractFilesJournal.META_PREFIX);
	private final List<Path> mPaths=new ArrayList<>();
	
	@Override
	public FileVisitResult postVisitDirectory(Path arg0, IOException arg1)
			throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path arg0,
			BasicFileAttributes arg1) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path arg0, BasicFileAttributes arg1)
			throws IOException {
		if(!Files.isSymbolicLink(arg0) && !arg0.getFileName().toString().startsWith(META_PREFIX)) {
			mPaths.add(arg0);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path arg0, IOException arg1)
			throws IOException {
		return FileVisitResult.CONTINUE;
	}
	
	public List<Path> dsts() {
		return Collections.unmodifiableList(mPaths);
	}
}