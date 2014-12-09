package com.hourglassapps.cpi_ii.report;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import com.hourglassapps.cpi_ii.lucene.IndexViewer;
import com.hourglassapps.cpi_ii.lucene.LuceneVisitor;
import com.hourglassapps.cpi_ii.lucene.ResultRelayer;
import com.hourglassapps.persist.Journal;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.URLUtils;

public class Queryer implements AutoCloseable {
	private final static String TAG=Queryer.class.getName();
	private final static int MAX_RESULTS=100;
	private final Journal<String,Path> mJournal;
	private final Analyzer mAnalyser;
	private final Deferred<Void,Exception,Ii<String,String>> mDeferred=new DeferredObject<>();
    private final QueryParser mParser;
    private final IndexReader mReader;
	private final IndexSearcher mSearcher;
	
	public Queryer(Journal<String,Path> pJournal, IndexViewer pIndex, Analyzer pAnalyser) throws IOException {
		mJournal=pJournal;
		mAnalyser=pAnalyser;
		mParser=new QueryParser(Version.LUCENE_4_10_0, LuceneVisitor.CONTENT.s(), pAnalyser);
		mReader=DirectoryReader.open(pIndex.dir());
	    mSearcher = new IndexSearcher(mReader);
	}
	
	public Promise<Void,Exception,Ii<String,String>> promise() {
		return mDeferred;
	}
	
	private String key(String pDst) {
		return pDst+".js";
	}
	
	public void search(Ii<String,String> pQueryDst) throws ParseException, IOException {
		if(mJournal.addedAlready(key(pQueryDst.snd())) || "".equals(pQueryDst.fst())) {
			Log.i(TAG, "found: "+Log.esc(pQueryDst));
			return;
		}
		try {
			Query q=mParser.parse(pQueryDst.fst());
			IndexViewer.interrogate(mReader, mSearcher, q, MAX_RESULTS, new ResultRelayer() {

				@Override
				public void run(IndexReader pReader, TopDocs pResults)
						throws IOException {

					ScoreDoc[] results=pResults.scoreDocs;
					for(int i=0; i<results.length; i++) {
						Document doc = mSearcher.doc(results[i].doc);
						Path path = Paths.get(doc.get(LuceneVisitor.PATH.s()));
						if (path != null) {
							mJournal.addNew(path);
						} else {
							assert(false);
						}
					}
				}

			});
			Log.i(TAG, "committing: "+Log.esc(pQueryDst));
			mJournal.commit(key(pQueryDst.snd()));
			mDeferred.notify(pQueryDst);
		} catch(RuntimeException|IOException|ParseException e) {
			throw e;
		}
	}

	@Override
	public void close() {
		mDeferred.resolve(null);
	}
}
