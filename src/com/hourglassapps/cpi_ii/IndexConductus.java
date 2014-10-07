package com.hourglassapps.cpi_ii;

import java.io.File;
import java.io.IOException;

import com.hourglassapps.serialise.JSONParser;
import com.hourglassapps.serialise.ParseException;
import com.hourglassapps.serialise.Preprocessor;
import com.hourglassapps.util.Ii;
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
			Preprocessor preprocessor=null;
			try {
				preprocessor=new Preprocessor(new File(args[0]));
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
