package com.hourglassapps.cpi_ii.web_search;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.ZeroCopyConsumer;
import org.jdeferred.Deferred;

import com.hourglassapps.util.Log;

public class DeferredZeroCopyConsumer extends ZeroCopyConsumer<File> {
    private final static String TAG=DeferredZeroCopyConsumer.class.getName();
    private final Deferred<ContentTypeSourceable,?,?> mDeferred;
    private int mDestKey;
    private final long mStartTime_ns;
    private final long mTimeToDie_ns;
    private boolean mAborted=false;

    private final URL mSrc;
    private final File mDest;
    private boolean mContentReceived=false;
    private long mMaxLen=0;
    private boolean mTooLarge=false;
    
    /**
     * 
     * @param pDestKey
     * @param pDest
     * @param pDeferred
     * @param pTimeout in ms
     * @throws FileNotFoundException
     */
    public DeferredZeroCopyConsumer(URL pSrc, int pDestKey, File pDest, Deferred<ContentTypeSourceable,?,?> pDeferred, long pTimeout_ms, long pMaxLen_bytes) 
	throws FileNotFoundException {
	super(pDest);
	mSrc=pSrc;
	mDest=pDest;
	mDestKey=pDestKey;
	mDeferred=pDeferred;
	mStartTime_ns=System.nanoTime();
	mTimeToDie_ns=mStartTime_ns+pTimeout_ms*1000000;
	mMaxLen=pMaxLen_bytes;
    }

    @Override
    protected void onResponseReceived(HttpResponse pResponse) {
	Log.i(TAG, "onResponseReceived. for "+Log.esc(mSrc));
	super.onResponseReceived(pResponse);
	HttpEntity entity=pResponse.getEntity();

	//note a server doesn't have to provide a content length header. In such cases -1 will be returned by getContentLength()
	long length=entity.getContentLength();
	if(length>mMaxLen) {
	    mTooLarge=true;
	}
    }
    
    @Override
    protected File process(HttpResponse response, File file,
			   ContentType contentType) {
	try {
	    assert !mAborted;
	    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
		//Event if this happens don't reject deferred as that would abort entire query
		Log.e(TAG, Log.esc("Download failed: "+mDeferred+" http reponse: "+response.getStatusLine().getStatusCode()+" for "+mSrc));
	    } else {
		Log.i(TAG, Log.esc("Resolved: "+Log.esc(mDeferred.toString()+" for "+mSrc)));
		//System.out.print('.');
	    }
	    mDeferred.resolve(result(response));
	} catch(Exception e) {
	    Log.e(TAG, e);
	    mDeferred.resolve(new ContentTypeSourceable(mDestKey, null));
	}
	return file;
    }

    @Override
    protected void onContentReceived(
				     final ContentDecoder pDecoder, final IOControl pIoCtrl) throws IOException {
	if(!mContentReceived) {
	    Log.i(TAG, "onContentReceived: "+mDest.toString()+" for "+Log.esc(mSrc));
	    mContentReceived=true;
	}
    	super.onContentReceived(pDecoder, pIoCtrl);
    	if(!pDecoder.isCompleted() && (System.nanoTime()>mTimeToDie_ns || mTooLarge)) {
	    Log.e(TAG, Log.esc("Timeout or too large: "+mDeferred));
	    pIoCtrl.shutdown();
	    mDeferred.resolve(new ContentTypeSourceable(mDestKey, null));
	    mAborted=true;
    	}
    }
    
    private ContentTypeSourceable result(HttpResponse pResponse) {
	Log.i(TAG, "result. for "+Log.esc(mSrc));
	Header contentType=pResponse.getFirstHeader("Content-Type");
	if(contentType!=null) {
	    return new ContentTypeSourceable(mDestKey, contentType.getValue());
	}
	return new ContentTypeSourceable(mDestKey, null);
    }
	
    @Override
    protected void releaseResources() {
	Log.i(TAG, "releaseResources. for "+Log.esc(mSrc));
	// Needed for Unknown Host errors
	// Connection Closed Exception
	// Connection Reset by Peer 
	//
	super.releaseResources();
	if(mDeferred.isPending()) {
	    mAborted=true;
	    mDeferred.resolve(new ContentTypeSourceable(mDestKey, null));
	    Log.e(TAG, Log.esc("Error connecting (possible SSL/TLS problem) for: "+mDeferred)+"started at "+(mStartTime_ns/1000000)+" aborted at: "+(System.nanoTime()/1000000));
	}
    }
}
