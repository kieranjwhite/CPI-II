package com.hourglassapps.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class URLUtils {
	private final static String TAG=URLUtils.class.getName();
	public final static String ENCODING=StandardCharsets.UTF_8.toString();

	public static String encode(String pString) throws UnsupportedEncodingException {
		return URLEncoder.encode(pString, ENCODING);
	}
	
	public static String decode(String pString) throws UnsupportedEncodingException {
		return URLDecoder.decode(pString, ENCODING);
	}
	
	public static URL reencode(URL pSource) throws UnsupportedEncodingException, MalformedURLException {
		String file=pSource.getFile();
		String newFile=file.replace("[", "%5B").replace("]", "%5D");
		return new URL(pSource.getProtocol(), pSource.getHost(), pSource.getPort(), 
				newFile);
	}
	
	private static void unitTests() {
		try {
			assert reencode(new URL("http://documentacatholicaomnia.eu/03d/0354-0430,_Augustinus,_Sermones_[5]_de_Diversis_(Serm._341-396),_LT.doc")).
			equals(new URL("http://documentacatholicaomnia.eu/03d/0354-0430,_Augustinus,_Sermones_%5B5%5D_de_Diversis_(Serm._341-396),_LT.doc"));
		} catch (UnsupportedEncodingException | MalformedURLException e) {
			Log.e(TAG, e);
		}
	}
	
	private static void usage() {
		System.out.println("Usage: URLUtils encode <String>");
		System.out.println("       URLUtils decode <String>");
		System.exit(-1);		
	}
	
	public static void main(String[] pArgs) throws UnsupportedEncodingException {
		if(pArgs.length<1 || pArgs.length>2) {
			usage();
		}
		switch(pArgs[0]) {
		case "encode":
			System.out.println(encode(pArgs[1]));
			break;
		case "decode":
			System.out.println(decode(pArgs[1]));
			break;
		case "test":
			unitTests();
			break;
		default:
			usage();
		}
	}
}
