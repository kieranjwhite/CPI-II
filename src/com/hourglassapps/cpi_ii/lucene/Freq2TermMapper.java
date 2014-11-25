package com.hourglassapps.cpi_ii.lucene;

import java.util.Comparator;
import java.util.List;

import com.hourglassapps.util.SortedMultiMap;
import com.hourglassapps.util.TreeArrayMultiMap;

public class Freq2TermMapper extends AbstractTermFreqMapper {
	private SortedMultiMap<Long,List<String>,String> mFreq2Terms=
			new TreeArrayMultiMap<Long, String>(new Comparator<Long>() {

				@Override
				public int compare(Long pFst, Long pSnd) {
					return -pFst.compareTo(pSnd);
				}});
	
	public void display() {
		for(Long f: mFreq2Terms.keySet()) {
			for(String t: mFreq2Terms.get(f)) {
				System.out.println(t+"\t"+f);
			}
		}			
	}

	@Override
	public void add(String pTerm, Long pFreq) {
		mFreq2Terms.addOne(pFreq, pTerm);
	}

}