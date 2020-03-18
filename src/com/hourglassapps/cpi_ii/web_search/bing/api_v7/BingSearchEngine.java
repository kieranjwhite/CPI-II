package com.hourglassapps.cpi_ii.web_search.bing.api_v7;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
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
import org.apache.http.config.SocketConfig;
import org.apache.http.Header;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.hourglassapps.cpi_ii.web_search.AbstractSearchEngine;
import com.hourglassapps.cpi_ii.web_search.Query;
import com.hourglassapps.cpi_ii.web_search.bing.api_v7.response.Response;
import com.hourglassapps.cpi_ii.web_search.bing.api_v7.ResponseFactory;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Option;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Throttle;
import com.hourglassapps.util.Thrower;
import com.hourglassapps.util.URLUtils;

@SuppressWarnings("deprecation")
public class BingSearchEngine extends AbstractSearchEngine implements Thrower {
    private final static String TAG=BingSearchEngine.class.getName();
    private final static boolean FILTER_BY_SITE=false;
    public final static int RESULTS_PER_PAGE=50;
	
    public static final String END_POINT_PREFIX="websearchii";
    public static final String AUTH_KEY = "akeyakeyakeyakeyakeyakey";

    private final static String SEARCH_PATH_PREFIX="/bing/v7.0/search?q=%27%28";
    private final static String SEARCH_URI_PREFIX=
	"https://"+END_POINT_PREFIX+".cognitiveservices.azure.com"+SEARCH_PATH_PREFIX;
    private final static String CLOSING_BRACKET="%29";
    private final static String NUMBER_SPECIFIER="%d";
    private final static String OFFSET_SUFFIX="&offset="+NUMBER_SPECIFIER;
    private final static String GENERAL_SUFFIX="&count="+NUMBER_SPECIFIER+"&responseFilter=webpages";
    private final static String EOQ_SUFFIX="%27";
	
    private final static String AUTH_HEADER="Ocp-Apim-Subscription-Key";
    private final static String AUTH_PREFIX="";

    private final static String ACCEPT_HEADER="Accept";
    private final static String ACCEPT="application/json";

    //private final static String RESPONSE_FILTER="-computation,-entities,-images,-news,-relatedsearches,-spellsuggestions,-timezone,-videos,webpages";
    
    private final static int MAX_RESULT_PAGES=2; //in addition we currently request 50 docs per page (ie the max allowed)
    //A length 0f 2047 is too long. I do know from experience that a length of 2007 works though so I'll round it off to 2000 and go with that.
    //private final static int TOTAL_QUERY_LEN=2047; //from http://stackoverflow.com/questions/15334531/what-are-the-query-length-limits-for-the-bing-websearch-api
    private final static int TOTAL_QUERY_LEN=2000;
    //public static final String AUTH_KEY = "d042b2df4a4346da9e5875875fde298e";

    static {
	assert RESULTS_PER_PAGE>=1 && RESULTS_PER_PAGE<=50; //per https://duckduckgo.com/l/?kh=-1&uddg=http%3A%2F%2Fwww.bing.com%2Fwebmaster%2Fcontent%2Fdevelopers%2FADM_FAQ_EXT.docx
    }
	
    private final int mMaxQueryLenOverhead=Integer.toString((MAX_RESULT_PAGES*RESULTS_PER_PAGE)-1).length();  
    private final StringBuilder mQuery=new StringBuilder();
    private final static int MAX_TIMEOUTS=3;
    
    protected ConcreteThrower<Exception> mThrower=new ConcreteThrower<Exception>();
    protected boolean mSearchInvoked=false;
    private final String mAccountKey;
    private ResponseFactory mFact=new ResponseFactory();
    //private final HttpClient mClient=new DefaultHttpClient();
    private HttpClient mClient;
    private String mBlacklistedPhrases="";
    private String mBlacklistedSites="";
    private int mOffset=0;
    private Option<Long> mTotalResultsPresentable=new Option<Long>();
    private final Throttle mThrottle;
    
    public BingSearchEngine(String pAccountKey, Throttle pThrottle) {
	//mAccountKey=new String(Base64.encodeBase64((':'+pAccountKey).getBytes()));
	mAccountKey=pAccountKey;
	mThrottle=pThrottle;
	mClient=clientInst();
    }
	
    public BingSearchEngine(Throttle pThrottle) {
	mAccountKey=null;
	mThrottle=pThrottle;
	mClient=clientInst();
    }

