package com.hourglassapps.cpi_ii.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import com.hourglassapps.util.Cache;
import com.hourglassapps.util.HashArrayMultiMap;
import com.hourglassapps.util.HashSetMultiMap;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.MultiMap;
import com.hourglassapps.util.NullIterable;
import com.hourglassapps.util.Clock;
import com.hourglassapps.util.Rtu;

public class Phrases {
	private final static String TAG=Phrases.class.getName();
	//private final Set<String> mPhrases=new HashSet<>();
	private static ThreadLocal<TermsEnum> TERM_ENUM=new ThreadLocal<>();
	private static ThreadLocal<DocsAndPositionsEnum> DOC_POS_ENUM=new ThreadLocal<>();
	private final Map<String,List<BytesRef>> mPhraseToRefs=new HashMap<>(); //these are in word order as per line of poem
	//private final Map<String,Map<BytesRef,Integer>> mPhraseToRefToIndex=new HashMap<>(); // term -> position in phrase
	private final Clock mTimes;
	//private Cache<Integer,Terms> mTermCache;
	private final Analyzer mAnalyser;
	private final IndexReader mReader;
	private boolean mBuilt=false;
	//private List<List<Integer>> mPositionAbsolutes=new ArrayList<>();
	
	private final MultiMap<BytesRef,List<String>,String> mFirstRefToPhrases=new HashArrayMultiMap<>();
	
	public Phrases(Analyzer pAnalyser, IndexReader pReader, Clock pTimes) throws IOException {
		mAnalyser=pAnalyser;
		//mTermCache=pTermCache;
		mTimes=pTimes;
		mReader=pReader;
	}
	
	public void reset() {
		mBuilt=false;
		//mPhrases.clear();
		mPhraseToRefs.clear();
		mFirstRefToPhrases.clear();
		//mPhraseToRefToIndex.clear();
	}
	
	public void add(String pPhrase) throws IOException {
		if(mBuilt) {
			throw new IllegalStateException("already built");
		}
		try(Clock w=mTimes.time("phrase constructor")) {
			//mPhrases.add(pPhrase);
			mPhraseToRefs.put(pPhrase, Collections.unmodifiableList(terms(mAnalyser, pPhrase)));
			Map<BytesRef,Integer> refToIdx=new HashMap<>();
			int idx=0;
			for(BytesRef ref: mPhraseToRefs.get(pPhrase)) {
				if(idx==0) {
					mFirstRefToPhrases.addOne(ref, pPhrase);
				}
				refToIdx.put(ref, idx);
				idx++;
			}
			//mPhraseToRefToIndex.put(pPhrase,Collections.unmodifiableMap(refToIdx));
			
		}		
	}
	
	public SpanFinder build() {
		mBuilt=true;
		return new SpanFinder();
	}
	
	@Override
	public String toString() {
		throw new UnsupportedOperationException(); //TODO get rid of this. The orig single phrase used this at one point
	}
	
	private static List<BytesRef> terms(Analyzer pAnalyser, String pPhrase) throws IOException {
		List<BytesRef> refs=new ArrayList<>();
		try(TokenStream ts=pAnalyser.tokenStream(LuceneVisitor.CONTENT.s(), new StringReader(pPhrase))) {
			CharTermAttribute termAtt=ts.addAttribute(CharTermAttribute.class);
			ts.reset();
			while(ts.incrementToken()) {
				String term=termAtt.toString();
				refs.add(new BytesRef(term));
			}
		}
		return Collections.unmodifiableList(refs);
	}
	
	public class SpanFinder {
		private final Set<BytesRef> mFirstRefs=new HashSet<>();
		private final Set<BytesRef> mLastRefs=new HashSet<>();
		
		private final NavigableMap<Integer,BytesRef> mPosToRef=new TreeMap<>();
		
		private SpanFinder() {
			for(List<BytesRef> phraseRefs: mPhraseToRefs.values()) {
				if(phraseRefs.size()==0) {
					continue;
				}
				mFirstRefs.add(phraseRefs.get(0));
				mLastRefs.add(phraseRefs.get(phraseRefs.size()-1));
			}
			
		}
		
