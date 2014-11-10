package com.hourglassapps.cpi_ii.web_search;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Future;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncCharConsumer;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.client.methods.ZeroCopyConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;
import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.hourglassapps.cpi_ii.IndexViewer;
import com.hourglassapps.cpi_ii.Journal;
import com.hourglassapps.cpi_ii.MainIndexConductus;
import com.hourglassapps.cpi_ii.MainListIndexTerms;
import com.hourglassapps.cpi_ii.web_search.bing.BingSearchEngine;
import com.hourglassapps.persist.DeferredFileJournal;
import com.hourglassapps.persist.FileSaver;
import com.hourglassapps.persist.LocalFileJournal;
import com.hourglassapps.persist.NullJournal;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Downloader;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Typed;

public class MainDownloader implements AutoCloseable {
	private final static String TAG=MainDownloader.class.getName();
	private final static Path JOURNAL=Paths.get("journal");
	private final static String PATH_ENCODING=StandardCharsets.UTF_8.toString();
	private final static Journal<String,URL> NULL_JOURNAL=new NullJournal<String,URL>();
	
	private CloseableHttpAsyncClient mClient=HttpAsyncClients.createDefault();
	
	public MainDownloader() {
		mClient.start();
	}
	
	
	public Promise<Void,IOException,Void> download(final URL pSource, final Path pDest) throws IOException {
		final Deferred<Void,IOException,Void> deferred=new DeferredObject<Void,IOException,Void>() {
			@Override
			public String toString() {
				return "DeferredObject: "+pSource.toString()+" to "+pDest.toString();
			}
		};
		try {
			ZeroCopyConsumer<File> consumer=new ZeroCopyConsumer<File>(pDest.toFile()){
				@Override
				protected File process(HttpResponse response, File file,
						ContentType contentType) throws Exception {
					if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
						//Event if this happens don't reject deferred as that would abort entire query
						Log.e(TAG, "Download failed: "+pSource+" http reponse: "+response.getStatusLine().getStatusCode());
						Log.i(TAG, "unexpected http response: "+deferred);
					} else {
						Log.i(TAG, "resolved: "+deferred);						
					}
					deferred.resolve(null);
					return file;
				}

				@Override
				protected void releaseResources() {
					/* Needed for Unknown Host errors
					 * Connection Closed Exception
					 * Connection Reset by Peer 
					 */
					super.releaseResources();
					if(deferred.isPending()) {
						deferred.resolve(null);
						Log.i(TAG, "possible timeout for: "+deferred);
					}
				}
				
			};
			Future<File> future=mClient.execute(HttpAsyncMethods.createGet(pSource.toString()), consumer, null);
			//future.get();
			return deferred;
		} catch(Exception e) {
			deferred.resolve(null);
			Log.i(TAG, "exception for: "+deferred);
			throw new IOException(e);
		}
	}
	
	public void downloadAll(boolean pDummyRun, String pPath) {
		try(final BingSearchEngine q=pDummyRun?new BingSearchEngine() : new BingSearchEngine(BingSearchEngine.AUTH_KEY)) {
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
			}, new Downloader<URL>(){

				@Override
				public Promise<Void, IOException, Void> download(URL pSrc,
						Path pDst) throws IOException {
					return MainDownloader.this.download(pSrc, pDst);
				}

			});

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
	public void close() throws IOException {
		mClient.close();
	}

	public static void main(String pArgs[]) throws IOException {
		if(pArgs.length<1) {
			System.out.println("Usage java com.hourglassapps.cpi_ii.web_search.MainDownload <STEM_FILE>");
			System.out.println("      java com.hourglassapps.cpi_ii.web_search.MainDownload --real <STEM_FILE>");
		}

		boolean dummyRun=true;
		int pathIdx=0;
		if("--real".equals(pArgs[0])) {
			dummyRun=false;
			pathIdx++;
		}
		if(dummyRun) {
			System.out.println("Dummy run...");
		} else {
			System.out.println("Querying search engine...");
		}
		String path=pArgs[pathIdx];

		try(MainDownloader downloader=new MainDownloader()) {
			downloader.downloadAll(dummyRun, path);
		}
	}

}
