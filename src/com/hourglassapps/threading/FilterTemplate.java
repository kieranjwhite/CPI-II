package com.hourglassapps.threading;

import java.util.Set;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Filter;
import com.hourglassapps.util.Ii;

/***
 * A MetaConverter instance can create a Converter from an instance of T to a set 
 * @author kieran
 *
 * @param <T>
 */
public interface FilterTemplate<I> extends Converter<I, ThreadFunction> {
}