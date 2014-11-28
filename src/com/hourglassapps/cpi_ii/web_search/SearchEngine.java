package com.hourglassapps.cpi_ii.web_search;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.hourglassapps.util.Ii;

public interface SearchEngine<C,K,Q,R> extends AutoCloseable {
	public Query<K,Q> formulate(C pDisjunctions) throws IOException;
	public Iterator<R> present(Query<K,Q> pQuery) throws IOException;
}
