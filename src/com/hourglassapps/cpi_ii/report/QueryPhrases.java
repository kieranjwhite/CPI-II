package com.hourglassapps.cpi_ii.report;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;

public class QueryPhrases {
	private final static String TAG=QueryPhrases.class.getName();
	
	private final List<String> mQueryPhrases;
	private final QueryParser mParser;
	private int[] mRankToDocId;
	private Path[] mRankToPath;
	private String[] mRankToTitle;
	private int mRank=0;
	private int mNumAnswers=0;
	private Accumulator<Integer> mDocIdHarvester;
	
	public QueryPhrases(QueryParser pParser, List<String> pQueryPhrases, Accumulator<Integer> pDocIdHarvester) {
		mQueryPhrases=pQueryPhrases;
		mParser=pParser;
		mDocIdHarvester=pDocIdHarvester;
	}
	
	public Query parse() throws ParseException {
		String query="\""+Rtu.join(mQueryPhrases, "\" \"")+"\"";
		Log.i(TAG, "query: "+query);
		if(!"".equals(query)) {
			Query q=mParser.parse(query);
			return q;
		}
		return null;
	}

	public void startAnswering(int pNumAnswers) {
		mNumAnswers=pNumAnswers;
		mRankToDocId=new int[pNumAnswers];
		mRankToPath=new Path[pNumAnswers];
		mRankToTitle=new String[pNumAnswers];
	}

	public void answer(int pDocId, Path pPath, String pTitle) {
		if(mRank>=mNumAnswers) {
			throw new IllegalStateException("never invoked startAnswering or to answer invoked too many times");
		}
		
		mDocIdHarvester.add(pDocId);
		mRankToDocId[mRank]=pDocId;
		mRankToPath[mRank]=pPath;
		mRankToTitle[mRank]=pTitle;
		
		mRank++;
	}
}
