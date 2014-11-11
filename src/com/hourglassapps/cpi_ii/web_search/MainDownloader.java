package com.hourglassapps.cpi_ii.web_search;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.client.methods.ZeroCopyConsumer;
import org.jdeferred.Deferred;
import org.jdeferred.Promise;

import com.hourglassapps.cpi_ii.IndexViewer;
import com.hourglassapps.cpi_ii.Journal;
import com.hourglassapps.cpi_ii.MainIndexConductus;
import com.hourglassapps.cpi_ii.MainListIndexTerms;
import com.hourglassapps.cpi_ii.web_search.bing.BingSearchEngine;
import com.hourglassapps.persist.DeferredFileJournal;
import com.hourglassapps.persist.NullJournal;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Downloader;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.URLUtils;

public class MainDownloader implements AutoCloseable, Downloader<URL> {
	private final static String TAG=MainDownloader.class.getName();
	private final static Path JOURNAL=Paths.get("journal");
	private final static String PATH_ENCODING=StandardCharsets.UTF_8.toString();
	private final static Journal<String,URL> NULL_JOURNAL=new NullJournal<String,URL>();
	
	private CloseableHttpAsyncClient mClient=HttpAsyncClients.createDefault();
	
	public MainDownloader() {
		mClient.start();
	}
	
	@Override
	public Promise<Void,IOException,Void> download(final URL pSource, final Path pDest) throws IOException {
		URL encoded=URLUtils.reencode(pSource);
		final Deferred<Void,IOException,Void> deferred=new DownloadableDeferredObject<Void,IOException,Void>(encoded, pDest);
		try {
			ZeroCopyConsumer<File> consumer=new DeferredZeroCopyConsumer(pDest.toFile(), deferred);
			mClient.execute(HttpAsyncMethods.createGet(encoded.toString()), consumer, null);
			return deferred;
		} catch(Exception e) {
			deferred.resolve(null);
			Log.e(TAG, e, Log.esc("Exception for: "+deferred));
			throw new IOException(e);
		}
	}
	
	public void downloadAll(boolean pDummyRun, String pPath) {
		try(final BingSearchEngine q=(pDummyRun?new BingSearchEngine() : new BingSearchEngine(BingSearchEngine.AUTH_KEY)).
				setFilter(new RandomFilter<URL>(123456, 0.0015385))) {
			Journal<String,URL> journal=pDummyRun?NULL_JOURNAL:new DeferredFileJournal<String,URL>(JOURNAL, 
					new Converter<String, String>() {

				@Override
				public String convert(String pIn) {
					//Creates a filename from pIn, a query's first disjunction
					try {
						return URLEncoder.encode(pIn, PATH_ENCODING);
					} catch (UnsupportedEncodingException e) {
						Log.e(TAG, e);
						System.exit(-1);
						return null;
					}
				}
			}, this);

			try(QueryThread<String> receiver=new QueryThread<String>(q, journal)) {
				receiver.start();
				if(!q.filterSites(new HashSet<String>(Arrays.<String>asList(new String[] {
						"catalogue.conductus.ac.uk", "diamm.ac.uk", "chmtl.indiana.edu/tml",
						"archive.org/details/analectahymnicam20drev",
						"archive.org/details/analectahymnica21drevuoft",
						"archive.org/details/analectahymnicam21drev"
				})))) {
					boolean blacklisted=q.filterPhrases(new HashSet<String>(Arrays.<String>asList(new String[] {
							"Cantum pulcriorem invenire", "DIAMM", "MUSICARUM LATINARUM", "Analecta hymnica"
					})));
					if(!blacklisted) {
						Log.e(TAG, "failed to blacklist");
					}
				}
				IndexViewer index=new IndexViewer(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX);
				MainListIndexTerms.listAllTokenExpansions(index, pPath, receiver);
			}
		} catch (Exception e) {
			Log.e(TAG, e);
		}

	}
	
	@Override
	public void reset() throws IOException {
		mClient.close();
		mClient=HttpAsyncClients.createDefault();
	}

	@Override
	public void close() throws IOException {
		mClient.close();
	}

	private static void usage() {
		System.out.println("Usage java com.hourglassapps.cpi_ii.web_search.MainDownload all <STEM_FILE>");
		System.out.println("      java com.hourglassapps.cpi_ii.web_search.MainDownload all --real <STEM_FILE>");
		System.out.println("      java com.hourglassapps.cpi_ii.web_search.MainDownload one <URL> <FILENAME>");
	}
	
	public static void main(String pArgs[]) throws IOException, InterruptedException {
		if(pArgs.length<2) {
			usage();
		}

		boolean all=false;
		int lastIdx=0;

		switch(pArgs[lastIdx]) {
		case "all":
			all=true;
			break;
		case "one":
			all=false;
			break;
		default:
			usage();
			System.exit(-1);
		}
		lastIdx++;
		try(MainDownloader downloader=new MainDownloader()) {
			if(all) {
				boolean dummyRun=true;
				if("--real".equals(pArgs[lastIdx])) {
					dummyRun=false;
					lastIdx++;
				}
				String stemPath=pArgs[lastIdx++];
				if(dummyRun) {
					System.out.println("Dummy run...");
				} else {
					System.out.println("Querying search engine...");
				}
				downloader.downloadAll(dummyRun, stemPath);
			} else {
				URL url=new URL(pArgs[lastIdx++]);
				Path dest=Paths.get(pArgs[lastIdx++]);
				downloader.download(url, dest).waitSafely();;
			}
		}
	}

}
