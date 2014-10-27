package com.hourglassapps.cpi_ii.web_search;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

public interface Query<T> {
	public Iterator<T> search(List<String> pDisjunctions) throws URISyntaxException, ClientProtocolException, IOException;
}
