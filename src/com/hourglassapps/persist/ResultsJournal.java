package com.hourglassapps.persist;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.exception.TikaException;

import com.hourglassapps.cpi_ii.lucene.TikaReader;
import com.hourglassapps.cpi_ii.web_search.MainDownloader;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.FileWrapper;
import com.hourglassapps.util.IdentityConverter;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Typed;

public class ResultsJournal extends AbstractFilesJournal<String,Ii<String,Path>> {
	private final Class<?> mTemplateClass;
	private final String mStart;
	private final String mEnd;
	private final Converter<Ii<String,Path>,String> mAddedToString;
	private final Path mDocDir;
	private final static Path JS_FILENAME=Paths.get("links.js");
	private final static Path PLAIN_TEXT_DIR=Paths.get("text");
	private FileWrapper mWrapper=null;
	private final FileCopyJournal mFiles;
	private final ConcreteThrower<UnsupportedEncodingException> mThrower=new ConcreteThrower<>();
	private final Shortener mShorten=new Shortener(mThrower);
	private final static Path DOCUMENT_DIR=MainDownloader.DOCUMENT_DIR;
	
	public ResultsJournal(Path pDir, Path pDocDir, final Converter<Ii<String,Path>, String> pAddedToString,
			Class<?> pTemplateClass, String pStart, String pEnd)
			throws IOException {
		super(pDir, new IdentityConverter<String>(), new Converter<Ii<String,Path>,Typed<Ii<String,Path>>>(){

			@Override
			public Typed<Ii<String,Path>> convert(final Ii<String,Path> pIn) {
				return new Typed<Ii<String,Path>>() {

					@Override
					public String extension() {
						return ".txt";
					}

					@Override
					public Ii<String,Path> get() {
						return pIn;
					}
					
				};
			}
			
		});
		mFiles=new FileCopyJournal(pDir.resolve(PLAIN_TEXT_DIR), new Converter<Path,String>(){

			@Override
			public String convert(Path pIn) {
				DOCUMENT_DIR.relativize(pIn);
				String journal=DOCUMENT_DIR.subpath(0,1).toString();
				int underscore_idx=journal.indexOf(MainDownloader.JOURNAL_NUM_DELIM);
				String journal_num;
				if(underscore_idx==-1) {
					journal_num=journal;
				} else {
					journal_num=journal.substring(0,underscore_idx);
				}
				List<String> parts=new ArrayList<>();
				parts.add(journal_num);
				parts.add(DOCUMENT_DIR.subpath(2,3).toString());
				parts.add(FilenameUtils.getBaseName(DOCUMENT_DIR.subpath(3,4).toString()));
				return mShorten.convert(Rtu.join(parts, " "));
			}
			
		});
		mDocDir=pDocDir.toRealPath();
		mAddedToString=pAddedToString;
		mTemplateClass=pTemplateClass;
		mStart=pStart;
		mEnd=pEnd;
	}

	private FileWrapper wrapper() throws IOException {
		if(mWrapper==null) {
			mWrapper=new FileWrapper(mTemplateClass, mStart, mEnd, mPartialDir.resolve(JS_FILENAME));	
		}
		return mWrapper;
	}
	
	/**
	 * 
	 * @param pPath
	 * @return a relative path to the original downloaded file (ie not a symlink)
	 * @throws IOException
	 */
	private Path normalisePath(Path pPath) throws IOException {
		return mDocDir.relativize(pPath.toRealPath());
	}
	
	private Path plainTextDest(Path pSrc) {
		
	}
	
	@Override
	protected void addNew(Typed<Ii<String,Path>> pLink) throws IOException {
		final Ii<String,Path> queryPath=pLink.get();
		Path source=normalisePath(queryPath.snd());
		final Path dest=dest(pLink);
		try {
			
			Rtu.copyFile(new TikaReader(new FileInputStream(), null), dest);
			mTrail.add(queryPath);
			PrintWriter writer=wrapper().writer();
			writer.println(mAddedToString.convert(queryPath));
			incFilename();
		} catch (TikaException e) {
			throw new IOException(e);
		}		
	}

	@Override
	public void commit(String pKey) throws IOException {
		wrapper().close();
		mWrapper=null;
		super.commit(pKey);
	}

	
	
	/*
	@Override 
	protected PrintWriter writer() {
		return mWrapper.writer();
	}
	
	@Override
	public void commit(String pKey) throws IOException {
		try(FileWrapper wrapper=new FileWrapper(mTemplateClass, mStart, mEnd, partial())) {
			mWrapper=wrapper;
			write();
		}
		tidyUp(pKey);
	}
	*/
}
