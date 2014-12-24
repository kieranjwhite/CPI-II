package com.hourglassapps.cpi_ii.report;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
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
import com.hourglassapps.cpi_ii.report.LineGenerator.Line;
import com.hourglassapps.cpi_ii.report.LineGenerator.LineType;
import com.hourglassapps.cpi_ii.web_search.MainDownloader;
import com.hourglassapps.persist.FileCopyJournal;
import com.hourglassapps.persist.ResultsJournal;
import com.hourglassapps.persist.Shortener;
import com.hourglassapps.persist.WrappedJournal;
import com.hourglassapps.serialise.ParseException;
import com.hourglassapps.serialise.PoemRecordXMLParser;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Filter;
import com.hourglassapps.util.IOIterator;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Typed;

public class MainReporter {
	private final static String TAG=MainReporter.class.getName();
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
	
	public void create() throws Exception {
		
		try(final ConcreteThrower<Exception> thrower=new ConcreteThrower<>()) {
			Analyzer analyser=StandardLatinAnalyzer.searchAnalyzer();
			final Converter<String,String> shortener=new Shortener(thrower);
			Converter<Line,String> queryToHashTag=new Converter<Line,String>() {
				private final static String TITLE_PREFIX="eprintid_";
				private final static String SINGLE_TAG="_single_";
				
				/* Counter to generate unique filenames -- only works if all filenames are generated on each run (which is currently the case)
				 * Otherwise an interrupting a run could result in a file being generated with one filename but linked to the report on a subsequent
				 * run as a different filename. */
				private int mOneWordCnt=0;

				@Override
				public String convert(Line pIn) {
					/*
					 * HashTags have a particular format.
					 * 
					 * If they contain a '_' character they are singled out for special treatment in result_list.js when displaying results 
					 * header and the word between the first two spaces (or '+' signs as they'll be url encoded) is displayed as a label.
					 * Then a colon is inserted followed by the remaining string. Any text preceding the first plus is not included in the
					 * header.
					 * 
					 * If there is no '_' then the entire string returned by this function is displayed in the header.
					 * 
					 */
					String cleaned=pIn.cleaned();
					assert cleaned.indexOf('_')==-1;
					assert !cleaned.matches("[0-9]");
					if(pIn.type()==LineType.TITLE) {
						return shortener.convert(TITLE_PREFIX+Long.toString(pIn.eprintId())+" Entire "+cleaned);
					} else {
						if(ONE_WORD_LINE_SPOTTER.accept(pIn)) {
							//we can use numbers to ensure there's no filename collision between this line and a normal multi-word line
							return shortener.convert(TITLE_PREFIX+Long.toString(pIn.eprintId())+SINGLE_TAG+(mOneWordCnt++)+" Adjacent "+cleaned);
						} else {
							return shortener.convert(cleaned);
						}
					}
				}
			};

			try(
					IndexViewer index=new IndexViewer(MainDownloader.downloadIndex());
					PoemsReport poems=new PoemsReport(mDest, queryToHashTag, thrower);
					ResultsJournal journal=new ResultsJournal(poems.resultsDir(), mDocDir, 
							new TitlePathConverter(thrower), 
							MainReporter.class, RESULT_START, RESULT_END);
					Queryer searcher=new Queryer(journal, index, analyser, QUERY_GENERATOR);
					PoemRecordXMLParser parser=new PoemRecordXMLParser(new BufferedReader(new FileReader(mXML.toFile())));
					IOIterator<PoemRecord> it=parser.throwableIterator();
					) {
				Promise<Void,Void,Ii<Line,String>> prom=poems.promise();
				prom.progress(new ProgressCallback<Ii<Line,String>>(){

					@Override
					public void onProgress(Ii<Line, String> progress) {
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
