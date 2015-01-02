package com.hourglassapps.cpi_ii.report;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import com.hourglassapps.cpi_ii.report.LineGenerator.Line;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;

public class QueryPhrases implements Comparable<QueryPhrases> {
	private final static String TAG=QueryPhrases.class.getName();
	
	private final List<String> mQueryPhrases;
	private final QueryParser mParser;
	private Answers mAnswers=null;
	private final Ii<Line,String> mLineDst;
	
	public QueryPhrases(QueryParser pParser, List<String> pQueryPhrases, Ii<Line,String> pLineDst) {
		mQueryPhrases=Collections.unmodifiableList(pQueryPhrases);
		mParser=pParser;
		mLineDst=pLineDst;
	}
	
	@Override
	public String toString() {
		return mLineDst.toString();
	}
	
	public Line line() {
		return mLineDst.fst();
	}
	
	public String dst() {
		return mLineDst.snd();
	}
	
	public List<String> phrases() {
		return mQueryPhrases;
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

	public Answers answers() {
		return mAnswers;
	}
	
	public boolean answered() {
		return mAnswers!=null;
	}
	
	public Answers startAnswering(int pNumAnswers, Accumulator<Integer> pDocIdHarvester) {
		if(mAnswers!=null) {
			return null; //can occur if there are duplicate lines in a poem
		}
		
		mAnswers=new Answers(pNumAnswers, pDocIdHarvester);
		return mAnswers;
	}

	public class Answers {
		private final int[] mRankToDocId;
		private final Path[] mRankToPath;
		private final String[] mRankToTitle;
		private int mSubmitted=0;
		private final int mNumAnswers;
		private final Accumulator<Integer> mDocIdHarvester;
		private final Map<Integer,DocResult> mDocIds=new HashMap<>();

		public Answers(int pNumAnswers, Accumulator<Integer> pDocIdHarvester) {
			mNumAnswers=pNumAnswers;
			mRankToDocId=new int[pNumAnswers];
			mRankToPath=new Path[pNumAnswers];
			mRankToTitle=new String[pNumAnswers];
			mDocIdHarvester=pDocIdHarvester;			
		}
		
		public void answer(int pDocId, Path pPath, String pTitle) {
			if(mSubmitted>=mNumAnswers) {
				throw new IllegalStateException("never invoked startAnswering or to answer invoked too many times");
			}
			
			mDocIds.put(pDocId, new DocResult());
			mDocIdHarvester.add(pDocId);
			mRankToDocId[mSubmitted]=pDocId;
			mRankToPath[mSubmitted]=pPath;
			mRankToTitle[mSubmitted]=pTitle;
			
			mSubmitted++;
		}
		
		public List<Result> results() {
			if(mSubmitted!=mNumAnswers) {
				throw new IllegalStateException("Now all answers have been submitted");
			}
			List<Result> results=new ArrayList<>();
			for(int rank=0; rank<mSubmitted; rank++) {
				results.add(new Result(mRankToTitle[rank], mRankToPath[rank], mDocIds.get(mRankToDocId[rank]).merge()));
			}
			return results;
		}
		
		public DocResult docResult(int pDocId) {
			if(mSubmitted!=mNumAnswers) {
				throw new IllegalStateException("Now all answers have been submitted");
			}
			return mDocIds.get(pDocId);
		}
	}

	public String firstPhrase() {
		if(mQueryPhrases.size()==0) {
			return null;
		}
		return mQueryPhrases.get(0);
	}

	
	@Override
	public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        }
        if (pOther == null) {
            return false;
		}
        if (!(pOther instanceof QueryPhrases)) {
        	return false;
        }
        
        QueryPhrases other=(QueryPhrases)pOther;
        return mLineDst.snd().equals(other.mLineDst.snd());
	}

	@Override
	public int hashCode() {
		return mLineDst.snd().hashCode();
	}

	@Override
	public int compareTo(QueryPhrases pOther) {
		if (this==pOther) {
			return 0;
		}
		if (pOther==null) {
			return 1;
		}

		String myDst=mLineDst.snd();
		String oDst=pOther.mLineDst.snd();
		
		assert myDst!=null;
		assert oDst!=null;
		
		return myDst.compareTo(oDst);

	}

	
}
