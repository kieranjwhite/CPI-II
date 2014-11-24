package com.hourglassapps.cpi_ii;

import java.io.IOException;

import org.jdeferred.Promise;

import com.hourglassapps.persist.Store;
import com.hourglassapps.util.Typed;

public interface Journal<K,A> extends Store<K,A,K> {
}
