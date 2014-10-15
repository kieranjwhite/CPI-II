package com.hourglassapps.cpi_ii.stem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.stempel.StempelStemmer;

import com.hourglassapps.cpi_ii.stem.lucene.AbstractStempelFilter;
import com.hourglassapps.util.MultiMap;

public final class StempelRecorderFilter extends StemRecorderFilter {
	private final class StempelFilter extends AbstractStempelFilter implements PreFilter {
		private CharSequence mInputToken=null;
		
		public StempelFilter(TokenStream in, StempelStemmer stemmer) {
			super(in, stemmer);
		}


		public StempelFilter(TokenStream in, StempelStemmer stemmer,
				int minLength) {
			super(in, stemmer, minLength);
		}


		/** Returns the next input Token, after being stemmed */
		@Override
		public boolean incrementToken() throws IOException {
			if (input.incrementToken()) {
				mInputToken=termAtt;
				stem();
				return true;
			} else {
				return false;
			}
		}

		@Override
		public CharSequence priorToken() {
			assert mInputToken!=null;
			return mInputToken;
		}

	}

	public StempelRecorderFilter(TokenStream pInput) throws IOException {
		super(pInput);
	}


	public <C extends Set<String>> StempelRecorderFilter(TokenStream pInput,
			MultiMap<String, C, String> stem2Expansions) throws IOException {
		super(pInput, stem2Expansions);
	}

	@Override
	protected TokenFilter stemmer(TokenFilter pInput) throws IOException {
		try(InputStream in=new FileInputStream(new File("/tmp/stempel/model.out"))) {
			return new StempelFilter(pInput, new StempelStemmer(in));
		}
	}

}