package com.hourglassapps.cpi_ii;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;

import com.hourglassapps.serialise.JSONParser;
import com.hourglassapps.serialise.ParseException;
import com.hourglassapps.serialise.RemoveUnescapesReader;
import com.hourglassapps.util.Log;

public class MainIndexConductus {
	private final static String TAG=MainIndexConductus.class.getName();
	public final static String UNSTEMMED_2_EPRINT_INDEX="unstemmed_index";
	public final static String STEMMED_2_UNSTEMMED_INDEX="stemmed_index";
	
	private final ConductusIndex mUnstemmed2EprintIdIndex;
	private final ConductusIndex mStemmed2UnstemmedIndex;
	
	private File mInput;
	
	private boolean mListExpansions=false;
	private boolean mSerialiseExpansions=false;
	
	public MainIndexConductus(String pInput) throws IOException {
		mInput=new File(pInput);
		mUnstemmed2EprintIdIndex=new ConductusIndex(new File(UNSTEMMED_2_EPRINT_INDEX)).
				setTokenizer(new StandardLatinAnalyzer()).
				enableStemmer(false);

		mStemmed2UnstemmedIndex=new ConductusIndex(new File(STEMMED_2_UNSTEMMED_INDEX)).
				enableStemmer(true);
	}

	public MainIndexConductus setListExpansions(boolean pList) {
		mListExpansions=pList;
		return this;
	}
	
	public MainIndexConductus setSerialiseExpansions(boolean pSer) {
		mSerialiseExpansions=pSer;
		return this;
	}
	
	private void displayExpansions() throws IOException {
		final ResultRelayer stemExpansionsLister=new ResultRelayer() {

			@Override
			public void run(IndexReader pReader, TopDocs pResults)
					throws IOException {
				for(int docIdx=0; docIdx<pResults.scoreDocs.length; docIdx++) {
					String[] unstemmedTerms=pReader.document(pResults.scoreDocs[docIdx].doc).getValues(FieldVal.KEY.s());
					assert(unstemmedTerms.length==1);
					System.out.println(unstemmedTerms[0]);
				}
				System.out.println();
			}

		};

		mStemmed2UnstemmedIndex.visitTerms(new TermHandler(){

			@Override
			public void run(TermsEnum pTerms) throws IOException {
				BytesRef term;
				while((term=pTerms.next())!=null) {
					//term should be unstemmed
					String termStr=term.utf8ToString();
					mStemmed2UnstemmedIndex.interrogate(FieldVal.CONTENT, termStr, stemExpansionsLister);
				}
			}

		});
	}

	public void index() throws ParseException, IOException {
		indexById();
		indexByUnstemmed();

		if(mListExpansions) {
			boolean indexed=mStemmed2UnstemmedIndex.displayStemGroups();
			assert indexed;
		}
		
		if(mSerialiseExpansions) {
			displayExpansions();
		}
	}
	
	private void indexById() throws IOException, ParseException {
		try(
				Indexer indexer=new Indexer(mUnstemmed2EprintIdIndex);
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
				indexer.add(Long.toString(record.id()), record.content());
			}
		}
	}

	public void indexByUnstemmed() throws IOException {
		try(Indexer indexer=new Indexer(mStemmed2UnstemmedIndex)) {
			mUnstemmed2EprintIdIndex.visitTerms(new TermHandler(){

				@Override
				public void run(TermsEnum pTerms) throws IOException {
					BytesRef term;
					while((term=pTerms.next())!=null) {
						//assert term is unstemmed
						//assert term is an n-gram
						String termStr=term.utf8ToString();
						indexer.add(termStr, termStr);
					}
				}
				
			});
		}
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
			case "--display-ngrams":
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
