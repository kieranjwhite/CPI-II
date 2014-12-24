package com.hourglassapps.util;

import java.io.IOException;
import java.io.InputStream;

public interface InputStreamFactory {
	InputStream wrap(InputStream pIn) throws IOException;
}
