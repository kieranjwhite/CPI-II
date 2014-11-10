package com.hourglassapps.cpi_ii.web_search.bing;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.lucene.util.BytesRefHash.MaxBytesLengthExceededException;

import com.hourglassapps.cpi_ii.web_search.AbstractSearchEngine;
import com.hourglassapps.cpi_ii.web_search.HttpQuery;
import com.hourglassapps.cpi_ii.web_search.Query;
import com.hourglassapps.cpi_ii.web_search.bing.response.Response;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Filter;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Thrower;

public class BingSearchEngine extends AbstractSearchEngine implements Thrower {
	private final static String TAG=BingSearchEngine.class.getName();
	public final static int RESULTS_PER_PAGE=50;
	
	private final static String SEARCH_PATH_PREFIX="/Bing/SearchWeb/Web?Query=%27%28";
	private final static String SEARCH_URI_PREFIX=
			"https://api.datamarket.azure.com"+SEARCH_PATH_PREFIX;
	private final static String CLOSING_BRACKET="%29";
	private final static String SEARCH_PATH_SUFFIX="%27&$top="+RESULTS_PER_PAGE+BingArgs.JSON_SPECIFIER;
	
	private final static String AUTH_HEADER="Authorization";
	private final static String AUTH_PREFIX="Basic ";
	
	private final static int MAX_RESULT_PAGES=2; //in addition we currently request 50 docs per page (ie the max allowed)
	//A length 0f 2047 is too long. I do know from experience that a length of 2007 works though so I'll round it off to 2000 and go with that.
	//private final static int TOTAL_QUERY_LEN=2047; //from http://stackoverflow.com/questions/15334531/what-are-the-query-length-limits-for-the-bing-websearch-api
	private final static int TOTAL_QUERY_LEN=2000;
	private final static Query<String,URL> NULL_QUERY=new HttpQuery<String>(uniqueName(Collections.<String>emptyList()));
	public static final String AUTH_KEY = "xD0E++DfZY7Sbumxx2QBuvmgOGliDgHuDIm0LzIGr3E=";

	static {
		assert RESULTS_PER_PAGE>=1 && RESULTS_PER_PAGE<=50; //per https://duckduckgo.com/l/?kh=-1&uddg=http%3A%2F%2Fwww.bing.com%2Fwebmaster%2Fcontent%2Fdevelopers%2FADM_FAQ_EXT.docx
	}
	
	/*
	 * The value of the __next field in Bing's response will take 
	 * the form of the original query + "&$skip=N" where N is the number of results to skip.
	 * We need to account for these extra characters when calculating the 
	 * max query length or else we won't be able to retrieve the next page
	 * of results.
	*/
	private final int mMaxQueryLenOverhead=("&$skip="+Integer.toString((MAX_RESULT_PAGES-1)*RESULTS_PER_PAGE)).length();  
	private final StringBuilder mQuery=new StringBuilder();
	
	private ConcreteThrower<Exception> mThrower=new ConcreteThrower<Exception>();
	private final String mAccountKey;
	private ResponseFactory mFact=new ResponseFactory();
	private final HttpClient mClient=new DefaultHttpClient();
	private boolean mSearchInvoked=false;
	private String mBlacklistedPhrases="";
	private String mBlacklistedSites="";
	private Filter<URL> mFilter=new Filter<URL>(){

		@Override
		public boolean accept(URL pArg) {
			return true;
		}
		
	};

	public BingSearchEngine(String pAccountKey) {
		mAccountKey=new String(Base64.encodeBase64((':'+pAccountKey).getBytes()));
	}
	
	public BingSearchEngine() {
		mAccountKey=null;
	}
	
	public BingSearchEngine setFilter(Filter<URL> pFilter) {
		mFilter=pFilter;
		return this;
	}
	
	@Override
	protected String encode(String pString) throws UnsupportedEncodingException {
		/*
		 * According to http://stackoverflow.com/questions/15334531/what-are-the-query-length-limits-for-the-bing-websearch-api
		 * queries are reencoded at Microsoft's end so that +s are converted to %20s. After that the length limit of 2048
		 * characters is applied. We therefore need to use %20s during encoding if we wish to check the length of
		 * our queries.
		 */
		return super.encode(pString).replaceAll("\\+", "%20");
	}
	
	@Override
	protected int maxQueryLen() {
		int maxLen=TOTAL_QUERY_LEN-(SEARCH_URI_PREFIX.length()+
				CLOSING_BRACKET.length()+mBlacklistedSites.length()+mBlacklistedPhrases.length()+
				SEARCH_PATH_SUFFIX.length()+mMaxQueryLenOverhead);
		assert(maxLen>0);
		return maxLen;
	}

	protected String encodeDisjunction(String pDisjunction) throws UnsupportedEncodingException {
		if(mQuery.length()==0) {
			return encode(pDisjunction);
		} else {
			return encode(" OR "+pDisjunction);
		}		
	}

