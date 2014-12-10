package com.hourglassapps.persist;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jdeferred.Deferred;
import org.jdeferred.DeferredManager;
import org.jdeferred.DonePipe;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DefaultDeferredManager;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MultipleResults;

import com.hourglassapps.cpi_ii.web_search.DownloadableDeferredObject;
import com.hourglassapps.cpi_ii.web_search.Sourceable;
import com.hourglassapps.persist.DoneStore;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Downloader;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Typed;

public class DeferredFilesJournal<K,C,R extends Sourceable> extends AbstractFilesJournal<K,C,Downloader<C,R>> {
	private final static String TAG=DeferredFilesJournal.class.getName();
	public final static String DONE_INDEX="done_index";
	public final static char TYPE_COLUMN_DELIMITER=' ';
	public final static String TYPE_UNKNOWN="UNKNOWN";
	public final static String TYPE_SYMLINK="SYMLINK";
	public final static String TYPES_FILENAME=CUSTOM_PREFIX+"types.txt";
	//TIMEOUT is in ms
	private final List<Promise<Void,IOException,Void>> mPromised;
	private final ConcreteThrower<IOException> mThrower=new ConcreteThrower<IOException>();
	private final DeferredManager mDeferredMgr=new DefaultDeferredManager();
	@SuppressWarnings("rawtypes")
	private final Promise[] mPendingArr=new Promise[]{};

	private PrintWriter mTypesWriter=null;
	private final Path mPartialDoneDir;
	private final Path mBetweenDoneDir;
	private final DoneStore mDone;
	
	public DeferredFilesJournal(final Path pDirectory,
			Converter<K, String> pFilenameGenerator,
			Downloader<C,R> pDownloader)
			throws IOException {
		super(pDirectory, pFilenameGenerator, pDownloader,0, new PreDeleteAction() {

			@Override
			public void run() throws IOException {
				Path partialDoneDir=partialDir(pDirectory).resolve(DONE_INDEX);
				if(Files.exists(partialDoneDir)) {
					AbstractFilesJournal.deleteFlatDir(partialDoneDir);
				}
			}
			
		});
		mPartialDoneDir=mPartialDir.resolve(DONE_INDEX);
		mBetweenDoneDir=pDirectory.resolve(DONE_INDEX);
		mDone=new DoneStore(mPartialDoneDir);
		mPromised=new ArrayList<>();
	}

	@Override
	public void addNew(final Typed<C> pLink) throws IOException {
		incFilename();
		final C source=pLink.get();
		trailAdd(source);
		int destKey=filename();

		final Path dest=dest(pLink);
		synchronized(this) {
			assert(Files.exists(mPartialDoneDir));
			if(mTypesWriter==null) {
				mTypesWriter=new PrintWriter(new BufferedWriter(new FileWriter(mPartialDir.resolve(TYPES_FILENAME).toString())));
			}

			Ii<String,String> srcDst=new Ii<>(pLink.get().toString(), dest.toString());
			if(mDone.addedAlready(srcDst)) {
				//If we return without adding to mPromised, commit() might not commit the transaction properly
				mTypesWriter.println(destKey+Character.toString(TYPE_COLUMN_DELIMITER)+TYPE_SYMLINK);
				
				Deferred<Void,IOException,Void> deferred=new DeferredObject<Void,IOException,Void>();
				deferred.resolve(null);
				mPromised.add(deferred);
				Log.i(TAG, Log.esc("Already downloaded: "+srcDst));
				return;
			}
		}
		
		/*
		 * Careful now -- be aware that holding a lock on this journal instance (which we don't) while invoking downloadLink could
		 * result in deadlock since downloadLink uses the HttpClient library for async downloads which itself acquires another
		 * lock from time to time, including in the HttpAsyncClient execute method and the completion callback. 
		 * If resolving a promise in one of the completion callbacks invokes code
		 * in this class via a 'then' callback that attempts to acquire a journal lock we could have a problem.
		 * 
		 * This is all hypothetical since we are not holding a journal lock when invoking downloadLink.
		 */
		final Promise<R,IOException,Void> download=mContentGenerator.downloadLink(source, destKey, dest);
		Promise<Void,IOException,Void> logContentType=download.then(
				new DonePipe<R,Void,IOException,Void>() {

					@Override
					public Promise<Void, IOException, Void> pipeDone(R pTypeInfo) {
						Deferred<Void,IOException,Void> def=new DeferredObject<Void,IOException,Void>();
						
						String src=pTypeInfo.src();
						if(src==null) {
							src=TYPE_UNKNOWN;
						}
						synchronized(DeferredFilesJournal.this) {
							mTypesWriter.println(pTypeInfo.dstKey()+Character.toString(TYPE_COLUMN_DELIMITER)+src);
							mDone.addNew(new Ii<>(source.toString(), dest.toString()));
						}
						def.resolve(null);
						return def;
					}
				}

				);
		mPromised.add(destKey-FIRST_FILENAME, logContentType);
	}

