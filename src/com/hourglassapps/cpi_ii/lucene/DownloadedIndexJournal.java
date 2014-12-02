package com.hourglassapps.cpi_ii.lucene;

import static java.util.EnumSet.noneOf;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;

import com.hourglassapps.cpi_ii.TextContentReaderFactory;

class DownloadedIndexJournal extends AbstractLuceneJournal<Path> {
	private final int mTid;
	
	public DownloadedIndexJournal(Indexer pIndexer, int pTid) throws IOException {
		super(pIndexer, (POSITION_TAG+'_')+pTid);
		mTid=pTid;
	}
	
	@Override
	public void addNew(Path pDir) throws IOException {
		FileVisitor<Path> visitor=new LuceneVisitor(indexer(), new TextContentReaderFactory(false));
		Files.walkFileTree(pDir, noneOf(FileVisitOption.class), 3, visitor);
	}
	
	@Override
	public void reset() {
		throw new UnsupportedOperationException();
	}
}