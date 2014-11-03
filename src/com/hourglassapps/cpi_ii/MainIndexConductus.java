package com.hourglassapps.cpi_ii;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import com.hourglassapps.cpi_ii.latin.LatinAnalyzer;
import com.hourglassapps.cpi_ii.latin.StandardLatinAnalyzer;
import com.hourglassapps.serialise.JSONParser;
import com.hourglassapps.serialise.ParseException;
import com.hourglassapps.serialise.RemoveUnescapesReader;
import com.hourglassapps.util.Log;

public class MainIndexConductus {
	private final static String TAG=MainIndexConductus.class.getName();
	private final static int NGRAM_LENGTH=3;
	public final static File UNSTEMMED_TERM_2_EPRINT_INDEX=new File("unstemmed_term_index");
	public final static File UNSTEMMED_2_EPRINT_INDEX=new File("unstemmed_index");
	public final static File UNSTEMMED_2_STEMMED_INDEX=new File("unstemmed_to_stemmed_index");
	
	private final LatinAnalyzer mNonStemmingAnalyser=new StandardLatinAnalyzer();
	private final LatinAnalyzer mStemmingAnalyser=new StandardLatinAnalyzer().setStemmer(LatinAnalyzer.STEMPEL_RECORDER_FACTORY);
	
	private final NGramAnalyzerBuilder mNonStemmingBuilder=new NGramAnalyzerBuilder(mNonStemmingAnalyser, NGRAM_LENGTH);
	private final NGramAnalyzerBuilder mStemmingBuilder=new NGramAnalyzerBuilder(mStemmingAnalyser, NGRAM_LENGTH);
	private final File mInput;
	
	private boolean mListExpansions=false;
	private boolean mSerialiseExpansions=false;
	
	public MainIndexConductus(String pInput) throws IOException {
		mInput=new File(pInput);
	}

	public MainIndexConductus setListExpansions(boolean pList) {
		mListExpansions=pList;
		return this;
	}
	
	public MainIndexConductus setSerialiseExpansions(boolean pSer) {
		mSerialiseExpansions=pSer;
		return this;
	}

	public void index() throws ParseException, IOException {
		try(Indexer idIndexer=new Indexer(UNSTEMMED_2_EPRINT_INDEX, mNonStemmingBuilder.build())) {
			indexById(idIndexer);
		}
		IndexViewer idIndexer=new Indexer(UNSTEMMED_2_EPRINT_INDEX, mNonStemmingBuilder.build());
		try (Indexer unstemmedIndexer=new Indexer(UNSTEMMED_2_STEMMED_INDEX, mStemmingBuilder.build(), true, false)) {
			indexByUnstemmed(idIndexer, unstemmedIndexer);
		}
		try(Indexer term2IdIndexer=new Indexer(UNSTEMMED_TERM_2_EPRINT_INDEX, mNonStemmingAnalyser)) {
			indexById(term2IdIndexer);
		}
		if(mListExpansions) {
			boolean indexed=mStemmingAnalyser.displayStemGroups();
			assert indexed;
		}
		
		if(mSerialiseExpansions) {
			boolean indexed=mStemmingAnalyser.storeStems(System.out);
			assert indexed;			
		}
	}
	
	private void indexById(Indexer pIndexer) throws IOException, ParseException {
		try(
				JSONParser<Long, String, PoemRecord> parser=new JSONParser<>(
						new RemoveUnescapesReader(
								new BufferedReader(
										new FileReader(mInput))), PoemRecord.class);
				) {
			while(parser.hasNext()) {
				PoemRecord record=parser.next();
				if(record==null) {
					//an exception will cause this -- it'll be thrown when parser is closed
					break;
				}
				if(record.ignore()) {
					continue;
				}
				pIndexer.add(Long.toString(record.id()), record.content());
			}
		}
	}

	public void indexByUnstemmed(final IndexViewer pIndexToVisit, final Indexer pIndexer) throws IOException {
			pIndexToVisit.visitTerms(new TermHandler(){

				@Override
				public void run(TermsEnum pTerms) {
					
					BytesRef term;
					try {
						while((term=pTerms.next())!=null) {
							//assert term is unstemmed
							//assert term is an n-gram
							String termStr=term.utf8ToString();
							pIndexer.add(termStr, termStr);
						}
					} catch (IOException e) {
						Log.e(TAG, e);
					}
				}
				
			});
	}

	public static void main(String[] args) {
		if(args.length<1 || args.length>3) {
			Log.e(TAG, "Must provide filename of JSON data and at most two args");
			System.exit(-1);
		}
		
		String inFile=null;
		boolean list=false, serialise=false;
		for(int i=0; i<args.length; i++) {
			String arg=args[i];
			switch(arg) {
			case "--display-words":
				list=true;
				break;
			case "--serialise":
				serialise=true;
				break;
			default:
				inFile=args[i];
			}
		}
		try {
			MainIndexConductus indexer=new MainIndexConductus(inFile)
			.setListExpansions(list).setSerialiseExpansions(serialise);
			indexer.index();
		} catch (ParseException | IOException e) {
			Log.e(TAG, e);
		}
	}
}
