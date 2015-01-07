package com.hourglassapps.persist;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;

import thredds.cataloggen.StandardCatalogBuilder;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.hourglassapps.util.Log;

public class MainHashTagDict {
	private final static String TAG=MainHashTagDict.class.getName();
	private final static ThreadLocal<JsonStringEncoder> JSON_ENCODER=new ThreadLocal<JsonStringEncoder>();
	private final static String PREFIX="J"; //html 4 requires that a hashtag id begins with a latin letter
	private StringBuilder mDict=new StringBuilder("{");
	private boolean mFst=true;
	
	public MainHashTagDict() {
		if(JSON_ENCODER.get()==null) {
			JSON_ENCODER.set(JsonStringEncoder.getInstance());
		}
	}

	public String toString() {
		return mDict.toString();
	}
	
	private void addKey(String pKey) {
		if(!mFst) {
			mDict.append(',');
		} else {
			mFst=false;
		}
		mDict.append('"').
			append(JSON_ENCODER.get().quoteAsString(pKey)).append('"').
			append(':');		
	}
	
	public void put(String pKey, String pVal) {
		addKey(pKey);
		if(pVal!=null) {
			mDict.append('"').
			append(JSON_ENCODER.get().quoteAsString(pVal)).
			append('"');
		} else {
			mDict.append("null");
		}
	}
	
	public void put(String pKey, int pVal) {
		addKey(pKey);
		mDict.append(pVal);		
	}
	
	/**
	 * Encodes the key, value pairs in a way that can be passed as a html hash id tag. 
	 * There is a corresponding javascript decode function, decodeHashId, in result_list.js
	 * @return Encoded key, value pairs
	 */
	public String encode() {
		mDict.append("}");
		String json=new String(mDict.toString());
		
		/*
		 * There's also Base64.encodeBase64URLSafe which would avoid the need for handling
		 * padding characters and transliterating + and /, however I'm not convinced all
		 * decoders could handle it.
		 */
		String encoded=Base64.encodeBase64String(json.getBytes(StandardCharsets.UTF_8));

		/* Base64 encoded text can end with zero to two '=' characters and these are not allowed
		 * in a html 4 hash id, so we strip them out and encode them in another way.
		 */
		
		int fstEq=encoded.indexOf('=');
		if(fstEq==-1) {
			fstEq=encoded.length();
		}
		int numEqs=encoded.length()-fstEq;
		assert numEqs<=2;
		String transliterated=encoded.substring(0,fstEq).replace('+', '-').replace('/', '_'); // + and / are not valid characters in a html4 hash id

		return PREFIX+numEqs+transliterated;
		
	}
	
	public static String decodeToStr(String pEncoded) {
		int padding_cnt=Integer.valueOf(pEncoded.substring(1,2));
		String stripped=pEncoded.substring(2);
		StringBuilder suffix=new StringBuilder();
		for(int eqIdx=0; eqIdx<padding_cnt; eqIdx++) {
			suffix.append('=');
		}
		String base64Encoded=(stripped+suffix.toString()).replace('_','/').replace('-','+');
		byte[] json=Base64.decodeBase64(base64Encoded);
		String decoded=new String(json,StandardCharsets.UTF_8);
		return decoded;
	}
	
	public static void main(String[] pArgs) {
		/*
		 * If there is no argument a (very) simple unit test will be run.
		 * Otherwise the first argument will be decoded and displayed. The argument should included all characters in the hash id after (but not including) the '#' character.
		 */
		String encoded;
		if(pArgs.length==0) {
			MainHashTagDict dict=new MainHashTagDict();
			//create the json string {t:"Entire: Ab+angelo+Maria+salutata",f:"Ab+angelo+Maria+salutata"}
			dict.put("t", "Entire: Ab+angelo+Maria+salutata");
			dict.put("f", "Ab+angelo+Maria+salutata");

			encoded=dict.encode();
		} else {
			encoded=pArgs[0];
		}
		String decoded=decodeToStr(encoded);
		if(pArgs.length==0 && !decoded.equals("{t:\"Entire: Ab+angelo+Maria+salutata\",f:\"Ab+angelo+Maria+salutata\"}")) {
			Log.e(TAG, "failed encode/decode: "+decoded);
		} else {
			Log.i(TAG, "decoded pArgs: "+pArgs[0]+" as: "+decoded);
		}
	}
}
