package com.hourglassapps.cpi_ii.web_search;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.ExpansionReceiver;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;

public abstract class AbstractDisjunctionRecorder extends ConcreteThrower<Exception> implements ExpansionReceiver<String> {
	private final static String TAG=AbstractDisjunctionRecorder.class.getName();
	private List<List<String>> mDisjunctions=new ArrayList<List<String>>();
	private Query<URI> mQ;	
	private DataOutputStream mOut;
	
	public AbstractDisjunctionRecorder(Query<URI> pQ) throws IOException {
		mQ=pQ;
		PipedInputStream consumer=new PipedInputStream();
		mOut=new DataOutputStream(new BufferedOutputStream(new PipedOutputStream(consumer)));
		new QueryThread(consumer).start();
	}
	
	private class QueryThread extends Thread {
		private DataInputStream mIn;
		
		public QueryThread(PipedInputStream pIn) {
			mIn=new DataInputStream(pIn);
		}
		
		public void run() {
			Iterator<URI> links;
			try {
				List<String> disjunctions=new ArrayList<>();
				while(true) {
					int numDisjunctions=mIn.readInt();
					for(int i=0; i<numDisjunctions; i++) {
						disjunctions.add(mIn.readUTF());
					}
					
					links=mQ.search(disjunctions);
					URI link;
					System.out.println("Results for: "+Rtu.join(disjunctions, " OR "));
					while(links.hasNext()){
						link=links.next();
						onLink(link);
					}

					disjunctions.clear();
				}
			} catch(IOException e) {
				Log.i(TAG, "quitting thread");
			}
		}
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
		try {
			Collections.sort(mDisjunctions, ExpansionComparator.NGRAM_PRIORITISER);
			List<String> joinedDisjunctions=join(mDisjunctions);
			if(fallThrough()) {
				return;
			}
			mOut.writeInt(joinedDisjunctions.size());
			for(String dis: joinedDisjunctions) {
				mOut.writeUTF(dis);
			}
		} catch (IOException e) {
			ctch(e);
		}
		mDisjunctions.clear();
		//System.exit(0);
	}
	
	/***
	 * onLink is invoked for each link in the results from the SearchEngine. onLink will not 
	 * be invoked on the same main thread.
	 * @param pLink
	 */
	abstract public void onLink(URI pLink);
}