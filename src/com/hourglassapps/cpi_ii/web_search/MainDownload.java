package com.hourglassapps.cpi_ii.web_search;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;

import com.hourglassapps.cpi_ii.IndexViewer;
import com.hourglassapps.cpi_ii.MainIndexConductus;
import com.hourglassapps.cpi_ii.MainListIndexTerms;
import com.hourglassapps.cpi_ii.web_search.bing.MainQuery;
import com.hourglassapps.util.Log;

public class MainDownload {
	private final static String TAG=MainDownload.class.getName();
	
	public static void main(String pArgs[]) throws IOException {
		if(pArgs.length<1) {
			System.out.println("Usage java com.hourglassapps.cpi_ii.web_search.MainDownload --real <STEM_FILE>");
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
			System.out.println("Querying searh engine...");
		}
		String path=pArgs[pathIdx];
		
		try(
				final MainQuery q=dummyRun?new MainQuery() : new MainQuery(MainQuery.AUTH_KEY);
				AbstractDisjunctionRelayer receiver=new AbstractDisjunctionRelayer(q){

					@Override
					public void onLink(URI pLink) {
						System.out.println("link: "+pLink);
					}
					
				};
		) {
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
					System.out.println();
				}
			}
			IndexViewer index=new IndexViewer(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX);
			MainListIndexTerms.listAllTokenExpansions(index, path, receiver);
		} catch (Exception e) {
			Log.e(TAG, e);
		}
	}
}
