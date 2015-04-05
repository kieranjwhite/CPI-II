package com.hourglassapps.persist;

import java.io.IOException;
import java.nio.file.Path;

public interface Linker {
	public void link(Path pLink, Path pOrig) throws IOException;
}
