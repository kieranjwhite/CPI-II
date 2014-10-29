package com.hourglassapps.cpi_ii.web_search;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.ExpansionReceiver;
import com.hourglassapps.util.Rtu;

public abstract class AbstractDisjunctionRecorder extends ConcreteThrower<Exception> implements ExpansionReceiver<String> {
	private List<List<String>> mDisjunctions=new ArrayList<List<String>>();
	private Query<URI> mQ;	
	
	public AbstractDisjunctionRecorder(Query<URI> pQ) {
		mQ=pQ;
	}
	
	@Override
	public void onExpansion(List<String> pExpansions) {
		mDisjunctions.add(new ArrayList<String>(pExpansions));
	}

	private List<String> join(List<List<String>> pDisjunctions) {
		List<String> allJoined=new ArrayList<String>();
		for(List<String> disjunction: pDisjunctions) {
			allJoined.add("\""+Rtu.join(disjunction, " ")+"\"");
			
		}
		return allJoined;
	}
	
	@Override
	public void onGroupDone(int pNumExpansions) {
		Iterator<URI> links;
		try {
			Collections.sort(mDisjunctions, ExpansionComparator.NGRAM_PRIORITISER);
			List<String> joinedDisjunctions=join(mDisjunctions);
			if(fallThrough()) {
				return;
			}
			links = mQ.search(joinedDisjunctions);
			URI link;
			System.out.println("Results for: "+Rtu.join(joinedDisjunctions, " OR "));
			while(links.hasNext()){
				link=links.next();
				onLink(link);
			}
		} catch (IOException e) {
			ctch(e);
		}
		mDisjunctions.clear();
		//System.exit(0);
	}
	
	abstract public void onLink(URI pLink);
}