package com.hourglassapps.cpi_ii.web_search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.client.methods.ZeroCopyConsumer;
import org.jdeferred.Deferred;
import org.jdeferred.Promise;

import com.hourglassapps.cpi_ii.CPIUtils;
import com.hourglassapps.cpi_ii.IndexViewer;
import com.hourglassapps.cpi_ii.Journal;
import com.hourglassapps.cpi_ii.MainIndexConductus;
import com.hourglassapps.cpi_ii.web_search.bing.BingSearchEngine;
import com.hourglassapps.persist.DeferredFileJournal;
import com.hourglassapps.persist.NullJournal;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Downloader;
import com.hourglassapps.util.Filter;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.MainHeartBeat;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Throttle;
import com.hourglassapps.util.Typed;
import com.hourglassapps.util.URLUtils;

public class MainDownloader implements AutoCloseable, Downloader<URL,ContentTypeSourceable> {
	private final static String TAG=MainDownloader.class.getName();
	private final static Path JOURNAL=Paths.get("journal");
	private final static Path TEST_JOURNAL=Paths.get("test_journal");
	private final static Journal<String,Typed<URL>> NULL_JOURNAL=new NullJournal<String,Typed<URL>>();
	
	private final static int CONNECT_TIMEOUT=10*1000;
	private final static int SOCKET_TIMEOUT=10*1000;
	private final static int COMPLETION_TIMEOUT=180*1000;
	
	private final RequestConfig mRequestConfig;

	private CloseableHttpAsyncClient mClient;
	
	public MainDownloader() {
		 mRequestConfig= RequestConfig.custom()
		            .setConnectTimeout(CONNECT_TIMEOUT)
		            .setSocketTimeout(SOCKET_TIMEOUT)
		            .build();
		mClient=client(mRequestConfig);
	}

	@Override
	public Promise<ContentTypeSourceable, IOException, Void> downloadLink(
			URL pSource, long pDstKey, Path pDst) throws IOException {
		URL encoded=URLUtils.reencode(pSource);
		final Deferred<ContentTypeSourceable,IOException,Void> deferred=
				new DownloadableDeferredObject<ContentTypeSourceable,IOException,Void>(encoded, pDst);
		try {
			ZeroCopyConsumer<File> consumer=new DeferredZeroCopyConsumer(pDstKey, pDst.toFile(), deferred, COMPLETION_TIMEOUT);
			mClient.execute(HttpAsyncMethods.createGet(encoded.toString()), consumer, null);
			return deferred;
		} catch(Exception e) {
			deferred.resolve(new ContentTypeSourceable(pDstKey, null));
			Log.e(TAG, e, Log.esc("Exception for: "+deferred));
			throw new IOException(e);
		}
	}	
	
	public void downloadSynchronous(URL pSource, Path pDst) throws IOException, InterruptedException {
		downloadLink(pSource, 0, pDst).waitSafely();
	}
	
	public static void setupBlacklist(RestrictedSearchEngine<String,URL,URL>  pSearchEngine) throws UnsupportedEncodingException {
		String blacklistedSites[]=new String[] {
				"catalogue.conductus.ac.uk",
				"diamm.ac.uk", 
				"chmtl.indiana.edu/tml",
				"archive.org/stream/analectahymnicam20drev",
				"archive.org/stream/analectahymnica21drevuoft",
				"archive.org/stream/analectahymnicam21drev"
		};
		/*
		if(!pSearchEngine.filterSites(new HashSet<String>(Arrays.<String>asList(blacklistedSites)))) {
			boolean blacklisted=pSearchEngine.filterPhrases(new HashSet<String>(Arrays.<String>asList(new String[] {
					//"Cantum pulcriorem invenire", //sites from Conductus don't seem to be returned 
					"DIAMM", 
					"MUSICARUM LATINARUM",
					"Galler Schule Processionshymnen dichten", //corresponding to analectahymnicam20drev
					"Binnenreime betrachtet werden k6nnten", //corresponding to analectahymnica21drevuoft
					"CANT10NE8 ET MUTETE" //corresponding to analectahymnicam21drev
			})));
			if(!blacklisted) {
				Log.e(TAG, "failed to blacklist");
			}
		}
		*/
	}
	
	public void downloadAll(String pPath, boolean pDummyRun) {
		System.out.println("About to download results for all queries.");
		Rtu.continuePrompt();
		try(final AbstractSearchEngine q=(pDummyRun?new BingSearchEngine() : new BingSearchEngine(BingSearchEngine.AUTH_KEY))) {
			Journal<String,Typed<URL>> journal=pDummyRun?NULL_JOURNAL:new DeferredFileJournal<String,URL,ContentTypeSourceable>(JOURNAL,
					JournalKeyConverter.SINGLETON, this);

			try(QueryThread<String> receiver=new QueryThread<String>(q, journal)) {
				//We'll limit our downloads to 5 every 1 sec
				receiver.setThrottle(new Throttle(5, 1, TimeUnit.SECONDS));
				receiver.start();
				setupBlacklist(q);
				IndexViewer index=new IndexViewer(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX);
				CPIUtils.listAllTokenExpansions(index, pPath, receiver);
			}
		} catch (Exception e) {
			Log.e(TAG, e);
		}

	}
	
