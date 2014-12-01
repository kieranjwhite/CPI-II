package com.hourglassapps.cpi_ii.web_search;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;

import com.hourglassapps.cpi_ii.TextContentReaderFactory;

/**
 * When indexing a just downloaded file in MainDownloader the file will have been saved
 * to the partial directory. However once the current query's transaction is complete the
 * file's parent directory will be moved. The reader returned by this instance
 * points to the file's current position in the partial directory, despite the reader function
 * being passed the file's eventual path.
 * @author kieran
 *
 */
public class RedirectedContentReader extends TextContentReaderFactory {
	private Path mTempDir;
	
	public RedirectedContentReader(boolean pSpecifyTypes, Path pTempDir) {
		super(pSpecifyTypes);
		mTempDir=pTempDir;
	}
	
	private Path resolve(Path pFile) {
		Path filename=pFile.getFileName();
		Path parent=pFile.getParent();
		assert parent!=null;
		Path grandParent=parent.getParent();
		assert grandParent!=null;
		
		return grandParent.resolve(mTempDir).resolve(filename);
	}
	
	@Override
	public Reader reader(Path pFile) throws IOException {
		if(!indexable(pFile)) {
			return null;
		}
		
		Path tempFile=resolve(pFile);
		return super.readerAlways(tempFile);
	}

}