		public Map<String,Set<DocSpan>> findIn(IndexReader pReader, int pDocId) throws IOException {
			try(Clock findWatch=mTimes.time("find")) {
				final Map<BytesRef,NavigableSet<Integer>> refsToPositions=new HashMap<>();
				final Map<Integer, Integer> posToStartOffsets=new HashMap<Integer,Integer>();
				final Map<Integer, Integer> posToEndOffsets=new HashMap<Integer,Integer>();

				Document doc=pReader.document(pDocId);
				
				Terms termVector;
				try(Clock vectorWatch=findWatch.time("vector")) {
					//termVector=mTermCache.get(pDocId);
					termVector=mReader.getTermVector(pDocId, LuceneVisitor.CONTENT.s());
				}
				if(termVector==null) {
					return Collections.emptyMap();
				}
				TermsEnum termsEnum=termVector.iterator(TERM_ENUM.get());

				SortedSet<BytesRef> sortedTerms=new TreeSet<>(termsEnum.getComparator());
				for(List<BytesRef> ref: mPhraseToRefs.values()) {
					sortedTerms.addAll(ref);
				}

				DocsAndPositionsEnum docPos=DOC_POS_ENUM.get();
				try(Clock termWatch=findWatch.time("terms")) {
					for(BytesRef ref: sortedTerms) {
						//System.out.println("term: "+termRef.fst()+" ref: "+termRef.snd().utf8ToString());
						if(!termsEnum.seekExact(ref)) {
							continue;
						}

						NavigableSet<Integer> positions=new TreeSet<>();
						refsToPositions.put(ref, positions);
						
						docPos=termsEnum.docsAndPositions(null,docPos);
						docPos.nextDoc();
						int numMatches=docPos.freq();
						assert(numMatches>0);

						try(Clock matchWatch=termWatch.time("matches")) {
							for(int matchNum=0; matchNum<numMatches; matchNum++) {
								int termPos=docPos.nextPosition(); /* nextPosition returns the number of terms into document the first match was found 
														  or for subsequent matches the number of tokens since the previous match */
								positions.add(termPos);
								mPosToRef.put(termPos, ref);
								
								if(mFirstRefs.contains(ref)) {
									posToStartOffsets.put(termPos, docPos.startOffset());
								}
								if(mLastRefs.contains(ref)) {
									posToEndOffsets.put(termPos, docPos.endOffset());
								}
							}
						}
					}
				}
				DOC_POS_ENUM.set(docPos);
				TERM_ENUM.set(termsEnum);

				return genPhraseToSpans(mPosToRef, posToStartOffsets, posToEndOffsets);
				/*
				return new Iterable<Ii<String,DocSpan>>(){

					@Override
					public Iterator<Ii<String,DocSpan>> iterator() {
						//return Phrases.this.iterator(refsToPositions, posToStartOffsets, posToEndOffsets);
						return Phrases.this.iterator(mPosToRef, posToStartOffsets, posToEndOffsets);
					}

				};
				*/
			}
	
		}

		private Map<String,Set<DocSpan>> genPhraseToSpans(final NavigableMap<Integer,BytesRef> pPosToRef, 
				final Map<Integer, Integer> pPosToStartOffsets, final Map<Integer, Integer> pPosToEndOffsets) {
			MultiMap<String,Set<DocSpan>,DocSpan> phraseToSpans=new HashSetMultiMap<>();
			for(Map.Entry<Integer,BytesRef> posRef: pPosToRef.entrySet()) {
				BytesRef ref=posRef.getValue();
				if(mFirstRefs.contains(ref)) {
					List<String> candidatePhrases=mFirstRefToPhrases.get(ref);
					assert candidatePhrases.size()>0; //since we know ref starts phrase
					for(String candidate: candidatePhrases) { //for each phrase beginning with ref
						boolean found=true;
						List<BytesRef> phraseTerms=mPhraseToRefs.get(candidate);
						int numPhraseTerms=phraseTerms.size();
						if(numPhraseTerms>1) {
							int withinPhrasePos=1;
							for(BytesRef phraseRef: phraseTerms.subList(1, numPhraseTerms)) { //for each remaining phraseRef in the phrase
								if(!Rtu.safeEq(phraseRef, pPosToRef.get(posRef.getKey()+withinPhrasePos))) {
									found=false;
									break;
								}
								withinPhrasePos++;
							}
						} //else one word phrase => we already have a match
						if(found) {
							int startPhrasePos=posRef.getKey();
							phraseToSpans.addOne(candidate, 
									new DocSpan(candidate, pPosToStartOffsets.get(startPhrasePos), pPosToEndOffsets.get(startPhrasePos+mPhraseToRefs.size())));
						}
					}
				}
			}
			return phraseToSpans;
		}
	}
	
