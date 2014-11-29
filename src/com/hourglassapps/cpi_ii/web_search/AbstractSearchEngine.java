package com.hourglassapps.cpi_ii.web_search;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hourglassapps.util.Filter;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.URLUtils;

public abstract class AbstractSearchEngine implements RestrictedSearchEngine<String,URL,URL>, AutoCloseable {
	protected final Query<String,URL> NULL_QUERY=new HttpQuery<String>(uniqueName(Collections.<String>emptyList()));

	@Override
	public Query<String,URL> formulate(List<String> pDisjunctions) 
			throws UnsupportedEncodingException, MalformedURLException {
		String uniqueName=uniqueName(pDisjunctions);
		Ii<URL, List<String>> queryRemainder=format(pDisjunctions);
		if(pDisjunctions.size()==queryRemainder.snd().size() || pDisjunctions.size()==0) {
			return NULL_QUERY;
		}
		return new HttpQuery<String>(uniqueName, queryRemainder.fst());
	}


	@Override
	public boolean filterPhrases(Set<String> pPhrases) throws UnsupportedEncodingException {
		return false;
	}

	protected String encode(String pString) throws UnsupportedEncodingException {
		return URLUtils.encode(pString);
	}
	
	@Override
	public boolean filterSites(Set<String> pSites) throws UnsupportedEncodingException {
		return false;
	}

	/**
	 * 
	 * @param pDisjunction to encode
	 * @return encoded String that is in a form suitable for appending to current query
	 * @throws UnsupportedEncodingException
	 */
	protected abstract String encodeDisjunction(String pDisjunction) throws UnsupportedEncodingException;

	/**
	 * Adds a new disjunction to the existing query
	 * @param pDisjunction
	 * @return true if addition was successful. false if not. Addition is only successful if len() after addition<=maxQueryLen()
	 * @throws UnsupportedEncodingException 
	 */
	protected abstract boolean addDisjunction(String pDisjunction) throws UnsupportedEncodingException;
	
	/**
	 * Determines the actual length of a a query if pDisjunction was be packaged up and <code>add</code>ed.
	 * @param pQuery
	 * @return how long pQuery will be when suitably escaped and formatted for sending to the
	 * search engine.
	 * @throws UnsupportedEncodingException 
	 * 
	 */
	protected int addedLen(String pDisjunction) throws UnsupportedEncodingException {
		return encodeDisjunction(pDisjunction).length();
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
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	protected abstract URL uri() throws MalformedURLException;
	
	protected String uniqueName(List<String> pDisjunctions) {
		if(pDisjunctions.size()>0) {
			return pDisjunctions.get(0);
		} else {
			return "_NULL_";
		}
	}
	
	/**
	 * 
	 * @param pDisjunctions each element is an exact phrase for the search engine. Not all
	 * disjunction may sent - those that are sent will be taken from the start of the provided 
	 * <code>List</code>. 
	 * @return First element is the <code>URI</code> representing query that is sent to search engine.
	 * The second element is a list of any disjunctions not included in the query.
	 * @throws UnsupportedEncodingException 
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	protected Ii<URL, List<String>> format(List<String> pDisjunctions) 
			throws UnsupportedEncodingException, MalformedURLException {
		int i=0;
		for(; i<pDisjunctions.size(); i++) {
			String dis=pDisjunctions.get(i);
			if(!addDisjunction(dis)) {
				break;
			}
		}
		try {
			return new Ii<URL, List<String>>(uri(), Collections.unmodifiableList(pDisjunctions.subList(i, pDisjunctions.size())));
		} finally {
			reset();
		}
	}
}
