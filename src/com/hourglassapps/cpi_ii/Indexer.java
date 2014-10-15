package com.hourglassapps.cpi_ii;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

public class Indexer implements AutoCloseable {
	private final static String TAG=Indexer.class.getName();
	
	private IndexWriter mWriter=null;
	private Directory mDir;
	private IndexWriterConfig mIwc;
	private ConductusIndex mIndex;
	
	public Indexer(ConductusIndex index) throws IOException {
		mIndex=index;
		mDir =index.dir();
		Analyzer analyzer =index.analyzer();
		mIwc = new IndexWriterConfig(Version.LUCENE_4_10_0, analyzer);
        mIwc.setOpenMode(OpenMode.CREATE);
        mWriter = new IndexWriter(mDir, mIwc);

	}
	
	public void add(long pEprintId, String pContent) throws IOException {
		if(mWriter==null) {
			mWriter=new IndexWriter(mDir, mIwc);
		}
        Document doc = new Document();

        Field idField =mIndex.eprintIdField(pEprintId);
        doc.add(idField);
        doc.add(mIndex.contentField(pContent));
        mWriter.addDocument(doc);

	}
	
	@Override
	public void close() throws IOException {
		if(mWriter!=null) {
			mWriter.close();
		}
	}
	
	public boolean storeStems(File pSaveFile) throws IOException {
		return mIndex.storeStems(pSaveFile);
	}
	
	public boolean displayStemGroups() {
		return mIndex.displayStemGroups();
	}
	
}
