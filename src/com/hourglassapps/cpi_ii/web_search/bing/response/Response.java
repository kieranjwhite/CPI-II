package com.hourglassapps.cpi_ii.web_search.bing.response;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The fields in this class are populated by the Jackson JSON parser.
 * Each field corresponds to a key-value pair in corresponding JSON object.
 * <p>
 * Setter implementations are required by Jackson and are invoked to set field values.
 */
public class Response {
	public enum Type { WebResult };
	
	private Resp mResponse;
	
	public void setResponse(Resp pResponse) {
		mResponse=pResponse;
	}
	
	public Resp response() {
		return mResponse;
	}
	
	public List<URI> urls() throws URISyntaxException {
		return mResponse.urls();
	}
	
	public URI next() throws URISyntaxException {
		return mResponse.next();
	}
}
