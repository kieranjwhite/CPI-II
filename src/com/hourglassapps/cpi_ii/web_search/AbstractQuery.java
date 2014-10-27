package com.hourglassapps.cpi_ii.web_search;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import com.hourglassapps.util.Ii;

public abstract class AbstractQuery implements Query<URI> {
	
	/**
	 * 
	 * @param pDisjunction to encode
	 * @return encoded String that is in a form suitable for appending to current query
	 * @throws UnsupportedEncodingException
	 */
	protected abstract String encode(String pDisjunction) throws UnsupportedEncodingException;

	/**
	 * Adds a new disjunction to the existing query
	 * @param pDisjunction
	 * @return true if addition was successful. false if not. Addition is only successful if len() after addition<=maxQueryLen()
	 * @throws UnsupportedEncodingException 
	 */
	protected abstract boolean addDisjunction(String pDisjunction) throws UnsupportedEncodingException;
	
	/**
	 * 
	 * @return maximum length of string that can be send as query to search engine excluding 
	 * parts of the query such as the path in a URL.
	 */
	protected abstract int maxQueryLen();
	
	/**
	 * Determines the actual length of a a query if pDisjunction was be packaged up and <code>add</code>ed.
	 * @param pQuery
	 * @return how long pQuery will be when suitably escaped and formatted for sending to the
	 * search engine.
	 * @throws UnsupportedEncodingException 
	 * 
	 */
	protected int addedLen(String pDisjunction) throws UnsupportedEncodingException {
		return encode(pDisjunction).length();
	}
	
	/**
	 * Calculates length of current query if packaged up for search engine.
	 * Bing for example requires a query to be in quotes.<p>
	 * len() will always be <=maxQueryLen()
	 * <code>queryLen(pQuery)<maxQueryLen()</code>. 
	 * @return length of current query.
	 */
	protected abstract int len();

	/**
	 * Resets query to that it no longer includes any disjunctions
	 */
	protected abstract void reset();

	/**
	 * Generates query, returning URI
	 * @return URI representing query
	 * @throws URISyntaxException 
	 */
	protected abstract URI uri() throws URISyntaxException;
	
	/**
	 * 
	 * @param pDisjunctions each is an exact phrase to be sent to search engine possibly as part of a larger query
	 * @return List of one or more text queries that will in turn be sent to search engine
	 * @throws UnsupportedEncodingException 
	 * @throws URISyntaxException 
	 */
	protected Ii<URI, List<String>> format(List<String> pDisjunctions) throws UnsupportedEncodingException, URISyntaxException {
		int i=0;
		for(; i<pDisjunctions.size(); i++) {
			String dis=pDisjunctions.get(i);
			if(!addDisjunction(dis)) {
				break;
			}
		}
		try {
			return new Ii<URI, List<String>>(uri(), Collections.unmodifiableList(pDisjunctions.subList(i, pDisjunctions.size())));
		} finally {
			reset();
		}
	}
	
}
