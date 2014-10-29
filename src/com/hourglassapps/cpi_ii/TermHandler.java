package com.hourglassapps.cpi_ii;

import java.io.IOException;

import org.apache.lucene.index.TermsEnum;

public interface TermHandler {
	public void run(TermsEnum pTerms) throws IOException;
}