package com.hourglassapps.persist;

import java.io.IOException;
import java.nio.file.Path;

import org.jdeferred.Promise;

public interface FileSaver<C> {
	Promise<Void,IOException,Void> save(C pSrc, Path pDst) throws IOException;
}
