package com.hourglassapps.cpi_ii;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

import com.hourglassapps.cpi_ii.stem.StemRecorderFilter;
import com.hourglassapps.serialise.Deserialiser;
import com.hourglassapps.util.AbstractComboExpander;
import com.hourglassapps.util.IdentityConverter;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.MultiMap;
import com.hourglassapps.util.SortedMultiMap;
import com.hourglassapps.util.TreeArrayMultiMap;

public class MainListIndexTerms {
	private final static String TAG=MainListIndexTerms.class.getName();
	public static void main(String[] args) {
		if(args.length>1) {
			Log.e(TAG, "Can accept at most one arg, the filename for serialised step group data");
			System.exit(-1);
		}
		
		boolean freqs=false;
		String stemFile=null;
		for(int i=0; i<args.length; i++) {
			String arg=args[i];
			switch(arg) {
			case "--display-freqs":
				freqs=true;
				break;
			default:
				stemFile=args[i];
			}
		}

		
		try {
			final ConductusIndex index=new ConductusIndex(new File("index"));
			if(freqs) {
				Freq2TermMapper mapper=new Freq2TermMapper();
				index.visitTerms(mapper);
				mapper.display();
			}
			
			if(stemFile!=null) {
				listAllTokenExpansions(index, stemFile);
			}
		} catch (NumberFormatException | IOException e) {
			Log.e(TAG, e);
		}
	}

	private static void listAllTokenExpansions(ConductusIndex index, String pStemFile) throws IOException {
		InputStream in;
		if("-".equals(pStemFile)) {
			in=System.in;
		} else {
			in=new BufferedInputStream(new FileInputStream(new File(pStemFile)));
		}
		
		Deserialiser<MultiMap<String, Set<String>, String>> deser=StemRecorderFilter.deserialiser();
		MultiMap<String, Set<String>, String> stem2Variants=deser.restore(in);
		
		final AbstractComboExpander<String, String> expander=
				new AbstractComboExpander<String, String>(stem2Variants, new IdentityConverter<String>()){

			@Override
			public void onExpansion(List<String> pExpansions) {
				if(pExpansions.size()>=1) {
					System.out.print(pExpansions.get(0));
				}
				for(String s: pExpansions.subList(1, pExpansions.size())) {
					System.out.print(' ');
					System.out.print(s);
				}
				System.out.println();
			}
			
		};
		TermHandler comboLister=new TermHandler(){

			@Override
			public void run(TermsEnum pTerms) throws IOException {
				BytesRef term;
				while((term=pTerms.next())!=null) {
					String ngram=term.utf8ToString();
					String terms[]=ngram.split(" ");
					int numPermutations=expander.expand(terms);
					if(numPermutations>0) {
						System.out.println();
					}
				}
			}
			
		};
		index.visitTerms(comboLister);
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
