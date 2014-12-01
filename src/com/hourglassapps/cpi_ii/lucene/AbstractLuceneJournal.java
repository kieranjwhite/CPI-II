package com.hourglassapps.cpi_ii.lucene;

import java.io.IOException;
import java.util.Comparator;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;

import com.hourglassapps.persist.Journal;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;

public abstract class AbstractLuceneJournal<C> implements Journal<String, C> {
	private final static String TAG=AbstractLuceneJournal.class.getName();
	private final Indexer mIndexer;
	public final FieldVal DOC_TAG=LuceneTransactionFields.DOC_TAG.fieldVal();
	public final FieldVal KEY=LuceneTransactionFields.KEY.fieldVal();
	public final static String POSITION_TAG="position";
	private final IndexReader mReader;
	private final IndexWriter mWriter;
	
	public enum LuceneTransactionFields {
		DOC_TAG(new FieldVal("tag", false)),
		KEY(new FieldVal("key", false));
		
		private final FieldVal mField;
		
		private LuceneTransactionFields(FieldVal pField) {
			mField=pField;
		}
		
		public FieldVal fieldVal() {
			return mField;
		}
	}

	public AbstractLuceneJournal(Indexer pIndexer) throws IOException {
		if(pIndexer.writer().getConfig().getOpenMode()!=OpenMode.CREATE) {
			throw new IllegalArgumentException("Index should be opened in create mode");
		}
		mIndexer=pIndexer;
		mWriter=mIndexer.writer();
		mReader=DirectoryReader.open(pIndexer.dir());
	}

	@Override
	public void close() throws IOException {
		mReader.close();
		mWriter.rollback();
	}
	
	@Override
	public boolean addExisting(final String pKey) throws IOException {
		ResultGenerator<String> resGen=new ResultGenerator<String>() {
			private String mFound=null;

			@Override
			public void run(IndexReader pReader, TopDocs pResults)
					throws IOException {
				if(pReader.numDocs()>0) {
					Document doc=pReader.document(pResults.scoreDocs[0].doc);
					mFound=doc.get(KEY.s());
				}
			}

			@Override
			public String result() {
				return mFound;
			}

		};
		mIndexer.interrogate(mReader, DOC_TAG, POSITION_TAG, 1, resGen);
		if(resGen.result()==null) {
			return false;
		}
		return resGen.result().compareTo(pKey)>=0;
	}

	@Override
	public void commit(String pKey) throws IOException {
		Document marker=new Document();
		marker.add(DOC_TAG.field(POSITION_TAG));
		marker.add(KEY.field(pKey));
		mWriter.updateDocument(DOC_TAG.term(POSITION_TAG), marker);
		mWriter.addDocument(marker);
		mWriter.commit();
	}

	@Override
	public void reset() throws IOException {
		mWriter.deleteAll();
	}

}
