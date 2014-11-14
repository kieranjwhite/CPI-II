package com.hourglassapps.cpi_ii.web_search;

enum Cmd {
	ALL("all"), RANDOM("random"), ONE("one"), DOWNLOAD("download");
	
	private final String mName;
	
	private Cmd(String pName) {
		mName=pName;
	}
	
	final public String s() {
		return mName;
	}
	
	public static Cmd inst(String pArg) throws UnrecognisedSyntaxException {
		if(Cmd.ALL.s().equals(pArg)) {
			return Cmd.ALL;
		} else if(Cmd.RANDOM.s().equals(pArg)) {
			return Cmd.RANDOM;
		} else if(Cmd.ONE.s().equals(pArg)) {
			return Cmd.ONE;
		} else if(Cmd.DOWNLOAD.s().equals(pArg)) {
			return Cmd.DOWNLOAD;
		} else {
			throw new UnrecognisedSyntaxException();
		}			
	}
}