package com.hourglassapps.util;

public interface Clock extends AutoCloseable {
	public Clock time(String pLabel);
	
	@Override
	public void close();
}