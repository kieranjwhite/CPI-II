package com.hourglassapps.cpi_ii;

import java.util.ArrayList;
import java.util.List;

public class StanzaText {
	private final String mName;
	private final List<String> mLines;

	/**
	 * 
	 * @param pName an arbitrary stanza name, displayed within the report
	 * @param pStanzaLines a list of the stanzas lines, also displayed in the report
	 */
	public StanzaText(String pName, List<String> pStanzaLines) {
		mName=pName;
		mLines=new ArrayList<>(pStanzaLines);
	}
	
	public String name() {
		return mName;
	}

	public List<String> lines() {
		return mLines;
	}
}