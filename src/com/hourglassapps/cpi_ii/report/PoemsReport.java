package com.hourglassapps.cpi_ii.report;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.util.Version;
import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import com.hourglassapps.cpi_ii.PoemRecord;
import com.hourglassapps.cpi_ii.PoemRecord.StanzaText;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.FileWrapper;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Rtu;

public class PoemsReport implements AutoCloseable {
	private final static String START="poems_start";
	private final static String MID="poems_mid";
	private final static String END="poems_end";
	private final static Ii<String,String> TITLE_TAGS=new Ii<>("<h3>","</h3>");
	private final static Ii<String,String> STANZA_TAGS=new Ii<>("<h4 class=\"heading\">","</h4>");
	private final Writer mOut;
	private final FileWrapper mWrapper;
	private final List<PoemRecord> mPoems=new ArrayList<>();
	private final Analyzer mAnalyzer;
	private final Deferred<Void,Void,Ii<String,String>> mDeferred=new DeferredObject();
	private final static Converter<List<String>,String> TOKENS_TO_REF=new Converter<List<String>,String>(){
		private final List<String> mOutToks=new ArrayList<>();
		private final Pattern mNonLetters=Pattern.compile("[^a-z]");
		private final static String JOINT="_";
		
		@Override
		public String convert(List<String> pIn) {
			mOutToks.clear();
			for(String token: pIn) {
				Matcher m=mNonLetters.matcher(token);
				mOutToks.add(m.replaceAll(JOINT));
			}
			return Rtu.join(mOutToks, JOINT)+".html";
		}
		
	};
	public PoemsReport(Path pDest, Analyzer pAnalyser) throws IOException {
		mWrapper=new FileWrapper(PoemsReport.class, START, END, pDest);
		mOut=mWrapper.writer();
		mAnalyzer=pAnalyser;
	}

	public void addTitle(PoemRecord pRec) throws IOException {
		Ii<Long,String> idTitle=new Ii<>(pRec.id(), pRec.getTitle());
		mOut.write("<a href=\"#e"+pRec.id()+"\" data-role=\"button\"><div align=\"left\">"+
				idTitle.snd()+"</div></a>\n");
		mPoems.add(pRec);
	}

	private List<String> tokenise(String pLine, List<String> pOut) throws IOException {
		pOut.clear();
		try(Reader reader=new StringReader(pLine.trim());
			TokenStream tokens=mAnalyzer.tokenStream(null, reader);) {
			CharTermAttribute termAtt=tokens.addAttribute(CharTermAttribute.class);
			tokens.reset();
			while(tokens.incrementToken()) {
				pOut.add(termAtt.toString());
			}
		}
		return pOut;
	}
	
	private String linkAndNotify(String pLine, List<String> pTokens) throws IOException {
		tokenise(pLine, pTokens);
		String link=TOKENS_TO_REF.convert(pTokens);
		mDeferred.notify(new Ii<String,String>(pLine,link));
		return href(pLine,link);

	}
	
	private String href(String pLine, String pLink) {
		return "<a href=\""+pLink+"\">"+pLine+"</a>";
	}
	
	private void addContent(PrintWriter pOut, PoemRecord pPoemRecord, List<String> pTokens) throws IOException {
		pOut.println("<div data-role=\"page\" id=\"e"+pPoemRecord.id()+"\" data-theme=\"a\">");
		pOut.println("<div class=\"poem\">");
		if(pPoemRecord.getTitle()!=null) {
			String title=pPoemRecord.getTitle();
			pOut.println(TITLE_TAGS.fst()+linkAndNotify(pPoemRecord.getTitle(), pTokens)+TITLE_TAGS.snd());
			pTokens.clear();
		}
		List<String> ref=pPoemRecord.refrain();
		if(ref.size()>0) {
			pOut.println(STANZA_TAGS.fst()+"Ref."+STANZA_TAGS.snd());
			pOut.println("<p>");
			for(String l: ref) {
				pOut.println("<br>"+linkAndNotify(l, pTokens)+"</br>");
				pTokens.clear();
			}
			pOut.println("</p>");
		}
		
		List<StanzaText> stanzas=pPoemRecord.stanzas();
		if(stanzas.size()>0) {
			for(StanzaText s: stanzas) {
				pOut.println(STANZA_TAGS.fst()+s.name()+STANZA_TAGS.snd());
				List<String> stanza=s.lines();
				if(stanza.size()>0) {
					pOut.println("<p>");
					for(String l: stanza) {
						pOut.println("<br>"+linkAndNotify(l, pTokens)+"</br>");
						pTokens.clear();
					}
					pOut.println("</p>");
				}
				
			}
		}

		pOut.println("</div>");
		//pOut.println("<iframe id=\"links\" name=\"links_frame\" frameboder=\"0\" src=\"\" width=\"100%\"></iframe>");
		pOut.println("</div>");
		
	}
	
	@Override
	public void close() throws IOException {
		try{
			PrintWriter out=mWrapper.insert(MID);
			List<String> lines=new ArrayList<>();
			List<String> tokens=new ArrayList<>();
			for(PoemRecord rec: mPoems) {
				addContent(out, rec, lines);
			}
		} finally {
			mWrapper.close();
		}
		mDeferred.resolve(null);
	}
	
	public Promise<Void,Void,Ii<String,String>> promise() {
		return mDeferred;
	}
}
