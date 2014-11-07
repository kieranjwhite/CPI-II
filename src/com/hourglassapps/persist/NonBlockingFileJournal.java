package com.hourglassapps.persist;

import java.io.IOException;
import java.net.SocketOption;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.jdeferred.Deferred;
import org.jdeferred.DeferredManager;
import org.jdeferred.DonePipe;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DefaultDeferredManager;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MultipleResults;

import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Typed;

public class NonBlockingFileJournal<K,C, S extends SelectableChannel & ReadableByteChannel & NetworkChannel> extends FileJournal<K,C,S> {
	final static String TAG=NonBlockingFileJournal.class.getName();
	final static int DEFAULT_BUFFER_SIZE=4096;
	private final ConcreteThrower<IOException> mThrower=new ConcreteThrower<IOException>();
	private final DeferredManager mDeferredMgr=new DefaultDeferredManager();
	@SuppressWarnings("rawtypes")
	private final Promise[] mPendingArr=new Promise[]{};
	private final FileCopyingThread<S> mCopier=new FileCopyingThread<S>();

	public NonBlockingFileJournal(Path pDirectory,
			Converter<K, String> pFilenameGenerator,
			Converter<C, S> pContentGenerator)
			throws IOException {
		super(pDirectory, pFilenameGenerator, pContentGenerator);
		mCopier.start();
	}

	@Override
	public void add(Typed<C> pContent) throws IOException {
		S channel=source(pContent);
		if(channel==null) {
			incFilename();
			return;
		}
		if(channel.isBlocking()) {
			throw new IOException("channel must be non blocking");
		}
		Path path=dest(pContent);
		mCopier.register(channel, path);
	}

	@Override
	public void commitEntry(final K pKey) throws IOException {
		Promise<Void, IOException, Void> p=mDeferredMgr.when(mCopier.promises().toArray(mPendingArr)).then(new DonePipe<MultipleResults,Void,IOException,Void>(){

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
			p.waitSafely();
		} catch (InterruptedException i) {
			mThrower.ctch(new IOException(i));
		}
		startEntry(); //Note this is invoked even if there's an exception
		mThrower.throwCaught(null);
	}

}
