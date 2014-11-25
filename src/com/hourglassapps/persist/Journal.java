package com.hourglassapps.persist;

import java.io.IOException;

import org.jdeferred.Promise;

import com.hourglassapps.util.Typed;

public interface Journal<K,A> extends Store<K,A,K> {
}
