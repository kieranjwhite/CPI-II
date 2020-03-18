package com.hourglassapps.cpi_ii.report;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import com.hourglassapps.cpi_ii.report.blacklist.MainBlacklistReported;
import com.hourglassapps.util.Log;

public class MainArchive {
    private final static String TAG=MainArchive.class.getName();
    private final static Path ARCHIVE_DIR=Paths.get("archives");
    private final static Path POEMS_DIR=Paths.get("poems");
    private final static Path DOCUMENTS_DIR=Paths.get("documents");
    
    private static void usage() {
	System.out.println("Usage: java -cp <CLASS_PATH> com.hourglassapps.cpi_ii.report.MainArchive <ARCHIVE_NAME>");
    }
	
    public static void main(String pArgs[]) {
	try {
	    if(pArgs.length<1) {
		usage();
		System.exit(-1);
	    }

	    if(!Files.exists(ARCHIVE_DIR)) {
		Files.createDirectory(ARCHIVE_DIR);
	    }

	    if(!Files.isDirectory(ARCHIVE_DIR) || !Files.isWritable(ARCHIVE_DIR)) {
		throw new IllegalStateException("Destination directory "+ARCHIVE_DIR.toString()+" not found");
	    }

	    Path archivePath=ARCHIVE_DIR.resolve(pArgs[0]);
	    if(Files.exists(archivePath)) {
		throw new IllegalStateException("A directory named "+archivePath+" already exists. The name provided must be unique.");
	    }

	    MainBlacklistReported blacklister=new MainBlacklistReported(POEMS_DIR, DOCUMENTS_DIR);
	    blacklister.exec();

	    Files.createDirectory(archivePath);
	    Path newPoemsDir=archivePath.resolve(POEMS_DIR);
	    Path newDocsDir=archivePath.resolve(DOCUMENTS_DIR);
	    
	    Files.move(POEMS_DIR, newPoemsDir);
	    Files.move(DOCUMENTS_DIR, newDocsDir);
	} catch(Exception e) {
	    Log.e(TAG, e);
	}
    }
}
