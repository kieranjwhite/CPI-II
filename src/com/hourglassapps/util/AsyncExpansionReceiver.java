package com.hourglassapps.util;

import java.io.IOException;
import java.util.List;

import org.jdeferred.Promise;

public interface AsyncExpansionReceiver<O,K> extends ExpansionReceiver<O>, Promiser<Void,IOException,K> {
	public Promise<Void,IOException,K> promise();
}
