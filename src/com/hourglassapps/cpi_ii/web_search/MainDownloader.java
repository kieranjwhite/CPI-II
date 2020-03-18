package com.hourglassapps.cpi_ii.web_search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.*;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.*;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.client.methods.ZeroCopyConsumer;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.ssl.TrustStrategy;

import org.jdeferred.Deferred;
import org.jdeferred.ProgressCallback;
import org.jdeferred.Promise;

import com.hourglassapps.cpi_ii.CPIUtils;
import com.hourglassapps.cpi_ii.MainIndexConductus;
import com.hourglassapps.cpi_ii.MainIndexDownloaded;
import com.hourglassapps.cpi_ii.lucene.IndexViewer;
import com.hourglassapps.cpi_ii.lucene.IndexingThread;
import com.hourglassapps.cpi_ii.web_search.bing.api_v7.BingSearchEngine;
import com.hourglassapps.persist.DeferredFilesJournal;
import com.hourglassapps.persist.Journal;
import com.hourglassapps.persist.NullJournal;
import com.hourglassapps.threading.FilterTemplate;
import com.hourglassapps.threading.JobDelegator;
import com.hourglassapps.threading.HashTemplate;
import com.hourglassapps.threading.RandomTemplate;
import com.hourglassapps.util.AsyncExpansionReceiver;
import com.hourglassapps.util.Closer;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Downloader;
import com.hourglassapps.util.ExpansionDistributor;
import com.hourglassapps.util.ExpansionReceiver;
import com.hourglassapps.util.Filter;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.MainHeartBeat;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Throttle;
import com.hourglassapps.util.Typed;
import com.hourglassapps.util.URLUtils;

public class MainDownloader implements AutoCloseable {
    private final static String TAG=MainDownloader.class.getName();
    private final static Converter<String,String> KEY_CONVERTER=JournalKeyConverter.SINGLETON;
    private final static Converter<URL, Typed<URL>> URL_CONVERTER=new Converter<URL, Typed<URL>>() {
	    @Override
	    public Typed<URL> convert(URL pIn) {
		return new TypedLink(pIn);
	    }
	};
    public final static Path DOCUMENT_DIR=Paths.get("documents");
    private final static String JOURNAL_NAME="journal";
    private final static Path JOURNAL=Paths.get(JOURNAL_NAME);
    public final static char JOURNAL_NUM_DELIM='_';
    public final static String THREAD_JOURNAL_NAME=JOURNAL_NUM_DELIM+JOURNAL_NAME;
    private final static Path TEST_JOURNAL=Paths.get("test_journal");
    private final static boolean BLACKLISTING=false;
    private final static Journal<String,URL> NULL_JOURNAL=new NullJournal<String,URL>();
	
    private final static int CONNECT_TIMEOUT=10*1000;
    private final static int SOCKET_TIMEOUT=10*1000;
    private final static int COMPLETION_TIMEOUT=180*1000;
    private final static long MAX_FILE_DOWNLOAD_SIZE=1024*1024*10;
    
    private final Closer mCloser=new Closer();
	
    public static Path downloadIndex() {
	return DOCUMENT_DIR.resolve(MainIndexDownloaded.INDEX_PATH);
    }
	
    public MainDownloader() {
		
    }

    public ApacheDownloader createClient() {
	ApacheDownloader downloader=new ApacheDownloader();
	mCloser.before(downloader);
	return downloader;
    }
	
    private static class ApacheDownloader implements AutoCloseable, Downloader<URL, ContentTypeSourceable> {

	private final Throttle mDownloadThrottle=new Throttle(3, 2000, TimeUnit.MILLISECONDS);
	private CloseableHttpAsyncClient mClient;
	private final RequestConfig mRequestConfig;

	public ApacheDownloader() {
	    mRequestConfig= RequestConfig.custom()
		.setConnectTimeout(CONNECT_TIMEOUT)
		.setSocketTimeout(SOCKET_TIMEOUT)
		.build();
	    mClient=client(mRequestConfig);

	}

	@Override
	public void reset() throws IOException {
	    mClient.close();
	    mClient=client(mRequestConfig);
	}

