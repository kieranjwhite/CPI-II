package com.hourglassapps.serialise;

public class ParseException extends SerialiseException {
	private String mMsg;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ParseException() {
		super();
	}

	public ParseException(String pMsg) {
		super(pMsg);
	}

	public ParseException(Throwable pEx) {
		super(pEx);
	}
	
	public ParseException setReceived(String pMsg) {
		mMsg=pMsg;
		return this;
	}
	
	public String getReceived() {
		return mMsg;
	}
}
