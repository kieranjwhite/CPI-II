package com.hourglassapps.cpi_ii.web_search;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.http.Header;
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
	private final long mTimeToDie;
	private boolean mAborted=false;
	
	/**
	 * 
	 * @param pDestKey
	 * @param pDest
	 * @param pDeferred
	 * @param pTimeout in ms
	 * @throws FileNotFoundException
	 */
	public DeferredZeroCopyConsumer(int pDestKey, File pDest, Deferred<ContentTypeSourceable,?,?> pDeferred, long pTimeout) 
			throws FileNotFoundException {
		super(pDest);
		mDestKey=pDestKey;
		mDeferred=pDeferred;
		mTimeToDie=System.currentTimeMillis()+pTimeout;
	}
	
	@Override
	protected File process(HttpResponse response, File file,
			ContentType contentType) {
		try {
			assert !mAborted;
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				//Event if this happens don't reject deferred as that would abort entire query
				Log.e(TAG, Log.esc("Download failed: "+mDeferred+" http reponse: "+response.getStatusLine().getStatusCode()));
			} else {
				Log.i(TAG, Log.esc("Resolved: "+mDeferred));
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
    	super.onContentReceived(pDecoder, pIoCtrl);
    	if(!pDecoder.isCompleted() && System.currentTimeMillis()>mTimeToDie) {
    		Log.e(TAG, Log.esc("Timeout: "+mDeferred));
    		pIoCtrl.shutdown();
    		mDeferred.resolve(new ContentTypeSourceable(mDestKey, null));
    		mAborted=true;
    	}
    }
    
	private ContentTypeSourceable result(HttpResponse pResponse) {
		Header contentType=pResponse.getFirstHeader("Content-Type");
		if(contentType!=null) {
			return new ContentTypeSourceable(mDestKey, contentType.getValue());
		}
		return new ContentTypeSourceable(mDestKey, null);
	}
	
	@Override
	protected void releaseResources() {
		// Needed for Unknown Host errors
		// Connection Closed Exception
		// Connection Reset by Peer 
		//
		super.releaseResources();
		if(mDeferred.isPending()) {
			mAborted=true;
			mDeferred.resolve(new ContentTypeSourceable(mDestKey, null));
			Log.e(TAG, Log.esc("Unknown error for: "+mDeferred));
		}
	}
}
