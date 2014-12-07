package com.hourglassapps.cpi_ii.report;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import com.hourglassapps.cpi_ii.PoemRecord;
import com.hourglassapps.cpi_ii.PoemRecord.StanzaText;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.FileWrapper;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.URLUtils;

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
	private final Deferred<Void,Void,Ii<String,String>> mDeferred=new DeferredObject<>();
	private final static Cleaner CLEANER=new Cleaner();
	private final Converter<String,String> mQueryToFilename;

	public PoemsReport(Path pDest, Analyzer pAnalyser, Converter<String,String> pQueryToFilename) throws IOException {
		mQueryToFilename=pQueryToFilename;
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
	
	private String linkAndNotify(String pLine) throws IOException {
		//tokenise(pLine, pTokens);
		String cleaned=CLEANER.convert(pLine);
		String key=mQueryToFilename.convert(cleaned);
		if(key!=null) {
			String link=key+".html";
			mDeferred.notify(new Ii<String,String>(cleaned,key));
			return href(pLine,link);
		}
		return pLine;

	}
	
	private String href(String pLine, String pLink) {
		return "<a href=\""+pLink+"\">"+pLine+"</a>";
	}
	
	private void addContent(PrintWriter pOut, PoemRecord pPoemRecord) throws IOException {
		pOut.println("<div data-role=\"page\" id=\"e"+pPoemRecord.id()+"\" data-theme=\"a\">");
		pOut.println("<div class=\"poem\">");
		if(pPoemRecord.getTitle()!=null) {
			String title=pPoemRecord.getTitle();
			pOut.println(TITLE_TAGS.fst()+linkAndNotify(pPoemRecord.getTitle())+TITLE_TAGS.snd());
		}
		List<String> ref=pPoemRecord.refrain();
		if(ref.size()>0) {
			pOut.println(STANZA_TAGS.fst()+"Ref."+STANZA_TAGS.snd());
			pOut.println("<p>");
			for(String l: ref) {
				pOut.println("<br>"+linkAndNotify(l)+"</br>");
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
						pOut.println("<br>"+linkAndNotify(l)+"</br>");
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
			List<String> tokens=new ArrayList<>();
			for(PoemRecord rec: mPoems) {
				addContent(out, rec);
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
