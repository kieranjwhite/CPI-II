package com.hourglassapps.cpi_ii.web_search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.client.methods.ZeroCopyConsumer;
import org.jdeferred.Deferred;
import org.jdeferred.ProgressCallback;
import org.jdeferred.Promise;

import com.hourglassapps.cpi_ii.CPIUtils;
import com.hourglassapps.cpi_ii.MainIndexConductus;
import com.hourglassapps.cpi_ii.MainIndexDownloaded;
import com.hourglassapps.cpi_ii.lucene.IndexViewer;
import com.hourglassapps.cpi_ii.lucene.IndexingThread;
import com.hourglassapps.cpi_ii.web_search.bing.BingSearchEngine;
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

public class MainDownloader implements AutoCloseable, Downloader<URL,ContentTypeSourceable> {
	private final static String TAG=MainDownloader.class.getName();
	private final static Converter<String,String> KEY_CONVERTER=JournalKeyConverter.SINGLETON;
	private final static Path DOCUMENT_DIR=Paths.get("documents");
	private final static String JOURNAL_NAME="journal";
	private final static Path JOURNAL=Paths.get(JOURNAL_NAME);
	private final static String THREAD_JOURNAL_NAME='_'+JOURNAL_NAME;
	private final static Path TEST_JOURNAL=Paths.get("test_journal");
	private final static boolean BLACKLISTING=false;
	private final static Journal<String,Typed<URL>> NULL_JOURNAL=new NullJournal<String,Typed<URL>>();
	
	private final static int CONNECT_TIMEOUT=10*1000;
	private final static int SOCKET_TIMEOUT=10*1000;
	private final static int COMPLETION_TIMEOUT=180*1000;
	private final RequestConfig mRequestConfig;

	private CloseableHttpAsyncClient mClient;
	
	public static Path downloadIndex() {
		return DOCUMENT_DIR.resolve(MainIndexDownloaded.INDEX_PATH);
	}
	
	public MainDownloader() {
		 mRequestConfig= RequestConfig.custom()
		            .setConnectTimeout(CONNECT_TIMEOUT)
		            .setSocketTimeout(SOCKET_TIMEOUT)
		            .build();
		mClient=client(mRequestConfig);
	}

