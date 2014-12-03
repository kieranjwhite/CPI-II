package com.hourglassapps.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.jdeferred.Deferred;
import org.jdeferred.ProgressCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

public class ExpansionDistributor<T extends Comparable<? super T>,O> implements ExpansionReceiver<T>, AutoCloseable {
	private final static String TAG=ExpansionReceiver.class.getName();
	private final List<AsyncExpansionReceiver<T,O>> mReceivers;
	private final List<Filter<List<List<T>>>> mFilters;
	private List<List<T>> mAllExpansions=new ArrayList<>();
	private List<List<List<T>>> mAllQueryExpansions=new ArrayList<>();
	private Deferred<Void,IOException,Ii<Integer,O>> mDeferred=new DeferredObject<>();
	private final Comparator<List<T>> mComparator;
	private final Closer mCloser=new Closer();
	
	private final Comparator<List<List<T>>> mQueryComparator=new Comparator<List<List<T>>>() {

		@Override
		public int compare(List<List<T>> arg0, List<List<T>> arg1) {
			return DISJUNCTION_COMPARATOR.compare(arg0.get(0), arg1.get(0));
		}
		
	};
	
	private final Comparator<List<T>> DISJUNCTION_COMPARATOR=new Comparator<List<T>>() {

		@Override
		public int compare(List<T> arg0, List<T> arg1) {
			assert(arg0.size()==arg1.size());
			for(int i=0; i<arg0.size(); i++) {
				T zeroth=arg0.get(i), first=arg1.get(i);
				if(!Rtu.safeEq(zeroth,first)) {
					return zeroth.compareTo(first);
				}
			}
			return 0;
		}
		
	};
	
	public ExpansionDistributor(
			List<Ii<AsyncExpansionReceiver<T,O>, Filter<List<List<T>>>>> pReceiverFilters,
			Comparator<List<T>> pComparator) {
		Ii<List<AsyncExpansionReceiver<T,O>>,List<Filter<List<List<T>>>>> receiversFilters=
				Ii.unzip(pReceiverFilters);
		mReceivers=receiversFilters.fst();
		mFilters=receiversFilters.snd();
		mComparator=pComparator;
		
		for(AsyncExpansionReceiver<T,O> rec: mReceivers) {
			mCloser.after(rec);
		}
		try {
			int tid=0;
			for(AsyncExpansionReceiver<T,O> rec: mReceivers) {

				Promise<Void,IOException,O> def=rec.promise();
				def.progress(progress(tid));
				tid++;
			} 
		} catch(RuntimeException e) {
			try {
				mCloser.close();
			} catch(Exception e1) {
				e.addSuppressed(e1);
			}
			throw e;
		}
		System.out.println("Generating queries (this will take about 5 mins)...");
	}

	public static <T extends Comparable<? super T>,O,R extends AsyncExpansionReceiver<T,O>> ExpansionDistributor<T,O> relay(R pReceiver, 
			Filter<List<List<T>>> pFilter, Comparator<List<T>> pComparator) {
		return new ExpansionDistributor<T,O>(
				Collections.singletonList(new Ii<AsyncExpansionReceiver<T,O>, Filter<List<List<T>>>>(pReceiver, pFilter)), 
				pComparator);
	}
	
	public Promise<Void,IOException,Ii<Integer,O>> promise() {
		return mDeferred;
	}
	
	@Override
	public void onExpansion(List<T> pExpansions) {
		mAllExpansions.add(new ArrayList<T>(pExpansions));
	}

	@Override
	public void onGroupDone(int pNumExpansions) {
		Collections.sort(mAllExpansions, mComparator);
		mAllQueryExpansions.add(mAllExpansions);
		mAllExpansions=new ArrayList<>();
	}

	public void act() {

		Collections.sort(mAllQueryExpansions, mQueryComparator);
		System.out.println("Retrieving documents...");
		if(mFilters==null) {
			for(List<List<T>> query: mAllQueryExpansions) {
				int i=0;
				List<List<T>> unmodifiableExpansions=Collections.unmodifiableList(query);
				assert unmodifiableExpansions.size()>0;
				for(AsyncExpansionReceiver<T,O> r: mReceivers) {
					for(List<T> expansions: unmodifiableExpansions) {
						r.onExpansion(expansions);
					}
				}
				mReceivers.get(i).onGroupDone(query.size());
			}
		} else {
			for(List<List<T>> query: mAllQueryExpansions) {
				int i=0;
				List<List<T>> unmodifiableExpansions=Collections.unmodifiableList(query);
				assert unmodifiableExpansions.size()>0;
				boolean routed=false;
				for(AsyncExpansionReceiver<T,O> r: mReceivers) {
					if(mFilters.get(i).accept(unmodifiableExpansions)) {
						routed=true;
						for(List<T> expansions: unmodifiableExpansions) {
							r.onExpansion(expansions);
						}
						r.onGroupDone(query.size());
					}
					i++;
				}
				//assert routed;
			}
		}
		mAllQueryExpansions.clear();
	}
	
	private ProgressCallback<O> progress(final int pTid) {
		return new ProgressCallback<O>(){

			@Override
			public void onProgress(O progress) {
				mDeferred.notify(new Ii<Integer,O>(pTid, progress));
			}
			
		};
	}
	
	@Override
	public void close() throws Exception {
		act();
		try {
			mCloser.close();
			mDeferred.resolve(null);
		} catch(Exception e) {
			mDeferred.reject(null);
			throw e;
		}
	}
}