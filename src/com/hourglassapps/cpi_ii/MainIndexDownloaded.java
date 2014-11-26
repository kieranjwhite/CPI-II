package com.hourglassapps.cpi_ii;

import static java.util.EnumSet.noneOf;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;

import com.hourglassapps.cpi_ii.latin.LatinAnalyzer;
import com.hourglassapps.cpi_ii.latin.StandardLatinAnalyzer;
import com.hourglassapps.cpi_ii.lucene.FileCrawler;
import com.hourglassapps.cpi_ii.lucene.Indexer;

public class MainIndexDownloaded {
	private final static String INDEX_PATH="downloaded_index";

	public static void main(String[] pDirs) throws IOException {
		try(
				@SuppressWarnings("resource")
				Analyzer analyser=new StandardLatinAnalyzer(LatinAnalyzer.PERSEUS_STOPWORD_FILE).
				setStemmer(LatinAnalyzer.STEMPEL_RECORDER_FACTORY);
				Indexer indexer=new Indexer(Paths.get(INDEX_PATH), analyser, true);
				) {
			FileVisitor<Path> visitor=new FileCrawler(indexer.writer(), new TextContentReaderFactory());
			for(String dir: pDirs) {
				Path path=Paths.get(dir);
				Files.walkFileTree(path, noneOf(FileVisitOption.class), 3, visitor);
			}
		}
	}
}
