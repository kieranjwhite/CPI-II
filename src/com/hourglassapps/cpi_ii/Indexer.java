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
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

public class Indexer extends IndexViewer implements AutoCloseable {
	private final static String TAG=Indexer.class.getName();
	
	private IndexWriter mWriter=null;
	private IndexWriterConfig mIwc;
	
	private static class CustomisedFieldVal extends FieldVal {

		CustomisedFieldVal(FieldVal pField, boolean pTokenise) {
			super(pField.s(), pTokenise);
		}
		
	}
	
	public final FieldVal mKeyField;
	public final FieldVal mContentField;
	
	public Indexer(File pDir, Analyzer pAnalyser, boolean pTokeniseKey, boolean pTokeniseContent) throws IOException {
		super(pDir);
		mIwc = new IndexWriterConfig(Version.LUCENE_4_10_0, pAnalyser);
        mIwc.setOpenMode(OpenMode.CREATE);
        mWriter = new IndexWriter(dir(), mIwc);

		mKeyField=new CustomisedFieldVal(FieldVal.KEY, pTokeniseKey);
		mContentField=new CustomisedFieldVal(FieldVal.CONTENT, pTokeniseContent);
	}
	
	public Indexer(File pDir, Analyzer pAnalyser) throws IOException {
		this(pDir, pAnalyser, true, true);
	}
	
	public void add(String pKey, String pContent) throws IOException {
		Document doc = new Document();

        Field idField=mKeyField.field(pKey);
        doc.add(idField);
        doc.add(mContentField.field(pContent));
        mWriter.addDocument(doc);

	}
	
	@Override
	public void close() throws IOException {
		if(mWriter!=null) {
			mWriter.close();
		}
	}
}
