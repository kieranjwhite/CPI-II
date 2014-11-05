package com.hourglassapps.cpi_ii.web_search;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.hourglassapps.cpi_ii.IndexViewer;
import com.hourglassapps.cpi_ii.Journal;
import com.hourglassapps.cpi_ii.MainIndexConductus;
import com.hourglassapps.cpi_ii.MainListIndexTerms;
import com.hourglassapps.cpi_ii.web_search.bing.BingSearchEngine;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.FileJournal;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Typed;

public class MainDownload {
	private final static String TAG=MainDownload.class.getName();
	private final static File JOURNAL=new File("journal");
	private final static String PATH_ENCODING=StandardCharsets.UTF_8.toString();
	private final static Journal<String,URL> NULL_JOURNAL=new Journal<String,URL>() {

		@Override
		public boolean has(String pKey) {
			return false;
		}

		@Override
		public void startEntry() throws IOException {
		}

		@Override
		public void add(Typed<URL> pContent) throws IOException {
		}

		@Override
		public void commitEntry(String pKey) throws IOException {
		}

		@Override
		public void reset() throws IOException {
		}
		
	};
	
	public static void main(String pArgs[]) throws IOException {
		if(pArgs.length<1) {
			System.out.println("Usage java com.hourglassapps.cpi_ii.web_search.MainDownload <STEM_FILE>");
			System.out.println("      java com.hourglassapps.cpi_ii.web_search.MainDownload --real <STEM_FILE>");
		}

		boolean dummyRun=true;
		int pathIdx=0;
		if("--real".equals(pArgs[0])) {
			dummyRun=false;
			pathIdx++;
		}
		if(dummyRun) {
			System.out.println("Dummy run...");
		} else {
			System.out.println("Querying search engine...");
		}
		String path=pArgs[pathIdx];

		try(final BingSearchEngine q=dummyRun?new BingSearchEngine() : new BingSearchEngine(BingSearchEngine.AUTH_KEY)) {
			Journal<String,URL> journal=dummyRun?NULL_JOURNAL:new FileJournal<String,URL>(JOURNAL, 
					new Converter<String, String>() {

				@Override
				public String convert(String pIn) {
					//Creates a filename from pIn, a query's first disjunction
					try {
						return URLEncoder.encode(pIn, PATH_ENCODING);
					} catch (UnsupportedEncodingException e) {
						Log.e(TAG, e);
						System.exit(-1);
						return null;
					}
				}
			},
			new Converter<URL, ReadableByteChannel>() {

				@Override
				public ReadableByteChannel convert(URL pIn) {
					//Creates a channel through which a link can be downloaded
					try {
						return Channels.newChannel(pIn.openStream());
					} catch(IOException e) {
						Log.e(TAG, e);
						return null;
					}
				}
			}
					);	
			try(QueryThread<String> receiver=new QueryThread<String>(q, journal)) {
				receiver.start();
				if(!q.filterSites(new HashSet<String>(Arrays.<String>asList(new String[] {
						"catalogue.conductus.ac.uk", "diamm.ac.uk", "chmtl.indiana.edu/tml",
						"archive.org/details/analectahymnicam20drev",
						"archive.org/details/analectahymnica21drevuoft",
						"archive.org/details/analectahymnicam21drev"
				})))) {
					boolean blacklisted=q.filterPhrases(new HashSet<String>(Arrays.<String>asList(new String[] {
							"Cantum pulcriorem invenire", "DIAMM", "MUSICARUM LATINARUM", "Analecta hymnica"
					})));
					if(!blacklisted) {
						Log.e(TAG, "failed to blacklist");
						System.out.println();
					}
				}
				IndexViewer index=new IndexViewer(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX);
				MainListIndexTerms.listAllTokenExpansions(index, path, receiver);
			}
		} catch (Exception e) {
			Log.e(TAG, e);
		}
	}
}
