package com.hourglassapps.cpi_ii.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
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

import com.hourglassapps.util.Ii;

public class Phrase {
	private final Analyzer mAnalyser;
	private static ThreadLocal<TermsEnum> TERM_ENUM=new ThreadLocal<>();
	private static ThreadLocal<DocsAndPositionsEnum> DOC_POS_ENUM=new ThreadLocal<>();
	private List<Ii<String,BytesRef>> mTermRefs;
	
	public Phrase(Analyzer pAnalyser, String pPhrase) throws IOException {
		mAnalyser=pAnalyser;
		mTermRefs=terms(pAnalyser, pPhrase);
	}
	
	private static List<Ii<String,BytesRef>> terms(Analyzer pAnalyser, String pPhrase) throws IOException {
		List<Ii<String,BytesRef>> termRefs=new ArrayList<>();
		try(TokenStream ts=pAnalyser.tokenStream(LuceneVisitor.CONTENT.s(), new StringReader(pPhrase))) {
			CharTermAttribute termAtt=ts.addAttribute(CharTermAttribute.class);
			ts.reset();
			while(ts.incrementToken()) {
				String term=ts.reflectAsString(true);
				termRefs.add(new Ii<String,BytesRef>(termAtt.toString(), new BytesRef(term)));
			}
		}
		return Collections.unmodifiableList(termRefs);
	}
	
	public List<DocSpan> findIn(IndexReader pReader, int pDocId, FieldVal pField) throws IOException {
		Document doc=pReader.document(pDocId);
		IndexableField path=doc.getField(LuceneVisitor.PATH.s());
		System.out.println("doc: "+path.stringValue());
		
		Terms termVector=pReader.getTermVector(pDocId, pField.s());
		List<DocSpan> spans=new ArrayList<>();
		
		TermsEnum termsEnum=TERM_ENUM.get();
		termsEnum=termVector.iterator(TERM_ENUM.get());
		if(termsEnum.next()==null) {
			assert false;
		}
		TERM_ENUM.set(termsEnum);

		DocsAndPositionsEnum docPos=DOC_POS_ENUM.get();
		BytesRef ref=null;
		while((ref=termsEnum.next())!=null) {
			docPos=termsEnum.docsAndPositions(null,docPos);
			docPos.nextDoc();
			//if(docPos.nextDoc()==0) {
			//	continue;
			//}
			int numMatches=docPos.freq();
			for(int matchNum=0; matchNum<numMatches; matchNum++) {
				int termPos=docPos.nextPosition();
				System.out.println("pos: "+termPos+" start: "+docPos.startOffset()+" end: "+docPos.endOffset());				
			}
			
		}
		
		/*
		DocsAndPositionsEnum docPos=DOC_POS_ENUM.get();
		long termPos;
		for(Ii<String,BytesRef> termRef: mTermRefs) {
			if(termsEnum.seekExact(termRef.snd())==false) {
				return Collections.emptyList();								
			}
			
			docPos=termsEnum.docsAndPositions(null, docPos, DocsAndPositionsEnum.FLAG_OFFSETS);
			if(docPos.nextDoc()!=0) {
				return Collections.emptyList();				
			}
			
			int numMatches=docPos.freq(), matchNum=0;
			while(matchNum++<numMatches) {
				termPos=docPos.nextPosition();
				System.out.println("term: "+termRef.fst()+" pos: "+termPos+" start: "+docPos.startOffset()+" end: "+docPos.endOffset());
			}
		}
		*/
		DOC_POS_ENUM.set(docPos);
		
		return spans;
	}
}
