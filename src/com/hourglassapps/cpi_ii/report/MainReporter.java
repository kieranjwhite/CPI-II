package com.hourglassapps.cpi_ii.report;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
	private final static String RESULTS="results";
	private final static String POEM_PANE_NAME="poems.html";
	private final static String POEM_DIR_NAME="poems";
	private final static Path DOCUMENT_DIR=Paths.get("documents");
	private final static String CSS="poem.css";
	
	private final static class PathConverter implements Converter<Path,String> {
		private final ConcreteThrower<Exception> mThrower;
		
		public PathConverter(ConcreteThrower<Exception> pThrower) {
			mThrower=pThrower;
		}
		
		@Override
		public String convert(Path pIn) {
			try {
				return pIn.toRealPath().toString();
			} catch(IOException e) {
				mThrower.ctch(e);
			}
			return null;
		}
		
	};
	
	private final Path mXML;
	private final Path mDocuments;
	private final Path mDest;
	
	private final static Converter<String,String> LINE_TO_REL_URL=
			new Converter<String,String>() {
		@Override
		public String convert(String pIn) {
			return null;
		}
	};

	public MainReporter(Path pXml, Path pDocuments, Path pDest) throws IOException {
		if(!Files.exists(pDest)) {
			Files.createDirectory(pDest);
		}
		if(!Files.isDirectory(pDest) || !Files.isWritable(pDest)) {
			throw new IllegalArgumentException(pDest+" must be a writeable and directory");
		}
		
		mXML=pXml;
		mDocuments=pDocuments;
		mDest=pDest;
	}
	
	private void copy() throws IOException {
		try(InputStream in=MainReporter.class.getResourceAsStream(CSS)) {
			Rtu.copyFile(in, mDest.resolve(CSS));
		}
	}
	
	public void create() throws Exception {
		copy();
		Analyzer analyser=StandardLatinAnalyzer.searchAnalyzer();

		try(final ConcreteThrower<Exception> thrower=new ConcreteThrower<>()) {
			Converter<String,String> queryToFilename=new Converter<String,String>() {
				@Override
				public String convert(String pIn) {
					try {
						return URLUtils.encode(pIn)+".html";
					} catch (UnsupportedEncodingException e) {
						thrower.ctch(e);
					}
					return null;
				}
			};


			try(
					IndexViewer index=new IndexViewer(MainDownloader.downloadIndex());
					FileJournal<Path> journal=new FileJournal<>(Paths.get(RESULTS), new PathConverter(thrower));
					Queryer searcher=new Queryer(journal, index, analyser);
					PoemsReport poems=new PoemsReport(mDest.resolve(POEM_PANE_NAME), analyser, queryToFilename);
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
				//prom.then(doneCallback, failCallback, progressCallback);

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
