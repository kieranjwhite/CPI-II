package com.hourglassapps.cpi_ii;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import com.hourglassapps.util.Log;
import com.hourglassapps.util.SortedMultiMap;
import com.hourglassapps.util.TreeArrayMultiMap;

public class MainListIndexTerms {
	private final static String TAG=MainListIndexTerms.class.getName();
	public static void main(String[] args) {
		try {
			ConductusIndex index=new ConductusIndex(new File("index"));
			Freq2TermMapper mapper=new Freq2TermMapper();
			index.visitTerms(mapper);
			mapper.display();
		} catch (NumberFormatException | IOException e) {
			Log.e(TAG, e);
		}
	}

	private static class Freq2TermMapper implements TermHandler {
		private SortedMultiMap<Long,List<String>,String> mFreq2Terms=
				new TreeArrayMultiMap<Long, String>(new Comparator<Long>() {

					@Override
					public int compare(Long pFst, Long pSnd) {
						return -pFst.compareTo(pSnd);
					}});

		@Override
		public void run(TermsEnum pTerms) throws IOException {
			BytesRef term;
			while((term=pTerms.next())!=null) {
				long freq=pTerms.totalTermFreq();
				mFreq2Terms.addOne(freq, term.utf8ToString());
			}
		}

		public void display() {
			for(Long f: mFreq2Terms.keySet()) {
				for(String t: mFreq2Terms.get(f)) {
					System.out.println(t+"\t"+f);
				}
			}			
		}

	}

}
