package com.hourglassapps.persist;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;

class FileCopyingThread<S extends SelectableChannel & ReadableByteChannel & NetworkChannel> extends Thread {
	private final static String TAG=FileCopyingThread.class.getName();
	private volatile boolean mRunning=true;
	private Selector mSelector=Selector.open();
	private Map<Channel,Ii<ByteBuffer,FileChannel>> mSrc2BufDst=new HashMap<>();
	private final Map<S,Deferred<Void,IOException,Void>> mSrc2Deferred=new HashMap<>();

	public FileCopyingThread() throws IOException {
		super("file copier");
	}

	private void close(SelectionKey pKey) throws IOException {
		Channel chan=pKey.channel();
		Ii<ByteBuffer,FileChannel> bufDst=mSrc2BufDst.get(chan);
		if(bufDst!=null) {
			bufDst.snd().close();
			mSrc2BufDst.remove(chan);
		}
		pKey.cancel();		
		mSrc2Deferred.get(chan).resolve(null);
	}
	
	@Override
	public void run() {
		try {
			while(mRunning) {
				int outstanding=mSelector.select();
				if(outstanding==0) {
					continue;
				}

				Set<SelectionKey> keys=mSelector.selectedKeys();
				Iterator<SelectionKey> keyIt=keys.iterator();
				while(keyIt.hasNext()) {
					SelectionKey k=keyIt.next();
					try {
						Channel chan=k.channel();
						assert chan instanceof ReadableByteChannel;
						assert chan instanceof SelectableChannel;
						assert chan instanceof NetworkChannel;
						assert k.attachment()!=null;
						assert k.attachment() instanceof Path;
						assert mSrc2BufDst.containsKey(chan);
						assert mSrc2Deferred.containsKey(chan);
						if(k.isConnectable()) {
							assert chan instanceof SocketChannel;
							/*
							 * This cast is safe since only SocketChannels can be connectable.
							 * See http://docs.oracle.com/javase/7/docs/api/java/nio/channels/SelectionKey.html#isConnectable()
							 */
							((SocketChannel)(chan)).finishConnect();
						} else if(k.isReadable()) {
							ReadableByteChannel readable=(ReadableByteChannel)chan;
							NetworkChannel networkable=(NetworkChannel)chan;
							Ii<ByteBuffer,FileChannel> bufDst=mSrc2BufDst.get(chan);
							if(bufDst==null) {

								int bufSize;
								try {
									bufSize=networkable.getOption(StandardSocketOptions.SO_RCVBUF);
								} catch(UnsupportedOperationException e) {
									//it seems SO_RCVBUF isn't an option for this type of socket
									bufSize=NonBlockingFileJournal.DEFAULT_BUFFER_SIZE;
								}
								ByteBuffer buf=ByteBuffer.allocate(bufSize);
							
								Path dest=(Path)k.attachment();
								assert !Files.exists(dest);
								
								/* Creating FileChannel is the last operation in this block that can trigger an IOException.
								 * Therefore any IOException prior to this (in this block) won't require us to close FileChannel.
								 */
								FileChannel dst=FileChannel.open(dest, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);

								bufDst=new Ii<ByteBuffer, FileChannel>(buf, dst);
								mSrc2BufDst.put(chan, bufDst);
							}
							if(readable.read(bufDst.fst())==-1) {
								//end-of-channel
								close(k);
								chan.close();
								continue;
							}
							ByteBuffer buf=bufDst.fst();
							FileChannel dst=bufDst.snd();
							buf.flip();
							while(buf.hasRemaining()) {
								dst.write(buf);
							}
							buf.clear();
						} else {
							assert false;
						}
					} catch(IOException e) {
						Log.e(TAG, e);
						close(k); /* Note the Deferred instance corresponding to this channel is resolved here even 
									 though an exception occurred. A rejection would cause the entire transaction
									 (ie all downloads for a given Bing query) to fail.
						 			*/
					} finally {
						keyIt.remove();
					}
				}

			}
		} catch(IOException e) {
			Log.e(NonBlockingFileJournal.TAG, e);
		}
	}
	
	public Set<? super Deferred<Void,IOException,Void>> promises() {
		Set<Promise<Void,IOException,Void>> promises=new HashSet<Promise<Void,IOException,Void>>();
		for(Deferred<Void,IOException,Void> def: mSrc2Deferred.values()) {
			promises.add(def);
		}
		return promises;
	}
	
	public void quit() {
		mRunning=false;
	}
	
	public void register(S pChannel, Path pDest) throws IOException {
		pChannel.register(mSelector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ, pDest);
		//mSrc2BufDst.put(pChannel, new Ii<>(buf, pDest));
		mSrc2Deferred.put(pChannel, new DeferredObject<Void,IOException,Void>());
	}
}