    private static CloseableHttpClient clientInst() {
	SocketConfig socketConfig = SocketConfig.custom()
            .setSoKeepAlive(false)
            .setSoLinger(1)
            .setSoReuseAddress(true)
            .setSoTimeout(5000).build();

	HttpClientBuilder builder = HttpClientBuilder.create();
	//builder.disableAutomaticRetries();
	//builder.disableContentCompression();
	//builder.disableCookieManagement();
	//builder.disableRedirectHandling();
	//builder.setConnectionReuseStrategy(new NoConnectionReuseStrategy());
	builder.setDefaultSocketConfig(socketConfig);

	CloseableHttpClient client=builder.build();
	return client;
    }
    
    private static CloseableHttpClient clientInst(CloseableHttpClient pOldClient) throws IOException {
	/*
	SocketConfig socketConfig = SocketConfig.custom()
            .setSoKeepAlive(false)
            .setSoLinger(1)
            .setSoReuseAddress(true)
            .setSoTimeout(5000)
            .setTcpNoDelay(true).build();
	*/
	if(pOldClient!=null) {
	    pOldClient.close();
	}
	CloseableHttpClient client=clientInst();
	return client;
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

    public static String generalSuffix() {
	return String.format(GENERAL_SUFFIX, RESULTS_PER_PAGE);
    }
    
    @Override
    public int maxQueryLen() {
	int maxLen=TOTAL_QUERY_LEN-(SEARCH_URI_PREFIX.length()+
				    CLOSING_BRACKET.length()+mBlacklistedSites.length()+
				    mBlacklistedPhrases.length()+EOQ_SUFFIX.length()+GENERAL_SUFFIX.length()+
				    OFFSET_SUFFIX.length()+
				    (mMaxQueryLenOverhead-NUMBER_SPECIFIER.length())*2);
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
    public Query<String,URL> formulate(List<String> pDisjunctions) 
	throws UnsupportedEncodingException, MalformedURLException {
	mSearchInvoked=true;
	if(mThrower.fallThrough()) {
	    return NULL_QUERY;
	}
	return super.formulate(pDisjunctions);
    }
    @Override
    protected boolean addDisjunction(String pDisjunction) throws UnsupportedEncodingException {
	Log.i(TAG, "adding disjunction: "+pDisjunction);
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
	Log.i(TAG, "resetting query");
	mOffset=0;
	mTotalResultsPresentable=new Option<Long>();
	mQuery.delete(0, mQuery.length());
    }

    private Option<Integer> pageCount(Option<Long> pTotalResultsPresentable) {
	//Assumes mOffset has already been updated for the next query
	//returns new Option<Integer>() if there are no more results
	Option<Integer> result=new Option<Integer>();
	try {
	    if(pTotalResultsPresentable.hasVal()) {
		assert pTotalResultsPresentable.val()>=0: "totalEstimatedMatches must be greater than 0";
		//pTotalEstimatatedMatches in the query response can change value between paging calls.
		//Consequently it's quite possible that mOffset > pTotalResultsPresentable coming into this method.
		long remaining=pTotalResultsPresentable.val()-mOffset;
		if(remaining<=0) {
		    result = new Option<Integer>();
		    return result;
		}
		int count=(int)Math.min(remaining, RESULTS_PER_PAGE);
		result=new Option<Integer>(count);
	    } else {
		//ie first page
		result=new Option<Integer>(RESULTS_PER_PAGE);
	    }
	    return result;
	} finally {
	    //That offset < totalEstimatedMatches-count is a
	    //requirement of the API.  See
	    //https://docs.microsoft.com/en-ie/rest/api/cognitiveservices-bingsearch/bing-web-api-v7-reference
	    //
	    //However if there are 10 matches and you wish to list 10
	    //per page from offset 0 that condition fails so I think
	    //the condition must be relaxed to <= for fear of losing
	    //the last result.
	    //
	    //In addition we won't know the value of
	    //totalEstimatedMatches until we read the first page of
	    //results -- so initially at least I don't see how to
	    //ensure that this requirement hold for the first page..
	    Log.i(TAG, "before pageCount postCondition. result: "+result+" pTotalResultsPresentable: "+pTotalResultsPresentable+" offset: "+mOffset);
	    assert !result.hasVal() ||
		(!pTotalResultsPresentable.hasVal() && result.val()==RESULTS_PER_PAGE) ||
		mOffset<=pTotalResultsPresentable.val()-result.val();
	}
    }
    
    @Override
    protected Option<URL> uri() throws MalformedURLException {
	String url=SEARCH_URI_PREFIX+mQuery.append(CLOSING_BRACKET).append(mBlacklistedSites).append(mBlacklistedPhrases).append("%27");
	Log.i(TAG, "uri: mTotalResultsPresentable: "+mTotalResultsPresentable);
	Option<Integer> countOpt=pageCount(mTotalResultsPresentable);
	Log.i(TAG, "uri: countOpt: "+countOpt);
	if(!countOpt.hasVal()) {
	    //don't query
	    return new Option<URL>();
	} else {
	    String formatted;
	    int count=countOpt.val();
	    if(mTotalResultsPresentable.hasVal()) {
		String suffix=GENERAL_SUFFIX+OFFSET_SUFFIX;
		formatted=String.format(suffix, count, mOffset);
	    } else {
		formatted=String.format(GENERAL_SUFFIX, count);
	    }
	    url+=formatted;
	    return new Option<URL>(new URL(url));
	}
    }
	
    private Response page(String pName, URL pQuery) throws ClientProtocolException, IOException, URISyntaxException {
	HttpGet get=new HttpGet(pQuery.toURI());
	get.setHeader(AUTH_HEADER, AUTH_PREFIX+mAccountKey);
	get.setHeader(ACCEPT_HEADER, ACCEPT);
	ResponseHandler<String> respHandler=new BasicResponseHandler();
	String body;
	if(mAccountKey==null) {
	    System.out.println(URLDecoder.decode(pQuery.toString(), URLUtils.ENCODING));
	    body="{\"d\":{\"results\":[]}}";
	} else {
	    Log.i(TAG, "name: "+pName);
	    System.out.println("query: "+pQuery.toString());
	    for(Header hdr: get.getAllHeaders()) {
		Log.i(TAG, hdr.getName()+" - "+hdr.getValue());
	    }

	    int timeoutCount=0;
	    while(true) {
		mThrottle.choke();
		try {
		    body=mClient.execute(get, respHandler);
		    break;
		} catch(IOException ex) {
		    Log.e(TAG, ex, "possible timeout sending query to Bing");
		    if(timeoutCount>=MAX_TIMEOUTS) {
			Log.e(TAG, "skipping query");
			throw ex;
		    } else {
			timeoutCount++;
		    }
		}
	    }

	}
	Log.i(TAG, "page body: "+Log.esc(body));
	return mFact.inst(body);				
    }
	
    @Override
    public Iterator<URL> present(final Query<String,URL> pQuery) throws IOException {
	try {
	    mSearchInvoked=true;
	    if(mThrower.fallThrough()) {
		return Collections.<URL>emptyList().iterator();
	    }
	    if(pQuery.empty()) {
		//no room for disjunctions in query
		return Collections.<URL>emptyList().iterator();
	    }
	    assert(pQuery.raw()!=null);
	    try {
		Response response=null;
		try {
		    response = page(pQuery.uniqueName(), pQuery.raw());
		} catch(IOException ex) {
		    Log.e(TAG, "present: "+ex.getMessage());
		    return Collections.<URL>emptyList().iterator();
		    //return Collections.emptyIterator<URL>();
		}
		final Response resp=response;
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
			    //return mPage.hasNext();
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
			//URL next=mResponse.next();
			long totalEstimatedMatches=mResponse.totalEstimatedMatches();
			Log.i(TAG, "retrieveMoree: totalEstimatedMatches: "+totalEstimatedMatches);
			mTotalResultsPresentable=new Option<Long>(Math.min(totalEstimatedMatches, MAX_RESULT_PAGES*RESULTS_PER_PAGE));
			Log.i(TAG, "retrieveMode: mTotalResultsPresentable: "+mTotalResultsPresentable);
			mOffset+=RESULTS_PER_PAGE;
			Option<URL> nextUrlOpt=uri();
			if(!nextUrlOpt.hasVal()) {
			    return false;
			}
			URL nextUrl=nextUrlOpt.val();
			try {
			    mResponse=page(pQuery.uniqueName(), nextUrl);
			} catch(IOException ex) {
			    Log.e(TAG, "present (in Iterator) "+ex.getMessage());
			    return false;
			}
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
	} catch(Throwable e) {
	    Log.e(TAG, e, "when sending query "+pQuery.uniqueName());
	    throw e;
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
	    blacklisted=encode(pJoinStart+blacklisted+pJoinEnd);
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
	if(FILTER_BY_SITE) {
	    String blacklisted=blacklist(pSites, " AND NOT site:", "");
	    assert(maxQueryLen()>=mBlacklistedSites.length());
	    if(blacklisted.length()+(maxQueryLen()-mBlacklistedSites.length())>0) {
		mBlacklistedSites=blacklisted;
		return true;
	    } else {
		return false;
	    }
	} else {
	    return false;
	}
    }
}
