package com.hourglassapps.cpi_ii.lucene;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;

import com.hourglassapps.cpi_ii.stem.snowball.lucene.MetaRead;

public interface MetaReadFactory {
	public MetaRead metaRead(Path pFile) throws IOException;
	public boolean indexable(Path pFile);
}
