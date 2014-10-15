package com.hourglassapps.cpi_ii.stem;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.hourglassapps.serialise.AbstractSerialiser;
import com.hourglassapps.serialise.Serialiser;
import com.hourglassapps.util.HashSetMultiMap;
import com.hourglassapps.util.MultiMap;

public abstract class StemRecorderFilter extends TokenFilter {
	private final Underlying mUnderlying;
	private final TokenStream mStemmer;
	private final MultiMap<String, Set<String>, String> mStem2Expansions;
	private final CharTermAttribute termAtt=addAttribute(CharTermAttribute.class);

	public StemRecorderFilter(TokenStream pInput) throws IOException {
		super(pInput);
		mStem2Expansions=new HashSetMultiMap<String, String>();
		mUnderlying=new Underlying(pInput);
		mStemmer=stemmer(mUnderlying);
	}

	public <C extends Set<String>> StemRecorderFilter(TokenStream pInput, MultiMap<String,C,String> stem2Expansions) throws IOException {
		super(pInput);
		mStem2Expansions=new HashSetMultiMap<String, String>(stem2Expansions);
		mUnderlying=new Underlying(pInput);
		mStemmer=stemmer(mUnderlying);
	}

	private final static class Underlying extends TokenFilter implements PreFilter {
		private final CharTermAttribute termAtt=addAttribute(CharTermAttribute.class);
		private String beforeStemming;
		
		protected Underlying(TokenStream input) {
			super(input);
		}

		@Override
		public CharSequence priorToken() {
			assert beforeStemming!=null;
			return beforeStemming;
		}

		@Override
		public boolean incrementToken() throws IOException {
			boolean result=input.incrementToken();
			beforeStemming=termAtt.toString();
			return result;
		}
		
	}
	
	protected abstract TokenFilter stemmer(TokenFilter pInput) throws IOException;
	
	@Override
	public boolean incrementToken() throws IOException {
		if(mStemmer.incrementToken()) {
			mStem2Expansions.addOne(termAtt.toString(), mUnderlying.priorToken().toString());
			return true;
		} else {
			return false;
		}
	}

	public Serialiser<MultiMap<String, Set<String>, String>> serialiser() {
		return new AbstractSerialiser<MultiMap<String, Set<String>, String>>() {

			@Override
			public void save(OutputStream pOut) throws IOException {
				try(DataOutputStream saver=new DataOutputStream(pOut)) {
					Set<String> keys=mStem2Expansions.keySet();
					int numKeys=keys.size();
					saver.writeInt(numKeys);
					for(String k: keys) {
						saver.writeUTF(k);
						Set<String> expansions=mStem2Expansions.get(k);
						saver.writeInt(expansions.size());
						for(String ex: expansions) {
							saver.writeUTF(ex);
						}
					}
				};
			}

			@Override
			public MultiMap<String, Set<String>, String> restore(InputStream pIn) throws IOException {
				try(DataInputStream loader=new DataInputStream(pIn)) {
					MultiMap<String, Set<String>, String> stem2Expansions=new HashSetMultiMap<String, String>();
					int numKeys=loader.readInt();
					for(int keyIdx=0; keyIdx<numKeys; keyIdx++) {
						String key=loader.readUTF();
						int numExpansions=loader.readInt();
						for(int expIdx=0; expIdx<numExpansions; expIdx++) {
							stem2Expansions.addOne(key, loader.readUTF());
						}
					}
					return stem2Expansions;
				}
			}
		};
	}

	public void displayGroups() {
		Set<String> keys=mStem2Expansions.keySet();
		for(String k: keys) {
			Set<String> expansions=mStem2Expansions.get(k);
			for(String ex: expansions) {
				System.out.println(ex);
			}
			System.out.println();
		}
	}
}
