package com.hourglassapps.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

/***
 * <p>Times the duration between creation of Clocks (via the time method) and closing them.
 * This duration is accumulated any time a new clock is created with the same name.
 * Time measured for Clocks returned by a StopWatch.time() method is subtracted from time measured by the
 * parent Clock when reporting, hence the ExclusiveTimeKeeper name.</p>
 * 
 * <p>Durations are logged when the Clock is closed.</p>
 * 
 * @author kieran
 *
 */
public class ExclusiveTimeKeeper implements Clock, AutoCloseable {
	private final static String TAG=ExclusiveTimeKeeper.class.getName();
	private final static String ARROW="->";
	private final Map<String,StopWatch> mLabelToWatches=new HashMap<>();
	private final Map<String,Long> mLabelToAccumulatedInterval=new HashMap<>();
	
	public ExclusiveTimeKeeper() {
	}
	
	private Ii<Long,String> report(String pParentLabel, Map<String,StopWatch> pChildWatches) {
		StringBuilder report=new StringBuilder();
		List<StopWatch> watches=new ArrayList<>();
		for(StopWatch w: pChildWatches.values()) {
			watches.add(w);
		}
		Collections.sort(watches);
		Collections.reverse(watches);
		
		long totalInnerInterval=0;
		for(StopWatch w: watches) {
			long interval=w.totalInterval();
			report.append(w.toString(pParentLabel, interval));
			totalInnerInterval+=interval;
		}
		
		return new Ii<Long,String>(totalInnerInterval, report.toString());
	}
	
	public String toString() {
		return report(null, mLabelToWatches).snd();
	}
	
	private StopWatch createWatch(Map<String,StopWatch> pLabelToWatches, 
				final Map<String,Long> pLabelToAccumulatedInterval, 
				final String pLabel) {
		if(!pLabelToAccumulatedInterval.containsKey(pLabel)) {
			pLabelToAccumulatedInterval.put(pLabel, 0l);
		}

		StopWatch w;
		long accumulated=pLabelToAccumulatedInterval.get(pLabel);
		if(pLabelToWatches.containsKey(pLabel)) {
			w=pLabelToWatches.get(pLabel);
			if(!w.mClosed) {
				throw new IllegalStateException("two stopwatches with same label ("+pLabel+") running simultaneously");
			}
			w.reset(accumulated);
		} else {
			w=new StopWatch(pLabel, accumulated);
			pLabelToWatches.put(pLabel, w);
		}
		w.promise().then(new DoneCallback<Long>(){

			@Override
			public void onDone(Long pInterval) {
				pLabelToAccumulatedInterval.put(pLabel, pLabelToAccumulatedInterval.get(pLabel)+pInterval);
			}
			
		});
		return w;		
	}
	
	/**
	 * Returns a clock that times the duration between its return and its subsequent closing.
	 */
	@Override
	public StopWatch time(String pLabel) {
		StopWatch w=createWatch(mLabelToWatches, mLabelToAccumulatedInterval, pLabel);
		return w;
	}
	
	private class StopWatch implements AutoCloseable, Comparable<StopWatch>, Promiser<Long,Void,Void>, Clock {
		private final Map<String,StopWatch> mLabelToWatches=new HashMap<>();
		private final Map<String,Long> mLabelToAccumulatedInterval=new HashMap<>();

		private final String mLabel;
		private long mStart;
		private boolean mClosed;
		private Deferred<Long,Void,Void> mDeferred;
		private long mAccumulatedInterval;
		
		private StopWatch(String pLabel, long pAccumulatedInterval) {
			mLabel=pLabel;
			reset(pAccumulatedInterval);
		}
		
		private void reset(long pAccumulatedInterval) {
			mClosed=false;
			mDeferred=new DeferredObject<>();
			mStart=System.nanoTime();
			mAccumulatedInterval=pAccumulatedInterval;			
		}
		
		public String toString() {
			return toString(null,totalInterval());
		}
		
		public String toString(String pLabPrefix, long pTotalInterval) {
			StringBuilder report=new StringBuilder();
			String prefix;
			if(pLabPrefix!=null) {
				prefix=pLabPrefix+ARROW+mLabel;
			} else {
				prefix=mLabel;
			}
			Ii<Long,String> childIntervalReport=report(prefix, mLabelToWatches);
			return report.append(prefix).append(": ").
					append((pTotalInterval-childIntervalReport.fst())/1000000).append("ms").
					append("\n").append(childIntervalReport.snd()).toString();
		}
		
		private long curInterval() {
			return System.nanoTime()-mStart;
		}
		
		private long totalInterval() {
			long total=mAccumulatedInterval;
			if(!mClosed) {
				total+=curInterval();
			}
			return total;
		}
		
		/**
		 * Any time spent between the returning of this new StopWatch and the closing of it later 
		 * will be subtracted from the time reported for 'this'. A separate entry in the report
		 * will created for the returned StopWatch instances.
		 */
		@Override
		public StopWatch time(String pLabel) {
			StopWatch w=createWatch(mLabelToWatches, mLabelToAccumulatedInterval, pLabel);
			return w;
		}
		
		@Override
		public void close() {
			if(!mClosed) {
				mClosed=true;
				long interval=curInterval();
				mAccumulatedInterval+=interval;
				mDeferred.resolve(interval);
			}
		}

		@Override
		public int compareTo(StopWatch pOther) {
			return Long.compare(mAccumulatedInterval, pOther.mAccumulatedInterval);
		}

		@Override
		public Promise<Long, Void, Void> promise() {
			return mDeferred;
		}
		
	}

	@Override
	public void close() {
		for(StopWatch w: mLabelToWatches.values()) {
			w.close();
		}
		Log.i(TAG, "Times: \n"+toString());
	}
}
