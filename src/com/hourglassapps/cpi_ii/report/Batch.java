package com.hourglassapps.cpi_ii.report;

import java.io.IOException;
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
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.SortedMultiMap;

public class Batch implements Accumulator<Integer> {
	private final SortedMultiMap<String, List<QueryPhrases>, QueryPhrases> mPhraseToQuery;
	private final Map<String, Map<Integer,QueryPhrases>> mPhraseToIdxToQueryPhrases;
	private final String mAfterPhrase;
	private final String mFirstPhrase;
	private final QueryParser mParser;
	private final Phrases mPhrases;
	private final Set<Integer> mDocIdsFound=new TreeSet<Integer>();
	
	Batch(QueryParser pParser, Phrases pPhrases, SortedMultiMap<String, List<QueryPhrases>, QueryPhrases> pPhraseToQuery,
			String pNextPhrase) {
		mPhrases=pPhrases;
		mParser=pParser;
		mAfterPhrase=pNextPhrase;
		mPhraseToQuery=pPhraseToQuery;
		mPhraseToIdxToQueryPhrases=new HashMap<>();
		String first=mPhraseToQuery.firstKey();
		if(first!=null) {
			mFirstPhrase=first;
		} else {
			mFirstPhrase=mAfterPhrase;
		}
	}
	
	public SpanFinder allPhrases() throws IOException {
		for(String phrase: mPhraseToQuery.keySet()) {
			mPhrases.add(phrase);
		}
		
		return mPhrases.build();
	}
	
	private boolean mine(String pPhrase, int pQueryIdx) {
		assert(mPhraseToQuery.containsKey(pPhrase));
		List<String> q=mPhraseToQuery.get(pPhrase).get(pQueryIdx).phrases();
		assert(q.size()>0);
		if(mFirstPhrase==null) {
			assert mAfterPhrase==null;
			return false;
		}
		String firstLine=q.get(0);
		return firstLine.compareTo(mFirstPhrase)>=0 && 
				(mAfterPhrase==null || firstLine.compareTo(mAfterPhrase)<0);
	}

	private boolean todo(String pPhrase, int pQueryIdx, String pCurrentPhrase, int pCurrentQueryIdx) {
		if(pPhrase==null || pQueryIdx==Integer.MAX_VALUE || !mine(pPhrase, pQueryIdx)) {
			return false;
		}
		List<QueryPhrases> phraseQueries=mPhraseToQuery.get(pPhrase);
		if(phraseQueries.get(pQueryIdx)==mPhraseToQuery.get(pCurrentPhrase).get(pCurrentQueryIdx)) {
			//either this query is not the responsibility of pPhrase, or it is but was already returned
			return false;
		}
		return Rtu.safeEq(pPhrase, phraseQueries.get(0));
	}
	
	public Iterable<QueryPhrases> queries() {
		return new Iterable<QueryPhrases>(){
			//Iterator<Ii<String,Integer>> mQueryIt=new QueryIterator();
			@Override
			public Iterator<QueryPhrases> iterator() {
				return new Iterator<QueryPhrases>() {
					private Iterator<Entry<String, List<QueryPhrases>>> phraseQueriesIt=mPhraseToQuery.entrySet().iterator();
					
					private Entry<String, List<QueryPhrases>> mQueryPhraseArgs=null;
					private int mQueryIdx=Integer.MAX_VALUE;
					private String mLastPhrase=null;
					private QueryPhrases mNext=null;
					
					{
						setNextPhrase();
					}
					
					private void setNextQueryIdx() {
						int idx=mQueryIdx;
						if(mQueryIdx<mQueryPhraseArgs.getValue().size()) {
							String phrase=mQueryPhraseArgs.getKey();
							
							if(todo(phrase, idx, mLastPhrase, mQueryIdx)) {
								mNext=mPhraseToQuery.get(phrase).get(mQueryIdx);
							}
							idx++;
						} else {
							mNext=null;
						}
						mQueryIdx=idx;
					}
					
					private void setNextPhrase() {
						setNextQueryIdx();
						while(mNext==null && phraseQueriesIt.hasNext()) {
							mQueryIdx=0;
							mQueryPhraseArgs=phraseQueriesIt.next();
							setNextQueryIdx();
						}
						mLastPhrase=mQueryPhraseArgs.getKey();
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
		assert(mPhraseToQuery.containsKey(pPhrase));
		assert(mPhraseToIdxToQueryPhrases.containsKey(pPhrase));
		
		Set<DocResult> partials=new HashSet<>();
		
		for(QueryPhrases queries: mPhraseToIdxToQueryPhrases.get(pPhrase).values()) {
			Answers answers=queries.answers();
			assert answers!=null;
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