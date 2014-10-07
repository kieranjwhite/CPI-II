package com.hourglassapps.cpi_ii;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.hourglassapps.util.Log;

public class Indexer {
	private final static String TAG=Indexer.class.getName();
	
	private final static String ID_KEY="eprintid";
	private final static String CONTENT_KEY="content";
	
	private IndexWriter mWriter=null;
	private Directory mDir;
	private IndexWriterConfig mIwc;
	
	public Indexer(File index_dir) throws IOException {
		if(!new File(System.getProperty("user.dir")).canWrite()) {
			Log.e(TAG, "cannot write to index dir's parent: "+System.getProperty("user.dir"));
			System.exit(1);
			
		}
		
		mDir = FSDirectory.open(index_dir);
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_10_0);
		mIwc = new IndexWriterConfig(Version.LUCENE_4_10_0, analyzer);
        mIwc.setOpenMode(OpenMode.CREATE);
        mWriter = new IndexWriter(mDir, mIwc);

	}
	
	public void add(long eprint_id, String content) throws IOException {
		if(mWriter==null) {
			mWriter=new IndexWriter(mDir, mIwc);
		}
        Document doc = new Document();

        Field idField = new LongField(ID_KEY, eprint_id, Field.Store.YES);
        doc.add(idField);
        doc.add(new TextField(CONTENT_KEY, content, TextField.Store.NO));
        mWriter.addDocument(doc);

	}
	
	public void close() throws IOException {
		if(mWriter!=null) {
			mWriter.close();
		}
	}
}
