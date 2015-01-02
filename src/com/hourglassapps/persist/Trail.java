package com.hourglassapps.persist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Log;

public class Trail<C> {
	private final static String TAG=Trail.class.getName();
	
	private final List<C> mTrail=new ArrayList<>();
	private final Path mPartialDir;
	
	public Trail(Path pPartialDir) {
		mPartialDir=pPartialDir;
	}
	
	public void add(C pSource) {
		mTrail.add(pSource);
	}
	
	public void clear() {
		mTrail.clear();
	}

	public void save(Path pDest) throws IOException {
		//Log.i(TAG, Log.esc("trail file: "+mPartialDir.resolve(AbstractFilesJournal.META_PREFIX+pDest.getFileName().toString()).toString()));
		try(PrintWriter out=
				new PrintWriter(new BufferedWriter(
						new FileWriter(mPartialDir.resolve(AbstractFilesJournal.META_PREFIX+pDest.getFileName().toString()).toString())))) {
			for(C source: mTrail) {
				out.println(source);
			}
		}
	}

	public Trail(Path pTrailPath, Converter<String,C> pParser) throws IOException {
		this(pTrailPath.getParent());
		List<String> urls=new ArrayList<>();
		try (BufferedReader reader=new BufferedReader(new FileReader(pTrailPath.toFile()))) {
			String line=reader.readLine();
			while(line!=null) {
				add(pParser.convert(line));
				line=reader.readLine();
			}
		}
	}
	
	public C url(Path pPath) {
		String filename=pPath.getFileName().toString();
		int extIdx=filename.lastIndexOf('.');
		String numberPart;
		if(extIdx==-1) {
			numberPart=filename;
		} else {
			numberPart=filename.substring(0, extIdx);
		}
		int urlIdx=Integer.parseInt(numberPart);
		assert(urlIdx>0);
		return mTrail.get(urlIdx-1);
	}
}