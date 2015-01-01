package com.hourglassapps.cpi_ii.report;

import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import com.hourglassapps.cpi_ii.lucene.DocSpan;

public class DocResult {
	SortedSet<DocSpan> mSpans=new TreeSet<>();;
	
	public void addSpan(DocSpan span) {
		mSpans.add(span);
	}
	
	public SortedSet<DocSpan> merge() {
		Iterator<DocSpan> spans=mSpans.iterator();
		DocSpan lastSpan=null;
		if(spans.hasNext()) {
			lastSpan=spans.next();
		}
		while(spans.hasNext()) {
			DocSpan span=spans.next();
			if(!lastSpan.merged(span)) {
				lastSpan=span;
			} else {
				spans.remove();
			}
		}
		mSpans=Collections.unmodifiableSortedSet(mSpans); //don't want to merge a second time or add more spans
		return mSpans;
	}
	
}