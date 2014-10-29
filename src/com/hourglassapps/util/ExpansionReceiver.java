package com.hourglassapps.util;

import java.util.List;

public interface ExpansionReceiver<O> {
	/**
	 * Subclasses will override this method to handle each output permutation. Each invocation of 
	 * onExpansion corresponds to one permutation.<p>
	 * @param pExpansions One permutation of outputs. Each element in <code>pExpansions</code> 
	 * corresponds to 
	 * its respective element which was passed to the <code>expand</code> method. So 
	 * <code>pExpansions.get(3)</code> 
	 * is one of the values <code>pTerms[3]</code> was mapped to in the <code>pOrig2Expansions</code> 
	 * constructor argument, assuming that there were at least 3 elements in <code>pTerms</code>.  
	 */
	public void onExpansion(List<O> pExpansions);
	
	public void onGroupDone(int pNumExpansions);
}
