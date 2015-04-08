package com.hourglassapps.cpi_ii.report.blacklist;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Log;

public class MainBlacklistReported {
	private final static String TAG=MainBlacklistReported.class.getName();
	private final static Path RESULTS_SUBDIR=Paths.get("results/completed");
	
	//private final static URLCanonicaliser URL_2_CANONICAL_URL=new URLCanonicaliser();
	private final Path mResultsDir;
	private final DocPath2URL mIndexReader;
	
	public MainBlacklistReported(Path pPoemsDir, Path pDocsDir) {
		mResultsDir=pPoemsDir.resolve(RESULTS_SUBDIR);
		mIndexReader=new DocPath2URL(pDocsDir);
	}
	
	public void exec() throws IOException {
		Set<String> urls=new HashSet<>();
		BlacklistAggregator aggregator=new BlacklistAggregator();
		Files.walkFileTree(mResultsDir, EnumSet.noneOf(FileVisitOption.class), 2, aggregator);
		Set<Path> docs=aggregator.get();
		for(Path doc: docs) {
			try {
				String urlStr=mIndexReader.convert(doc);
				mIndexReader.throwCaught(IOException.class);

				//URL url=URL_2_CANONICAL_URL.convert(new URL(urlStr));
				//URL_2_CANONICAL_URL.throwCaught(MalformedURLException.class);
				
				urls.add(urlStr);
			} catch(Throwable e) {
				Log.e(TAG, e);
			}
		}
		
		for(String url: urls) {
			System.out.println(url);
		}
	}
	
	private static void usage() {
		System.out.println("MainBlacklistReported <POEMS_DIRECTORY> <DOCUMENTS_DIRECTORY>");
	}
	
		
	public static void main(String pArgs[]) {
		try {
			if(pArgs.length!=2) {
				usage();
				System.exit(-1);
			}
			
			Path poemsDir=Paths.get(pArgs[0]);
			Path docsDir=Paths.get(pArgs[1]);
			
			MainBlacklistReported blacklister=new MainBlacklistReported(poemsDir, docsDir); 
			blacklister.exec();
			
		} catch(Exception e) {
			Log.e(TAG,e);
		}
	}
	
}
