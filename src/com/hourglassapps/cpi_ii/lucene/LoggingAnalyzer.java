package com.hourglassapps.cpi_ii.lucene;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.hourglassapps.cpi_ii.stem.IdentityFilter;
import com.hourglassapps.util.Log;

public class LoggingAnalyzer extends AnalyzerWrapper {
	private final static String TAG=LoggingAnalyzer.class.getName();
	private Analyzer mDelegate;
	public LoggingAnalyzer(Analyzer pDelegate) {
		super();
		mDelegate=pDelegate;
	}
	
	private final static class LoggingFilter extends TokenFilter {
		private final CharTermAttribute mTermAtt = addAttribute(CharTermAttribute.class);
		
		protected LoggingFilter(TokenStream input) {
			super(input);
		}

		@Override
		public boolean incrementToken() throws IOException {
			if (input.incrementToken()) {
				char termBuffer[] = mTermAtt.buffer();
				final int length = mTermAtt.length();
				String term=new String(termBuffer, 0, length);
				Log.v(TAG, "token: "+term);
				return true;
			} else {
				return false;
			}
		}

	}
	
	@Override
	protected Analyzer getWrappedAnalyzer(String fieldName) {
		return mDelegate;
	}

	@Override
	protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
		LoggingFilter filter = new LoggingFilter(components.getTokenStream());
		return new TokenStreamComponents(components.getTokenizer(), filter);
	}

}
