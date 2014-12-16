package com.hourglassapps.persist;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;

import com.hourglassapps.cpi_ii.CPIFields;
import com.hourglassapps.cpi_ii.lucene.FieldVal;
import com.hourglassapps.cpi_ii.lucene.Indexer;
import com.hourglassapps.cpi_ii.lucene.ResultGenerator;
import com.hourglassapps.util.Ii;

/**
 * Class to detect and symlink to any URL that has previously been downloaded when presented
 * with a download link.
 * @author kieran
 *
 */
public class DoneStore implements Store<Ii<String,String>,Ii<String,String>,Path> {
	private final static String TAG=DoneStore.class.getName();
	private final Path mDir;
	
	private Indexer mIndex=null;
	
	//Maps the URL of a document to its path in partial dir
	private Map<String,String> mPending=new HashMap<>(); 
	
	private enum DoneFields {
		//SRC is the URL of a document, DST is the path of the document in final directory (ie not the journal's partial directory)
		SRC(new FieldVal("src", false)), DST(new FieldVal("dst", false));
		
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
	public DoneStore(Path pDir) throws IOException {
		mDir=pDir;
		index(mDir).close();
		mIndex=index(mDir);
	}

	private static Indexer index(Path pDir) throws IOException {
		return new Indexer(pDir, null, false);
	}
	
	private boolean indexHas(final Ii<String,String> pKey) throws IOException {
		if(mIndex==null) {
			mIndex=index(mDir);
		}
		ResultGenerator<Boolean> resGen=new ResultGenerator<Boolean>() {
			private Boolean mFound=Boolean.FALSE;

			@Override
			public void run(IndexReader pReader, TopDocs pResults)
					throws IOException {
				if(pReader.numDocs()>0) {
					String original=pReader.document(pResults.scoreDocs[0].doc, 
							Collections.singleton(DoneFields.DST.fieldVal().s())).get(DoneFields.DST.fieldVal().s());
					link(Paths.get(pKey.snd()), Paths.get(original));
					mFound=Boolean.TRUE;
				}
			}

			@Override
			public Boolean result() {
				return mFound;
			}

		};
		try {
			List<IndexCommit> commits=DirectoryReader.listCommits(mIndex.writer().getDirectory());
			assert(commits.size()==1);
			try(IndexReader reader=DirectoryReader.open(commits.get(0))) {
				mIndex.interrogate(reader, DoneFields.SRC.fieldVal(), pKey.fst(), 1, resGen);
			}
		} catch(IndexNotFoundException e) {
			//no commits yet
			return false;
		}
		
		return resGen.result();
	}
	
	/**
	 * Checks whether a link has already been downloaded and if so creates a symbolic link to the original.
	 * This method must be idempotent if a DoneStore is to be used in conjunction with a DeferredFilesJournal. 
	 */
	@Override
	public boolean addedAlready(Ii<String,String> pSrcDst) throws IOException {
		if(mPending.containsKey(pSrcDst.fst())) {
			link(Paths.get(pSrcDst.snd()), Paths.get(mPending.get(pSrcDst.fst())));
			return true;
		}
		return indexHas(pSrcDst);
	}

	private void link(Path pLink, Path pOrig) throws IOException {
		try {
			Files.createSymbolicLink(pLink, pLink.relativize(pOrig));
		} catch(FileAlreadyExistsException e) {
			//necessary to catch this since this function needs to be idempotent
		}
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