	@Override
	public Promise<ContentTypeSourceable, IOException, Void> downloadLink(
									      URL pSource, int pDstKey, Path pDst) throws IOException {
	    URL encoded=URLUtils.reencode(pSource);
	    //encoded=new URL("http", encoded.getHost(), encoded.getPort(), encoded.getFile());

	    /*
	     * This deferred should be rejected for any exceptions that are caused by local issues. That should
	     * cause the QueryThread and consequently the process to quit.
	     * Where there's an exception due to a failed download due to a HTTP error or other network / remote site
	     * issues the deferred should be accepted causing only that download to be skipped. Remaining downloads
	     * for the same query, and later queries, will continue as normal. 
	     */
	    final Deferred<ContentTypeSourceable,IOException,Void> deferred=
		new DownloadableDeferredObject<ContentTypeSourceable,IOException,Void>(encoded, pDst);
	    try {
		//if(encoded.equals(new URL("https://archive.org/stream/lettressecrtes01cathuoft/lettressecrtes01cathuoft_djvu.txt"))) {
		ZeroCopyConsumer<File> consumer=new DeferredZeroCopyConsumer(encoded, pDstKey, pDst.toFile(), deferred, COMPLETION_TIMEOUT, MAX_FILE_DOWNLOAD_SIZE);
		
		mDownloadThrottle.choke();
		Log.i(TAG, "getting: "+Log.esc(encoded.toString()));
		mClient.execute(HttpAsyncMethods.createGet(encoded.toString()), consumer, null);
		/*
		  } else {
		  Log.i(TAG, "creating empty: "+pDst+" for "+Log.esc(encoded));
		  FileOutputStream out=null;
		  try {
		  out=new FileOutputStream(pDst.toFile());
		  } finally {
		  if(out!=null) {
		  out.close();
		  }
		  }
		  deferred.resolve(new ContentTypeSourceable(pDstKey, "text/plain"));
		  }
		*/
	    } catch(IllegalArgumentException e) {
		//This will be caused by a URISyntaxException triggered by illegal characters in some download links
		deferred.resolve(new ContentTypeSourceable(pDstKey, null));
		Log.e(TAG, e);
	    } catch(Exception e) {
		deferred.resolve(new ContentTypeSourceable(pDstKey, null));
		Log.e(TAG, e, Log.esc("Exception for: "+deferred));
		throw new IOException(e);
	    }
	    /*
	    try {
		while(deferred.isPending()) {
		    Thread.sleep(1000);
		}
	    } catch(InterruptedException e) {
		Log.i(TAG, "***Interrupted***");
	    }
	    */
	    return deferred;
	}

	public void downloadSynchronous(URL pSource, Path pDst) throws IOException, InterruptedException {
	    downloadLink(pSource, 0, pDst).waitSafely();
	}
		
	@Override
	public void close() throws Exception {
	    mClient.close();
	}	
    }
	
    public static RestrictedSearchEngine<String,URL,URL> setupBlacklist(RestrictedSearchEngine<String,URL,URL>  pSearchEngine) throws UnsupportedEncodingException {
	if(BLACKLISTING) {
	    String blacklistedSites[]=new String[] {
		"catalogue.conductus.ac.uk",
		"diamm.ac.uk", 
		"chmtl.indiana.edu/tml",
		"archive.org/stream/analectahymnicam20drev",
		"archive.org/stream/analectahymnica21drevuoft",
		"archive.org/stream/analectahymnicam21drev"
	    };

	    if(!pSearchEngine.filterSites(new HashSet<String>(Arrays.<String>asList(blacklistedSites)))) {
		boolean blacklisted=pSearchEngine.filterPhrases(new HashSet<String>(Arrays.<String>asList(new String[] {
				//"Cantum pulcriorem invenire", //sites from Conductus don't seem to be returned 
				"DIAMM", 
				"MUSICARUM LATINARUM", //corresponding to TML
				"Galler Schule Processionshymnen dichten", //corresponding to analectahymnicam20drev
				"Binnenreime betrachtet werden k6nnten", //corresponding to analectahymnica21drevuoft
				"CANT10NE8 ET MUTETE" //corresponding to analectahymnicam21drev
			    })));
		if(!blacklisted) {
		    Log.e(TAG, "failed to blacklist");
		}
	    }
	}
	return pSearchEngine;
    }
	
	
	
    public void downloadAll(String pPath, boolean pDummyRun, Set<String> pFilteredURLs) {
	System.out.println("About to download results for all queries.");
	Rtu.continuePrompt();
	try {
	    Journal<String,URL> journal=pDummyRun?NULL_JOURNAL:new DeferredFilesJournal<String,URL,ContentTypeSourceable>(
															  JOURNAL, pFilteredURLs,
															  KEY_CONVERTER, URL_CONVERTER, this.createClient());

	    Throttle throttle=new Throttle(1, 668, TimeUnit.MILLISECONDS);
	    try(QueryThread<String> receiver=new QueryThread<String>(1,setupBlacklist(
										      (pDummyRun?new BingSearchEngine(throttle) : new BingSearchEngine(BingSearchEngine.AUTH_KEY, throttle))), journal);
		IndexViewer index=new IndexViewer(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX);) {
		receiver.start();
		CPIUtils.listAllTokenExpansions(index, pPath, receiver);
	    }
	} catch (Exception e) {
	    Log.e(TAG, e);
	}

    }
	
