package com.hourglassapps.cpi_ii.web_search;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;

/***
 * RestrictedQuery instances allow certain pages to be filtered from results.
 * By invoking <code>blacklistSites</code> and <code>blacklistPhrases</code> 
 * a page will be 
 * filtered if it contains either one or more of the blacklisted phrases or 
 * is in the set in blacklisted sites, assuming both methods return <code>true</code>.
 * @author kieran
 *
 * @param <T> type of Result returned.
 */
public interface RestrictedSearchEngine<K,Q,R> extends SearchEngine<List<String>,K,Q,R> {
	/***
	 * <code>RestrictedQuery<code> instance will try to ensure that search 
	 * engine filters out pages containing specified phrases in a search.<p>
	 * The list of phrases is considered exhaustive and will replace any set
	 * of phrases previously provided through this method.
	 * @param pPhrases Pages containing any of <code>pPhrases</code> might be filtered from search results.
	 * @return <code>true</code> is returned if <code>RestrictedQuery</code> instance is capable 
	 * of generating a blacklist of phrases for search engine.
	 * <code>false</code> is returned otherwise and if so the RestrictedQuery implementation
	 * will continue to behave as it did before this method invocation.
	 * @throws UnsupportedEncodingException 
	 */
	public boolean filterPhrases(Set<String> pPhrases) throws UnsupportedEncodingException;

	/***
	 * <code>RestrictedQuery<code> instance will try to ensure that search 
	 * engine filters out pages from any of these sites in a search.<p>
	 * The list of sites is considered exhaustive and will replace any set
	 * of sites previously provided through this method.
	 * @param pSites Pages from sites might be filtered from search results.
	 * @return <code>true</code> is returned if <code>RestrictedQuery</code> instance is capable 
	 * of generating a blacklist of sites for search engine.
	 * <code>false</code> is returned otherwise and if so the RestrictedQuery implementation
	 * will continue to behave as it did before this method invocation.
	 */
	public boolean filterSites(Set<String> pSites) throws UnsupportedEncodingException;
}
