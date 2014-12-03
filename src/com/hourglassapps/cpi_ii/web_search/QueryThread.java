package com.hourglassapps.cpi_ii.web_search;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import com.hourglassapps.persist.Journal;
import com.hourglassapps.threading.Consumer;
import com.hourglassapps.util.AsyncExpansionReceiver;
import com.hourglassapps.util.Closer;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.ExpansionReceiver;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Throttle;
import com.hourglassapps.util.Typed;

public class QueryThread<K> extends Thread implements AsyncExpansionReceiver<String,K>, Consumer<List<List<String>>> {
	private final static String TAG=QueryThread.class.getName();
	private static int NAME=0;
	private final static List<List<String>> TERMINAL_RECORD=Collections.<List<String>>emptyList();
	private final List<List<String>> mDisjunctions=new ArrayList<List<String>>();
	private final Deque<List<List<String>>> mInbox=new ArrayDeque<>();
	private final ConcreteThrower<Exception> mThrower=new ConcreteThrower<>();
	private final SearchEngine<List<String>, K, URL, URL> mQ;	
	private final Journal<K,Typed<URL>> mJournal;
	private Throttle mThrottle=Throttle.NULL_THROTTLE;
	private final Deferred<Void, IOException, K> mDeferred=new DeferredObject<>();
	private Closer mCloser=new Closer();
	
	public QueryThread(int pNumThreads, SearchEngine<List<String>,K,URL,URL> pQ, Journal<K,Typed<URL>> pJournal) throws IOException {
		super("query "+NAME++);
		mQ=pQ;
		mJournal=pJournal;
		mCloser.after(mJournal).after(mQ).after(mThrower);
	}
	
	public QueryThread<K> setThrottle(Throttle pThrottle) {
		if(getState()==Thread.State.NEW) {
			mThrottle=pThrottle;
		} else {
			throw new IllegalStateException("Can only change throttle setting prior to starting thread");
		}
		return this;
	}

	public void search(Query<K,URL> pQuery) throws IOException {
		Iterator<URL> links=mQ.present(pQuery);
		Typed<URL> source;
		while(links.hasNext()){
			mThrottle.choke();
			final URL link=links.next();
			source=new TypedLink(link);
			mJournal.addNew(source);
		}
		//Log.i(TAG, "committing: "+pQuery.uniqueName()+" tid: "+Thread.currentThread().getId());
		mJournal.commit(pQuery.uniqueName());		
	}
	
	private synchronized List<List<String>> unshift() {
		List<List<String>> disjunctions=null;
		while((disjunctions=mInbox.pollFirst())==null) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		return disjunctions; 
	}
	
	@Override
	public synchronized void push(List<List<String>> pDisjunctions) {
		mInbox.addLast(new ArrayList<>(pDisjunctions));
		notify();
	}
	
	@Override
	public void run() {
		try {
			boolean skipped=false;
			List<List<String>> booleanQuery=unshift();
			while(runnable(booleanQuery)) {
				List<String> disjunctions=concat(booleanQuery);
				//Log.i(TAG, "query tid: "+Thread.currentThread().getId()+" "+disjunctions.toString());
				Query<K,URL> query=mQ.formulate(disjunctions);
				K name=query.uniqueName();
				if(!mJournal.addedAlready(name)) {
					skipped=false;
					search(query);
				} else {
					//Log.i(TAG, "skipping: "+name+" tid: "+Thread.currentThread().getId());
					if(!skipped) {
						System.out.println("Skipping over work done...");
						skipped=true;
					}
				}
				mDeferred.notify(name);
				booleanQuery=unshift();
			}
		} catch(Throwable e) {
			Log.e(TAG, e);
		} finally {
			Log.i(TAG, "Quitting query thread: "+Thread.currentThread().getId());
		}
	}

	private synchronized boolean runnable(List<List<String>> pQuery) {
		return pQuery.size()>0;
	}
	
	private synchronized void forceQuit() {
		mInbox.addFirst(TERMINAL_RECORD);
		notify();
	}

	private synchronized void quitWhenFinished() {
		push(TERMINAL_RECORD);
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
			if(mThrower.fallThrough()) {
				return;
			}
			push(mDisjunctions);
		} finally {
			mDisjunctions.clear();
		}
	}

	@Override
	public void close() throws Exception {
		try {
			try {
				quitWhenFinished();
				join();
			} finally {
				try {
					mCloser.close();
				} finally {
					mJournal.close();
					mDeferred.resolve(null);
				}
			}
		} catch(Exception e) {
			mDeferred.reject(null);
			throw e;
		}
	}

	@Override
	public Promise<Void, IOException, K> promise() {
		return mDeferred;
	}
	
}