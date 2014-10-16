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
	private ConductusIndex mIndex;
	
	public MainListIndexTerms() throws IOException {
		mIndex=new ConductusIndex(new File("index"));
	}
	
	public static void main(String[] args) {
		try {
			MainListIndexTerms reporter=new MainListIndexTerms();
			reporter.interrogateIndex();
		} catch (NumberFormatException | IOException e) {
			Log.e(TAG, e);
		}

	}

	private void interrogateIndex() throws IOException {
		try(IndexReader reader=DirectoryReader.open(mIndex.dir())) {
			SortedMultiMap<Long,List<String>,String> freq2Terms=
					new TreeArrayMultiMap<Long, String>(new Comparator<Long>() {

						@Override
						public int compare(Long pFst, Long pSnd) {
							return -pFst.compareTo(pSnd);
						}});
			Terms terms=SlowCompositeReaderWrapper.wrap(reader).terms(mIndex.CONTENT_KEY);
			TermsEnum e=terms.iterator(null);
			BytesRef term;
			while((term=e.next())!=null) {
				long freq=e.totalTermFreq();
				freq2Terms.addOne(freq, term.utf8ToString());
			}
			
			for(Long f: freq2Terms.keySet()) {
				for(String t: freq2Terms.get(f)) {
					System.out.println(t+"\t"+f);
				}
			}
		}
		
	}

}
