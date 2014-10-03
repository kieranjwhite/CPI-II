package com.hourglassapps.cpi_ii;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.queryparser.xml.ParserException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hourglassapps.serialise.ParseException;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.ThrowableIterator;

public class JSONParser<I,C, R extends Record<I,C>> implements ThrowableIterator<Ii<I,C>> {
	private JsonParser mParser;
	private JsonToken mNextToken;
	private ObjectMapper mMapper=new ObjectMapper();
	private Throwable mThrowable=null;
	private Class<R> mClass;
	
	public JSONParser(String pFilename, Class<R> pClass) throws IOException, ParseException  {
		mClass=pClass;
		JsonFactory f=new JsonFactory();
		try {
			mParser=f.createJsonParser(new File(pFilename));
		} catch (JsonParseException e) {
			throw new ParseException(e);
		}
		mParser.nextToken(); //advances mParser to start of arrray
		mNextToken=mParser.nextToken(); //advances mParser to first element
	}
	
	@Override
	public boolean hasNext() {
		return mNextToken==JsonToken.START_OBJECT;
	}

	@Override
	public Ii<I,C> next() {
		R rec;
		try {
			rec = mMapper.readValue(mParser, mClass);
			mNextToken=mParser.nextToken();
		} catch (Throwable e) {
			mThrowable=e;
			mNextToken=JsonToken.NOT_AVAILABLE;
			return null;
		}
		return new Ii<I,C>(rec.id(), rec.content());
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void throwCaught() throws Throwable {
		if(mThrowable!=null) {
			throw mThrowable;
		}
	}

}
