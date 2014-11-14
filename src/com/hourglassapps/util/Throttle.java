package com.hourglassapps.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.hourglassapps.cpi_ii.web_search.QueryThread;

public class Throttle {
	private final static String TAG=Throttle.class.getName();
	public final static Throttle NULL_THROTTLE=new Throttle();
		
	private final StampDelayed mDurationTemplate;
	private final int mMaxElements;
	private final BlockingQueue<Delayed> mInFlight;

	private static class StampDelayed implements Delayed {
		private long mDurationMs;
		private final long mDelayTill;
		
		public StampDelayed(long pDuration, TimeUnit pUnit) {
			mDurationMs=pUnit.toMillis(pDuration);
			mDelayTill=System.currentTimeMillis()+mDurationMs;
		}

		private long msRemaining() {
			return mDelayTill-System.currentTimeMillis();
		}
		
		@Override
		public long getDelay(TimeUnit pRequiredUnit) {
			return pRequiredUnit.convert(msRemaining(), TimeUnit.MILLISECONDS);
		}
		
		public Delayed copy() {
			return new StampDelayed(mDurationMs, TimeUnit.MILLISECONDS);
		}

		@Override
		public int compareTo(Delayed pOther) {
			if(pOther instanceof StampDelayed) {
				return Long.compare(mDelayTill, ((StampDelayed)pOther).mDelayTill);
			}
			return Long.compare(msRemaining(), pOther.getDelay(TimeUnit.MILLISECONDS));
		}
	}
	
	private Throttle() {
		mDurationTemplate=null;
		mInFlight=null;
		mMaxElements=0;
	}
	
	/**
	 * A call to choke of this instance blocks until the earliest prior invocation of <code>choke</code> 
	 * of the last <code>pMaxElements</code> invocations is at least <code>pDuration pUnit</code>s old. 
	 */
	public Throttle(int pMaxElements, long pDuration, TimeUnit pUnit) {
		mMaxElements=Math.max(pMaxElements,1);
		mInFlight=new DelayQueue<Delayed>();
		mDurationTemplate=new StampDelayed(pDuration, pUnit);
	}
	
	/**
	 * May block in order to throttle calling thread.
	 */
	public void choke() {
		if(mDurationTemplate!=null) {
			assert(mInFlight!=null);
			try {
				if(mInFlight.size()>=mMaxElements) {
					mInFlight.take();
				}
				mInFlight.add(mDurationTemplate.copy());
			} catch(InterruptedException e) {
				//Okay let this download proceed
			}			
		}		
	}
	
	
}