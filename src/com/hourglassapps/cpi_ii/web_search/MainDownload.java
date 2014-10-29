package com.hourglassapps.cpi_ii.web_search;

import java.io.IOException;
import java.net.URI;

import com.hourglassapps.cpi_ii.IndexViewer;
import com.hourglassapps.cpi_ii.MainIndexConductus;
import com.hourglassapps.cpi_ii.MainListIndexTerms;
import com.hourglassapps.cpi_ii.web_search.bing.MainQuery;
import com.hourglassapps.util.Log;

public class MainDownload {
	private final static String TAG=MainDownload.class.getName();
	
	public static void main(String pArgs[]) throws IOException {
		if(pArgs.length!=1) {
			System.out.println("Usage java com.hourglassapps.cpi_ii.web_search.MainDownload <STEM_FILE>");
		}

		try(
				final MainQuery q=new MainQuery(MainQuery.AUTH_KEY);
				AbstractDisjunctionRecorder receiver=new AbstractDisjunctionRecorder(q){

					@Override
					public void onLink(URI pLink) {
						System.out.println(pLink);
					}
					
				};
		) {
			IndexViewer index=new IndexViewer(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX);
			MainListIndexTerms.listAllTokenExpansions(index, pArgs[0], receiver);
		} catch (Exception e) {
			Log.e(TAG, e);
		}
	}
}
