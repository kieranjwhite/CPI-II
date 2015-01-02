package com.hourglassapps.persist;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
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
import com.hourglassapps.util.URLUtils;

public class ResultsJournal extends AbstractFilesJournal<String,Result> {
	private final static String TAG=ResultsJournal.class.getName();
	private final static int MAX_PATH_LEN=38;
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
	//private final Converter<String,String> mShorten=new Shortener(MAX_PATH_LEN, mThrower);
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
					/* KW 30/12/2014. mShorten is not needed here as the longest path will never exceed 255 chars .
					 * (BTW the longest stemmed trigram is 46 chars only, but here we're dealing with unstemmed trigrams).
					 * If it turns out I'm wrong and shortening is required don't use mShorten like this anyway -- if there's a crash,
					 * after restart files from the document journal will be traversed in a different order (skipping some
					 * at the start), breaking the Shortener -- so you'll need a different solution -- one that actually works.
					 * Another problem with this 'solution' is that the generated filename needs to be transformable into the one
					 * the file in question was saved under in the document Journal -- I don't think the output from mShorten is.
					 */
					//return mShorten.convert(Rtu.join(parts, " ")); 
					return URLUtils.encode(Rtu.join(parts, " "));
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
