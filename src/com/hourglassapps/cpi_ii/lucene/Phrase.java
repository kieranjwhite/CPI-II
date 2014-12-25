package com.hourglassapps.cpi_ii.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;

import com.hourglassapps.util.Ii;

public class Phrase {
	private final String mPhrase;
	private final Analyzer mAnalyser;
	private static ThreadLocal<TermsEnum> TERM_ENUM=new ThreadLocal<>();
	private static ThreadLocal<DocsAndPositionsEnum> DOC_POS_ENUM=new ThreadLocal<>();
	private List<BytesRef> mRefs;
	
	public Phrase(Analyzer pAnalyser, String pPhrase) throws IOException {
		mPhrase=pPhrase;
		mAnalyser=pAnalyser;
		mRefs=terms(pAnalyser, pPhrase);
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
		return refs;
	}
	
	public List<DocSpan> findIn(IndexReader pReader, int pDocId, FieldVal pField) throws IOException {
		Document doc=pReader.document(pDocId);
		IndexableField path=doc.getField(LuceneVisitor.PATH.s());
		//System.out.println("doc: "+path.stringValue());
		
		Terms termVector=pReader.getTermVector(pDocId, pField.s());
		List<DocSpan> spans=new ArrayList<>();
		
		TermsEnum termsEnum=TERM_ENUM.get();
		termsEnum=termVector.iterator(TERM_ENUM.get());
		
		Collections.sort(mRefs, termsEnum.getComparator());
		DocsAndPositionsEnum docPos=DOC_POS_ENUM.get();

		for(BytesRef ref: mRefs) {
			//System.out.println("term: "+termRef.fst()+" ref: "+termRef.snd().utf8ToString());
			if(!termsEnum.seekExact(ref)) {
				continue;
			}

			docPos=termsEnum.docsAndPositions(null,docPos);
			docPos.nextDoc();
			int numMatches=docPos.freq();
			assert(numMatches>0);

			for(int matchNum=0; matchNum<numMatches; matchNum++) {
				int termPos=docPos.nextPosition();
				//System.out.println("term: "+termRef.snd().utf8ToString()+" pos: "+termPos+" start: "+docPos.startOffset()+" end: "+docPos.endOffset());				
			}
		}
		DOC_POS_ENUM.set(docPos);
		TERM_ENUM.set(termsEnum);

		return spans;
	}
}
