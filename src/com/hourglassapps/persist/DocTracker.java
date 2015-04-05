package com.hourglassapps.persist;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.TopDocs;

import com.hourglassapps.cpi_ii.lucene.FieldVal;
import com.hourglassapps.cpi_ii.lucene.ResultGenerator;

public class DocTracker implements ResultGenerator<Boolean> {
	private final Linker mLinker;
	private final FieldVal mField;
	private String mLink=null;
	private Boolean mFound=Boolean.FALSE;
	
	public DocTracker(FieldVal pField, Linker pLinker) {
		mField=pField;
		mLinker=pLinker;
	}
	
	public void setLink(String pLink) {
		mFound=Boolean.FALSE;
		mLink=pLink;
	}
	
	public void set() {
		mFound=Boolean.FALSE;
		mLink=null;
	}
	
	@Override
	public void run(IndexReader pReader, TopDocs pResults)
			throws IOException {
		if(pReader.numDocs()>0) {
			if(mLink!=null) {
				String original=pReader.document(pResults.scoreDocs[0].doc, 
					Collections.singleton(mField.s())).get(mField.s());
				if(!mField.isNull(original)) {
					mLinker.link(Paths.get(mLink), Paths.get(original));
				}
			}
			mFound=Boolean.TRUE;
		}
	}

	@Override
	public Boolean result() {
		return mFound;
	}

	
}
