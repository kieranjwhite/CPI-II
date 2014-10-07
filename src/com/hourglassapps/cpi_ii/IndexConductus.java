package com.hourglassapps.cpi_ii;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import com.hourglassapps.serialise.JSONParser;
import com.hourglassapps.serialise.RemoveUnescapesReader;
import com.hourglassapps.util.Log;

public class IndexConductus {
	private final static String TAG=IndexConductus.class.getName();
	
	public IndexConductus() {
	}
	
	public static void main(String[] args) {
		if(args.length!=1) {
			Log.e(TAG, "Must provide filename of JSON data");
			System.exit(-1);
		}
		
		Indexer indexer=null;
		try {
			indexer=new Indexer(new File("index"));
			Reader preprocessor=null;
			try {
				preprocessor=new RemoveUnescapesReader(new BufferedReader(new FileReader(new File(args[0]))));
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
		} catch (Throwable e) {
			Log.e(TAG, e);
		} finally {
			try {
				if(indexer!=null) {
					indexer.close();
				}
			} catch (IOException e) {
				Log.e(TAG, e);
			}
		}
	}
}
