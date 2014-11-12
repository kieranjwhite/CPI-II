package com.hourglassapps.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.jdeferred.Promise;

public interface Downloader<S> {
	public void reset() throws IOException;
	public Promise<Void,IOException,Void> downloadLink(S pSrc, Path pDst) throws IOException;
}
