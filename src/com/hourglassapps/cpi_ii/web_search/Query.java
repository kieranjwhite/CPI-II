package com.hourglassapps.cpi_ii.web_search;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public interface Query<T> {
	public Iterator<T> search(List<String> pDisjunctions) throws IOException;
}