	@Override
	public Promise<ContentTypeSourceable, IOException, Void> downloadLink(
			URL pSource, int pDstKey, Path pDst) throws IOException {
		URL encoded=URLUtils.reencode(pSource);

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
			ZeroCopyConsumer<File> consumer=new DeferredZeroCopyConsumer(pDstKey, pDst.toFile(), deferred, COMPLETION_TIMEOUT);
			mClient.execute(HttpAsyncMethods.createGet(encoded.toString()), consumer, null);
		} catch(IllegalArgumentException e) {
			//This will be caused by a URISyntaxException triggered by illegal characters in some download links
			deferred.resolve(new ContentTypeSourceable(pDstKey, null));
			Log.e(TAG, e);
		} catch(Exception e) {
			deferred.resolve(new ContentTypeSourceable(pDstKey, null));
			Log.e(TAG, e, Log.esc("Exception for: "+deferred));
			throw new IOException(e);
		}
		return deferred;
	}	
	
	public void downloadSynchronous(URL pSource, Path pDst) throws IOException, InterruptedException {
		downloadLink(pSource, 0, pDst).waitSafely();
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
						"MUSICARUM LATINARUM",
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
	
	public void downloadAll(String pPath, boolean pDummyRun) {
		System.out.println("About to download results for all queries.");
		Rtu.continuePrompt();
		try {
			Journal<String,Typed<URL>> journal=pDummyRun?NULL_JOURNAL:new DeferredFilesJournal<String,URL,ContentTypeSourceable>(JOURNAL,
					JournalKeyConverter.SINGLETON, this);

			try(QueryThread<String> receiver=new QueryThread<String>(1,setupBlacklist(
							(pDummyRun?new BingSearchEngine() : new BingSearchEngine(BingSearchEngine.AUTH_KEY))), journal);
					IndexViewer index=new IndexViewer(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX);) {
				//We'll limit our downloads to 5 every 1 sec
				receiver.setThrottle(new Throttle(5, 1, TimeUnit.SECONDS));
				receiver.start();
				CPIUtils.listAllTokenExpansions(index, pPath, receiver);
			}
		} catch (Exception e) {
			Log.e(TAG, e);
		}

	}
	
	public QueryThread<String> setupQuery(int pNumThreads, Journal<String,Typed<URL>> pJournal) throws Exception {
		QueryThread<String> receiver=new QueryThread<String>(pNumThreads,
				setupBlacklist(new BingSearchEngine(BingSearchEngine.AUTH_KEY)), pJournal);

		//We'll limit our downloads to 5 every 1 sec
		receiver.setThrottle(new Throttle(5, 1, TimeUnit.SECONDS));
		receiver.start();
		return receiver;
	}

	public void downloadFiltered(String pStemPath, Filter<List<List<String>>> pFilter) throws Exception {
		Journal<String,Typed<URL>> journal=new DeferredFilesJournal<String,URL,ContentTypeSourceable>(
				JOURNAL, KEY_CONVERTER, this);

		try(QueryThread<String> receiver=setupQuery(1,journal);
				IndexViewer index=new IndexViewer(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX);
				ExpansionDistributor<String,String> dist=ExpansionDistributor.relay(receiver, pFilter, ExpansionComparator.NGRAM_PRIORITISER);
				) {
			CPIUtils.listAllTokenExpansions(index, pStemPath, dist);
		}		
	}
	
	public void downloadOne(String pQueryName, URL pURL) {
		try {
			Journal<String,Typed<URL>> journal=new DeferredFilesJournal<String,URL,ContentTypeSourceable>(TEST_JOURNAL, 
					JournalKeyConverter.SINGLETON, this);

			try(QueryThread<String> receiver=new QueryThread<String>(1,
					setupBlacklist(new BingSearchEngine(BingSearchEngine.AUTH_KEY)), journal)) {
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
		System.out.println("      java com.hourglassapps.cpi_ii.web_search.MainDownload partition <STEM_FILE> <NUM_PROCESSES> <MOD_RESULT>");
		System.out.println("      java com.hourglassapps.cpi_ii.web_search.MainDownload threads <STEM_FILE> <NUM_PROCESSES>");
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
				case THREADS:
					if(pArgs.length!=3) {
						throw new UnrecognisedSyntaxException();
					}
					stemPath=pArgs[lastIdx++];
					int numThreads=Integer.valueOf(pArgs[lastIdx++]);
					System.out.println(numThreads+" thread download beginning...");
					//downloader.downloadAndIndex(stemPath, numThreads, new HashTemplate<List<List<String>>>());
					downloader.downloadAndIndex(stemPath, numThreads, new RandomTemplate<List<List<String>>>(numThreads, 123456, 0.0008));
					break;
				case PARTITION:
					if(pArgs.length!=4) {
						throw new UnrecognisedSyntaxException();
					}
					stemPath=pArgs[lastIdx++];
					int numProcesses=Integer.valueOf(pArgs[lastIdx++]);
					int modResult=Integer.valueOf(pArgs[lastIdx++]);
					System.out.println("Querying search engine with partitioned queries ("+modResult+"/"+numProcesses+")...");
					downloader.downloadFiltered(stemPath, 
							new JobDelegator<List<List<String>>>(numProcesses, new HashTemplate<List<List<String>>>()).filter(modResult));
					break;
				case RANDOM:
					if(pArgs.length!=3) {
						throw new UnrecognisedSyntaxException();
					}
					stemPath=pArgs[lastIdx++];
					int seed=Integer.valueOf(pArgs[lastIdx++]);
					System.out.println("Querying search engine with random queries...");
					//downloader.downloadFiltered(stemPath, 
					//		new ConverterReceiver<List<List<String>>>(new RandomConverter<List<List<String>>>(seed, 0.0015385)).filter());
					downloader.downloadFiltered(stemPath, 
							new JobDelegator<List<List<String>>>(1, new RandomTemplate<List<List<String>>>(1, seed, 0.0046155)).filter());
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
		} catch(Exception e) {
			Log.e(TAG, e);
		}
	}

	private void downloadAndIndex(String stemPath, int numThreads, FilterTemplate<List<List<String>>> pFilter) throws Exception {
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
			for(int t=0; t<numThreads; t++) {
				DeferredFilesJournal<String,URL,ContentTypeSourceable> journal=
						new DeferredFilesJournal<String,URL,ContentTypeSourceable>(
								DOCUMENT_DIR.resolve(Integer.toString(t)+THREAD_JOURNAL_NAME), KEY_CONVERTER, this);
				receiver=setupQuery(numThreads, journal);
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
						indexer.push(new QueryRecord<String>(pTidDir.fst(), pTidDir.snd(), journals.get(pTidDir.fst()).path(KEY_CONVERTER.convert(pTidDir.snd()))));
					}
				});
				CPIUtils.listAllTokenExpansions(index, stemPath, dist);
			}
		}
	}

}
