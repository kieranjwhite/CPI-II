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
import com.hourglassapps.cpi_ii.web_search.Query;
import com.hourglassapps.cpi_ii.web_search.bing.response.Response;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.ThrowableIterator;

public class MainQuery extends AbstractQuery {
	private final static String TAG=MainQuery.class.getName();
	
	private final static String SEARCH_PATH_PREFIX="/Bing/SearchWeb/Web?Query=%27";
	private final static String SEARCH_URI_PREFIX=
			"https://api.datamarket.azure.com"+SEARCH_PATH_PREFIX;
	private final static String SEARCH_PATH_SUFFIX="%27&$format=Json";
	
	private final static String AUTH_HEADER="Authorization";
	private final static String AUTH_PREFIX="Basic ";
	
	private final static int TOTAL_QUERY_LEN=2047;
	private final static String ENCODING=StandardCharsets.UTF_8.toString();
	private final StringBuilder mQuery=new StringBuilder();
	
	private final String mAccountKey;
	private ResponseFactory mFact=new ResponseFactory();
	
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

	private Response page(HttpClient pClient, URI pQuery) throws ClientProtocolException, IOException {
		HttpGet get=new HttpGet(pQuery);
		get.setHeader(AUTH_HEADER, AUTH_PREFIX+mAccountKey);
		ResponseHandler<String> respHandler=new BasicResponseHandler();
		String body=pClient.execute(get, respHandler);
		System.out.println("response: "+body);
		return mFact.inst('{'+body+'}');		
	}

	@SuppressWarnings("deprecation")
	@Override
	public ThrowableIterator<URI> search(List<String> pDisjunctions) throws URISyntaxException, ClientProtocolException, IOException {
		Ii<URI, List<String>> queryMoreDisjunctions=format(pDisjunctions);
		
		final HttpClient client=new DefaultHttpClient();
		try {
			final Response resp=page(client, queryMoreDisjunctions.fst());
			return new ThrowableIterator<URI>() {
				private Response mResponse=resp;
				private Iterator<URI> mPage=mResponse.urls().iterator();
				private Exception mCaught;
				
				@Override
				public boolean hasNext() {
					try {
						return mPage.hasNext() || retrievedMore();
					} catch (IOException | URISyntaxException e) {
						mCaught=e;
						return false;
					}
				}

				private boolean retrievedMore() throws ClientProtocolException, IOException, URISyntaxException {
					assert !mPage.hasNext();
					mResponse=page(client, mResponse.next());
					mPage=mResponse.urls().iterator();
					return mPage.hasNext();
				}

				@Override
				public URI next() {
					return mPage.next();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public void throwCaught() throws Exception {
					throw mCaught;
				}
				
			};
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public static void main(String[] pArgs) {
		Query<URI> q=new MainQuery("xD0E++DfZY7Sbumxx2QBuvmgOGliDgHuDIm0LzIGr3E=");
		try {
			q.search(Collections.singletonList("test"));
		} catch (URISyntaxException | IOException e) {
			Log.e(TAG, e);
		}
	}
}
