package com.hourglassapps.util;

import org.jdeferred.Promise;

public interface Promiser<A,B,C> extends AutoCloseable {
	public Promise<A,B,C> promise();
}
