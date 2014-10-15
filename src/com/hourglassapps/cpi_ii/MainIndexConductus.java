package com.hourglassapps.cpi_ii;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

import com.hourglassapps.serialise.JSONParser;
import com.hourglassapps.serialise.RemoveUnescapesReader;
import com.hourglassapps.util.Log;

public class MainIndexConductus {
	private final static String TAG=MainIndexConductus.class.getName();
	
	public MainIndexConductus() {
	}

	public static void index(String pInputPath) {
		try(Indexer indexer=new Indexer(new ConductusIndex(new File("index")))) {
			Reader preprocessor=null;
			try {
				preprocessor=new RemoveUnescapesReader(new BufferedReader(new FileReader(new File(pInputPath))));
				JSONParser<Long, String, PoemRecord> parser=new JSONParser<>(preprocessor, PoemRecord.class);
				while(parser.hasNext()) {
					PoemRecord record=parser.next();
					if(record==null) {
						//an exception will cause this
						break;
					}
					if(record.ignore()) {
						continue;
					}
					indexer.add(record.id(), record.content());
				}

				parser.throwCaught();
			} finally {
				if(preprocessor!=null) {
					preprocessor.close();
				}
			}
			
			indexer.displayStemGroups();
			
		} catch (Throwable e) {
			Log.e(TAG, e);
		}
	}
	
	public static void main(String[] args) {
		if(args.length!=1) {
			Log.e(TAG, "Must provide filename of JSON data");
			System.exit(-1);
		}
		index(args[0]);
	}
}
