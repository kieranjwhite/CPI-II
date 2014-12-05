package com.hourglassapps.cpi_ii.report;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

public class MainReporter {
	private final static String TAG=MainReporter.class.getName();
	public final Path DOWNLOAD_PATH=Paths.get("downloaded_index");
	private final static String POEM_PANE_NAME="poems.html";
	private final static String POEM_DIR_NAME="poems";
	private final static Converter<Ii<Long,String>,String> POEM_ID_TITLE_TO_REL_URL=
			new Converter<Ii<Long,String>,String>() {
		@Override
		public String convert(Ii<Long, String> pIn) {
			return pIn.fst()+"_"+pIn.snd()+"/index.html";
		}
	};

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
			if(!Files.exists(dest)) {
				Files.createDirectory(dest);
			}
			if(!Files.isDirectory(dest) || !Files.isWritable(dest)) {
				Log.e(TAG, dest+" must be a writeable and directory");
				System.exit(-1);
			}


			try(
					PoemsPane poems=new PoemsPane(dest.resolve(POEM_PANE_NAME), POEM_ID_TITLE_TO_REL_URL);
					PoemRecordXMLParser parser=new PoemRecordXMLParser(new BufferedReader(new FileReader(xml.toFile())));
					IOIterator<PoemRecord> it=parser.throwableIterator();
					) {
				while(it.hasNext()) {
					PoemRecord rec=it.next();
					String title=rec.getTitle();
					long id=rec.id();
					poems.add(new Ii<Long,String>(id, title));
				}
			}
		} catch(Exception e) {
			Log.e(TAG,e);
		}
	}
}
