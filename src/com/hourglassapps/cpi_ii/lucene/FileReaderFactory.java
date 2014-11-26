package com.hourglassapps.cpi_ii.lucene;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;

public interface FileReaderFactory {
	Reader reader(Path pFile) throws IOException;
}
