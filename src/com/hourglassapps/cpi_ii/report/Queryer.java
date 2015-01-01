package com.hourglassapps.cpi_ii.report;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;
import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import com.hourglassapps.cpi_ii.lucene.DocSpan;
import com.hourglassapps.cpi_ii.lucene.IndexViewer;
import com.hourglassapps.cpi_ii.lucene.LuceneVisitor;
import com.hourglassapps.cpi_ii.lucene.Phrases;
import com.hourglassapps.cpi_ii.lucene.Phrases.SpanFinder;
import com.hourglassapps.cpi_ii.lucene.ResultRelayer;
import com.hourglassapps.cpi_ii.report.LineGenerator.Line;
import com.hourglassapps.cpi_ii.report.QueryPhrases.Answers;
import com.hourglassapps.persist.Journal;
import com.hourglassapps.util.Cache;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.ExclusiveTimeKeeper;

public class Queryer implements AutoCloseable {
	private final static String TAG=Queryer.class.getName();
	private final static int MAX_RESULTS=100;
	
	private final Journal<String,Result> mJournal;
	private final Analyzer mAnalyser;
	private final Deferred<Void,Exception,Ii<Line,String>> mDeferred=new DeferredObject<>();
    private final QueryParser mParser;
    private final IndexReader mReader;
	private final IndexSearcher mSearcher;
	//private final Converter<Line,List<String>> mLineToQuery;
	private final ExclusiveTimeKeeper mTimes=new ExclusiveTimeKeeper();
	private int mSearchCnt=0;
	private final Cache<Integer,Terms> mTermCache;
	private final ConcreteThrower<IOException> mThrower=new ConcreteThrower<>();
	private final Batcher mPartials;
	
	public Queryer(Journal<String,Result> pJournal, IndexViewer pIndex, 
			Analyzer pAnalyser, Converter<Line,List<String>> pQueryGenerator, int pNumBatches) throws IOException {
		mJournal=pJournal;
		mAnalyser=pAnalyser;
		mParser=new QueryParser(Version.LUCENE_4_10_0, LuceneVisitor.CONTENT.s(), pAnalyser);
		mReader=DirectoryReader.open(pIndex.dir());
	    mSearcher = new IndexSearcher(mReader);
	    //mLineToQuery=pQueryGenerator;
	    mPartials=new Batcher(pNumBatches, pQueryGenerator, mParser, new Phrases(mAnalyser, mReader, mTimes));
	    mTermCache=new Cache<Integer,Terms>(new Converter<Integer,Terms>(){
	    	
			@Override
			public Terms convert(Integer pDocId) {
				try {
					return mReader.getTermVector(pDocId, LuceneVisitor.CONTENT.s());
				} catch(IOException e) {
					mThrower.ctch(e);
				}
				return null;
			}
	    	
	    }) {

			@Override
			public <E extends Exception> void throwCaught(Class<E> pCatchable)
					throws Throwable {
				mThrower.throwCaught(pCatchable.asSubclass(IOException.class));
			}
	    	
	    };
	}
	
	public Promise<Void,Exception,Ii<Line,String>> promise() {
		return mDeferred;
	}
	
	private void docSearch(final Batch pBatch) throws ParseException, IOException {
		for(final QueryPhrases qPhrases: pBatch.queries()) {
			if(qPhrases.answered()) {
				continue;
			}
			final Query q=qPhrases.parse();
			IndexViewer.interrogate(mReader, mSearcher, q, MAX_RESULTS, new ResultRelayer() {

				@Override
				public void run(IndexReader pReader, TopDocs pResults)
						throws IOException {
					ScoreDoc[] results=pResults.scoreDocs;
					Answers ans=pBatch.startAnswering(qPhrases, results.length);
					for(int i=0; i<results.length; i++) {

						Document doc = mSearcher.doc(results[i].doc);
						Path path=Paths.get(doc.get(LuceneVisitor.PATH.s()));
						String title=doc.get(LuceneVisitor.TITLE.s());
						if (path != null) {
							if(title==null) {
								title="";
							}
							//mJournal.addNew(new Result(title, path, Collections.unmodifiableSortedSet(docSpans))); //unmodifiableSortedSet or not, DocSpan instances are mutable so we could be more defensive here
							ans.answer(results[i].doc, path, title);
						} else {
							assert(false);
						}
					}
				}
			});
		}
	}
	
	private void search(Batch pBatch) throws ParseException, IOException {
		try {
			docSearch(pBatch);
			SpanFinder spans=pBatch.allPhrases();
			for(Integer docId: pBatch.docIds()) {
				for(Map.Entry<String, Set<DocSpan>> phraseSpans: spans.findIn(mReader, docId).entrySet()) {
					Set<DocResult> results=pBatch.docResults(docId, phraseSpans.getKey());
					for(DocResult result: results) {
						for(DocSpan span: phraseSpans.getValue()) {
							result.addSpan(span);
						}
					}
				}
			}
			
			for(final QueryPhrases qPhrases: pBatch.queries()) {
				Answers answers=qPhrases.answers();
				if(answers!=null) {
					for(Result res: answers.results()) {
						mJournal.addNew(res);
					}
				}
				mJournal.commit(qPhrases.dst());
			}			
			
			/*
			final List<Phrases> phrases=new ArrayList<>();
			List<String> phraseStrs=mLineToQuery.convert(pLineDst.fst());
			for(String phrase: phraseStrs) {
				assert(phrase.length()!=0);
				phrases.add(new Phrases(mAnalyser, mReader, mTimes));
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
							
							for(Phrases p: phrases) {
								Iterator<DocSpan> spans=p.findIn(pReader, results[i].doc).iterator();
								while(spans.hasNext()) {
									DocSpan span=spans.next();
									docSpans.add(span);
								}
								mThrower.throwCaught(IOException.class);
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
						mSearchCnt++;
						if(mSearchCnt%100==0) {
							Log.i(TAG, "Searches: "+mSearchCnt+"\n"+mTimes.toString());
						}
					}

				});
				
			}
			//Log.i(TAG, "committing: "+Log.esc(pLineDst));
			mJournal.commit(pLineDst.snd());
			mDeferred.notify(pLineDst);
			*/
		} catch(RuntimeException|IOException|ParseException e) {
			throw e;
		}
		
		
	}
	
	public void search() throws ParseException, IOException {
		for(Batch batch: mPartials) {
			search(batch);
		}
	}
	
	public void include(Ii<Line,String> pLineDst) throws IOException {
		if(mJournal.addedAlready(pLineDst.snd()) || "".equals(pLineDst.fst())) {
			//Log.i(TAG, "found: "+Log.esc(pLineDst));
			return;
		}
		mPartials.add(pLineDst);
	}

	@Override
	public void close() {
		mDeferred.resolve(null);
		mTimes.close();
	}
}
