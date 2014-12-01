package com.hourglassapps.util;

import java.io.IOException;
import java.util.List;

import org.jdeferred.Promise;

public interface AsyncExpansionReceiver<O,K> extends ExpansionReceiver<O> {
	public Promise<Void,IOException,K> promise();
}
