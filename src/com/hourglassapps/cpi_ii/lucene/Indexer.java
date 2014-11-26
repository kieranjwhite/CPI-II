package com.hourglassapps.cpi_ii.lucene;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

public class Indexer extends IndexViewer implements AutoCloseable {
	private final static String TAG=Indexer.class.getName();
	
	private final IndexWriter mWriter;
	private IndexWriterConfig mIwc;
	
	public Indexer(File pDir, Analyzer pAnalyser, boolean pForceCreate) throws IOException {
		super(pDir);
		mIwc = new IndexWriterConfig(Version.LUCENE_4_10_0, pAnalyser);
        mIwc.setOpenMode(pForceCreate?OpenMode.CREATE:OpenMode.CREATE_OR_APPEND);
        mWriter = new IndexWriter(dir(), mIwc);

	}

	public IndexWriter writer() {
		return mWriter;
	}
	
	public Indexer(File pDir, Analyzer pAnalyser) throws IOException {
		this(pDir, pAnalyser, true);
	}
	
	public Indexer(Path pDir, Analyzer pAnalyzer, boolean pForceCreate) throws IOException {
		this(pDir.toFile(), pAnalyzer, pForceCreate);
	}
	
	public Indexer(Path pDir, Analyzer pAnalyzer) throws IOException {
		this(pDir.toFile(), pAnalyzer, true);
	}
	
	public void add(Field... pFields) throws IOException {
		if(pFields.length==0) {
			return;
		}
		Document doc = new Document();
		for(Field f: pFields) {
			doc.add(f);
		}
        mWriter.addDocument(doc);

	}
	
	@Override
	public void close() throws IOException {
		mWriter.close();
	}

	public void wipe() throws IOException {
		mWriter.deleteAll();
	}
}
