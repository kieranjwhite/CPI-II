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
import java.io.Reader;

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
import com.hourglassapps.util.Log;

/**
 * {@link Analyzer} for Latin.
 */
@SuppressWarnings("unused")
public final class LatinAnalyzer extends StopwordAnalyzerBase {
	private final static String TAG=LatinAnalyzer.class.getName();
  private final CharArraySet stemExclusionSet;
  
  /** File containing default Latin stopwords. */
  public final static String DEFAULT_STOPWORD_FILE = "/com/hourglassapps/cpi_ii/latin/la.stop";
  private StemRecorderFilter mRecorder;
  
  private final static Factory STEMPEL_RECORDER_FACTORY=new StemRecorderFilter.Factory() {

	@Override
	public StemRecorderFilter inst(TokenStream pInput) throws IOException {
		return new StempelRecorderFilter(pInput, new File("data/com/hourglassapps/cpi_ii/latin/stem/stempel/model.out"));
	}
	  
  };
  
  private final static Factory SNOWBALL_RECORDER_FACTORY=new StemRecorderFilter.Factory() {

	@Override
	public StemRecorderFilter inst(TokenStream pInput) throws IOException {
		return new SnowballRecorderFilter(pInput, new LatinStemmer());
	}
	  
  };
  
  //Set this as the DEFAULT_STEMMER_FACTORY to disable stemming
  private final static Factory IDENTITY_RECORDER_FACTORY=new StemRecorderFilter.Factory() {

	@Override
	public StemRecorderFilter inst(TokenStream pInput) throws IOException {
		return new IdentityRecorderFilter(pInput);
	}
	  
  };
  
  private final static Factory DEFAULT_STEMMER_FACTORY=STEMPEL_RECORDER_FACTORY;
  
  /*
   * TODO consider for Latin
  private static final CharArraySet DEFAULT_ARTICLES = CharArraySet.unmodifiableSet(
      new CharArraySet(
          Arrays.asList(
              "d", "m", "b"
          ), true));
  */
  
  /** TODO consider for Latin
   * When StandardTokenizer splits t‑athair into {t, athair}, we don't
   * want to cause a position increment, otherwise there will be problems
   * with phrase queries versus tAthair (which would not have a gap).
   *
  private static final CharArraySet HYPHENATIONS = CharArraySet.unmodifiableSet(
      new CharArraySet(
          Arrays.asList(
              "h", "n", "t"
          ), true));
  */
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
   * @deprecated Use {@link #LatinAnalyzer()}
   */
  @Deprecated
  public LatinAnalyzer(Version matchVersion) {
    this(matchVersion, DefaultSetHolder.DEFAULT_STOP_SET);
  }
  
  /**
   * Builds an analyzer with the given stop words.
   *
   * @param stopwords a stopword set
   */
  public LatinAnalyzer(CharArraySet stopwords) {
    this(stopwords, CharArraySet.EMPTY_SET);
  }

  /**
   * @deprecated Use {@link #LatinAnalyzer(CharArraySet)}
   */
  @Deprecated
  public LatinAnalyzer(Version matchVersion, CharArraySet stopwords) {
    this(matchVersion, stopwords, CharArraySet.EMPTY_SET);
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

  /**
   * @deprecated Use {@link #LatinAnalyzer(CharArraySet,CharArraySet)}
   */
  @Deprecated
  public LatinAnalyzer(Version matchVersion, CharArraySet stopwords, CharArraySet stemExclusionSet) {
    super(matchVersion, stopwords);
    this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(
        matchVersion, stemExclusionSet));
  }

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
    final Tokenizer source = new StandardTokenizer(getVersion(), reader);
    TokenStream result = new StandardFilter(getVersion(), source);
    /* TODO consider for Latin kw
    StopFilter s = new StopFilter(getVersion(), result, HYPHENATIONS);
    if (!getVersion().onOrAfter(Version.LUCENE_4_4)) {
      s.setEnablePositionIncrements(false);
    }
    result = s;
    */
    /* TODO consider for Latin kw
     * result = new ElisionFilter(result, DEFAULT_ARTICLES);
     */
    result=new NumeralFilter(result);
    result = new LatinLowerCaseFilter(result);
    result = new StopFilter(getVersion(), result, stopwords);
    if(!stemExclusionSet.isEmpty()) {
      result = new SetKeywordMarkerFilter(result, stemExclusionSet);
    }
    try {
    	mRecorder=DEFAULT_STEMMER_FACTORY.inst(result);
    	return new TokenStreamComponents(source, mRecorder);
    } catch(IOException e) {
    	Log.e(TAG, e);
    	return new TokenStreamComponents(source, result);
    }
  }
  
  public boolean storeStems(File pSaveFile) throws IOException {
	  if(mRecorder!=null) {
		  mRecorder.serialiser().save(pSaveFile);
		  return true;
	  }
	  return false;
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