package com.hourglassapps.cpi_ii.report.blacklist;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlacklistAggregator implements FileVisitor<Path> {
	private final static Path LINKS_FILE=Paths.get("links.js");
	private final static Pattern mDocPathPattern=Pattern.compile("^\\{t:.*p:\"(documents/.*/[0-9]+(?:\\.[^\"]+))\",s:\\[.*$");
	//private final static Pattern mDocPathPattern=Pattern.compile("^\\{t:.*p:\"(documents/.*/[0-9]+.*)\",s:\\[.*$");
	private final SortedSet<Path> mDocuments=new TreeSet<>();

	@Override
	public FileVisitResult postVisitDirectory(Path arg0,
			IOException arg1) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path arg0,
			BasicFileAttributes arg1) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path pFile, BasicFileAttributes arg1)
			throws IOException {
		if(LINKS_FILE.equals(pFile.getFileName())) {
			try(BufferedReader in=new BufferedReader(new FileReader(pFile.toFile()))) {
				String line=in.readLine();
				while(line!=null) {
					Matcher groups=mDocPathPattern.matcher(line);
					if(groups.matches()) {
						String docFile=groups.group(1);
						if(docFile!=null) {
							mDocuments.add(Paths.get(docFile));
						}
					}
					line=in.readLine();
				}
			}
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path arg0, IOException arg1)
			throws IOException {
		return FileVisitResult.CONTINUE;
	}
	
	public SortedSet<Path> get() {
		return Collections.unmodifiableSortedSet(mDocuments);
	}
}