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

import com.hourglassapps.cpi_ii.lucene.MetaReadFactory;
import com.hourglassapps.cpi_ii.lucene.TikaReader;
import com.hourglassapps.cpi_ii.stem.snowball.lucene.MetaRead;
import com.hourglassapps.cpi_ii.web_search.TypedLink;
import com.hourglassapps.persist.AbstractFilesJournal;
import com.hourglassapps.persist.DeferredFilesJournal;
import com.hourglassapps.util.Log;

public class TextContentReaderFactory implements MetaReadFactory {
	private final static String TAG=TextContentReaderFactory.class.getName();

	private final static String TYPE_DELIMITER=Character.toString(DeferredFilesJournal.TYPE_COLUMN_DELIMITER);
	private final static TypeFileFinder TYPE_FINDER=new TypeFileFinder();
	private Path mLastTypesParent=null;
	private final Map<Integer,String> mFileNumToType=new HashMap<>();
	private final boolean mSpecifyTypes;
	
	public TextContentReaderFactory(boolean pSpecifyTypes) {
		mSpecifyTypes=pSpecifyTypes;
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
				if(DeferredFilesJournal.TYPE_UNKNOWN.equals(mimeType) ||
						DeferredFilesJournal.TYPE_SYMLINK.equals(mimeType)) {
					mimeType=null;
				}
				mFileNumToType.put(fileNum, mimeType);
			}
		}

		return true;
	}

	public String type(Path pFile) throws IOException {
		Path parent=pFile.getParent();
		int fileNum=fileNum(pFile);
		if((mLastTypesParent==null || !Files.isSameFile(parent, mLastTypesParent)) && !reloadedTypes(parent)) {
			return null;
		}
		if(!mFileNumToType.containsKey(fileNum)) {
			throw new IllegalStateException(fileNum+" missing from directory's types map");
		}
		return mFileNumToType.get(fileNum(pFile));
	}
	
	public static String leaf(Path pFile) {
		String leaf=pFile.getFileName().toString();
		if(leaf.length()==0 ||
				leaf.charAt(0)==AbstractFilesJournal.META_PREFIX) {
			return null;
		}
		return leaf;
	}
	
	public static int fileNum(Path pFilename) throws IOException {
		String leaf=pFilename.toString();
		int extensionIdx=leaf.lastIndexOf(TypedLink.FILE_EXTENSION_DELMITER);
		String start;
		if(extensionIdx==-1) {
			start=leaf;
		} else {
			start=leaf.substring(0, extensionIdx);
		}
		try {
			return Integer.parseInt(start);
		} catch(NumberFormatException e) {
			throw new IOException("Leaf: "+leaf,e);
		}
	}

	@Override
	public boolean indexable(Path pFile) {
		if(pFile==null) {
			throw new IllegalArgumentException("null path");
		}
		Path parent=pFile.getParent();
		if(parent==null) {
			throw new IllegalArgumentException(pFile+" has no parent");
		}
		if(Files.isSymbolicLink(pFile) ||
				DeferredFilesJournal.DONE_INDEX.equals(parent.getFileName().toString())) {
			return false;
		}
		return leaf(pFile)!=null;
	}
	
	protected MetaRead readerAlways(Path pFile) throws IOException {
		try(InputStream in=new FileInputStream(pFile.toFile())) {
			if(mSpecifyTypes) {
				return new TikaReader(in, type(pFile));
			} else {
				return new TikaReader(in, null);
			}
		} catch (TikaException e) {
			throw new IOException(e);
		}		
	}
	
	@Override
	public MetaRead metaRead(Path pFile) throws IOException {
		if(!indexable(pFile)) {
			return null;
		}
		return readerAlways(pFile);
	}
}