	@Override
	protected boolean addDisjunction(String pDisjunction) throws UnsupportedEncodingException {
		String encoded=encodeDisjunction("\""+pDisjunction+"\"");
		if(len()+encoded.length()<=maxQueryLen()) {
			mQuery.append(encoded);
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected int len() {
		return mQuery.length();
	}

	@Override
	protected void reset() {
		mQuery.delete(0, mQuery.length());
	}

	@Override
	protected URL uri() throws MalformedURLException {
		return new URL(SEARCH_URI_PREFIX+mQuery.append(CLOSING_BRACKET).append(mBlacklistedSites).append(mBlacklistedPhrases).append(SEARCH_PATH_SUFFIX).toString());
	}
	
	private Response page(URL pQuery) throws ClientProtocolException, IOException, URISyntaxException {
		HttpGet get=new HttpGet(pQuery.toURI());
		get.setHeader(AUTH_HEADER, AUTH_PREFIX+mAccountKey);
		ResponseHandler<String> respHandler=new BasicResponseHandler();
		final String body;
		if(mAccountKey==null) {
			System.out.println(URLDecoder.decode(pQuery.toString(), ENCODING));
			body="{\"d\":{\"results\":[]}}";
		} else if(mFilter.accept(pQuery)) {
			Log.i(TAG, URLDecoder.decode(pQuery.toString(), ENCODING));
			body=mClient.execute(get, respHandler);	
		} else {
			body="{\"d\":{\"results\":[]}}";
		}
		return mFact.inst(body);		
	}

	@Override
	public Query<String,URL> formulate(List<String> pDisjunctions) 
			throws UnsupportedEncodingException, MalformedURLException {
		mSearchInvoked=true;
		if(mThrower.fallThrough()) {
			return NULL_QUERY;
		}
		String uniqueName=uniqueName(pDisjunctions);
		Ii<URL, List<String>> queryRemainder=format(pDisjunctions);
		if(pDisjunctions.size()==queryRemainder.snd().size() || pDisjunctions.size()==0) {
			return NULL_QUERY;
		}
		return new HttpQuery<String>(uniqueName, queryRemainder);
	}

	@Override
	public Iterator<URL> present(Query<String,URL> pQuery) throws IOException {
		mSearchInvoked=true;
		if(mThrower.fallThrough()) {
			return Collections.<URL>emptyList().iterator();
		}
		if(pQuery.done()) {
			//no room for disjunctions in query
			return Collections.<URL>emptyList().iterator();
		}
		assert(pQuery.raw()!=null);
		try {
			final Response resp = page(pQuery.raw());
			return new Iterator<URL>() {
				private int mPageNum=0;
				private Response mResponse=resp;
				private Iterator<URL> mPage=mResponse.urls().iterator();

				@Override
				public boolean hasNext() {
					if(mThrower.fallThrough()) {
						return false;
					}
					try {
						return mPage.hasNext() || retrievedMore();
					} catch (IOException | URISyntaxException e) {
						mThrower.ctch(e);
						return false;
					}
				}

				private boolean retrievedMore() throws ClientProtocolException, IOException, URISyntaxException {
					assert !mPage.hasNext();
					if(mPageNum+1>=MAX_RESULT_PAGES) {
						return false;
					}
					URL next=mResponse.next();
					if(next==null) {
						return false;
					}
					mResponse=page(next);
					mPageNum++;
					mPage=mResponse.urls().iterator();
					return mPage.hasNext();
				}

				@Override
				public URL next() {
					assert mPageNum<MAX_RESULT_PAGES;
					return mPage.next();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		} catch (URISyntaxException e) {
			mThrower.ctch(e);
			return Collections.<URL>emptyList().iterator();
		}
	}

	@Override
	public void close() throws Exception {
		mThrower.close();
		mClient.getConnectionManager().shutdown();
	}

	@Override
	public <E extends Exception> void throwCaught(Class<E> pCatchable)
			throws Exception {
		mThrower.throwCaught(pCatchable);
	}

	private String blacklist(Set<String> pFiltered, String pJoinStart, String pJoinEnd) throws UnsupportedEncodingException {
		if(mSearchInvoked) {
			throw new IllegalStateException("Cannot alter filter once search has been invoked");
		}
		String blacklisted=Rtu.join(new ArrayList<String>(pFiltered), pJoinEnd+pJoinStart);
		if(blacklisted.length()>0) {
			blacklisted=encode(pJoinStart+blacklisted+"\"");
		}
		return blacklisted;
	}
	
	@Override
	public boolean filterPhrases(Set<String> pPhrases) throws UnsupportedEncodingException {
		String blacklisted=blacklist(pPhrases, " AND NOT \"", "\"");
		assert(maxQueryLen()>=mBlacklistedPhrases.length());
		if(blacklisted.length()+(maxQueryLen()-mBlacklistedPhrases.length())>0) {
			mBlacklistedPhrases=blacklisted;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean filterSites(Set<String> pSites) throws UnsupportedEncodingException {
		String blacklisted=blacklist(pSites, " AND NOT site:", "");
		assert(maxQueryLen()>=mBlacklistedSites.length());
		if(blacklisted.length()+(maxQueryLen()-mBlacklistedSites.length())>0) {
			mBlacklistedSites=blacklisted;
			return true;
		} else {
			return false;
		}
	}
}
