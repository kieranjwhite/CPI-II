package com.hourglassapps.cpi_ii.lucene;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.hourglassapps.util.Log;

public class TikaReader extends Reader {
	private final static String TAG=TikaReader.class.getName();
	
	//in characters
	private final static int MAX_CONTENT_SIZE=64*1024*1024;	
	
	private final StringReader mDelegate;
	
	public TikaReader(InputStream pInput, String pMimeType) throws IOException, SAXException, TikaException {
		// Copied mostly from http://stackoverflow.com/questions/6713927/extract-the-contenttext-of-a-url-using-tika
		ContentHandler handler=new BodyContentHandler(MAX_CONTENT_SIZE);
		Metadata metadata=new Metadata();
		if(pMimeType!=null) {
			metadata.set(Metadata.CONTENT_TYPE, pMimeType);
		}
		Parser parser=new AutoDetectParser();
		try {
			parser.parse(pInput, handler, metadata, new ParseContext());
		} catch(SAXException e) {
			Log.e(TAG, e);
		}
		mDelegate=new StringReader(handler.toString());
	}

	@Override
	public void close() throws IOException {
		mDelegate.close();
	}

	@Override
	public int read(char[] arg0, int arg1, int arg2) throws IOException {
		return mDelegate.read(arg0, arg1, arg2);
	}

}
