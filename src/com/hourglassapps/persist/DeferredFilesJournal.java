package com.hourglassapps.persist;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
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
import com.hourglassapps.util.IdentityConverter;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Typed;

public class DeferredFilesJournal<K,C,R extends Sourceable> extends AbstractFilesJournal<K,C> {
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
	private boolean mResumingAfterInterruption=false;
	private PrintWriter mTypesWriter=null;
	//private final Path mPartialDoneDir;
	//private final Path mBetweenDoneDir;
	private final Path mDoneDir;
	private final DoneStore mDone;
	private K mLastAdded=null;
	private final Downloader<C,R> mContentGenerator;
	
	public DeferredFilesJournal(final Path pDirectory,
			Converter<K, String> pFilenameGenerator, Converter<C,Typed<C>> pToTyped,
			Downloader<C,R> pDownloader)
			throws IOException {
		super(pDirectory, pFilenameGenerator, pToTyped, 0);
		mContentGenerator=pDownloader;
		mDoneDir=pDirectory.resolve(DONE_INDEX);
		mDone=new DoneStore(mDoneDir);
		mPromised=new ArrayList<>();
	}

	@Override
	public boolean addedAlready(K pKey) throws IOException {
		boolean added=super.addedAlready(pKey);
		if(added) {
			mResumingAfterInterruption=true;
		} else {
			tryCommitLastDone(mLastAdded);			
		}
		mLastAdded=pKey;
		return added;
	}

	private synchronized void commitLastDone(K pLastAdded) throws IOException {
		/* This method must be idempotent as a crash in the wrong place could result in it being 
		 * invoked more than once for the same pLastAdded.
		 */
		Path dest=destDir(pLastAdded);
		mDone.commit(dest);
	}
	
	private List<Ii<String,Path>> checkWhatsDone(K pKey) throws IOException {
		Path dir=destDir(pKey);
		IndexedVisitor visitor=new IndexedVisitor();
		Files.walkFileTree(dir, EnumSet.noneOf(FileVisitOption.class), 1, visitor);
		Trail<String> trail=new Trail<>(dir.resolve(AbstractFilesJournal.META_PREFIX+dir.getFileName().toString()), new IdentityConverter<String>());
		List<Ii<String,Path>> srcDsts=new ArrayList<>();
		for(Path p: visitor.dsts()) {
			srcDsts.add(new Ii<String,Path>(trail.url(p), p));
		}
		return srcDsts;
	}
	
	private synchronized void tryCommitLastDone(K pLastAdded) throws IOException {
		if(mResumingAfterInterruption) {
			assert mLastAdded!=null;
			//assert mDone.mPending.size()==0 
			List<Ii<String,Path>> srcDsts=checkWhatsDone(mLastAdded);
			for(Ii<String,Path> srcDst: srcDsts) {
				Ii<String,String> srcDstStr=new Ii<>(srcDst.fst(), srcDst.snd().toString());
				mDone.addNew(srcDstStr);
			}
			
			mResumingAfterInterruption=false;
		}
		if(mLastAdded!=null) {
			commitLastDone(mLastAdded);
		}

	}
	
	@Override
	protected void addNew(final Typed<C> pLink) throws IOException {
		incFilename();
		final C source=pLink.get();
		mTrail.add(source);
		int destKey=filename();

		final Path dest=dest(pLink);
		synchronized(this) {
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

		super.tidyUp(pDest);
	}

	@Override
	protected void startEntry() throws IOException {
		assert nonePending(mPromised);
		mPromised.clear();
		super.startEntry();
		assert(Thread.currentThread().getId()==mQueryTid);
	}
	
	@Override
	public synchronized void reset() throws IOException {
		mDone.reset();
		super.reset();
	}

}
