package com.hourglassapps.cpi_ii.report;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.jdeferred.DoneCallback;
import org.jdeferred.DoneFilter;
import org.jdeferred.DonePipe;
import org.jdeferred.ProgressCallback;
import org.jdeferred.Promise;

import com.hourglassapps.cpi_ii.PoemRecord;
import com.hourglassapps.cpi_ii.latin.StandardLatinAnalyzer;
import com.hourglassapps.cpi_ii.lucene.IndexViewer;
import com.hourglassapps.cpi_ii.report.LineGenerator.Line;
import com.hourglassapps.cpi_ii.report.LineGenerator.LineType;
import com.hourglassapps.cpi_ii.web_search.MainDownloader;
import com.hourglassapps.persist.MainHashTagDict;
import com.hourglassapps.persist.ResultsJournal;
import com.hourglassapps.persist.Shortener;
import com.hourglassapps.serialise.ParseException;
import com.hourglassapps.serialise.PoemRecordXMLParser;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Filter;
import com.hourglassapps.util.IOIterator;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Typed;
import com.hourglassapps.util.URLUtils;

public class MainReporter {
	private final static String TAG=MainReporter.class.getName();
	private final static int MAX_PATH_LEN=32;
	public final Path DOWNLOAD_PATH=Paths.get("downloaded_index");
	private final static String POEM_DIR_NAME="poems";
	final static Path DOCUMENT_DIR=Paths.get("./documents");
	private final static String RESULT_START="results_start";
	private final static String RESULT_END="results_end";
	private final Path mXML;
	private final Path mDest;
	private final Path mDocDir;
	
	private final static Filter<Line> ONE_WORD_LINE_SPOTTER=new Filter<Line>(){
		@Override
		public boolean accept(Line pIn) {
			return pIn.cleaned().indexOf(' ')==-1;
		}
	};
	private final static Converter<Line,List<String>> QUERY_GENERATOR=new QueryGenerator(ONE_WORD_LINE_SPOTTER);
	
	public MainReporter(Path pXml, Path pDocuments, Path pDest) throws IOException {
		mDocDir=pDocuments;
		if(!Files.exists(pDest)) {
			Files.createDirectory(pDest);
		}
		if(!Files.isDirectory(pDest) || !Files.isWritable(pDest)) {
			throw new IllegalArgumentException(pDest+" must be a writeable and directory");
		}
		
		mXML=pXml;
		mDest=pDest;
	}
	
	public void create(int pNumBatches) throws Exception {
		
		try(final ConcreteThrower<Exception> thrower=new ConcreteThrower<>()) {
			Analyzer analyser=StandardLatinAnalyzer.searchAnalyzer();
			final Converter<String,String> shortener=new Shortener(MAX_PATH_LEN, thrower);
			Converter<Line,Ii<String,String>> queryToHashIdFilename=new Converter<Line,Ii<String,String>>() {
				private final static String TITLE_PREFIX="eprintid_";
				private final static String SINGLE_TAG="_single_";
				private int mOneWordCnt=0;
				@Override
				public Ii<String,String> convert(Line pIn) {

					MainHashTagDict hashTag=new MainHashTagDict();

					String cleaned=pIn.cleaned();
					assert cleaned.indexOf('_')==-1;
					assert !cleaned.matches("[0-9]");

					String shortenedLine=shortener.convert(cleaned);
					String filename; //this filename will be one of the files in poems/results/completed/
					
					if(pIn.type()==LineType.TITLE) {
						hashTag.put("t", "Entire: "+shortenedLine);
						filename=TITLE_PREFIX+pIn.eprintId();
					} else {
						if(ONE_WORD_LINE_SPOTTER.accept(pIn)) {
							hashTag.put("t", "Adjacent: "+shortenedLine);
							filename=TITLE_PREFIX+pIn.eprintId()+SINGLE_TAG+(mOneWordCnt++);
						} else {
							filename=shortenedLine;
						}
					}
					hashTag.put("f", filename);
					return new Ii<String,String>(hashTag.encode(), filename); //this will be passed to the Queryer instance view the onProgress method invocation below
				}
			};

			try(
					IndexViewer index=new IndexViewer(MainDownloader.downloadIndex());
					ResultsJournal journal=new ResultsJournal(PoemsReport.resultsDir(mDest), mDocDir,
							new TitlePathConverter(thrower), 
							MainReporter.class, RESULT_START, RESULT_END);
					Queryer searcher=new Queryer(journal, index, analyser, QUERY_GENERATOR, pNumBatches);
					) {
				Promise<Void,Void,Ii<Line,String>> prom;
				try(	
						PoemRecordXMLParser parser=new PoemRecordXMLParser(new BufferedReader(new FileReader(mXML.toFile())));
						IOIterator<PoemRecord> it=parser.throwableIterator();
						PoemsReport poems=new PoemsReport(mDest, queryToHashIdFilename, thrower);
						) {
					prom=poems.promise();
					prom.progress(new ProgressCallback<Ii<Line,String>>(){

						@Override
						public void onProgress(Ii<Line, String> pProgress) {
							try {
								searcher.include(pProgress);
							} catch(IOException e) {
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
				prom.then(new DoneCallback<Void>(){

					@Override
					public void onDone(Void result) {
						try {
							searcher.search();
						} catch(IOException | org.apache.lucene.queryparser.classic.ParseException e) {
							thrower.ctch(e);
						}
					}
					
				});
			}		
		}
	}

	private static void usage() {
		System.out.println("MainReporter <CONDUCTUS_XML_EXPORT> <NUM_BATCHES>");
	}

	public static void main(String pArgs[]) throws IOException, ParseException {
		try {
			if(pArgs.length!=2) {
				usage();
				System.exit(-1);
			}

			Path xml=Paths.get(pArgs[0]);
			int numBatches=Integer.valueOf(pArgs[1]);
			Path dest=Paths.get(POEM_DIR_NAME);
			new MainReporter(xml, DOCUMENT_DIR, dest).create(numBatches);
		} catch(Exception e) {
			Log.e(TAG,e);
		}
	}
}
