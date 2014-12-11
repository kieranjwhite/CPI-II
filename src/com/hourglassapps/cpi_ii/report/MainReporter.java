package com.hourglassapps.cpi_ii.report;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.jdeferred.DoneFilter;
import org.jdeferred.DonePipe;
import org.jdeferred.ProgressCallback;
import org.jdeferred.Promise;

import com.hourglassapps.cpi_ii.PoemRecord;
import com.hourglassapps.cpi_ii.latin.StandardLatinAnalyzer;
import com.hourglassapps.cpi_ii.lucene.IndexViewer;
import com.hourglassapps.cpi_ii.web_search.MainDownloader;
import com.hourglassapps.persist.FileJournal;
import com.hourglassapps.persist.WrappedJournal;
import com.hourglassapps.serialise.ParseException;
import com.hourglassapps.serialise.PoemRecordXMLParser;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.IOIterator;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.URLUtils;

public class MainReporter {
	private final static String TAG=MainReporter.class.getName();
	public final Path DOWNLOAD_PATH=Paths.get("downloaded_index");
	private final static String POEM_DIR_NAME="poems";
	final static Path DOCUMENT_DIR=Paths.get("./documents");
	private final static String RESULT_START="results_start";
	private final static String RESULT_END="results_end";
	private final Path mXML;
	private final Path mDest;
	
	public MainReporter(Path pXml, Path pDocuments, Path pDest) throws IOException {
		if(!Files.exists(pDest)) {
			Files.createDirectory(pDest);
		}
		if(!Files.isDirectory(pDest) || !Files.isWritable(pDest)) {
			throw new IllegalArgumentException(pDest+" must be a writeable and directory");
		}
		
		mXML=pXml;
		mDest=pDest;
	}
	
	public void create() throws Exception {
		Analyzer analyser=StandardLatinAnalyzer.searchAnalyzer();

		try(final ConcreteThrower<Exception> thrower=new ConcreteThrower<>()) {
			Converter<String,String> queryToFilename=new Converter<String,String>() {
				private final static int MAX_LEN=196;
				private Map<String,String> mLongNameToShorter=new HashMap<>();
				private int mCurSuffix=0;
				
				@Override
				public String convert(String pIn) {
					try {
						String encoded=URLUtils.encode(pIn);
						assert encoded.indexOf('_')==-1;
						if(encoded.length()>MAX_LEN) {
							/*
							 * Assumes convert is invoked in a fixed order. If that order changes
							 * (eg if the the conductus xml export is modified) a new report will 
							 * have to be generated from scratch (ie by deleting the directory poems/results).
							 */
							if(mLongNameToShorter.containsKey(pIn)) {
								encoded=mLongNameToShorter.get(pIn);
							} else {
								encoded=(encoded.substring(0,MAX_LEN)+'_')+(mCurSuffix++);
								mLongNameToShorter.put(pIn, encoded);
							}
						}
						Paths.get(encoded); //This will trigger an InvalidPathException if encoded is still too long -- if that happens reduce MAX_LEN
						return encoded;
					} catch (UnsupportedEncodingException e) {
						thrower.ctch(e);
					}
					return null;
				}
			};


			try(
					IndexViewer index=new IndexViewer(MainDownloader.downloadIndex());
					PoemsReport poems=new PoemsReport(mDest, queryToFilename);
					WrappedJournal<Ii<String,Path>> journal=new WrappedJournal<>(poems.resultsDir(), new TitlePathConverter(thrower), 
							MainReporter.class, RESULT_START, RESULT_END);
					Queryer searcher=new Queryer(journal, index, analyser);
					PoemRecordXMLParser parser=new PoemRecordXMLParser(new BufferedReader(new FileReader(mXML.toFile())));
					IOIterator<PoemRecord> it=parser.throwableIterator();
					) {
				Promise<Void,Void,Ii<String,String>> prom=poems.promise();
				prom.progress(new ProgressCallback<Ii<String,String>>(){

					@Override
					public void onProgress(Ii<String, String> progress) {
						try {
							searcher.search(progress);
						} catch(IOException | org.apache.lucene.queryparser.classic.ParseException e) {
							thrower.ctch(e);
						}
					}

				});

				while(it.hasNext()) {
					if(thrower.fallThrough()) {
						break;
					}
					PoemRecord rec=it.next();
					if(!PoemRecord.LANG_LATIN.equals(rec.getLanguage())) {
						continue;
					}

					poems.addTitle(rec);
				}
				poems.genContent();
			}		
		}
	}
	
	private static void usage() {
		System.out.println("MainReporter <CONDUCTUS_XML_EXPORT>");
	}

	public static void main(String pArgs[]) throws IOException, ParseException {
		try {
			if(pArgs.length!=1) {
				usage();
				System.exit(-1);
			}

			Path xml=Paths.get(pArgs[0]);
			Path dest=Paths.get(POEM_DIR_NAME);
			new MainReporter(xml, DOCUMENT_DIR, dest).create();
		} catch(Exception e) {
			Log.e(TAG,e);
		}
	}
}
