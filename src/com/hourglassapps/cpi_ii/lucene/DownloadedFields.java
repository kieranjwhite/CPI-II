package com.hourglassapps.cpi_ii.lucene;

public enum DownloadedFields {
	PATH(new FieldVal("key", false)), 
	POSITION(new FieldVal("pos", false)),
	TITLE(new FieldVal("title", false)),
	CONTENT(new FieldVal("content", true, false));
	
	private final FieldVal mField;
	
	private DownloadedFields(FieldVal pField) {
		mField=pField;
	}
	
	public FieldVal fieldVal() {
		return mField;
	}
}