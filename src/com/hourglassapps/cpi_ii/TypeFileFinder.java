package com.hourglassapps.cpi_ii;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import com.hourglassapps.persist.AbstractFileJournal;
import com.hourglassapps.persist.DeferredFileJournal;

public class TypeFileFinder implements FileVisitor<Path> {
	private Path mTypesFile=null;
	
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
	public FileVisitResult visitFile(Path pFile, BasicFileAttributes arg1)
			throws IOException {
		if(pFile!=null && pFile.getFileName().toString().startsWith(DeferredFileJournal.TYPES_FILENAME)) {
			mTypesFile=pFile;
			return FileVisitResult.TERMINATE;
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path arg0, IOException arg1)
			throws IOException {
		return FileVisitResult.CONTINUE;
	}

	public Path types() {
		return mTypesFile;
	}
	
}