	/*
	private boolean nextAt(final NavigableMap<Integer,BytesRef> pPosToRef, int pRequiredPos) {
		Integer found=pPosToRef.ceilingKey(pRequiredPos);
		return Integer.valueOf(pRequiredPos).equals(found);
	}
	
	private class SearchState {
		private int mStartPos=0;
		private final List<BytesRef> mOrigRefs;
		//private final Map<BytesRef,Integer> mRefToIndex;
		private final Map<BytesRef,NavigableSet<Integer>> mRefsToPositions;
		private final Map<BytesRef,Integer> mRefToIndex;
		
		public SearchState(String pPhrase, final Map<BytesRef,NavigableSet<Integer>> pRefsToPositions) {
			mRefsToPositions=pRefsToPositions;
			mOrigRefs=mPhraseToRefs.get(pPhrase);
			mRefToIndex=mPhraseToRefToIndex.get(pPhrase);
		}
		
		private void setStartBound(int pos) {
			mStartPos=pos;
		}
		
		private int getRequiredPos(BytesRef pRef) {
			return mStartPos+(mRefToIndex.get(pRef)-0);
		}

		private boolean matches(int pPosition) {
			setStartBound(pPosition);
			for(int i=1; i<mOrigRefs.size(); i++) {
				BytesRef ref=mOrigRefs.get(i);
				if(!nextAt(mRefsToPositions, ref, getRequiredPos(ref))) {
					return false;
				}
			}
			return true;
		}		

		public int nextMatchingPosition(int pStartIdx) {
			if(pStartIdx==Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			NavigableSet<Integer> possibleStartPositions=mRefsToPositions.get(mOrigRefs.get(0));
			if(possibleStartPositions==null) {
				return Integer.MAX_VALUE;
			}
			for(int p: possibleStartPositions.tailSet(pStartIdx, true)) {
				if(matches(p)) {
					return p;
				}
			}
			return Integer.MAX_VALUE;
		}
	}
	
	private class SearchState {
		private int mStartPos=0;
		private final List<BytesRef> mOrigRefs;
		//private final Map<BytesRef,Integer> mRefToIndex;
		private final NavigableMap<Integer,BytesRef> mPosToRef;
		private final Map<BytesRef,Integer> mRefToIndex;
		
		public SearchState(String pPhrase, final NavigableMap<Integer,BytesRef> pPosToRef) {
			mPosToRef=pPosToRef;
			mOrigRefs=mPhraseToRefs.get(pPhrase);
			mRefToIndex=mPhraseToRefToIndex.get(pPhrase);
		}
		
		private void setStartBound(int pos) {
			mStartPos=pos;
		}
		
		private int getRequiredPos(BytesRef pRef) {
			return mStartPos+(mRefToIndex.get(pRef)-0);
		}

		private boolean matches(int pPosition) {
			setStartBound(pPosition);
			for(int i=1; i<mOrigRefs.size(); i++) {
				BytesRef ref=mOrigRefs.get(i);
				if(!nextAt(mRefsToPositions, ref, getRequiredPos(ref))) {
					return false;
				}
			}
			return true;
		}		

		public int nextMatchingPosition(int pStartIdx) {
			if(pStartIdx==Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			NavigableSet<Integer> possibleStartPositions=mRefsToPositions.get(mOrigRefs.get(0));
			if(possibleStartPositions==null) {
				return Integer.MAX_VALUE;
			}
			for(int p: possibleStartPositions.tailSet(pStartIdx, true)) {
				if(matches(p)) {
					return p;
				}
			}
			return Integer.MAX_VALUE;
		}
	}
	
	private Iterator<Ii<String,DocSpan>> iterator(final Map<BytesRef,NavigableSet<Integer>> pRefsToPositions, 
			final Map<Integer, Integer> pPosToStartOffsets, final Map<Integer, Integer> pPosToEndOffsets) {
		final Iterator<String> phraseIt=mPhrases.iterator();
		return new Iterator<Ii<String,DocSpan>>() {
			private Iterator<Ii<String,DocSpan>> mSpanIt=null;
			
			public void setupSpanIt() {
				if(mSpanIt!=null && mSpanIt.hasNext()) {
					return;
				}
				while(mSpanIt==null || !mSpanIt.hasNext()) {
					if(phraseIt.hasNext()) {
						mSpanIt=phraseIterator(phraseIt.next(), pRefsToPositions, pPosToStartOffsets, pPosToEndOffsets);
					} else {
						break;
					}
				}
				return;
			}

			@Override
			public boolean hasNext() {
				setupSpanIt();
				return mSpanIt.hasNext();
			}
			
			@Override
			public Ii<String,DocSpan> next() {
				setupSpanIt();
				return mSpanIt.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
		
	}
	
	private Iterator<Ii<String,DocSpan>> phraseIterator(final String pPhrase, final Map<BytesRef,NavigableSet<Integer>> pRefsToPositions, 
			final Map<Integer, Integer> pPosToStartOffsets, final Map<Integer, Integer> pPosToEndOffsets) {
		final SearchState positions=new SearchState(pPhrase,pRefsToPositions);
		return new Iterator<Ii<String,DocSpan>>(){
			private int mMatchStart=positions.nextMatchingPosition(0);

			@Override
			public boolean hasNext() {
				return mMatchStart!=Integer.MAX_VALUE;
			}

			@Override
			public Ii<String,DocSpan> next() {
				try {
					return new Ii<String,DocSpan>(pPhrase , new DocSpan(pPhrase, pPosToStartOffsets.get(mMatchStart), pPosToEndOffsets.get(mMatchStart+mPhraseToRefs.size()-1)));
				} finally {
					mMatchStart=positions.nextMatchingPosition(mMatchStart+1);
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
		
	}
	*/
}
