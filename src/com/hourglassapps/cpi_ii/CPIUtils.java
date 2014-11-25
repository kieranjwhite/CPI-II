package com.hourglassapps.cpi_ii;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import com.hourglassapps.cpi_ii.lucene.IndexViewer;
import com.hourglassapps.cpi_ii.lucene.TermHandler;
import com.hourglassapps.cpi_ii.stem.StemRecorderFilter;
import com.hourglassapps.util.Combinator;
import com.hourglassapps.util.ExpansionReceiver;
import com.hourglassapps.util.MultiMap;

public class CPIUtils {
	public static void listAllTokenExpansions(
			IndexViewer pIndex, 
			String pStemFile, 
			final ExpansionReceiver<String> pReceiver) 
			throws IOException {
		InputStream in;
		if("-".equals(pStemFile)) {
			in=System.in;
		} else {
			in=new BufferedInputStream(new FileInputStream(new File(pStemFile)));
		}
		
		MultiMap<String, Set<String>, String> stem2Variants=StemRecorderFilter.deserialiser().restore(in);
		
		/* A null 2nd argument to the AbstractComboExpander constructor eliminates n-grams containing '_' terms
		 * An IdentityConverter instance retains these n-grams.
		 */
		final Combinator<String, String> expander=
				new Combinator<String, String>(stem2Variants, null, pReceiver);
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
		pIndex.visit(CPIFields.KEY.fieldVal(), comboLister);
	}


}
