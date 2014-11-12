package com.hourglassapps.cpi_ii.web_search;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.hourglassapps.cpi_ii.Journal;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.ExpansionReceiver;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Typed;

public class QueryThread<K> extends Thread implements AutoCloseable, ExpansionReceiver<String> {
	private final static String TAG=QueryThread.class.getName();
	
	private final List<List<String>> mDisjunctions=new ArrayList<List<String>>();
	private final PipedInputStream mIn=new PipedInputStream();
	private final DataOutputStream mOut=new DataOutputStream(new BufferedOutputStream(new PipedOutputStream(mIn)));
	private final ConcreteThrower<Exception> mThrower=new ConcreteThrower<>();
	private final SearchEngine<List<String>, K, URL, URL> mQ;	
	private final Journal<K,URL> mJournal;
	
	public QueryThread(SearchEngine<List<String>,K,URL,URL> pQ, Journal<K,URL> pJournal) throws IOException {
		super("query");
		mQ=pQ;
		mJournal=pJournal;
	}
	
	public void search(Query<K,URL> pQuery) throws IOException {
		Iterator<URL> links=mQ.present(pQuery);
		Typed<URL> source;
		while(links.hasNext()){
			try {
				final URL link=links.next();
				source=new TypedLink(link);
				mJournal.add(source);
			} catch(IOException e) {
				Log.e(TAG, e); //we just want to skip over this link, not abort the whole thing
			}
		}
		mJournal.commitEntry(pQuery.uniqueName());		
	}
	
	@Override
	public void run() {
		try (DataInputStream in=new DataInputStream(mIn)){
			List<String> disjunctions=new ArrayList<>();
			boolean skipped=false;
			while(true) {
				int numDisjunctions=in.readInt();
				for(int i=0; i<numDisjunctions; i++) {
					disjunctions.add(in.readUTF());
				}

				Query<K,URL> query=mQ.formulate(disjunctions);
				K name=query.uniqueName();
				if(!mJournal.has(name)) {
					skipped=false;
					search(query);
				} else {
					if(!skipped) {
						System.out.println("Skipping over work done...");
						skipped=true;
					}
				}

				disjunctions.clear();
			}
		} catch(EOFException e) {
			Log.i(TAG, "quitting thread");
		} catch(IOException e) {
			Log.e(TAG, e);
		} 
	}

	@Override
	public void onExpansion(List<String> pExpansions) {
		mDisjunctions.add(new ArrayList<String>(pExpansions));
	}

	private List<String> concat(List<List<String>> pDisjunctions) {
		List<String> allJoined=new ArrayList<String>();
		for(List<String> disjunction: pDisjunctions) {
			allJoined.add(Rtu.join(disjunction, " "));
			
		}
		return allJoined;
	}
	
	@Override
	public void onGroupDone(int pNumExpansions) {
		try {
			Collections.sort(mDisjunctions, ExpansionComparator.NGRAM_PRIORITISER);
			List<String> joinedDisjunctions=concat(mDisjunctions);
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
		join();
		mThrower.close();
	}
	
}