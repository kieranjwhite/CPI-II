package com.hourglassapps.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.jdeferred.Promise;

public interface Downloader<S,R> {
	public void reset() throws IOException;
	public Promise<R,IOException,Void> downloadLink(S pSrc, int pDstKey, Path pDst) throws IOException;
}
