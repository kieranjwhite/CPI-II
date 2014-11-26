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

public class TikaReader extends Reader {
	private final StringReader mDelegate;
	
	public TikaReader(InputStream pInput, String pMimeType) throws IOException, SAXException, TikaException {
		// Copied mostly from http://stackoverflow.com/questions/6713927/extract-the-contenttext-of-a-url-using-tika
		ContentHandler handler=new BodyContentHandler();
		Metadata metadata=new Metadata();
		if(pMimeType!=null) {
			metadata.set(Metadata.CONTENT_TYPE, pMimeType);
		}
		Parser parser=new AutoDetectParser();
		parser.parse(pInput, handler, metadata, new ParseContext());
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
