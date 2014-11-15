package com.hourglassapps.cpi_ii;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import com.hourglassapps.cpi_ii.stem.StemRecorderFilter;
import com.hourglassapps.util.Combinator;
import com.hourglassapps.util.ExpansionReceiver;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.MultiMap;

public class IndexViewer {
	private final static String TAG=IndexViewer.class.getName();
	
	private Directory mDir;
	
	public IndexViewer(File pDir) throws IOException {
		mDir=FSDirectory.open(pDir);
	}
	
	public Directory dir() {
		return mDir;
	}
	
	public void visitTerms(TermHandler pConsumer) throws IOException {
		visit(FieldVal.CONTENT, pConsumer);
	}

	public void visit(FieldVal pField, TermHandler pConsumer) throws IOException {
		try(AtomicReader reader=AtomicReader.open(dir()).leaves().get(0).reader()) {
			Fields fields=reader.fields();
			if(fields!=null) {
				Terms terms=fields.terms(pField.s());
				if(terms!=null) {
					TermsEnum e=terms.iterator(null);
					pConsumer.run(e);
				}
			}
		}

	}

	public void interrogate(FieldVal pSearchField, String pKey, int pNumResults, ResultRelayer pRelayer) throws IOException {
		try(IndexReader reader=DirectoryReader.open(dir())) {
			interrogate(reader, pSearchField, pKey, pNumResults, pRelayer);
		}
	}

	public void interrogate(FieldVal pSearchField, String pSought, ResultRelayer pRelayer) throws IOException {
		try(IndexReader reader=DirectoryReader.open(dir())) {
			/*
			for (int i=0; i<reader.maxDoc(); i++) {
			    Document doc = reader.document(i);
			    System.out.println("doc: "+doc.get(pKeyField.s()));
			}
			*/
			interrogate(reader, pSearchField, pSought, reader.numDocs(), pRelayer);
		}
	}

	private void interrogate(IndexReader pReader, FieldVal pSearchField, String pSought, int pNumResults, ResultRelayer pRelayer) throws IOException {
		IndexSearcher searcher = new IndexSearcher(pReader);
		interrogate(pReader, searcher, pSearchField, pSought, pNumResults, pRelayer);
	}

	public void listAllTokenExpansions(String pStemFile, 
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
		visit(FieldVal.KEY, comboLister);
	}

	public static void interrogate(IndexReader pReader, IndexSearcher pSearcher, FieldVal pSearchField, String pSought, int pNumResults, ResultRelayer pRelayer) throws IOException {
		TopDocs results = pSearcher.search(pSearchField.query(pSought), pNumResults);
		if(results.totalHits<1) {
			//Log.i(TAG, "unrecognised key "+pSought);
			return;
		}
		pRelayer.run(pReader, results);
		
	}


}
