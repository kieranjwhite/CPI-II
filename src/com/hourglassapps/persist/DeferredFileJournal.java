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
import com.hourglassapps.util.Typed;

public class DeferredFileJournal<K,C,S> extends AbstractFileJournal<K,C,Promise<S,IOException,Void>> {
	private final static String TAG=DeferredFileJournal.class.getName(); 
	
	private final Set<Promise<Void,IOException,Void>> mDeferred=new HashSet<>();
	private final ConcreteThrower<IOException> mThrower=new ConcreteThrower<IOException>();
	private final DeferredManager mDeferredMgr=new DefaultDeferredManager();
	@SuppressWarnings("rawtypes")
	private final Promise[] mPendingArr=new Promise[]{};

	public DeferredFileJournal(Path pDirectory,
			Converter<K, String> pFilenameGenerator,
			Converter<C, Promise<S, IOException, Void>> pContentGenerator)
			throws IOException {
		super(pDirectory, pFilenameGenerator, pContentGenerator,0);
	}
	
	@Override
	public void add(final Typed<C> pLink) throws IOException {
		incFilename();
		mDeferred.add(source(pLink).then(new DonePipe<S,Void,IOException,Void>(){

			@Override
			public Promise<Void, IOException, Void> pipeDone(S pContent) {
				Deferred<Void,IOException,Void> deferred=new DeferredObject<Void,IOException,Void>();
				if(pContent==null) {
					deferred.resolve(null);
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
				return deferred;
			}
			
		}));
	}
	
	@Override
	public void commitEntry(final K pKey) throws IOException {
		Promise<S, IOException, Void> p=mDeferredMgr.when(mDeferred.toArray(mPendingArr)).then(
				new DonePipe<MultipleResults,S,IOException,Void>(){

			@Override
			public Promise<S, IOException, Void> pipeDone(
					MultipleResults result) {
						Deferred<S,IOException,Void> deferred=new DeferredObject<S,IOException,Void>();
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
			p.waitSafely();
		} catch (InterruptedException i) {
			mThrower.ctch(new IOException(i));
		}
		startEntry(); //Note this is invoked even if there's an exception
		mThrower.throwCaught(null);
	}

}
