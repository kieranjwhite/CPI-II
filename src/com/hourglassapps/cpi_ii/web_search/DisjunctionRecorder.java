package com.hourglassapps.cpi_ii.web_search;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.ExpansionReceiver;
import com.hourglassapps.util.Rtu;

public class DisjunctionRecorder extends ConcreteThrower<Exception> implements ExpansionReceiver<String> {
	private List<String> mDisjunctions=new ArrayList<String>();
	private Query<URI> mQ;	
	
	public DisjunctionRecorder(Query<URI> pQ) {
		mQ=pQ;
	}
	
	@Override
	public void onExpansion(List<String> pExpansions) {
		String disjunction="\""+Rtu.join(pExpansions, " ")+"\"";
		mDisjunctions.add(disjunction);
	}

	@Override
	public void onGroupDone(int pNumExpansions) {
		Iterator<URI> links;
		try {
			links = mQ.search(mDisjunctions);
			URI link;
			System.out.println("Results for: "+Rtu.join(mDisjunctions, " OR "));
			while(links.hasNext()){
				link=links.next();
				System.out.println(link);
			}
		} catch (IOException e) {
			ctch(e);
		}
		mDisjunctions.clear();
	}
	
}