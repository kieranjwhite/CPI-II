package com.hourglassapps.serialise;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hourglassapps.cpi_ii.Record;
import com.hourglassapps.util.ThrowableIterator;

public class JSONParser<I,C, R extends Record<I,C>> implements Closeable, ThrowableIterator<R> {
	private JsonParser mParser;
	private JsonToken mNextToken;
	private ObjectMapper mMapper=new ObjectMapper();
	private IOException mThrowable=null;
	private Class<R> mClass;
	private final Reader mPreprocessor;
	
	public JSONParser(Reader pPreprocessor, Class<R> pClass) throws IOException, ParseException {
		mPreprocessor=pPreprocessor;
		mClass=pClass;
		JsonFactory f=new JsonFactory();
		try {
			mParser=f.createJsonParser(pPreprocessor);
		} catch (JsonParseException e) {
			throw new ParseException(e);
		}
		mParser.nextToken(); //advances mParser to start of array
		mNextToken=mParser.nextToken(); //advances mParser to first element
	}
	
	@Override
	public boolean hasNext() {
		return mNextToken==JsonToken.START_OBJECT;
	}

	@Override
	public R next() {
		R rec;
		try {
			rec = mMapper.readValue(mParser, mClass);
			mNextToken=mParser.nextToken();
		} catch (IOException e) {
			mThrowable=e;
			mNextToken=JsonToken.NOT_AVAILABLE;
			return null;
		}
		return rec;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void throwCaught() throws IOException {
		if(mThrowable!=null) {
			throw mThrowable;
		}
	}

	@Override
	public void close() throws IOException {
		throwCaught();
		mPreprocessor.close();
	}

}
