package com.hourglassapps.persist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;

import com.hourglassapps.cpi_ii.CPIFields;
import com.hourglassapps.cpi_ii.FieldVal;
import com.hourglassapps.cpi_ii.Indexer;
import com.hourglassapps.cpi_ii.ResultGenerator;
import com.hourglassapps.util.Ii;

public class DoneStore implements Store<Ii<String,String>,Ii<String,String>,Path> {
	private final static String TAG=DoneStore.class.getName();
	private final Path mDir;
	private Indexer mIndex=null;
	private Map<String,String> mPending=new HashMap<>(); 
	
	private enum DoneFields {
		SRC(new FieldVal("src", false)), DST(new FieldVal("dst", false));
		
		private final FieldVal mField;
		
		DoneFields(FieldVal pField) {
			mField=pField;
		}
		
		public FieldVal fieldVal() {
			return mField;
		}
	}
	
	public DoneStore(Path pDir) throws IOException {
		mDir=pDir;
		index(mDir).close();
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
					String original=pReader.document(pResults.scoreDocs[0].doc, Collections.singleton(DoneFields.DST.fieldVal().s())).get(DoneFields.DST.fieldVal().s());
					link(Paths.get(pKey.snd()), Paths.get(original));
					mFound=Boolean.TRUE;
				}
			}

			@Override
			public Boolean result() {
				return mFound;
			}

		};
		mIndex.interrogate(DoneFields.SRC.fieldVal(), pKey.fst(), 1, resGen);
		
		return resGen.result();
	}
	
	@Override
	public boolean addExisting(Ii<String,String> pSrcDst) throws IOException {
		if(mPending.containsKey(pSrcDst.fst())) {
			link(Paths.get(pSrcDst.snd()), Paths.get(mPending.get(pSrcDst.fst())));
			return true;
		}
		return indexHas(pSrcDst);
	}

	private void link(Path pLink, Path pOrig) throws IOException {
		Files.createSymbolicLink(pLink, pLink.relativize(pOrig));
	}

	@Override
	public void addNew(Ii<String,String> pSrcDst) {
		if(pSrcDst==null) {
			return;
		}
		mPending.put(pSrcDst.fst(), pSrcDst.snd()); //destination is unique so make that the key
	}

	private void newTrans() throws IOException {
		assert(mIndex!=null);
		mIndex.close();
		mIndex=null;
		mPending.clear();		
	}
	
	@Override
	public void commit(Path pDestDir) throws IOException {
		if(mPending.size()==0) {
			return;
		}
		if(mIndex==null) {
			mIndex=index(mDir);
		}
		for(String src: mPending.keySet()) {
			Path dst=pDestDir.resolve(Paths.get(mPending.get(src)).getFileName());
			mIndex.add(DoneFields.SRC.fieldVal().field(src), DoneFields.DST.fieldVal().field(dst.toString()));
		}
		newTrans();
	}

	@Override
	public void reset() throws IOException {
		if(mIndex==null) {
			mIndex=index(mDir);
		}
		mIndex.wipe();
		newTrans();
	}
}
