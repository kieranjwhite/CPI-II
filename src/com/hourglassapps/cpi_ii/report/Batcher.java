package com.hourglassapps.cpi_ii.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;

import org.apache.lucene.queryparser.classic.QueryParser;

import com.hourglassapps.cpi_ii.lucene.Phrases;
import com.hourglassapps.cpi_ii.report.LineGenerator.Line;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.SortedMultiMap;
import com.hourglassapps.util.TreeArrayMultiMap;

public class Batcher implements Iterable<Batch> {
	private final static int EXPECTED_MAX_NUM_LINES=27000;
	private SortedMultiMap<String, List<QueryPhrases>, QueryPhrases> mPhraseToFullQuery=null;
	private int mNumBatches;
	private final List<Ii<Line,String>> mLineDsts=new ArrayList<>(EXPECTED_MAX_NUM_LINES);
	private final Converter<Line,List<String>> mLineToQuery;
	private final QueryParser mParser;
	private final Phrases mPhrases;
	
	public Batcher(int pNumBatches, Converter<Line,List<String>> pLineToQuery, QueryParser pParser, Phrases pPhrases) {
		mNumBatches=pNumBatches;
		mLineToQuery=pLineToQuery;
		mParser=pParser;
		mPhrases=pPhrases;
	}
	
	public void add(Ii<Line,String> pLineDst) {
		mLineDsts.add(pLineDst);
	}
	
	private class BatchIterator implements Iterator<Batch> {
		private final int mPhrasesPerBatch;

		private String mNextPhrase=null;
		private SortedMultiMap<String, List<QueryPhrases>, QueryPhrases> mNextBatch;
		
		public BatchIterator() {
			if(mPhraseToFullQuery==null) {
				mPhraseToFullQuery=new TreeArrayMultiMap<>();
				for(Ii<Line,String> lineDst: mLineDsts) {
					List<String> queryPhrases=mLineToQuery.convert(lineDst.fst());
					List<String> queryPhrasesLower=new ArrayList<>(queryPhrases.size());
					for(String phrase: queryPhrases) {
						queryPhrasesLower.add(phrase.toLowerCase());
					}
					//Collections.sort(queryPhrases); //necessary for Batch instance to be able to check if same line is repeated within a poem
					QueryPhrases qPhrases=new QueryPhrases(mParser, queryPhrasesLower, lineDst);
					for(String phrase: queryPhrasesLower) {
						mPhraseToFullQuery.addOne(phrase, qPhrases);
					}
				}
			}
			int numPhrases=mPhraseToFullQuery.size();
			mPhrasesPerBatch=1+numPhrases/mNumBatches;
			mNextPhrase=nextBatch(mNextPhrase);
		}
		
		private String nextBatch(String pNextPhrase) {
			if(pNextPhrase!=null) {
				mNextBatch=TreeArrayMultiMap.view(mPhraseToFullQuery.tailMap(pNextPhrase));
			} else {
				mNextBatch=mPhraseToFullQuery;					
			}
			int phraseNum=0;
			String nextPhrase=null;
			for(String phrase: mNextBatch.keySet()) {
				nextPhrase=phrase;
				if(phraseNum>=mPhrasesPerBatch) {
					break;
				}
				phraseNum++;
			}
			return nextPhrase;
		}
		
		@Override
		public boolean hasNext() {
			return mNextBatch.size()>0;
		}

		@Override
		public Batch next() {
			try {
				mPhrases.reset();
				return new Batch(mPhrases, mNextBatch, mNextPhrase);
			} finally {
				mNextPhrase=nextBatch(mNextPhrase);
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	public Iterator<Batch> iterator() {
		return new BatchIterator();
	}
	
}
