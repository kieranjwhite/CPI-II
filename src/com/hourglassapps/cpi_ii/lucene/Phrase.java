package com.hourglassapps.cpi_ii.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
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
import com.hourglassapps.util.NullIterable;
import com.hourglassapps.util.Clock;

public class Phrase {
	private final static String TAG=Phrase.class.getName();
	private final String mPhrase;
	private static ThreadLocal<TermsEnum> TERM_ENUM=new ThreadLocal<>();
	private static ThreadLocal<DocsAndPositionsEnum> DOC_POS_ENUM=new ThreadLocal<>();
	private final List<BytesRef> mOrigRefs; //these are in word order as per line of poem
	private final Map<BytesRef,Integer> mRefToIndex;
	private final Clock mTimes;
	private Cache<Integer,Terms> mTermCache;
	
	//private List<List<Integer>> mPositionAbsolutes=new ArrayList<>();
	
	public Phrase(Analyzer pAnalyser, String pPhrase, Clock pTimes, Cache<Integer,Terms> pTermCache) throws IOException {
		mTermCache=pTermCache;
		mTimes=pTimes;
		try(Clock w=mTimes.time("phrase constructor")) {
			mPhrase=pPhrase;
			mOrigRefs=Collections.unmodifiableList(terms(pAnalyser, pPhrase));
			Map<BytesRef,Integer> refToIdx=new HashMap<>();
			int idx=0;
			for(BytesRef ref: mOrigRefs) {
				refToIdx.put(ref, idx);
				idx++;
			}
			mRefToIndex=Collections.unmodifiableMap(refToIdx);
		}
	}
	
	@Override
	public String toString() {
		return mPhrase;
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
	
	public Iterable<DocSpan> findIn(IndexReader pReader, int pDocId) throws IOException {
		try(Clock findWatch=mTimes.time("find")) {
			final Map<BytesRef,NavigableSet<Integer>> refsToPositions=new HashMap<>();
			final Map<Integer, Integer> posToStartOffsets=new HashMap<Integer,Integer>();
			final Map<Integer, Integer> posToEndOffsets=new HashMap<Integer,Integer>();

			BytesRef firstRef=mOrigRefs.get(0);

			Document doc=pReader.document(pDocId);
			IndexableField path=doc.getField(LuceneVisitor.PATH.s());
			//System.out.println("doc: "+path.stringValue());

			Terms termVector;
			try(Clock vectorWatch=findWatch.time("vector")) {
				termVector=mTermCache.get(pDocId);
			}
			if(termVector==null) {
				return new NullIterable<DocSpan>();
			}
			TermsEnum termsEnum=termVector.iterator(TERM_ENUM.get());

			List<BytesRef> sortedTerms=new ArrayList<>(mOrigRefs);
			Collections.sort(sortedTerms, termsEnum.getComparator());

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
							if(ref==firstRef) {
								posToStartOffsets.put(termPos, docPos.startOffset());
							}
							posToEndOffsets.put(termPos, docPos.endOffset());
							//System.out.println("term: "+termRef.snd().utf8ToString()+" pos: "+termPos+" start: "+docPos.startOffset()+" end: "+docPos.endOffset());				
						}
					}
				}
			}
			DOC_POS_ENUM.set(docPos);
			TERM_ENUM.set(termsEnum);

			return new Iterable<DocSpan>(){

				@Override
				public Iterator<DocSpan> iterator() {
					return Phrase.this.iterator(refsToPositions, posToStartOffsets, posToEndOffsets);
				}

			};
		}
	}

	private boolean nextAt(final Map<BytesRef,NavigableSet<Integer>> pRefsToPositions, BytesRef pRef, int pRequiredPos) {
		NavigableSet<Integer> refPositions=pRefsToPositions.get(pRef);
		if(refPositions==null) {
			return false;
		}
		Integer found=refPositions.ceiling(pRequiredPos);
		return Integer.valueOf(pRequiredPos).equals(found);
	}

	private class SearchState {
		private int mStartPos=0;
		private final Map<BytesRef,NavigableSet<Integer>> mRefsToPositions;
		
		public SearchState(final Map<BytesRef,NavigableSet<Integer>> pRefsToPositions) {
			mRefsToPositions=pRefsToPositions;
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
	
	private Iterator<DocSpan> iterator(final Map<BytesRef,NavigableSet<Integer>> pRefsToPositions, 
			final Map<Integer, Integer> mPosToStartOffsets, final Map<Integer, Integer> mPosToEndOffsets) {
		final SearchState positions=new SearchState(pRefsToPositions);
		return new Iterator<DocSpan>(){
			private int mMatchStart=positions.nextMatchingPosition(0);

			@Override
			public boolean hasNext() {
				return mMatchStart!=Integer.MAX_VALUE;
			}

			@Override
			public DocSpan next() {
				try {
					return new DocSpan(mPosToStartOffsets.get(mMatchStart), mPosToEndOffsets.get(mMatchStart+mOrigRefs.size()-1));
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
}
