package com.hourglassapps.cpi_ii.report;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
			final Converter<String,String> shortener=new PathShortener(thrower);
			Converter<Line,String> queryToFilename=new Converter<Line,String>() {
				private final static String TITLE_PREFIX="eprint_id_";
				
				@Override
				public String convert(Line pIn) {
					String cleaned=pIn.cleaned();
					assert !cleaned.matches("[0-9]");
					if(pIn.type()==LineType.TITLE) {
						return TITLE_PREFIX+Long.toString(pIn.eprintId());
					} else {
						return shortener.convert(cleaned);
					}
				}
			};


			try(
					IndexViewer index=new IndexViewer(MainDownloader.downloadIndex());
					PoemsReport poems=new PoemsReport(mDest, queryToFilename);
					WrappedJournal<Ii<String,Path>> journal=new WrappedJournal<>(poems.resultsDir(), new TitlePathConverter(thrower), 
							MainReporter.class, RESULT_START, RESULT_END);
					Queryer searcher=new Queryer(journal, index, analyser, new Converter<Line,String>() {

						@Override
						public String convert(Line pIn) {
							if(pIn.type()==LineType.TITLE) {
								Set<String> body=new HashSet<>();
								body.add("\""+pIn.cleaned()+"\"");
								Line l=pIn.next();
								while(l!=null) {
									if(l.type()!=LineType.BODY) {
										continue;
									}
									body.add(convert(l));
									l=l.next();
								}
								return Rtu.join(new ArrayList<>(body), " ");
							} else {
								return "\""+pIn.cleaned()+"\"";
							}
						}
						
					});
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
				poems.genContent(thrower);
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
