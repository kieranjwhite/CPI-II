package com.hourglassapps.cpi_ii.lucene;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class IndexViewer implements AutoCloseable {
	private final static String TAG=IndexViewer.class.getName();
	
	private Directory mDir;
	
	public IndexViewer(Path pDir) throws IOException {
		mDir=FSDirectory.open(pDir.toFile());
	}
	
	public IndexViewer(File pDir) throws IOException {
		mDir=FSDirectory.open(pDir);
	}
	
	public Directory dir() {
		return mDir;
	}
	
	public void visit(FieldVal pField, TermHandler pConsumer) throws IOException {
		try(AtomicReader reader=AtomicReader.open(dir()).leaves().get(0).reader()) {
			Fields fields=reader.fields();
			if(fields!=null) {
				Terms terms=fields.terms(pField.s());
				if(terms!=null) {
					TermsEnum e=terms.iterator(null);
					pConsumer.run(e);
				}
			}
		}

	}

	public void interrogate(FieldVal pSearchField, String pKey, int pNumResults, ResultRelayer pRelayer) throws IOException {
		try(IndexReader reader=DirectoryReader.open(dir())) {
			interrogate(reader, pSearchField, pKey, pNumResults, pRelayer);
		}
	}

	public void interrogate(FieldVal pSearchField, String pSought, ResultRelayer pRelayer) throws IOException {
		try(IndexReader reader=DirectoryReader.open(dir())) {
			/*
			for (int i=0; i<reader.maxDoc(); i++) {
			    Document doc = reader.document(i);
			    System.out.println("doc: "+doc.get(pKeyField.s()));
			}
			*/
			interrogate(reader, pSearchField, pSought, reader.numDocs(), pRelayer);
		}
	}

	public void interrogate(IndexReader pReader, FieldVal pSearchField, String pSought, int pNumResults, ResultRelayer pRelayer) throws IOException {
		IndexSearcher searcher = new IndexSearcher(pReader);
		interrogate(pReader, searcher, pSearchField, pSought, pNumResults, pRelayer);
	}

	public static void interrogate(IndexReader pReader, IndexSearcher pSearcher, FieldVal pSearchField, String pSought, int pNumResults, ResultRelayer pRelayer) throws IOException {
		interrogate(pReader, pSearcher, pSearchField.query(pSought), pNumResults, pRelayer);
	}

	public static void interrogate(IndexReader pReader, IndexSearcher pSearcher, Query pQuery, int pNumResults, ResultRelayer pRelayer) throws IOException {
		TopDocs results = pSearcher.search(pQuery, Math.max(pNumResults,1));
		if(results.totalHits<1) {
			//Log.i(TAG, "unrecognised key "+pSought);
			return;
		}
		pRelayer.run(pReader, results);
		
	}

	@Override
	public void close() throws IOException {
		mDir.close();
	}


}
