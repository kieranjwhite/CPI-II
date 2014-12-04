package com.hourglassapps.serialise;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hourglassapps.cpi_ii.Record;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.IOIterator;
import com.hourglassapps.util.ThrowingIterable;

public class JSONParser<I,C, R extends Record<I,C>> implements ThrowingIterable<R> {
	private JsonParser mParser;
	private JsonToken mNextToken;
	private ObjectMapper mMapper=new ObjectMapper();
	//private IOException mThrowable=null;
	private Class<R> mClass;
	private final Reader mPreprocessor;
	private final JsonFactory mF=new JsonFactory();

	public JSONParser(Reader pPreprocessor, Class<R> pClass) throws IOException, ParseException {
		mPreprocessor=pPreprocessor;
		mClass=pClass;
	}

	@Override
	public Iterator<R> iterator() {
		return throwableIterator();
	}

	@Override
	public IOIterator<R> throwableIterator() {
		final ConcreteThrower<IOException> thrower=new ConcreteThrower<IOException>();
		try {
			mParser=mF.createJsonParser(mPreprocessor);
			mParser.nextToken(); //advances mParser to start of array
			mNextToken=mParser.nextToken(); //advances mParser to first element
		} catch (IOException e1) {
			thrower.ctch(e1);
			mNextToken=JsonToken.NOT_AVAILABLE;
		}

		return new IOIterator<R>() {
			@Override
			public boolean hasNext() {
				if(thrower.fallThrough()) {
					return false;
				}
				return mNextToken==JsonToken.START_OBJECT;
			}

			@Override
			public R next() {
				R rec;
				try {
					rec = mMapper.readValue(mParser, mClass);
					mNextToken=mParser.nextToken();
				} catch (IOException e) {
					thrower.ctch(e);
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
			public void close() throws IOException {
				throwCaught(null);
				mPreprocessor.close();
			}

			@Override
			public <E extends Exception> void throwCaught(Class<E> pCatchable) throws IOException {
				thrower.throwCaught(IOException.class);
			}

		};
	}

}
