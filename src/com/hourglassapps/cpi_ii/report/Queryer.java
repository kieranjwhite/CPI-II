package com.hourglassapps.cpi_ii.report;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
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

import com.hourglassapps.cpi_ii.lucene.DocSpan;
import com.hourglassapps.cpi_ii.lucene.IndexViewer;
import com.hourglassapps.cpi_ii.lucene.LuceneVisitor;
import com.hourglassapps.cpi_ii.lucene.Phrase;
import com.hourglassapps.cpi_ii.lucene.ResultRelayer;
import com.hourglassapps.cpi_ii.report.LineGenerator.Line;
import com.hourglassapps.persist.Journal;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.URLUtils;

public class Queryer implements AutoCloseable {
	private final static String TAG=Queryer.class.getName();
	private final static int MAX_RESULTS=100;
	private final Journal<String,Result> mJournal;
	private final Analyzer mAnalyser;
	private final Deferred<Void,Exception,Ii<Line,String>> mDeferred=new DeferredObject<>();
    private final QueryParser mParser;
    private final IndexReader mReader;
	private final IndexSearcher mSearcher;
	private final Converter<Line,List<String>> mLineToQuery;
	
	public Queryer(Journal<String,Result> pJournal, IndexViewer pIndex, 
			Analyzer pAnalyser, Converter<Line,List<String>> pQueryGenerator) throws IOException {
		mJournal=pJournal;
		mAnalyser=pAnalyser;
		mParser=new QueryParser(Version.LUCENE_4_10_0, LuceneVisitor.CONTENT.s(), pAnalyser);
		mReader=DirectoryReader.open(pIndex.dir());
	    mSearcher = new IndexSearcher(mReader);
	    mLineToQuery=pQueryGenerator;
	}
	
	public Promise<Void,Exception,Ii<Line,String>> promise() {
		return mDeferred;
	}
	
	public void search(Ii<Line,String> pLineDst) throws ParseException, IOException {
		if(mJournal.addedAlready(pLineDst.snd()) || "".equals(pLineDst.fst())) {
			//Log.i(TAG, "found: "+Log.esc(pLineDst));
			return;
		}
		try {
			final List<Phrase> phrases=new ArrayList<>();
			List<String> phraseStrs=mLineToQuery.convert(pLineDst.fst());
			for(String phrase: phraseStrs) {
				phrases.add(new Phrase(mAnalyser, phrase));
			}
			String query="\""+Rtu.join(phraseStrs,"\" \"")+"\"";
			Log.i(TAG, "query: "+query);
			if(!"".equals(query)) {
				Query q=mParser.parse(query);
				IndexViewer.interrogate(mReader, mSearcher, q, MAX_RESULTS, new ResultRelayer() {

					@Override
					public void run(IndexReader pReader, TopDocs pResults)
							throws IOException {

						final SortedSet<DocSpan> docSpans=new TreeSet<>();
						ScoreDoc[] results=pResults.scoreDocs;
						for(int i=0; i<results.length; i++) {
							
							for(Phrase p: phrases) {
								Iterator<DocSpan> spans=p.findIn(pReader, results[i].doc, LuceneVisitor.CONTENT).iterator();
								while(spans.hasNext()) {
									DocSpan span=spans.next();
									docSpans.add(span);
								}
							}
							
							Iterator<DocSpan> spans=docSpans.iterator();
							DocSpan lastSpan=null;
							if(spans.hasNext()) {
								lastSpan=spans.next();
							}
							while(spans.hasNext()) {
								DocSpan span=spans.next();
								if(!lastSpan.merged(span)) {
									lastSpan=span;
								} else {
									spans.remove();
								}
							}
							
							Document doc = mSearcher.doc(results[i].doc);
							Path path=Paths.get(doc.get(LuceneVisitor.PATH.s()));
							String title=doc.get(LuceneVisitor.TITLE.s());
							if (path != null) {
								if(title==null) {
									title="";
								}
								mJournal.addNew(new Result(title, path, Collections.unmodifiableSortedSet(docSpans))); //unmodifiableSortedSet or not, DocSpan instances are mutable so we could be more defensive here
							} else {
								assert(false);
							}
							docSpans.clear();
						}
					}

				});
			}
			//Log.i(TAG, "committing: "+Log.esc(pLineDst));
			mJournal.commit(pLineDst.snd());
			mDeferred.notify(pLineDst);
		} catch(RuntimeException|IOException|ParseException e) {
			throw e;
		}
	}

	@Override
	public void close() {
		mDeferred.resolve(null);
	}
}
