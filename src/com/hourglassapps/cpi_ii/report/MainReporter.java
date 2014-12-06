package com.hourglassapps.cpi_ii.report;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.hourglassapps.cpi_ii.PoemRecord;
import com.hourglassapps.serialise.ParseException;
import com.hourglassapps.serialise.PoemRecordXMLParser;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.IOIterator;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;

public class MainReporter {
	private final static String TAG=MainReporter.class.getName();
	public final Path DOWNLOAD_PATH=Paths.get("downloaded_index");
	private final static String POEM_PANE_NAME="poems.html";
	private final static String POEM_DIR_NAME="poems";
	private final static Path DOCUMENT_DIR=Paths.get("documents");
	private final static String CSS="poem.css";
	
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
	
	public void create() throws IOException, ParseException {
		copy();
		try(
				PoemsPanel poems=new PoemsPanel(mDest.resolve(POEM_PANE_NAME));
				PoemRecordXMLParser parser=new PoemRecordXMLParser(new BufferedReader(new FileReader(mXML.toFile())));
				IOIterator<PoemRecord> it=parser.throwableIterator();
				) {
			while(it.hasNext()) {
				PoemRecord rec=it.next();
				if(!PoemRecord.LANG_LATIN.equals(rec.getLanguage())) {
					continue;
				}

				poems.addTitle(rec);
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
