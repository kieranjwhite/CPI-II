package com.hourglassapps.cpi_ii.web_search;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.client.methods.ZeroCopyConsumer;
import org.jdeferred.Deferred;

import com.hourglassapps.util.Log;

public class DeferredZeroCopyConsumer extends ZeroCopyConsumer<File> {
	private final static String TAG=DeferredZeroCopyConsumer.class.getName();
	private final Deferred<ContentTypeSourceable,?,?> mDeferred;
	private long mDestKey;
	
	public DeferredZeroCopyConsumer(long pDestKey, File pDest, Deferred<ContentTypeSourceable,?,?> pDeferred) throws FileNotFoundException {
		super(pDest);
		mDestKey=pDestKey;
		mDeferred=pDeferred;
	}
	
	@Override
	protected File process(HttpResponse response, File file,
			ContentType contentType) {
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			//Event if this happens don't reject deferred as that would abort entire query
			Log.e(TAG, Log.esc("Download failed: "+mDeferred+" http reponse: "+response.getStatusLine().getStatusCode()));
		} else {
			Log.i(TAG, Log.esc("Resolved: "+mDeferred));						
		}
		mDeferred.resolve(result(response));
		return file;
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
			mDeferred.resolve(null);
			Log.e(TAG, Log.esc("Unknown error for: "+mDeferred));
		}
	}
}
