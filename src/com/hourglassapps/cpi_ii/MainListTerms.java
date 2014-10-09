package com.hourglassapps.cpi_ii;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;

import com.hourglassapps.util.Log;

public class MainListTerms {
	private final static String TAG=MainListTerms.class.getName();
	
	private ConductusIndex mIndex;
	
	public MainListTerms() throws IOException {
		mIndex=new ConductusIndex(new File("index"));
	}
	
	public static void main(String[] args) {
		if(args.length!=1) {
			Log.e(TAG, "Must provide eprintid");
			System.exit(-1);
		}

		try {
			MainListTerms reporter=new MainListTerms();
			reporter.interrogateIndex(Long.parseLong(args[0]));
		} catch (NumberFormatException | IOException e) {
			Log.e(TAG, e);
		}
	}

	private static double idf(int pTotalDocs, int pTermDocs) {
		return Math.log(pTotalDocs)-Math.log(pTermDocs);
	}
	
	private static double tfIdf(int pTotalDocs, int pTermDocs, long tf) {
		return tf*idf(pTotalDocs, pTermDocs);
	}
	
	private void showDocTerms(IndexReader reader, int pDocId, int pTotalDocs) throws IOException {
		SortedMap<Double, List<String>> scores=new TreeMap<>();
		
	    Document doc = reader.document(pDocId);         
	    Terms termVector = reader.getTermVector(pDocId, ConductusIndex.CONTENT_KEY);
	    TermsEnum itr = termVector.iterator(null);
	    BytesRef term = null;

	    while ((term = itr.next()) != null) {               
	        String termText = term.utf8ToString();                              
	        long tf = itr.totalTermFreq(); //yup just using raw tf right now
	        int termDocs = reader.docFreq(mIndex.term(termText));
	        
	        Double tfIdf=tfIdf(pTotalDocs, termDocs, tf);
	        
	        List<String> terms;
	        if(scores.containsKey(tfIdf)) {
	        	terms=scores.get(tfIdf);
	        } else {
	        	terms=new ArrayList<>();
	        	scores.put(tfIdf, terms);
	        }
	        terms.add(termText);
	    }
	    
	    for(Map.Entry<Double, List<String>> scoreTerm: scores.entrySet()) {
	    	for(String termText: scoreTerm.getValue()) {
	    		Log.i(TAG, termText+": "+scoreTerm.getKey()+"\n");
	    	}
	    	
	    }
	}
	
	public void interrogateIndex(long pEprintId) throws IOException {
		IndexReader reader=null;
		try {
			reader=DirectoryReader.open(mIndex.dir());
			IndexSearcher searcher = new IndexSearcher(reader);

			TopDocs results = searcher.search(mIndex.eprintIdQuery(pEprintId), 1);
			if(results.totalHits<1) {
				Log.i(TAG, "unrecognised eprintid "+pEprintId);
				return;
			}
			int docId=results.scoreDocs[0].doc;
			int totalDocs=reader.numDocs();
			showDocTerms(reader, docId, totalDocs);
		} finally {
			if(reader!=null) {
				reader.close();
			}
		}
	}
	
}
