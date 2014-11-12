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
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Typed;

public class DeferredFileJournal<K,C> extends AbstractFileJournal<K,C,Downloader<C>> {
	private final static String TAG=DeferredFileJournal.class.getName();
	//TIMEOUT is in ms
	private final static int DEFAULT_BASE_TIMEOUT=1000*60;
	private final static int DEFAULT_EXTRA_TIMEOUT=1000*4;
	
	private final Set<Promise<Void,IOException,Void>> mPromised;
	private final ConcreteThrower<IOException> mThrower=new ConcreteThrower<IOException>();
	private final DeferredManager mDeferredMgr=new DefaultDeferredManager();
	@SuppressWarnings("rawtypes")
	private final Promise[] mPendingArr=new Promise[]{};

	private int mBaseTimeout=DEFAULT_BASE_TIMEOUT;
	private int mExtraTimeout=DEFAULT_EXTRA_TIMEOUT;

	public DeferredFileJournal(Path pDirectory,
			Converter<K, String> pFilenameGenerator,
			Downloader<C> pDownloader)
			throws IOException {
		super(pDirectory, pFilenameGenerator, pDownloader,0);
		mPromised=new HashSet<>();
		assert nonePending(mPromised);
		mPromised.clear();
	}

	public DeferredFileJournal<K,C> setTimeout(int pBase, int pExtra) {
		mBaseTimeout=pBase;
		mExtraTimeout=pExtra;
		return this;
	}
	
	@Override
	public void add(final Typed<C> pLink) throws IOException {
		incFilename();
		C source=pLink.get();
		trailAdd(source);
		mPromised.add(mContentGenerator.download(source, dest(pLink)));
	}

	@Override
	public void commitEntry(final K pKey) throws IOException {
		if(mPromised.size()>0) {
			Promise<Void, IOException, Void> commitment=mDeferredMgr.when(mPromised.toArray(mPendingArr)).then(
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
			hold(pKey, commitment);
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

	private int calcTimeout(int pNumTransactionDownloads) {
		return mBaseTimeout+pNumTransactionDownloads*mExtraTimeout;
	}
	
	private void hold(final K pKey, Promise<Void,IOException,Void> pCommitment) {
		//TODO call tidyup
		try {
			int timeout=calcTimeout(mPromised.size());
			pCommitment.waitSafely(timeout);
			boolean pending=false;
			for(Promise<?,?,?> initialPromise:mPromised) {
				if(initialPromise.isPending()) {
					Log.e(TAG, Log.esc("Promise timed out after +"+(timeout/1000)+"s: "+initialPromise.toString()));
					pending=true;
				}
			}
			if(false) {
				Rtu.continuePrompt();				
			}
			if(pending) {
				mContentGenerator.reset();
				mPromised.clear();					
			}
			
			//p.waitSafely();
		} catch(IOException e) {
			mThrower.ctch(e);
		} catch (InterruptedException i) {
			mThrower.ctch(new IOException(i));
		}		
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