    public QueryThread<String> setupQuery(int pNumThreads, Journal<String,URL> pJournal, Throttle pThrottle) throws Exception {
	QueryThread<String> receiver=new QueryThread<String>(pNumThreads,
							     setupBlacklist(new BingSearchEngine(BingSearchEngine.AUTH_KEY, pThrottle)), pJournal);

	//receiver.setThrottle(pThrottle);
	receiver.start();
	return receiver;
    }

    public void downloadFiltered(String pStemPath, Filter<List<List<String>>> pFilter, Set<String> pFilteredURLs) throws Exception {
	Journal<String,URL> journal=new DeferredFilesJournal<String,URL,ContentTypeSourceable>(
											       JOURNAL, pFilteredURLs, KEY_CONVERTER, URL_CONVERTER, this.createClient());

	try(QueryThread<String> receiver=setupQuery(1, journal, new Throttle(1, 334, TimeUnit.MILLISECONDS));
	    IndexViewer index=new IndexViewer(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX);
	    ExpansionDistributor<String,String> dist=ExpansionDistributor.relay(receiver, pFilter, ExpansionComparator.NGRAM_PRIORITISER);
	    ) {
	    CPIUtils.listAllTokenExpansions(index, pStemPath, dist);
	}		
    }
	
    public void downloadOne(String pQueryName, URL pURL) {
	try {
	    Journal<String,URL> journal=new DeferredFilesJournal<String,URL,ContentTypeSourceable>(
												   TEST_JOURNAL, Collections.<String>emptySet(),
												   KEY_CONVERTER, URL_CONVERTER, this.createClient());

	    try(QueryThread<String> receiver=new QueryThread<String>(1,
								     setupBlacklist(new BingSearchEngine(BingSearchEngine.AUTH_KEY, Throttle.NULL_THROTTLE)), journal)) {
		receiver.search(new HttpQuery<String>(pQueryName, pURL));
	    }
	} catch (Exception e) {
	    Log.e(TAG, e);
	}

    }
    
