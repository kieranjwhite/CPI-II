package com.hourglassapps.cpi_ii.web_search.bing;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
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

import com.hourglassapps.cpi_ii.web_search.AbstractQuery;
import com.hourglassapps.cpi_ii.web_search.bing.response.Response;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Thrower;

public class MainQuery extends AbstractQuery implements Thrower {
	private final static String TAG=MainQuery.class.getName();
	
	private final static String SEARCH_PATH_PREFIX="/Bing/SearchWeb/Web?Query=%27%28";
	private final static String SEARCH_URI_PREFIX=
			"https://api.datamarket.azure.com"+SEARCH_PATH_PREFIX;
	private final static int RESULTS_PER_PAGE=50;
	private final static String CLOSING_BRACKET="%29";
	private final static String SEARCH_PATH_SUFFIX="%27&$top="+RESULTS_PER_PAGE+BingArgs.JSON_SPECIFIER;
	
	private final static String AUTH_HEADER="Authorization";
	private final static String AUTH_PREFIX="Basic ";
	
	private final static int MAX_RESULT_PAGES=2; //in addition we currently request 50 docs per page (ie the max allowed)
	private final static int TOTAL_QUERY_LEN=2047; //from http://stackoverflow.com/questions/15334531/what-are-the-query-length-limits-for-the-bing-websearch-api
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

	private String mBlacklistedPhrases="";
	
	public MainQuery(String pAccountKey) {
		mAccountKey=new String(Base64.encodeBase64((':'+pAccountKey).getBytes()));
	}
	
	@Override
	protected int maxQueryLen() {
		int maxLen=TOTAL_QUERY_LEN-(SEARCH_PATH_PREFIX.length()+
				CLOSING_BRACKET.length()+mBlacklistedPhrases.length()+
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
		String encoded=encodeDisjunction(pDisjunction);
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
	protected URI uri() throws URISyntaxException {
		return new URI(SEARCH_URI_PREFIX+mQuery.append(CLOSING_BRACKET).append(mBlacklistedPhrases).append(SEARCH_PATH_SUFFIX).toString());
	}

	private Response page(URI pQuery) throws ClientProtocolException, IOException {
		HttpGet get=new HttpGet(pQuery);
		get.setHeader(AUTH_HEADER, AUTH_PREFIX+mAccountKey);
		ResponseHandler<String> respHandler=new BasicResponseHandler();
		//assert false;
		System.out.println("query: "+URLDecoder.decode(pQuery.toString(), ENCODING));
		String body=mClient.execute(get, respHandler);
		System.out.println("response: "+body);
		return mFact.inst(body);		
	}

	@Override
	public Iterator<URI> search(List<String> pDisjunctions) throws IOException {
		if(mThrower.fallThrough()) {
			return Collections.<URI>emptyList().iterator();
		}
		//return Collections.<URI>emptyList().iterator();
		Ii<URI, List<String>> queryMoreDisjunctions;
		try {
			queryMoreDisjunctions = format(pDisjunctions);
			if(queryMoreDisjunctions.snd().size()==pDisjunctions.size()) {
				//no room for disjunctions in query
				return Collections.<URI>emptyList().iterator();
			}
			final Response resp=page(queryMoreDisjunctions.fst());
			return new Iterator<URI>() {
				private int mPageNum=0;
				private Response mResponse=resp;
				private Iterator<URI> mPage=mResponse.urls().iterator();

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
					mResponse=page(mResponse.next());
					mPageNum++;
					mPage=mResponse.urls().iterator();
					return mPage.hasNext();
				}

				@Override
				public URI next() {
					assert mPageNum<MAX_RESULT_PAGES;
					return mPage.next();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		} catch (URISyntaxException e1) {
			mThrower.ctch(e1);
			return Collections.<URI>emptyList().iterator();
		}
	}

	public static void main(String[] pArgs) {
		try(MainQuery q=new MainQuery(AUTH_KEY);) {
			Iterator<URI> results=q.search(Collections.singletonList("test"));
			while(results.hasNext()) {
				results.next();
			}
		} catch (Exception e) {
			Log.e(TAG, e);
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

	@Override
	public boolean filterPhrases(Set<String> pPhrases) throws UnsupportedEncodingException {
		String blacklisted=Rtu.join(new ArrayList<String>(pPhrases), "\" AND NOT \"");
		if(blacklisted.length()>0) {
			blacklisted=encode(" AND NOT \""+blacklisted+"\"");
		}
		assert(maxQueryLen()>=mBlacklistedPhrases.length());
		if(blacklisted.length()+(maxQueryLen()-mBlacklistedPhrases.length())>0) {
			mBlacklistedPhrases=blacklisted;
			return true;
		} else {
			return false;
		}
	}

}
