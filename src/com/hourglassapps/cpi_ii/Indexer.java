package com.hourglassapps.cpi_ii;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.util.Version;

public class Indexer implements AutoCloseable {
	private final static String TAG=Indexer.class.getName();
	
	private IndexWriter mWriter=null;
	private IndexWriterConfig mIwc;
	private ConductusIndex mIndex;
	
	public Indexer(ConductusIndex index) throws IOException {
		mIndex=index;
		Analyzer analyzer =index.nGramAnalyser();
		mIwc = new IndexWriterConfig(Version.LUCENE_4_10_0, analyzer);
        mIwc.setOpenMode(OpenMode.CREATE);
        mWriter = new IndexWriter(mIndex.dir(), mIwc);

	}
	
	public void add(String pKey, String pContent) throws IOException {
		Document doc = new Document();

        Field idField=FieldVal.KEY.field(pKey);
        doc.add(idField);
        doc.add(FieldVal.CONTENT.field(pContent));
        mWriter.addDocument(doc);

	}
	
	@Override
	public void close() throws IOException {
		if(mWriter!=null) {
			mWriter.close();
		}
	}
	
	public boolean storeStems(OutputStream pSave) throws IOException {
		return mIndex.storeStems(pSave);
	}
	
	public Set<String> tokenExpansions(String pToken) {
		return mIndex.tokenExpansions(pToken);
	}
	
	public boolean displayStemGroups() {
		return mIndex.displayStemGroups();
	}
	
}
