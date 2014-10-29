package com.hourglassapps.cpi_ii;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import com.hourglassapps.cpi_ii.stem.StemRecorderFilter;
import com.hourglassapps.serialise.Deserialiser;
import com.hourglassapps.util.AbstractComboExpander;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.ExpansionReceiver;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.MultiMap;

public class MainListIndexTerms {
	private final static String TAG=MainListIndexTerms.class.getName();
	public static void main(String[] args) {
		if(args.length>1) {
			Log.e(TAG, "Can accept at most one arg, the filename for serialised stem group data");
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
			final IndexViewer index=new IndexViewer(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX);
			if(freqs) {
				Freq2TermMapper mapper=new Freq2TermMapper();
				index.visit(FieldVal.KEY, mapper);
				mapper.display();
			}
			
			if(stemFile!=null) {
				listAllTokenExpansions(index, stemFile, new ExpansionReceiver<String>(){
					//final AbstractComboExpander<String, String> expander=
					//		new AbstractComboExpander<String, String>(stem2Variants, new IdentityConverter<String>()){

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

					@Override
					public void onGroupDone(int pNumExpansions) {
						if(pNumExpansions>0) {
							System.out.println();
						}

					}
				});
			}
		} catch (NumberFormatException | IOException e) {
			Log.e(TAG, e);
		}
	}

	public static void listAllTokenExpansions(IndexViewer pIndex, String pStemFile, 
			final ExpansionReceiver<String> pReceiver) 
			throws IOException {
		InputStream in;
		if("-".equals(pStemFile)) {
			in=System.in;
		} else {
			in=new BufferedInputStream(new FileInputStream(new File(pStemFile)));
		}
		
		Deserialiser<MultiMap<String, Set<String>, String>> deser=StemRecorderFilter.deserialiser();
		MultiMap<String, Set<String>, String> stem2Variants=deser.restore(in);
		
		//make the 2nd argument to the AbstractComboExpander constructor null to eliminate n-grams containing '_' terms
		final AbstractComboExpander<String, String> expander=
				new AbstractComboExpander<String, String>(stem2Variants, null, pReceiver);
		TermHandler comboLister=new TermHandler() {
			@Override
			public void run(TermsEnum pTerms) throws IOException {
				BytesRef term;
				while((term=pTerms.next())!=null) {
					String ngram=term.utf8ToString();
					String terms[]=ngram.split(" ");
					int numPermutations=expander.expand(terms);
					pReceiver.onGroupDone(numPermutations);
				}
			}
		};
		pIndex.visit(FieldVal.KEY, comboLister);
	}
}
