package com.hourglassapps.cpi_ii.stem;

import java.io.IOException;

public interface PreFilter {
	public boolean incrementToken() throws IOException;
	public CharSequence priorToken();
}
