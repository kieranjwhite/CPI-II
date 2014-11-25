package com.hourglassapps.cpi_ii;

import com.hourglassapps.cpi_ii.lucene.FieldVal;

public enum CPIFields {
	KEY(new FieldVal("key", true)), 
	CONTENT(new FieldVal("content", true));
	
	private final FieldVal mField;
	
	private CPIFields(FieldVal pField) {
		mField=pField;
	}
	
	public FieldVal fieldVal() {
		return mField;
	}
}
