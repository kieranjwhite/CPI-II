package com.hourglassapps.cpi_ii;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.hourglassapps.util.MultiMap;

public abstract class AbstractComboExpander<I,O> {
	private final Converter<I, O> mConverter;
	private final MultiMap<I, Set<O>, O> mOrig2Expansions;
	
	public AbstractComboExpander(MultiMap<I, Set<O>, O> pOrig2Expansions, Converter<I, O> pConverter) {
		mConverter=pConverter;
		mOrig2Expansions=pOrig2Expansions;
	}
	
	//recursive
	private void expand(I[] pTerms, final int pStartIdx, List<O> pOut) {
		assert pTerms.length==pOut.size();
		if(pStartIdx>=pTerms.length) {
			onExpansion(pOut);
			return;
		}
		
		int nextIdx=pStartIdx+1;
		Set<O> expansions=mOrig2Expansions.get(pTerms[pStartIdx]);
		if(expansions==null) {
			if(mConverter!=null) {
				pOut.set(pStartIdx, mConverter.convert(pTerms[pStartIdx]));
				expand(pTerms, nextIdx, pOut);
			} //else if no converter provided terms missing mOrig2Expansions cause entire expansion group to be ignored
		} else {
			for(O expansion: expansions) {
				pOut.set(pStartIdx, expansion);
				expand(pTerms, nextIdx, pOut);
			}
		}
	}
	
	public void expand(I[] pTerms) {
		List<O> out=new ArrayList<O>(pTerms.length);
		for(int i=0; i<pTerms.length; i++) {
			out.add(mConverter.convert(pTerms[i]));
		}
		expand(pTerms, 0, out);
	}
	
	abstract public void onExpansion(List<O> pExpansions);
}
