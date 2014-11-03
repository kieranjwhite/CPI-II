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

public abstract class AbstractQueryThread extends Thread implements AutoCloseable, ExpansionReceiver<String> {
	private final static String TAG=AbstractQueryThread.class.getName();
	
	private final List<List<String>> mDisjunctions=new ArrayList<List<String>>();
	private final PipedInputStream mIn=new PipedInputStream();
	private final DataOutputStream mOut=new DataOutputStream(new BufferedOutputStream(new PipedOutputStream(mIn)));
	private final ConcreteThrower<Exception> mThrower=new ConcreteThrower<>();
	private final Query<URI> mQ;	
	
	public AbstractQueryThread(Query<URI> pQ) throws IOException {
		super("query");
		mQ=pQ;
	}
	
	@Override
	public void run() {
		Iterator<URI> links;
		try (DataInputStream in=new DataInputStream(mIn)){
			List<String> disjunctions=new ArrayList<>();
			while(true) {
				int numDisjunctions=in.readInt();
				for(int i=0; i<numDisjunctions; i++) {
					disjunctions.add(in.readUTF());
				}
				
				links=mQ.search(disjunctions);
				URI link;
				//System.out.println(Rtu.join(disjunctions, " OR "));
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
			if(mThrower.fallThrough()) {
				return;
			}
			mOut.writeInt(joinedDisjunctions.size());
			for(String dis: joinedDisjunctions) {
				mOut.writeUTF(dis);
			}
		} catch (IOException e) {
			mThrower.ctch(e);
		}
		mDisjunctions.clear();
	}
	
	@Override
	public void close() throws Exception {
		mOut.close();
		mThrower.close();
	}
	
	/***
	 * onLink is invoked for each link in the results from the SearchEngine. onLink will not 
	 * be invoked on the main thread.
	 * @param pLink
	 */
	abstract public void onLink(URI pLink);
}