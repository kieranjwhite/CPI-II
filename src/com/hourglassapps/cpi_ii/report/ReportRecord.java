package com.hourglassapps.cpi_ii.report;

import java.util.List;

import com.hourglassapps.cpi_ii.Record;
import com.hourglassapps.cpi_ii.StanzaText;

public interface ReportRecord<I> extends Record<I, String> {
	public String getTitle();
	
	/**
	 * 
	 * @return a list of lines in the refrain and an empty list if there is no refrain
	 */
	public List<String> refrain();
	
	/**
	 * 
	 * @return a list of 0 or more StanzaTexts
	 */
	public List<StanzaText> stanzas();
}
