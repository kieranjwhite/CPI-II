package com.hourglassapps.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Permutator<I,O> {
	private final Converter<I, O> mConverter;
	private final MultiMap<I, Set<O>, O> mOrig2Expansions;
	private final ExpansionReceiver<O> mReceiver;
	
	/**
	 * An instance of this class will look up each element of an array in pOrig2Expansions
	 * for possible output values and generate all permutations by returning all possible
	 * output values for each each input element in turn.  
	 * @param pOrig2Expansions A MultiMap listing all possible output values for different input values
	 * @param pConverter Where an input value is not found in pOrig2Expansions this generates an
	 * output value. If pConverter is null then if an input value is not found no output values will
	 * be generated in response to an invocation of the <code>expand</code> method.
	 */
	public Permutator(MultiMap<I, Set<O>, O> pOrig2Expansions, Converter<I, O> pConverter, ExpansionReceiver<O> pReceiver) {
		mConverter=pConverter;
		mOrig2Expansions=pOrig2Expansions;
		mReceiver=pReceiver;
	}
	
	//recursive
	private int expand(I[] pTerms, final int pStartIdx, List<O> pOut) {
		assert pTerms.length==pOut.size();
		if(pStartIdx>=pTerms.length) {
			mReceiver.onExpansion(pOut);
			return 1;
		}
		
		int numExpansions=0;
		int nextIdx=pStartIdx+1;
		Set<O> expansions=mOrig2Expansions.get(pTerms[pStartIdx]);
		if(expansions==null) {
			if(mConverter!=null) {
				pOut.set(pStartIdx, mConverter.convert(pTerms[pStartIdx]));
				return expand(pTerms, nextIdx, pOut);
			} //else if no converter provided terms missing mOrig2Expansions cause entire expansion group to be ignored
		} else {
			
			for(O expansion: expansions) {
				pOut.set(pStartIdx, expansion);
				numExpansions+=expand(pTerms, nextIdx, pOut);
			}
			
		}
		return numExpansions;
	}
	
	/**
	 * Provides an array of input values. In response each element is passed to the MultiMap used to
	 * instantiate the class to retrieve a set of output values for that element. All permutations of
	 * these output values are then passed to the <code>onExpansion</code> method.
	 * @param pTerms array of inputs
	 */
	public int expand(I[] pTerms) {
		List<O> out=new ArrayList<O>(pTerms.length);
		for(int i=0; i<pTerms.length; i++) {
			out.add(null);
		}
		return expand(pTerms, 0, out);
	}
	
}