	@Override
	protected synchronized void tryTidy(Path pDest) throws IOException {
		super.tryTidy(pDest);
	}

	@Override
	public void commit(final K pKey) throws IOException {
		if(mQueryTid==-1) {
			mQueryTid=Thread.currentThread().getId();
		}
		if(mPromised.size()>0) {
			Promise<Void, IOException, Void> commitment;
			commitment=mDeferredMgr.when(mPromised.toArray(mPendingArr)).then(
					new DonePipe<MultipleResults,Void,IOException,Void>(){

						@Override
						public Promise<Void, IOException, Void> pipeDone(
								MultipleResults result) {
							Deferred<Void,IOException,Void> deferred=new DeferredObject<Void,IOException,Void>();
							Path dest=destDir(pKey);
							try {
								tryTidy(dest);
								deferred.resolve(null);
							} catch(IOException e) {
								deferred.reject(e);
							}
							return deferred;
						}
					}).fail(new FailCallback<IOException>(){

						@Override
						public void onFail(IOException e) {
							/*
							 * e is any IOException thrown during the async download that is not caught
							 * By invoking mThrow.ctch(e) and later mThrower.throwCaught we cause the process
							 * to exit without committing the transaction. This is the desired outcome for any
							 * IOException triggered by a local issue.
							 * 
							 * IOExceptions caused by trouble communicating with a remote server should
							 * be caught and handled in a manner that should not cause the exception to
							 * bubble-up to this method. The desired outcome for these is that the download
							 * is skipped but the rest of the transaction and subsequent downloads should
							 * otherwise proceed as normal.
							 */
							synchronized(DeferredFilesJournal.this) {
								mThrower.ctch(e);
							}
						}});	
			synchronized(this) {
				mThrower.throwCaught(null);
			}
			hold(pKey, commitment);
		} else {
			synchronized(this) {
				try {
					Path dest=destDir(pKey);
					tryTidy(dest);
				} catch(IOException e) {
					mThrower.ctch(e);
				}
			}
		}
		startEntry(); //Note this is invoked even if there's an exception
		mThrower.throwCaught(null);
	}

	@SuppressWarnings("unused")
	private void hold(final K pKey, Promise<Void,IOException,Void> pCommitment) throws IOException {
		try {
			pCommitment.waitSafely();
		} catch (InterruptedException i) {
			synchronized(this) {
				mThrower.ctch(new IOException(i));
			}
		}	
	}
	
	private static <R> boolean nonePending(Collection<Promise<R,IOException,Void>> pPromised) {
		for(Promise<?,?,?> p: pPromised) {
			if(p.isPending()) {
				return false;
			}
		}
		return true;
	}
	
	private volatile long mQueryTid=-1;
	@Override
	protected synchronized void tidyUp(Path pDest) throws IOException {
		if(mTypesWriter!=null) {
			mTypesWriter.close();
			mTypesWriter=null;
		} //else we're committing a transaction with no downloads, so don't worry about it

		assert(mPartialDoneDir.toFile().exists());
		mDone.commit(pDest);
		super.tidyUp(pDest);
		Path src=pDest.resolve(DONE_INDEX);
		Log.v(TAG, "move to parent src ("+Thread.currentThread().getId()+"): "+Log.esc(src)+" dst: "+Log.esc(mBetweenDoneDir));
		Files.move(src, mBetweenDoneDir, StandardCopyOption.ATOMIC_MOVE);
		Log.v(TAG, "move to parent done");
	}

	@Override
	protected void startEntry() throws IOException {
		assert nonePending(mPromised);
		mPromised.clear();
		super.startEntry();
		assert(Thread.currentThread().getId()==mQueryTid);
		Log.v(TAG, "move to partial src ("+Thread.currentThread().getId()+"): "+Log.esc(mBetweenDoneDir)+" dst: "+Log.esc(mPartialDoneDir));
		Files.move(mBetweenDoneDir, mPartialDoneDir, StandardCopyOption.ATOMIC_MOVE);
		Log.v(TAG, "move to partial done");
	}
	
	@Override
	public synchronized void reset() throws IOException {
		mDone.reset();
		super.reset();
	}

	@Override
	public void close() {
	}

}