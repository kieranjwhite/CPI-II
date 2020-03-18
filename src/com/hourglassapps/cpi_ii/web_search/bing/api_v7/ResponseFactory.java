package com.hourglassapps.cpi_ii.web_search.bing.api_v7;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hourglassapps.cpi_ii.web_search.bing.api_v7.response.Response;

public class ResponseFactory {
	private ObjectMapper mMapper=new ObjectMapper();
	private JsonParser mParser;
	
	public Response inst(String pToParse) throws JsonParseException, IOException {
	    JsonFactory f=new JsonFactory();
	    try(
		Reader reader=new StringReader(pToParse);
		) {
		mParser=f.createJsonParser(reader);
		JsonToken nextToken=mParser.nextToken(); //advances mParser to first element
		assert nextToken==JsonToken.START_OBJECT;
		Response resp=mMapper.readValue(mParser, Response.class);
		return resp;
	    }
	}
}
