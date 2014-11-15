package com.hourglassapps.cpi_ii.web_search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

enum Cmd {
	ALL("all"), PARTITION("partition"), RANDOM("random"), ONE("one"), DOWNLOAD("download");
	
	private final static Set<Cmd> CMDS=new HashSet<Cmd>(Arrays.asList(
			new Cmd[]{ALL, PARTITION, RANDOM, ONE, DOWNLOAD}));
	
	private final String mName;
	
	private Cmd(String pName) {
		mName=pName;
	}
	
	final public String s() {
		return mName;
	}
	
	public static Cmd inst(String pArg) throws UnrecognisedSyntaxException {
		for(Cmd c: CMDS) {
			if(c.s().equals(pArg)) {
				return c;
			}
		}
		throw new UnrecognisedSyntaxException();
	}
}