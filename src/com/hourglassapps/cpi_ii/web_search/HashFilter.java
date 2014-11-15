package com.hourglassapps.cpi_ii.web_search;

import java.net.URL;

import com.hourglassapps.util.Filter;

public class HashFilter implements Filter<URL> {
	private final int mModResult;
	private final int mNumProcesses;
	
	public HashFilter(int pModResult, int pNumProcesses) throws IllegalArgumentException {
		if(pNumProcesses<1) {
			throw new IllegalArgumentException("Number of processes must be >=1");
		}
		if(pModResult<0 || pModResult>=pNumProcesses) {
			throw new IllegalArgumentException("Mod result must be >=0 && < number of processes");
		}
		mModResult=pModResult;
		mNumProcesses=pNumProcesses;
	}
	
	@Override
	public boolean accept(URL pArg) {
		return (pArg.hashCode()%mNumProcesses)==mModResult;
	}

}
