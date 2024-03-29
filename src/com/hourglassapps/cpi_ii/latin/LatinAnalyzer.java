package com.hourglassapps.cpi_ii.latin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

import com.hourglassapps.cpi_ii.NumeralFilter;
import com.hourglassapps.cpi_ii.stem.IdentityRecorderFilter;
import com.hourglassapps.cpi_ii.stem.SnowballRecorderFilter;
import com.hourglassapps.cpi_ii.stem.StemRecorderFilter;
import com.hourglassapps.cpi_ii.stem.StemRecorderFilter.Factory;
import com.hourglassapps.cpi_ii.stem.snowball.lucene.SnowballFilter;
import com.hourglassapps.cpi_ii.stem.StempelRecorderFilter;
import com.hourglassapps.serialise.AbstractSerialiser;
import com.hourglassapps.serialise.Serialiser;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.MultiMap;

/**
 * {@link Analyzer} for Latin.
 */
@SuppressWarnings("unused")
public abstract class LatinAnalyzer extends StopwordAnalyzerBase {
	private final static String TAG=LatinAnalyzer.class.getName();
	private final CharArraySet stemExclusionSet;

	/* File containing 92 Latin stopwords from the Perseus project:
	 * http://sourceforge.net/projects/perseus-hopper/files/perseus-hopper/hopper-20110527/hopper-source-20110527.tar.gz/download
	 */
	public final static String PERSEUS_STOPWORD_FILE = "/com/hourglassapps/cpi_ii/latin/la.stop";
	
	public final static String DEFAULT_STOPWORD_FILE = "/com/hourglassapps/cpi_ii/empty.stop";
	private StemRecorderFilter mRecorder;

	public final static Factory STEMPEL_RECORDER_FACTORY=new StemRecorderFilter.Factory() {

		@Override
		public StemRecorderFilter inst(TokenStream pInput) throws IOException {
			return new StempelRecorderFilter(pInput, true, new File("data/com/hourglassapps/cpi_ii/latin/stem/stempel/model.out"));
		}

	};

	public final static Factory SNOWBALL_RECORDER_FACTORY=new StemRecorderFilter.Factory() {

		@Override
		public StemRecorderFilter inst(TokenStream pInput) throws IOException {
			return new SnowballRecorderFilter(pInput, true, new LatinStemmer());
		}

	};

	public final static Factory IDENTITY_RECORDER_FACTORY=new StemRecorderFilter.Factory() {

		@Override
		public StemRecorderFilter inst(TokenStream pInput) throws IOException {
			return new IdentityRecorderFilter(pInput);
		}

	};

	private Factory mStemmerFactory=IDENTITY_RECORDER_FACTORY;
	private boolean mRealised=false;

	/**
	 * Returns an unmodifiable instance of the default stop words set.
	 * @return default stop words set.
	 */
	public static CharArraySet getDefaultStopSet(){
		return DefaultSetHolder.DEFAULT_STOP_SET;
	}

	/**
	 * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer class 
	 * accesses the static final set the first time.;
	 */
	private static class DefaultSetHolder {
		static final CharArraySet DEFAULT_STOP_SET;

		static {
			try {
				DEFAULT_STOP_SET = loadStopwordSet(true, 
						LatinAnalyzer.class, DEFAULT_STOPWORD_FILE, "#");
			} catch (IOException ex) {
				// default set should always be present as it is part of the
				// distribution (JAR)
				throw new RuntimeException("Unable to load default stopword set");
			}
		}
	}

	/**
	 * Builds an analyzer with the default stop words: {@link #DEFAULT_STOPWORD_FILE}.
	 */
	public LatinAnalyzer() {
		this(DefaultSetHolder.DEFAULT_STOP_SET);
	}

	/**
	 * Builds an analyzer with the given stop words.
	 *
	 * @param stopwords a stopword set
	 */
	public LatinAnalyzer(CharArraySet stopwords) {
		this(stopwords, CharArraySet.EMPTY_SET);
	}

	public LatinAnalyzer(String pStoplistFile) throws IOException {
		this(loadStopwordSet(true, LatinAnalyzer.class, pStoplistFile, "#"), CharArraySet.EMPTY_SET);
	}
	
	/**
	 * Builds an analyzer with the given stop words. If a non-empty stem exclusion set is
	 * provided this analyzer will add a {@link SetKeywordMarkerFilter} before
	 * stemming.
	 * 
	 * @param stopwords a stopword set
	 * @param stemExclusionSet a set of terms not to be stemmed
	 */
	public LatinAnalyzer(CharArraySet stopwords, CharArraySet stemExclusionSet) {
		super(stopwords);
		this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
	}

	public LatinAnalyzer setStemmer(Factory pStemmerFactory) {
		assert !mRealised;
		mStemmerFactory=pStemmerFactory;
		return this;
	}

	protected abstract Ii<Tokenizer, TokenStream> underlyingTokeniser(Reader pReader);
	
	/**
	 * Creates a
	 * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
	 * which tokenizes all the text in the provided {@link Reader}.
	 * 
	 * @return A
	 *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
	 *         built from an {@link StandardTokenizer} filtered with
	 *         {@link StandardFilter}, {@link LatinLowerCaseFilter}, {@link StopFilter}
	 *         , {@link SetKeywordMarkerFilter} if a stem exclusion set is
	 *         provided and {@link SnowballFilter}.
	 */
	@Override
	protected TokenStreamComponents createComponents(String fieldName,
			Reader reader) {
		mRealised=true;
		Ii<Tokenizer, TokenStream> srcResult=underlyingTokeniser(reader);
		try {
			mRecorder=mStemmerFactory.inst(srcResult.snd());
			return new TokenStreamComponents(srcResult.fst(), mRecorder);
		} catch(IOException e) {
			Log.e(TAG, e);
			return new TokenStreamComponents(srcResult.fst(), srcResult.snd());
		}
	}

	public boolean storeStems(OutputStream pSave) throws IOException {
		if(mRecorder!=null) {
			mRecorder.serialiser().save(pSave);
			return true;
		}
		return false;
	}

	public Serialiser<MultiMap<String, Set<String>, String>> expansionsSerialiser() {
		if(mRecorder!=null) {
			return mRecorder.serialiser();
		} else {
			return new AbstractSerialiser<MultiMap<String, Set<String>, String>>() {

				@Override
				public void save(OutputStream pOut) throws IOException {
					return;
				}

			};
		}
	}

	public Set<String> tokenExpansions(String pStem) {
		if(mRecorder!=null) {
			return mRecorder.expansions(pStem);
		} else {
			return Collections.emptySet();
		}
	}

	public boolean displayStemGroups() {
		if(mRecorder!=null) {
			mRecorder.displayGroups();
			return true;
		} else {
			return false;
		}
	}
}
