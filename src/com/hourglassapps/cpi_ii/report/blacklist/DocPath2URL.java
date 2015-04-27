package com.hourglassapps.cpi_ii.report.blacklist;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.hourglassapps.persist.AbstractFilesJournal;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Thrower;

public class DocPath2URL implements Converter<Path, String>, Thrower {
	private final static String TAG=DocPath2URL.class.getName();
	private final static Path DOCUMENTS_DIRNAME=Paths.get("documents");
	private final Path mBaseDir;
	private final ConcreteThrower<IOException> mThrower=new ConcreteThrower<>();
	
	private Path mCurIndexFile=null;
	private List<String> mCurURLs=new ArrayList<String>();
	
	public DocPath2URL(Path pDocDir) {
		if(!pDocDir.getFileName().equals(DOCUMENTS_DIRNAME)) {
			Log.e(TAG, "the documents directory filename should be documents");
		}
		mBaseDir=pDocDir;
	}

	private List<String> readIndex(Path pIndexFile) throws FileNotFoundException, IOException {
		List<String> urls=new ArrayList<>();
		try(BufferedReader in=new BufferedReader(new FileReader(pIndexFile.toFile()))) {
			String line=in.readLine();
			while(line!=null) {
				urls.add(line);
				line=in.readLine();
			}
		}
		return urls;
	}
	
	@Override
	public String convert(Path pFile) {
		try {
			Path absoluteFile=mBaseDir.resolve(pFile.subpath(1,  pFile.getNameCount()));
			if(absoluteFile.getNameCount()==0) {
				throw new IllegalArgumentException("path too short: "+pFile);
			}
			Path filename=absoluteFile.getFileName();
			int entryNum=Integer.parseInt(FilenameUtils.getBaseName(filename.toString()));
			if(entryNum==0) {
				throw new IllegalArgumentException("entry number must be >0 "+pFile);			
			}

			Path ngramDir=absoluteFile.subpath(0, absoluteFile.getNameCount()-1);
			String ngramName=ngramDir.getFileName().toString();
			Path urlIndexFile=ngramDir.resolve(AbstractFilesJournal.META_PREFIX+ngramName);
			if(!Rtu.safeEq(urlIndexFile, mCurIndexFile)) {
				mCurIndexFile=urlIndexFile;
				mCurURLs.clear();
				mCurURLs.addAll(readIndex(urlIndexFile));
			}

			if(entryNum>mCurURLs.size()) {
				throw new IllegalArgumentException("entry number too large: "+entryNum+" vs "+mCurURLs.size()+" for "+pFile.toString());
			}

			return mCurURLs.get(entryNum-1);
		} catch(IOException e) {
			mThrower.ctch(e);
			return null;
		}
	}

	@Override
	public void close() throws IOException {
		mThrower.close();
	}

	@Override
	public <E extends Exception> void throwCaught(Class<E> pCatchable)
			throws Throwable {
		mThrower.throwCaught(pCatchable);
	}
}
