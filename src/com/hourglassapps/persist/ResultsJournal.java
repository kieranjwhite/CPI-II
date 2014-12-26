package com.hourglassapps.persist;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.hourglassapps.cpi_ii.lucene.TikaReader;
import com.hourglassapps.cpi_ii.report.Result;
import com.hourglassapps.cpi_ii.web_search.MainDownloader;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.FileWrapper;
import com.hourglassapps.util.IdentityConverter;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.InputStreamFactory;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Typed;

public class ResultsJournal extends AbstractFilesJournal<String,Result> {
	private final static String TAG=ResultsJournal.class.getName();
	private final Class<?> mTemplateClass;
	private final String mStart;
	private final String mEnd;
	private final Converter<Result,String> mAddedToString;
	private final Path mDocDir;
	private final static Path JS_FILENAME=Paths.get("links.js");
	private final static Path PLAIN_TEXT_DIR=Paths.get("text");
	private FileWrapper mWrapper=null;
	private final FileCopyJournal mFiles;
	private final ConcreteThrower<Exception> mThrower=new ConcreteThrower<>();
	private final Shortener mShorten=new Shortener(mThrower);
	//private final static Path DOCUMENT_DIR=MainDownloader.DOCUMENT_DIR;
	
	public ResultsJournal(Path pDir, Path pDocDir, final Converter<Result, String> pAddedToString,
			Class<?> pTemplateClass, String pStart, String pEnd)
			throws IOException {
		super(pDir, new IdentityConverter<String>(), new Converter<Result,Typed<Result>>(){

			@Override
			public Typed<Result> convert(final Result pIn) {
				return new Typed<Result>() {

					@Override
					public String extension() {
						return ".txt";
					}

					@Override
					public Result get() {
						return pIn;
					}
					
				};
			}
			
		});
		mFiles=new FileCopyJournal(pDir.resolve(PLAIN_TEXT_DIR), new Converter<Path,String>(){

			@Override
			public String convert(Path pIn) {
				try {
					Path relativeIn=normalisePath(pIn);
					String journal=relativeIn.subpath(0,1).toString();
					int underscore_idx=journal.indexOf(MainDownloader.JOURNAL_NUM_DELIM);
					String journal_num;
					if(underscore_idx==-1) {
						journal_num=journal;
					} else {
						journal_num=journal.substring(0,underscore_idx);
					}
					List<String> parts=new ArrayList<>();
					parts.add(journal_num);
					parts.add(relativeIn.subpath(2,3).toString());
					parts.add(FilenameUtils.getBaseName(relativeIn.subpath(3,4).toString()));
					return mShorten.convert(Rtu.join(parts, " "));
				} catch(IOException e) {
					mThrower.ctch(e);
					return null;
				}
			}
			
		}, new InputStreamFactory(){
			private Parser mParser=new AutoDetectParser();

			@Override
			public InputStream wrap(InputStream pIn) throws IOException {

				ContentHandler handler=new BodyContentHandler(TikaReader.MAX_CONTENT_SIZE);
				Metadata metadata=new Metadata();
				try {
					mParser.parse(pIn, handler, metadata, new ParseContext());
				} catch(TikaException e) {
					throw new IOException(e);
				} catch(SAXException e) {
					Log.e(TAG, e);
				}
				String content=handler.toString();
				return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
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
	
	@Override
	protected void addNew(Typed<Result> pLink) throws IOException {
		try {
			mThrower.throwCaught(IOException.class);
		} catch(Exception e) {
			throw (IOException)e;
		}
		final Result r=pLink.get();
		Path source=r.path();
		if(!mFiles.addedAlready(source)) {
			mFiles.commit(source);
		}
		mTrail.add(r);
		PrintWriter writer=wrapper().writer();
		String added=mAddedToString.convert(r);
		writer.println(added);
	}

	@Override
	public void commit(String pKey) throws IOException {
		try {
			mThrower.throwCaught(IOException.class);
		} catch(Exception e) {
			throw (IOException)e;
		}
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
