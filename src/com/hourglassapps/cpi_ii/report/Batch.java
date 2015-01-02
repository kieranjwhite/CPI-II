package com.hourglassapps.cpi_ii.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

import org.apache.lucene.queryparser.classic.QueryParser;

import com.hourglassapps.cpi_ii.lucene.Phrases;
import com.hourglassapps.cpi_ii.lucene.Phrases.SpanFinder;
import com.hourglassapps.cpi_ii.report.QueryPhrases.Answers;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.NullIterable;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.SortedMultiMap;
import com.hourglassapps.util.TreeArrayMultiMap;

public class Batch implements Accumulator<Integer> {
	private final static String TAG=Batch.class.getName();
	private final SortedMultiMap<String, List<QueryPhrases>, QueryPhrases> mPhraseToQueryFull;
	private final SortedMultiMap<String, List<QueryPhrases>, QueryPhrases> mPhraseToQuery;
	private final Phrases mPhrases;
	private final Set<Integer> mDocIdsFound=new TreeSet<Integer>();
	private final int mSize;
	
	public Batch(Phrases pPhrases, SortedMultiMap<String, List<QueryPhrases>, QueryPhrases> pPhraseToQueryFull,
			String pStartPhrase, String pNextPhrase) {
		mPhraseToQueryFull=pPhraseToQueryFull;
		mPhrases=pPhrases;
		
		SortedMultiMap<String, List<QueryPhrases>, QueryPhrases> phraseToQuery;
		if(pNextPhrase==null) {
			phraseToQuery=pPhraseToQueryFull;
		} else {
			phraseToQuery=TreeArrayMultiMap.view(pPhraseToQueryFull.headMap(pNextPhrase));
		}
		if(pStartPhrase!=null) {
			phraseToQuery=TreeArrayMultiMap.view(phraseToQuery.tailMap(pStartPhrase));
		}
		mPhraseToQuery=phraseToQuery;
		
		int size=0;
		for(List<QueryPhrases> mPhrases: mPhraseToQuery.values()) {
			size+=mPhrases.size();
		}
		mSize=size;
	}

	
	public int size() {
		return mSize;
	}
	
	public SpanFinder allPhrases() throws IOException {
		if(!mPhrases.built()) {
			for(List<QueryPhrases> phrases: mPhraseToQuery.values()) {
				for(QueryPhrases qPhrases: phrases) {
					for(String phrase: qPhrases.phrases()) {
						mPhrases.add(phrase);
					}
				}
			}
		}
		return mPhrases.build();
	}
	
	/**
	 * Determine (1) if this Batch instance should handle the query represent by pPhrase, pQueryIdx pair and (2) if the first
	 * phrase of the query matches the pPhrase key. The purpose of the second condition is to ensure that multi-phrase queries
	 * aren't processes twice.
	 * @param pPhrase
	 * @param pQueryIdx
	 * @return
	 */
	private boolean todo(String pPhrase, int pQueryIdx) {
		return mPhraseToQuery.containsKey(pPhrase) && Rtu.safeEq(mPhraseToQuery.get(pPhrase).get(pQueryIdx).firstPhrase(), pPhrase);
	}

	public Iterable<QueryPhrases> queries() {
		if(mPhraseToQuery.size()==0) {
			return new NullIterable<>();
		}
		return new Iterable<QueryPhrases>(){
			//Iterator<Ii<String,Integer>> mQueryIt=new QueryIterator();
			@Override
			public Iterator<QueryPhrases> iterator() {
				return new Iterator<QueryPhrases>() {
					private Iterator<Entry<String, List<QueryPhrases>>> phraseQueriesIt=mPhraseToQuery.entrySet().iterator();
					{
						assert phraseQueriesIt.hasNext();
					}
					
					private Entry<String, List<QueryPhrases>> mQueryPhraseArgs=phraseQueriesIt.next();
					private int mQueryIdx=0;
					private QueryPhrases mNext=null;
					
					{
						setNextPhrase();
					}
					
					private void setNextQueryIdx() {
						while(mQueryIdx<mQueryPhraseArgs.getValue().size()) {
							String phrase=mQueryPhraseArgs.getKey();

							if(todo(phrase, mQueryIdx)) {
								mNext=mPhraseToQuery.get(phrase).get(mQueryIdx);
								mQueryIdx++;
								return;
							}
							mQueryIdx++;
						}
						mNext=null;
					}

					private void setNextPhrase() {
						setNextQueryIdx();
						while(mNext==null && phraseQueriesIt.hasNext()) {
							mQueryIdx=0;
							mQueryPhraseArgs=phraseQueriesIt.next();						
							setNextQueryIdx();
						}
					}
					
					@Override
					public boolean hasNext() {
						return mNext!=null;
					}

					@Override
					public QueryPhrases next() {
						try {
							return mNext;
						} finally {
							setNextPhrase();
						}
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
		};
	}

	@Override
	public void add(Integer pEl) {
		mDocIdsFound.add(pEl);
	}
	
	public Set<Integer> docIds() {
		return Collections.unmodifiableSet(mDocIdsFound);
	}
	
	public Set<DocResult> docResults(int pDocId, String pPhrase) {
		Set<DocResult> partials=new HashSet<>();
		for(QueryPhrases queries: mPhraseToQueryFull.get(pPhrase)) {
			Answers answers=queries.answers();
			if(answers==null) {
				//Log.i(TAG, Log.esc("skipping query for: "+queries.dst()+" It seems to be a duplicate."));
				continue;
			} else {
				//Log.i(TAG, Log.esc("recording results for: "+queries.dst()));					
			}
			DocResult partial=answers.docResult(pDocId);
			if(partial!=null) {
				partials.add(partial);
			}
		}

		return partials;
	}

	public Answers startAnswering(QueryPhrases qPhrases, int pNumAnswers) {
		return qPhrases.startAnswering(pNumAnswers, this);
	}
}