package com.hourglassapps.util;

import com.hourglassapps.util.TimeKeeper.StopWatch;

public interface Clock {
	public StopWatch time(String pLabel);
}