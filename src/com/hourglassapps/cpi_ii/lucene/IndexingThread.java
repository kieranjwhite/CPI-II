package com.hourglassapps.cpi_ii.lucene;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;

import com.hourglassapps.cpi_ii.latin.StandardLatinAnalyzer;
import com.hourglassapps.cpi_ii.web_search.Query;
import com.hourglassapps.cpi_ii.web_search.QueryRecord;
import com.hourglassapps.persist.Journal;
import com.hourglassapps.threading.Consumer;
import com.hourglassapps.threading.FilterTemplate;
import com.hourglassapps.threading.RandomTemplate;
import com.hourglassapps.threading.SkipTemplate;
import com.hourglassapps.util.Closer;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;

public class IndexingThread extends Thread implements Consumer<QueryRecord<String>>, AutoCloseable {
	private final static String TAG=IndexingThread.class.getName();
	private final static int NUM_SKIPS_BEFORE_COMMIT=40; //a commit is slow so we don't do it for each query
	private final static long LOG_SIZE_INTERVAL=600*1000; //10 mins
	private final static QueryRecord<String> TERMINAL_RECORD=new QueryRecord<String>(0,null,null);
	private final Deque<QueryRecord<String>> mInbox=new ArrayDeque<>();
	private final Closer mCloser=new Closer();
	private final List<Journal<String,Path>> mJournals=new ArrayList<>();
	private final FilterTemplate<String> mCommitDecider;
	private final int mNumFeederThreads;
	private final List<Ii<Boolean,String>> mCommittableKey;
	private long mLastLogTime=0;
	
	public IndexingThread(Path pIndexDir, int pNumFeederThreads) throws Exception {
		super("indexer");
		
		mNumFeederThreads=pNumFeederThreads;
		mCommittableKey=new ArrayList<>(pNumFeederThreads);
		Ii<Boolean,String> committableKeyInit=new Ii<>(Boolean.FALSE, null);
		for(int t=0; t<pNumFeederThreads; t++) {
			mCommittableKey.add(committableKeyInit);
		}
		
		mCommitDecider=new SkipTemplate<String>(pNumFeederThreads, NUM_SKIPS_BEFORE_COMMIT);
		try {
			Analyzer analyser=StandardLatinAnalyzer.searchAnalyzer();
			mCloser.after(analyser);
			final Indexer indexer=new Indexer(pIndexDir, analyser, false);
			mCloser.after(indexer);

			for(int i=0; i<pNumFeederThreads; i++) {
				Journal<String,Path> journal=new DownloadedIndexJournal(indexer, i);
				mCloser.after(journal);
				mJournals.add(journal);
			}

			Thread closer=new Thread() {

				@Override
				public void run() {
					try {
						forceQuit();
						IndexingThread.this.join();
						synchronized(mCloser) {
							mCloser.close();
						}
					} catch (Exception e) {
						Log.e(TAG,e);
					}
				}

			};
			Runtime.getRuntime().addShutdownHook(closer);

		} catch(IOException|RuntimeException e) {
			synchronized(mCloser) {
				mCloser.close();
			}
			throw e;
		}
	}
	
	@Override
	public void run() {
		try {
			QueryRecord<String> record=unshift();			
			while(runnable(record)) {
				int tid=record.tid();
				String key=record.key();
				Path dir=record.dir();
				Journal<String,Path> journal=mJournals.get(tid);
				Ii<Boolean,String> lastCommittableKey=mCommittableKey.get(tid);
				boolean timeToCheck=mCommitDecider.convert(key).accept(tid, mNumFeederThreads);
				if(timeToCheck) {
					if(lastCommittableKey.snd()!=null) {
						//previous transaction is committed at the start of a new transaction
						journal.commit(lastCommittableKey.snd());
					}
  					mCommittableKey.set(tid, new Ii<Boolean,String>(!journal.addedAlready(key), key));
				}
				if(mCommittableKey.get(tid).fst()) {
					journal.addNew(dir);
				}
				record=unshift();
			}
		} catch(Throwable e) {
			Log.e(TAG, e);
		} finally {
			Log.i(TAG, "Quitting indexing thread: "+Thread.currentThread().getId());
		}

	}
	
	private synchronized QueryRecord<String> unshift() {
		QueryRecord<String> record;
		while((record=mInbox.pollFirst())==null) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		long time=System.currentTimeMillis();
		if(time-mLastLogTime>LOG_SIZE_INTERVAL) {
			Log.i(TAG, "indexing inbox size: "+mInbox.size());
			mLastLogTime=time;
		}
		return record; 
	}
	
	@Override
	public synchronized void push(QueryRecord<String> pDatum) {
		mInbox.addLast(pDatum);
		notify();
	}

	private synchronized boolean runnable(QueryRecord<?> pRec) {
		return pRec!=TERMINAL_RECORD;
	}
	
	private synchronized void forceQuit() {
		mInbox.addFirst(TERMINAL_RECORD);
		notify();
	}
	
	private void quitWhenFinished() throws InterruptedException, IOException {
		push(TERMINAL_RECORD);
		join();
	}

	private synchronized void commitLast() throws IOException {
		for(int tid=0; tid<mNumFeederThreads; tid++) {
			Journal<String,Path> journal=mJournals.get(tid);
			Ii<Boolean,String> lastCommittableKey=mCommittableKey.get(tid);
			if(lastCommittableKey.snd()!=null) {
				journal.commit(lastCommittableKey.snd());
			}
		}		
	}
	
	@Override
	public void close() throws Exception {
		quitWhenFinished();
		synchronized(mCloser) {
			if(mCloser.closed()) {
				return;
			}
			commitLast();
			mCloser.close();
		}
	}
}
