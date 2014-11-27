package com.hourglassapps.cpi_ii;

import static java.util.EnumSet.noneOf;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

import com.hourglassapps.cpi_ii.latin.LatinAnalyzer;
import com.hourglassapps.cpi_ii.latin.StandardLatinAnalyzer;
import com.hourglassapps.cpi_ii.lucene.LuceneVisitor;
import com.hourglassapps.cpi_ii.lucene.Indexer;
import com.hourglassapps.cpi_ii.stem.StemRecorderFilter;
import com.hourglassapps.cpi_ii.stem.StempelRecorderFilter;
import com.hourglassapps.util.Log;

public class MainIndexDownloaded {
	private final static String TAG=MainIndexDownloaded.class.getName();
	private final static String INDEX_PATH="downloaded_index";

	public static void main(String[] pDirs) throws IOException {
		Thread closer;
		try(
				@SuppressWarnings("resource")
				Analyzer analyser=new StandardLatinAnalyzer(LatinAnalyzer.PERSEUS_STOPWORD_FILE).
				setStemmer(new StemRecorderFilter.Factory() {

					@Override
					public StemRecorderFilter inst(TokenStream pInput) throws IOException {
						return new StempelRecorderFilter(pInput, false, new File("data/com/hourglassapps/cpi_ii/latin/stem/stempel/model.out"));
					}

				});
				final Indexer indexer=new Indexer(Paths.get(INDEX_PATH), analyser, false);
				) {
			closer=new Thread() {

				@Override
				public void run() {
					try {
						indexer.close();
					} catch (IOException e) {
						Log.e(TAG, e);
					}
				}
				
			};
			Runtime.getRuntime().addShutdownHook(closer);
			FileVisitor<Path> visitor=new LuceneVisitor(indexer, new TextContentReaderFactory(false));
			for(String dir: pDirs) {
				Path path=Paths.get(dir);
				Files.walkFileTree(path, noneOf(FileVisitOption.class), 3, visitor);
			}
		}
		Runtime.getRuntime().removeShutdownHook(closer);
	}
}
