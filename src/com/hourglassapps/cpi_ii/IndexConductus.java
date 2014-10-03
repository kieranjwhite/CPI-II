package com.hourglassapps.cpi_ii;

import java.io.File;
import java.io.IOException;

import com.hourglassapps.serialise.ParseException;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;

public class IndexConductus {
	private final static String TAG=IndexConductus.class.getName();
	
	public IndexConductus() {
	}
	
	public void main(String[] args) {
		if(args.length!=1) {
			Log.e(TAG, "Must provide filename of JSON data");
			System.exit(-1);
		}
		
		Indexer indexer=null;
		try {
			indexer=new Indexer(new File("index"));
			JSONParser<Long, String, PoemRecord> parser=new JSONParser(args[0], PoemRecord.class);
			while(parser.hasNext()) {
				Ii<Long, String> record=parser.next();
				if(record==null) {
					//an exception will cause this
					break;
				}
				indexer.add(record.fst(), record.snd());
			}
			
			indexer.close();
			
			Throwable t=parser.caught();
			if(t!=null) {
				throw t;
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
