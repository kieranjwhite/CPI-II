package com.hourglassapps.cpi_ii.report;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

import org.apache.lucene.queryparser.classic.QueryParser;

import com.hourglassapps.cpi_ii.lucene.Phrases;
import com.hourglassapps.cpi_ii.lucene.Phrases.SpanFinder;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Rtu;

public class Batch implements Accumulator<Integer> {
	private final SortedMap<String, List<List<String>>> mPhraseToQuery;
	private final Map<String, Map<Integer,QueryPhrases>> mPhraseToIdxToQueryPhrases;
	private final String mAfterPhrase;
	private final String mFirstPhrase;
	private final QueryParser mParser;
	private final Phrases mPhrases;
	private final Set<Integer> mDocIdsFound=new TreeSet<Integer>();
	
	Batch(QueryParser pParser, Phrases pPhrases, SortedMap<String, List<List<String>>> pPhraseToQuery,
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
		List<String> q=mPhraseToQuery.get(pPhrase).get(pQueryIdx);
		assert(q.size()>0);
		if(mFirstPhrase==null) {
			assert mAfterPhrase==null;
			return false;
		}
		String firstAlphabetically=q.get(0);
		return firstAlphabetically.compareTo(mFirstPhrase)>=0 && 
				(mAfterPhrase==null || firstAlphabetically.compareTo(mAfterPhrase)<0);
	}

	private boolean todo(String pPhrase, int pQueryIdx, String pCurrentPhrase, int pCurrentQueryIdx) {
		if(pPhrase==null || pQueryIdx==Integer.MAX_VALUE || !mine(pPhrase, pQueryIdx)) {
			return false;
		}
		List<List<String>> phraseQueries=mPhraseToQuery.get(pPhrase);
		if(phraseQueries.get(pQueryIdx)==mPhraseToQuery.get(pCurrentPhrase).get(pCurrentQueryIdx)) {
			//either this query is not the responsibility of pPhrase, or it is but was already returned
			return false;
		}
		return Rtu.safeEq(pPhrase, phraseQueries.get(0));
	}
	
	private class QueryIterator implements Iterator<Ii<String,Integer>> {
		private final Iterator<String> mPhrases=mPhraseToQuery.keySet().iterator();
		
		private int mPhraseQueriesSize;
		
		private String mPhrase=null;
		private int mQueryIdx=Integer.MAX_VALUE;
		
		private Ii<String,Integer> mNext;
		
		public QueryIterator() {
			Ii<String,Integer> next=nextQueryFromPhrases();
			mPhrase=next.fst();
			mQueryIdx=next.snd();
		}
		
		private int nextQuery(int pIdx) {
			while(pIdx<mPhraseQueriesSize) {
				pIdx++;
				if(todo(mPhrase, pIdx, mPhrase, mQueryIdx)) {
					break;
				}
			}
			if(todo(mPhrase, pIdx, mPhrase, mQueryIdx)) {
				return pIdx;
			} else {
				return Integer.MAX_VALUE;
			}
		}
		
		private Ii<String,Integer> nextQueryFromPhrases() {
			while((mQueryIdx=nextQuery(mQueryIdx))==Integer.MAX_VALUE && mPhrases.hasNext()) {
				mQueryIdx=0;
				mPhrase=mPhrases.next();
				mPhraseQueriesSize=mPhraseToQuery.get(mPhrase).size();
			}
			return new Ii<String,Integer>(mPhrase, mQueryIdx);
		}
		
		@Override
		public boolean hasNext() {
			return mQueryIdx==Integer.MAX_VALUE;
		}

		@Override
		public Ii<String,Integer> next() {
			try {
				return mNext;
			} finally {
				mNext=nextQueryFromPhrases();
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	public Iterable<QueryPhrases> querie() {
		return new Iterable<QueryPhrases>(){
			Iterator<Ii<String,Integer>> mQueryIt=new QueryIterator();
			@Override
			public Iterator<QueryPhrases> iterator() {
				return new Iterator<QueryPhrases>() {

					@Override
					public boolean hasNext() {
						return mQueryIt.hasNext();
					}

					@Override
					public QueryPhrases next() {
						Ii<String,Integer> phraseIdx=mQueryIt.next();
						String phrase=phraseIdx.fst();
						Map<Integer,QueryPhrases> idxToQueryPhrases=mPhraseToIdxToQueryPhrases.get(phrase);
						if(idxToQueryPhrases==null) {
							idxToQueryPhrases=new HashMap<Integer,QueryPhrases>(mPhraseToQuery.get(phrase).size());
							mPhraseToIdxToQueryPhrases.put(phrase, idxToQueryPhrases);
						}
						int idx=phraseIdx.snd();
						QueryPhrases q=idxToQueryPhrases.get(idx);
						if(q==null) {
							q=new QueryPhrases(mParser, mPhraseToQuery.get(phraseIdx.fst()).get(phraseIdx.snd()), Batch.this);
							idxToQueryPhrases.put(idx,q);
						}
						return q;
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
	
	public Set<PartialResult> partialResults(int pDocId, String pPhrase) {
		assert(mPhraseToQuery.containsKey(pPhrase));
		assert(mPhraseToIdxToQueryPhrases.containsKey(pPhrase));
		for(Map<Integer,QueryPhrases> idxToQuery: mPhraseToIdxToQueryPhrases.get(pPhrase).values()) {
			for(QueryPhrases q=idxToQuery.values()) {
				
			}
		}
	}
}