package com.hourglassapps.cpi_ii.web_search.bing;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import com.hourglassapps.cpi_ii.web_search.AbstractQuery;
import com.hourglassapps.cpi_ii.web_search.bing.response.Response;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Thrower;

public class MainQuery extends AbstractQuery implements Thrower {
	private final static String TAG=MainQuery.class.getName();
	
	private final static String SEARCH_PATH_PREFIX="/Bing/SearchWeb/Web?Query=%27";
	private final static String SEARCH_URI_PREFIX=
			"https://api.datamarket.azure.com"+SEARCH_PATH_PREFIX;
	private final static String SEARCH_PATH_SUFFIX="%27&$format=Json&$top=50";
	
	private final static String AUTH_HEADER="Authorization";
	private final static String AUTH_PREFIX="Basic ";
	
	private final static int MAX_RESULT_PAGES=2; //in addition we currently request 50 docs per page (ie the max allowed)
	private final static int TOTAL_QUERY_LEN=2047;
	private final static String ENCODING=StandardCharsets.UTF_8.toString();

	public static final String AUTH_KEY = "xD0E++DfZY7Sbumxx2QBuvmgOGliDgHuDIm0LzIGr3E=";
	private final StringBuilder mQuery=new StringBuilder();
	
	private ConcreteThrower<Exception> mThrower=new ConcreteThrower<Exception>();
	private final String mAccountKey;
	private ResponseFactory mFact=new ResponseFactory();
	private final HttpClient mClient=new DefaultHttpClient();
	
	public MainQuery(String pAccountKey) {
		mAccountKey=new String(Base64.encodeBase64((':'+pAccountKey).getBytes()));
	}
	
	@Override
	protected int maxQueryLen() {
		return TOTAL_QUERY_LEN-(SEARCH_PATH_PREFIX.length()+SEARCH_PATH_SUFFIX.length());
	}

	protected String encode(String pDisjunction) throws UnsupportedEncodingException {
		if(mQuery.length()==0) {
			return URLEncoder.encode(pDisjunction, ENCODING);
		} else {
			return URLEncoder.encode(" OR "+pDisjunction, ENCODING);
		}		
	}

	@Override
	protected boolean addDisjunction(String pDisjunction) throws UnsupportedEncodingException {
		String encoded=encode(pDisjunction);
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
		return new URI(SEARCH_URI_PREFIX+mQuery.append(SEARCH_PATH_SUFFIX).toString());
	}

	private Response page(URI pQuery) throws ClientProtocolException, IOException {
		HttpGet get=new HttpGet(pQuery);
		get.setHeader(AUTH_HEADER, AUTH_PREFIX+mAccountKey);
		ResponseHandler<String> respHandler=new BasicResponseHandler();
		//assert false;
		String body=mClient.execute(get, respHandler);
		System.out.println("response: "+body);
		return mFact.inst(body);		
	}

	@SuppressWarnings("deprecation")
	@Override
	public Iterator<URI> search(List<String> pDisjunctions) throws IOException {
		if(mThrower.fallThrough()) {
			return Collections.<URI>emptyList().iterator();
		}
		//return Collections.<URI>emptyList().iterator();
		Ii<URI, List<String>> queryMoreDisjunctions;
		try {
			queryMoreDisjunctions = format(pDisjunctions);
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
}
