package com.hourglassapps.cpi_ii.lucene;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import com.hourglassapps.util.Log;
import com.hourglassapps.util.SortedMultiMap;
import com.hourglassapps.util.TreeArrayMultiMap;

public abstract class AbstractTermFreqMapper implements TermHandler {
	private final static String TAG=AbstractTermFreqMapper.class.getName();
	
	@Override
	public void run(TermsEnum pTerms) {
		BytesRef term;
		try {
			while((term=pTerms.next())!=null) {
				long freq=pTerms.totalTermFreq();
				add(term.utf8ToString(), freq);
			}
		} catch (IOException e) {
			Log.e(TAG, e);
		}
		
	}
	
	public abstract void add(String pTerm, Long pFreq);
}