    //Code for configuring SSL/TLS is taken from https://stackoverflow.com/questions/28263055/configuring-ssl-in-apache-httpasyncclient
    static public SSLContext getSSLContext() {
	final TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
	try {
	    final SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
	    sslContext.getServerSessionContext().setSessionCacheSize(1000);
	    return sslContext;
	} catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
	}
	return null;
    }

    //Code for configuring SSL/TLS is taken from https://stackoverflow.com/questions/28263055/configuring-ssl-in-apache-httpasyncclient
    static public Registry<SchemeIOSessionStrategy> getSSLRegistryAsync() {
	return RegistryBuilder.<SchemeIOSessionStrategy>create()
            .register("http", NoopIOSessionStrategy.INSTANCE)
            .register("https", new SSLIOSessionStrategy(
							getSSLContext(), null, null, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)).build();
    }

    //Code for configuring SSL/TLS is taken from https://stackoverflow.com/questions/28263055/configuring-ssl-in-apache-httpasyncclient
    static public PoolingNHttpClientConnectionManager getPoolingNHttpClientConnectionManager() {
	try {
	    final PoolingNHttpClientConnectionManager connectionManager =
                new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor(IOReactorConfig.DEFAULT), getSSLRegistryAsync());
	    connectionManager.setMaxTotal(8);
	    connectionManager.setDefaultMaxPerRoute(8);

	    return connectionManager;
	} catch (IOReactorException e) {
	}
	return null;
    }

    private static CloseableHttpAsyncClient client(RequestConfig pRequestConfig) {
	//Code for configuring SSL/TLS is taken from https://stackoverflow.com/questions/28263055/configuring-ssl-in-apache-httpasyncclient
	//System.setProperty("javax.net.debug", "ssl,handshake");
	System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");

	CloseableHttpAsyncClient client=HttpAsyncClients.custom().setConnectionManager(getPoolingNHttpClientConnectionManager()).setDefaultRequestConfig(pRequestConfig).build();
	client.start();	
	return client;
    }
	
    @Override
    public void close() throws Exception {
	mCloser.close();
    }

    private static void usage() {
	System.out.println("Usage cat <FILTERED_URLS_FILE> | java -cp <CLASS_PATH> com.hourglassapps.cpi_ii.web_search.MainDownloader all <STEM_FILE>");
	System.out.println("      cat <FILTERED_URLS_FILE> | java -cp <CLASS_PATH> com.hourglassapps.cpi_ii.web_search.MainDownloader all --real <STEM_FILE>");
	System.out.println("      cat <FILTERED_URLS_FILE> | java -cp <CLASS_PATH> com.hourglassapps.cpi_ii.web_search.MainDownloader random <STEM_FILE> <SEED>");
	System.out.println("      cat <FILTERED_URLS_FILE> | java -cp <CLASS_PATH> com.hourglassapps.cpi_ii.web_search.MainDownloader partition <STEM_FILE> <NUM_PROCESSES> <MOD_RESULT>");
	System.out.println("      cat <FILTERED_URLS_FILE> | java -cp <CLASS_PATH> com.hourglassapps.cpi_ii.web_search.MainDownloader threads <STEM_FILE> <NUM_PROCESSES>");
	System.out.println("      cat <URL_QUERY> | java com.hourglassapps.cpi_ii.web_search.MainDownloader one <KEY_NAME>");
	System.out.println("      java com.hourglassapps.cpi_ii.web_search.MainDownload download <URL> <FILENAME>");
	System.exit(-1);
    }

    private static List<String> readStdIn() throws IOException {
	List<String> lines=new ArrayList<>();
	try(
	    BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
	    ) {
	    String line=reader.readLine();
	    while(line!=null) {
		lines.add(line);
		line=reader.readLine();
	    }
	}
	return lines;
    }

    public static void main(String pArgs[]) throws IOException, InterruptedException {
	if(pArgs.length<2) {
	    usage();
	}
		
	int lastIdx=0;
	try {
			
	    Cmd cmd=Cmd.inst(pArgs[lastIdx++]);
	    Set<String> filteredURLs=null;
	    switch(cmd) {
	    case ALL:
	    case THREADS:
	    case PARTITION:
	    case RANDOM:
		filteredURLs=new HashSet<>(readStdIn());
	    }
			
	    try(MainDownloader downloaderManager=new MainDownloader()) {
		String stemPath;
		switch(cmd) {
		case ALL:
		    if(pArgs.length!=2 && pArgs.length!=3) {
			throw new UnrecognisedSyntaxException();
		    }
		    boolean dummyRun=true;
		    if("--real".equals(pArgs[lastIdx])) {
			dummyRun=false;
			lastIdx++;
		    }
		    stemPath=pArgs[lastIdx++];
		    if(dummyRun) {
			System.out.println("Dummy run...");
		    } else {
			System.out.println("Querying search engine with all queries...");
		    }
					
		    downloaderManager.downloadAll(stemPath, dummyRun, filteredURLs);
		    break;
		case THREADS:
		    if(pArgs.length!=3) {
			throw new UnrecognisedSyntaxException();
		    }
		    stemPath=pArgs[lastIdx++];
		    int numThreads=Integer.valueOf(pArgs[lastIdx++]);
		    System.out.println(numThreads+" thread download beginning...");

		    /* For testing we currently pass a RandomTemplate instance to the downloadAnIndex method. This ensures that only a small
		     * proportion of randomly selection queries are submitted to Bing. For a full run where all queries are submitted please
		     * replace the current call to download.downloadAndIndex by replacing the current line with the commented out line below it. 
		     * 
		     */

		    //The probability in the RandomTemplate
		    //constructor below determines the number of
		    //queries each thread must process. So if you
		    //increase the number of threads you also increase
		    //the number of queries sent.
		    
		    //downloader.downloadAndIndex(stemPath, numThreads, new RandomTemplate<List<List<String>>>(numThreads, 123456, 0.013361), filteredURLs);
		    //downloaderManager.downloadAndIndex(stemPath, numThreads, new RandomTemplate<List<List<String>>>(numThreads, 345612, 0.00030539), filteredURLs);
		    //downloaderManager.downloadAndIndex(stemPath, numThreads, new RandomTemplate<List<List<String>>>(numThreads, 123456, 0.00060539), filteredURLs);
		    downloaderManager.downloadAndIndex(stemPath, numThreads, new HashTemplate<List<List<String>>>(), filteredURLs);
					
		    break;
		case PARTITION:
		    if(pArgs.length!=4) {
			throw new UnrecognisedSyntaxException();
		    }
		    stemPath=pArgs[lastIdx++];
		    int numProcesses=Integer.valueOf(pArgs[lastIdx++]);
		    int modResult=Integer.valueOf(pArgs[lastIdx++]);
		    System.out.println("Querying search engine with partitioned queries ("+modResult+"/"+numProcesses+")...");
		    downloaderManager.downloadFiltered(stemPath, 
						       new JobDelegator<List<List<String>>>(numProcesses, new HashTemplate<List<List<String>>>()).filter(modResult), filteredURLs);
		    break;
		case RANDOM:
		    if(pArgs.length!=3) {
			throw new UnrecognisedSyntaxException();
		    }
		    stemPath=pArgs[lastIdx++];
		    int seed=Integer.valueOf(pArgs[lastIdx++]);
		    System.out.println("Querying search engine with random queries...");
		    downloaderManager.downloadFiltered(stemPath, 
						       new JobDelegator<List<List<String>>>(1, new RandomTemplate<List<List<String>>>(1, seed, 0.0046155)).filter(), filteredURLs);
		    break;
		case ONE:
		    if(pArgs.length!=2) {
			throw new UnrecognisedSyntaxException();
		    }
		    try(
			BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
			) {
			URL queryUrl=new URL(reader.readLine()+BingSearchEngine.generalSuffix());
			String key=pArgs[lastIdx++];
			if(key.indexOf(File.separatorChar)!=-1) {
			    throw new UnrecognisedSyntaxException();
			}
			downloaderManager.downloadOne(key, queryUrl);
		    }
		    break;
		case DOWNLOAD:
		    if(pArgs.length!=3) {
			System.out.println("num args: "+pArgs.length);
			throw new UnrecognisedSyntaxException();
		    }
		    URL downloadUrl=new URL(pArgs[lastIdx++]);
		    Path dest=Paths.get(pArgs[lastIdx++]);
		    downloaderManager.createClient().downloadSynchronous(downloadUrl, dest);
		    break;
		}
	    }
	} catch(UnrecognisedSyntaxException e) {
	    usage();
	    System.exit(-1);
	} catch(Exception e) {
	    Log.e(TAG, e);
	}
    }

    private void downloadAndIndex(String stemPath, int numThreads, FilterTemplate<List<List<String>>> pFilter, Set<String> pFilteredURLs) throws Exception {
	if(!Files.exists(DOCUMENT_DIR)) {
	    Files.createDirectories(DOCUMENT_DIR);
	} else {
	    if(!Files.isDirectory(DOCUMENT_DIR) || !Files.isWritable(DOCUMENT_DIR) || !Files.isReadable(DOCUMENT_DIR)) {
		throw new IOException(DOCUMENT_DIR.toString()+" must be a readable/writeable directory");
	    }
	}
	try(final IndexingThread indexer=new IndexingThread(downloadIndex(), numThreads)) {
			
	    indexer.start();
	    QueryThread<String> receiver=null;
	    final List<AsyncExpansionReceiver<String, String>> receivers=new ArrayList<>();
	    final List<DeferredFilesJournal<String,URL,ContentTypeSourceable>> journals=new ArrayList<>();
	    List<Filter<List<List<String>>>> filters=
		new JobDelegator<List<List<String>>>(numThreads, pFilter).filters();
	    Throttle throttle=new Throttle(1, 434, TimeUnit.MILLISECONDS);
	    for(int t=0; t<numThreads; t++) {
		DeferredFilesJournal<String,URL,ContentTypeSourceable> journal=
		    new DeferredFilesJournal<String,URL,ContentTypeSourceable>(
									       DOCUMENT_DIR.resolve(Integer.toString(t)+THREAD_JOURNAL_NAME), pFilteredURLs, KEY_CONVERTER, URL_CONVERTER, this.createClient());
		receiver=setupQuery(numThreads, journal, throttle);
		journals.add(journal);
		receivers.add(receiver);
	    }
			
	    try(IndexViewer index=new IndexViewer(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX);
		ExpansionDistributor<String,String> dist=
		new ExpansionDistributor<String,String>(
							Ii.zip(receivers, filters), 
							ExpansionComparator.NGRAM_PRIORITISER);
		) {
		dist.promise().progress(new ProgressCallback<Ii<Integer,String>>(){

			@Override
			public void onProgress(
					       Ii<Integer, String> pTidDir) {
			    indexer.push(new QueryRecord<String>(pTidDir.fst(), pTidDir.snd(), journals.get(pTidDir.fst()).path(
																KEY_CONVERTER.convert(pTidDir.snd()))));
			}
		    });
		CPIUtils.listAllTokenExpansions(index, stemPath, dist);
	    }
	}
    }

}
