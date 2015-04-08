package com.hourglassapps.persist;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;

import com.hourglassapps.cpi_ii.lucene.FieldVal;
import com.hourglassapps.cpi_ii.lucene.Indexer;
import com.hourglassapps.util.Ii;

/**
 * Class to detect and symlink to any URL that has previously been downloaded when presented
 * with a download link.
 * @author kieran
 *
 */
public class DoneStore implements Store<Ii<String,String>,Ii<String,String>,Path> {
	private final static String TAG=DoneStore.class.getName();
	//private final static String NULL_PATH="__null__";
	private final Path mDir;
	public final static Linker LINKER=new Linker() {
		public void link(Path pLink, Path pOrig) throws IOException {
			try {
				Files.createSymbolicLink(pLink, pLink.relativize(pOrig));
			} catch(FileAlreadyExistsException e) {
				//necessary to catch this since this function needs to be idempotent
			}
		}		
	};

	private final DocTracker mResGen=new DocTracker(DoneFields.DST.fieldVal(), LINKER); //this can't be static if you're using multiple download threads
	private Indexer mIndex=null;
	
	//Maps the URL of a document to its path in partial dir
	private Map<String,String> mPending=new HashMap<>(); 
	
	private enum DoneFields {
		//SRC is the URL of a document, DST is the path of the document in final directory (ie not the journal's partial directory)
		SRC(new FieldVal("src", false)), DST(new FieldVal("dst", false){
			private final String NULL_VAL="__null__";
			private final Field NULL_FIELD=field(NULL_VAL);
			
			@Override
			public boolean isNull(String pFieldVal) {
				return NULL_VAL.equals(pFieldVal);
			}
			
			@Override
			public Field nullField() {
				return NULL_FIELD;
			}
		});
		
		private final FieldVal mField;
		
		DoneFields(FieldVal pField) {
			mField=pField;
		}
		
		public FieldVal fieldVal() {
			return mField;
		}
	}
	
	/**
	 * 
	 * @param pDir Directory where a record of which URLs have been downloaded is saved
	 * @throws IOException
	 */
	public DoneStore(Path pDir, Set<String> pFilteredURLs) throws IOException {
		mDir=pDir;
		index(mDir).close();
		mIndex=index(mDir);

		List<IndexCommit> commits=DirectoryReader.listCommits(mIndex.writer().getDirectory());
		if(commits.size()==1) {
			try(IndexReader reader=DirectoryReader.open(commits.get(0))) {
				insertURLs(mResGen, mIndex, reader, pFilteredURLs);
			} catch(IndexNotFoundException e) {
				insertURLs(mResGen, mIndex, null, pFilteredURLs);
			}
		} else {
			insertURLs(mResGen, mIndex, null, pFilteredURLs);			
		}
	}

		/**
		 * Facilitates adding URLs to DoneStore prior to download so that these URLs can be skipped.
		 * @param pReader If this IndexReader is null then it will be assumed that none of the provided URLs have already been committed to the DoneStore
		 * @param pFilteredURLs. URLs to be added to DoneStore prior to downloading documents. 
		 * @throws IOException 
		 */
	private static void insertURLs(DocTracker pResGen, Indexer pIndex, IndexReader pReader, Set<String> pFilteredURLs) throws IOException {
		for(String filtered: pFilteredURLs) {
			if(!indexHas(pResGen, pIndex, pReader, filtered)) {				
				pIndex.add(DoneFields.SRC.fieldVal().field(filtered), DoneFields.DST.fieldVal().nullField());
			}
		}
		pIndex.writer().commit();		
	}
	
	private static Indexer index(Path pDir) throws IOException {
		return new Indexer(pDir, null, false);
	}
		
	private boolean indexHas(final Ii<String,String> pKey) throws IOException {
		if(mIndex==null) {
			mIndex=index(mDir);
		}

		mResGen.setLink(pKey.snd());

		List<IndexCommit> commits=DirectoryReader.listCommits(mIndex.writer().getDirectory());
		if(commits.size()==1) {
			try(IndexReader reader=DirectoryReader.open(commits.get(0))) {
				mIndex.interrogate(reader, DoneFields.SRC.fieldVal(), pKey.fst(), 1, mResGen);
			} catch(IndexNotFoundException e) {
				//no commits yet
				return false;
			}
		} else {
			assert(commits.size()==0);
		}
		
		return mResGen.result();
	}
	
	private static boolean indexHas(DocTracker pResGen, Indexer pIndex, IndexReader pReader, String pURL) throws IOException {
		if(pIndex==null) {
			return false;
		}
		
		pResGen.set();
		pIndex.interrogate(pReader, DoneFields.SRC.fieldVal(), pURL, 1, pResGen);
		return pResGen.result();
	}
	
	/**
	 * Checks whether a link has already been downloaded and if so creates a symbolic link to the original.
	 * This method must be idempotent if a DoneStore is to be used in conjunction with a DeferredFilesJournal. 
	 */
	@Override
	public boolean addedAlready(Ii<String,String> pSrcDst) throws IOException {
		if(mPending.containsKey(pSrcDst.fst())) {
			LINKER.link(Paths.get(pSrcDst.snd()), Paths.get(mPending.get(pSrcDst.fst())));
			return true;
		}
		return indexHas(pSrcDst);
	}

	/**
	 * Idempotent method
	 */
	@Override
	public void addNew(Ii<String,String> pSrcDst) {
		if(pSrcDst==null) {
			return;
		}
		mPending.put(pSrcDst.fst(), pSrcDst.snd()); //destination is unique so make that the key
	}

	private void newTrans() throws IOException {
		mIndex.writer().commit();
		mPending.clear();		
	}
	
	@Override
	public void commit(Path pDestDir) throws IOException {
		if(mPending.size()==0) {
			return;
		}

		for(String src: mPending.keySet()) {
			Path dst=pDestDir.resolve(Paths.get(mPending.get(src)).getFileName());
			mIndex.add(DoneFields.SRC.fieldVal().field(src), DoneFields.DST.fieldVal().field(dst.toString()));
		}
		newTrans();
	}

	@Override
	public void reset() throws IOException {
		mIndex.wipe();
		newTrans();
	}

	@Override
	public void close() throws IOException {
		mIndex.writer().rollback();
		mIndex.close();
	}
}
