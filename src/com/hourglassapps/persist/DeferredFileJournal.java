package com.hourglassapps.persist;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

import org.jdeferred.Deferred;
import org.jdeferred.DeferredManager;
import org.jdeferred.DoneCallback;
import org.jdeferred.DonePipe;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DefaultDeferredManager;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MultipleResults;

import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Downloader;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Typed;

public class DeferredFileJournal<K,C> extends AbstractFileJournal<K,C,Downloader<C>> {
	private final static String TAG=DeferredFileJournal.class.getName(); 
	
	private final Set<Promise<Void,IOException,Void>> mPromised;
	private final ConcreteThrower<IOException> mThrower=new ConcreteThrower<IOException>();
	private final DeferredManager mDeferredMgr=new DefaultDeferredManager();
	@SuppressWarnings("rawtypes")
	private final Promise[] mPendingArr=new Promise[]{};

	public DeferredFileJournal(Path pDirectory,
			Converter<K, String> pFilenameGenerator,
			Downloader<C> pDownloader)
			throws IOException {
		super(pDirectory, pFilenameGenerator, pDownloader,0);
		mPromised=new HashSet<>();
		assert nonePending(mPromised);
		mPromised.clear();
	}

	@Override
	public void add(final Typed<C> pLink) throws IOException {
		incFilename();
		C source=pLink.get();
		trailAdd(source);
		mPromised.add(mContentGenerator.download(source, dest(pLink)));
	}

	/*
} else {
	try {
		Path dest=dest(pLink);
		try(PrintWriter out=new PrintWriter(new BufferedWriter(new FileWriter(dest.toFile())))) {
			out.print(pContent);
		}
		deferred.resolve(null);
	} catch(IOException e) {
		deferred.reject(e);
	}
}
*/
	@Override
	public void commitEntry(final K pKey) throws IOException {
		if(mPromised.size()>0) {
			Promise<Void, IOException, Void> p=mDeferredMgr.when(mPromised.toArray(mPendingArr)).then(
					new DonePipe<MultipleResults,Void,IOException,Void>(){

						@Override
						public Promise<Void, IOException, Void> pipeDone(
								MultipleResults result) {
							Deferred<Void,IOException,Void> deferred=new DeferredObject<Void,IOException,Void>();
							Path dest=destDir(pKey);
							try {
								tidyUp(dest);
								deferred.resolve(null);
							} catch(IOException e) {
								deferred.reject(e);
							}
							return deferred;
						}
					}).fail(new FailCallback<IOException>(){

						@Override
						public void onFail(IOException e) {
							mThrower.ctch(e);
						}});	
			try {
				
				p.waitSafely(1000*180);
				for(Promise<?,?,?> initialPromise:mPromised) {
					if(initialPromise.isPending()) {
						Log.e(TAG, Log.esc("Promise timed out: "+initialPromise.toString()));
						System.exit(-1);
					}
				}
				
				//p.waitSafely();
			} catch (InterruptedException i) {
				mThrower.ctch(new IOException(i));
			}
		} else {
			try {
				Path dest=destDir(pKey);
				tidyUp(dest);
			} catch(IOException e) {
				mThrower.ctch(e);
			}
		}
		startEntry(); //Note this is invoked even if there's an exception
		mThrower.throwCaught(null);
	}

	private static boolean nonePending(Set<Promise<Void,IOException,Void>> pPromised) {
		for(Promise<?,?,?> p: pPromised) {
			if(p.isPending()) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void startEntry() throws IOException {
		assert nonePending(mPromised);
		mPromised.clear();
		super.startEntry();
	}

}
