package com.hourglassapps.cpi_ii;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import com.hourglassapps.cpi_ii.lucene.FileReaderFactory;
import com.hourglassapps.cpi_ii.lucene.TikaReader;
import com.hourglassapps.cpi_ii.web_search.TypedLink;
import com.hourglassapps.persist.AbstractFileJournal;
import com.hourglassapps.persist.DeferredFileJournal;
import com.hourglassapps.util.Log;

public class TextContentReaderFactory implements FileReaderFactory {
	private final static String TAG=TextContentReaderFactory.class.getName();
	private final static String TYPE_DELIMITER=Character.toString(DeferredFileJournal.TYPE_COLUMN_DELIMITER);
	private final static TypeFileFinder TYPE_FINDER=new TypeFileFinder();
	private Path mLastTypesParent=null;
	private final Map<Integer,String> mFileNumToType=new HashMap<>();
	
	public TextContentReaderFactory() {
	}

	private boolean reloadedTypes(Path pParent) throws IOException {
		assert pParent!=null;
		Files.walkFileTree(pParent, EnumSet.noneOf(FileVisitOption.class), 1, TYPE_FINDER);
		Path typeFile=TYPE_FINDER.types();
		if(typeFile==null) {
			return false;
		}
		mLastTypesParent=pParent;
		mFileNumToType.clear();
		try(BufferedReader typeReader=Files.newBufferedReader(typeFile, StandardCharsets.UTF_8)) {
			String line;
			while((line=typeReader.readLine())!=null) {
				int delimIdx=line.indexOf(' ');
				assert delimIdx>0;
				String parts[]=line.split(TYPE_DELIMITER, 2);
				assert parts.length==2;
				int fileNum=Integer.parseInt(parts[0]);
				String mimeType=parts[1];
				if(DeferredFileJournal.TYPE_UNKNOWN.equals(mimeType) ||
						DeferredFileJournal.TYPE_SYMLINK.equals(mimeType)) {
					mimeType=null;
				}
				mFileNumToType.put(fileNum, mimeType);
			}
		}
		
		return true;
	}
	
	@Override
	public Reader reader(Path pFile) throws IOException {
		if(pFile==null) {
			throw new IllegalArgumentException("null path");
		}
		Path parent=pFile.getParent();
		if(parent==null) {
			throw new IllegalArgumentException(pFile+" has no parent");
		}
		if(Files.isSymbolicLink(pFile) ||
				DeferredFileJournal.DONE_INDEX.equals(parent.getFileName().toString())) {
			return null;
		}
		String leaf=pFile.getFileName().toString();
		if(leaf.length()==0 ||
				leaf.charAt(0)==AbstractFileJournal.META_PREFIX) {
			return null;
		}

		int fileNum, extensionIdx=leaf.lastIndexOf(TypedLink.FILE_EXTENSION_DELMITER);
		String start;
		if(extensionIdx==-1) {
			start=leaf;
		} else {
			start=leaf.substring(0, extensionIdx);
		}
		try {
			fileNum=Integer.parseInt(start);

			if((mLastTypesParent==null || !Files.isSameFile(parent, mLastTypesParent)) && !reloadedTypes(parent)) {
				return null;
			}
			if(!mFileNumToType.containsKey(fileNum)) {
				throw new IllegalStateException(fileNum+" missing from directory's types map");
			}
			try(InputStream in=new FileInputStream(pFile.toFile())) {
				//return new TikaReader(in, mFileNumToType.get(fileNum));
				return new TikaReader(in, null);
			} catch (TikaException e) {
				throw new IOException(e);
			}
		} catch(NumberFormatException e) {
			throw new IOException("Path: "+pFile.toString(),e);
		}
	}
}