	public void downloadFiltered(String pPath, Filter<URL> pFilter) {
		try(final AbstractSearchEngine q=(new BingSearchEngine(BingSearchEngine.AUTH_KEY)).
				setFilter(pFilter)) {
			Journal<String,Typed<URL>> journal=new DeferredFileJournal<String,URL,ContentTypeSourceable>(
					JOURNAL, JournalKeyConverter.SINGLETON, this);

			try(QueryThread<String> receiver=new QueryThread<String>(q, journal)) {
				//We'll limit our downloads to 5 every 1 sec
				receiver.setThrottle(new Throttle(5, 1, TimeUnit.SECONDS));
				receiver.start();
				setupBlacklist(q);
				IndexViewer index=new IndexViewer(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX);
				CPIUtils.listAllTokenExpansions(index, pPath, receiver);
			}
		} catch (Exception e) {
			Log.e(TAG, e);
		}

	}
	
	public void downloadOne(String pQueryName, URL pURL) {
		try(final BingSearchEngine q=new BingSearchEngine(BingSearchEngine.AUTH_KEY)) {
			Journal<String,Typed<URL>> journal=new DeferredFileJournal<String,URL,ContentTypeSourceable>(TEST_JOURNAL, 
					JournalKeyConverter.SINGLETON, this);

			try(QueryThread<String> receiver=new QueryThread<String>(q, journal)) {
				setupBlacklist(q);
				receiver.search(new HttpQuery<String>(pQueryName, pURL));
			}
		} catch (Exception e) {
			Log.e(TAG, e);
		}

	}
	
	private static CloseableHttpAsyncClient client(RequestConfig pRequestConfig) {
		CloseableHttpAsyncClient client=HttpAsyncClients.custom().setDefaultRequestConfig(pRequestConfig).build();
		client.start();	
		return client;
	}
	
	@Override
	public void reset() throws IOException {
		mClient.close();
		mClient=client(mRequestConfig);
	}

	@Override
	public void close() throws IOException {
		mClient.close();
	}

	private static void usage() {
		System.out.println("Usage java com.hourglassapps.cpi_ii.web_search.MainDownload all <STEM_FILE>");
		System.out.println("      java com.hourglassapps.cpi_ii.web_search.MainDownload all --real <STEM_FILE>");
		System.out.println("      java com.hourglassapps.cpi_ii.web_search.MainDownload random <STEM_FILE> <SEED>");
		System.out.println("      java com.hourglassapps.cpi_ii.web_search.MainDownload partition <STEM_FILE> <MOD_RESULT> <NUM_PROCESSES>");
		System.out.println("      echo <URL_QUERY> | java com.hourglassapps.cpi_ii.web_search.MainDownload one <KEY_NAME>");
		System.out.println("      java com.hourglassapps.cpi_ii.web_search.MainDownload download <URL> <FILENAME>");
	}
	
	public static void main(String pArgs[]) throws IOException, InterruptedException {
		if(pArgs.length<2) {
			usage();
		}

		int lastIdx=0;

		try {
			Cmd cmd=Cmd.inst(pArgs[lastIdx++]);
			try(MainDownloader downloader=new MainDownloader()) {
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
					
					downloader.downloadAll(stemPath, dummyRun);
					break;
				case PARTITION:
					if(pArgs.length!=4) {
						throw new UnrecognisedSyntaxException();
					}
					stemPath=pArgs[lastIdx++];
					int modResult=Integer.valueOf(pArgs[lastIdx++]);
					int numProcesses=Integer.valueOf(pArgs[lastIdx++]);
					System.out.println("Querying search engine with partitioned queries ("+modResult+"/"+numProcesses+")...");
					downloader.downloadFiltered(stemPath, new HashFilter(modResult, numProcesses));						
					break;
				case RANDOM:
					if(pArgs.length!=3) {
						throw new UnrecognisedSyntaxException();
					}
					stemPath=pArgs[lastIdx++];
					int seed=Integer.valueOf(pArgs[lastIdx++]);
					System.out.println("Querying search engine with random queries...");
					//downloader.downloadFiltered(stemPath, new RandomFilter<URL>(seed, 0.0015385));						
					downloader.downloadFiltered(stemPath, new RandomFilter<URL>(seed, 0.0046155));						
					break;
				case ONE:
					if(pArgs.length!=2) {
						throw new UnrecognisedSyntaxException();
					}
					try(
							BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
							) {
						URL queryUrl=new URL(reader.readLine()+BingSearchEngine.GENERAL_SUFFIX);
						String key=pArgs[lastIdx++];
						if(key.indexOf(File.separatorChar)!=-1) {
							throw new UnrecognisedSyntaxException();
						}
						downloader.downloadOne(key, queryUrl);
					}
					break;
				case DOWNLOAD:
					if(pArgs.length!=3) {
						System.out.println("num args: "+pArgs.length);
						throw new UnrecognisedSyntaxException();
					}
					URL downloadUrl=new URL(pArgs[lastIdx++]);
					Path dest=Paths.get(pArgs[lastIdx++]);
					downloader.downloadSynchronous(downloadUrl, dest);
					break;
				}
			}
		} catch(UnrecognisedSyntaxException e) {
			usage();
			System.exit(-1);
		}
	}

}
