package com.hourglassapps.cpi_ii.web_search;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import com.hourglassapps.cpi_ii.CPIFields;
import com.hourglassapps.cpi_ii.MainIndexConductus;
import com.hourglassapps.cpi_ii.lucene.AbstractTermFreqMapper;
import com.hourglassapps.cpi_ii.lucene.FieldVal;
import com.hourglassapps.cpi_ii.lucene.FoundGenerator;
import com.hourglassapps.cpi_ii.lucene.IndexViewer;
import com.hourglassapps.cpi_ii.lucene.TermHandler;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;

public class ExpansionComparator implements Comparator<List<String>> {
	private final static String TAG=ExpansionComparator.class.getName();

	public final static Comparator<List<String>> NGRAM_PRIORITISER;
	static {
		Comparator<List<String>> tmp;
		try {
			tmp=new ExpansionComparator();
		} catch(IOException e) {
			Log.e(TAG, e);
			tmp=new Comparator<List<String>>() {

				@Override
				public int compare(List<String> arg0, List<String> arg1) {
					return 0;
				}
				
			};
		}
		NGRAM_PRIORITISER=tmp;
	}
	
	private final IndexReader mReader=DirectoryReader.open(FSDirectory.open(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX));
	private final IndexSearcher mSearcher=new IndexSearcher(mReader);
	private final IndexViewer mTermIndex=new IndexViewer(MainIndexConductus.UNSTEMMED_TERM_2_EPRINT_INDEX);
	private Map<String, Long> mTerm2Freq=term2Freq();
	
	public ExpansionComparator() throws IOException {
		
	}
	
	private Map<String, Long> term2Freq() throws IOException {
		final Map<String, Long> term2Freq=new HashMap<String, Long>();
		TermHandler mapper=new AbstractTermFreqMapper() {

			@Override
			public void add(String pTerm, Long pFreq) {
				term2Freq.put(pTerm, pFreq);
			}
		};
		mTermIndex.visit(CPIFields.CONTENT.fieldVal(), mapper);
		return term2Freq;
	}
	
	private boolean presentInCollection(List<String> pToken) throws IOException {
		if(pToken.size()>=1) {
			String tokenJoined=Rtu.join(pToken, " ");
			FoundGenerator relay=new FoundGenerator();
			IndexViewer.interrogate(mReader, mSearcher, CPIFields.CONTENT.fieldVal(), tokenJoined, 1, relay);
			return relay.result();
		} else {
			return true;
		}
	}

	private double geometricFreq(List<String> pToken) {
		double result=1;
		for(String term: pToken) {
			if(mTerm2Freq.containsKey(term)) {
				result*=mTerm2Freq.get(term);
			} else {
				return 0;
			}
		}
		return result;
	}
	
	@Override
	public int compare(List<String> p0, List<String> p1) {
		boolean p0Present;
		try {
			p0Present = presentInCollection(p0);
			boolean p1Present=presentInCollection(p1);
			if(p0Present==p1Present) {
				double gP0=geometricFreq(p0);
				double gP1=geometricFreq(p1);
				if(gP0<gP1) {
					return +1;
				} else if(gP0>gP1) {
					return -1;
				} else {
					return 0;
				}
			} else {
				if(p0Present) {
					return -1;
				} else {
					return +1;
				}
			}
		} catch (IOException e) {
			Log.e(TAG, e);
			return 0;
		}
	}

}
