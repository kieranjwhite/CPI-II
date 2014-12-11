package com.hourglassapps.cpi_ii.report;

import java.io.IOException;
import java.io.InputStream;
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
	private final static String CSS="poem.css";
	private final static String FORWARD_HTML="fwd.html";
	private final static String RESULTS_HTML="result_list.html";
	private final static String POEMS_JS="poems.js";
	private final static String RESULTS_JS="result_list.js";
	private final static String RTU_JS="rtu.js";
	private final static String RTU_DOMLESS_JS="rtu_domless.js";
	private final static String POEM_PANE_NAME="poems.html";
	private final static String RESULTS_DIR="results"; //If this is changed then the corresponding string in result_list.html must be changed
	private final static String START="poems_start";
	private final static String MID="poems_mid";
	private final static String END="poems_end";
	private final static Ii<String,String> TITLE_TAGS=new Ii<>("<h3>","</h3>");
	private final static Ii<String,String> STANZA_TAGS=new Ii<>("<h4 class=\"heading\">","</h4>");
	private final Writer mOut;
	private final FileWrapper mWrapper;
	private final List<PoemRecord> mPoems=new ArrayList<>();
	private final Deferred<Void,Void,Ii<String,String>> mDeferred=new DeferredObject<>();
	private final static Cleaner CLEANER=new Cleaner();
	private final Converter<String,String> mQueryToFilename;
	private final Path mResultsDir;
	
	public PoemsReport(Path pDest, Converter<String,String> pQueryToFilename) throws IOException {
		copy(CSS, pDest);
		copy(POEMS_JS, pDest);
		copy(FORWARD_HTML, pDest);
		copy(RESULTS_HTML, pDest);
		copy(RESULTS_JS, pDest);
		copy(RTU_JS, pDest);
		copy(RTU_DOMLESS_JS, pDest);
		
		mResultsDir=pDest.resolve(RESULTS_DIR);
		mQueryToFilename=pQueryToFilename;
		mWrapper=new FileWrapper(PoemsReport.class, START, END, pDest.resolve(POEM_PANE_NAME));
		mOut=mWrapper.writer();
	}

	private void copy(String pSrc, Path pDest) throws IOException {
		try(InputStream in=MainReporter.class.getResourceAsStream(pSrc)) {
			Rtu.copyFile(in, pDest.resolve(pSrc));
		}
	}
	
	public void addTitle(PoemRecord pRec) throws IOException {
		Ii<Long,String> idTitle=new Ii<>(pRec.id(), pRec.getTitle());
		mOut.write("<a href=\"#e"+pRec.id()+"\" data-role=\"button\"><div align=\"left\">"+
				idTitle.snd()+"</div></a>\n");
		mPoems.add(pRec);
	}
	
	private String linkAndNotify(long pId, String pLine) throws IOException {
		//tokenise(pLine, pTokens);
		String cleaned=CLEANER.convert(pLine);
		String key=mQueryToFilename.convert(cleaned);
		if(key!=null) {
			mDeferred.notify(new Ii<String,String>(cleaned,key));
			return href(pId, pLine,key);
		}
		return pLine;

	}
	
	private String href(long pId, String pLine, String pLink) {
		return "<a href=\"fwd.html#"+pLink+"\" target=\"results_"+pId+"\">"+pLine+"</a>";
	}
	
	private void addContent(PrintWriter pOut, PoemRecord pPoemRecord) throws IOException {
		pOut.println("<div data-role=\"page\" id=\"e"+pPoemRecord.id()+"\" data-theme=\"a\">");
		pOut.println("<div class=\"poem\">");
		long id=pPoemRecord.id();
		if(pPoemRecord.getTitle()!=null) {
			pOut.println(TITLE_TAGS.fst()+linkAndNotify(id, pPoemRecord.getTitle())+TITLE_TAGS.snd());
		}
		List<String> ref=pPoemRecord.refrain();
		if(ref.size()>0) {
			pOut.println(STANZA_TAGS.fst()+"Ref."+STANZA_TAGS.snd());
			pOut.println("<p>");
			for(String l: ref) {
				pOut.println(linkAndNotify(id, l)+"<br>");
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
						pOut.println(linkAndNotify(id, l)+"<br>");
					}
					pOut.println("</p>");
				}
				
			}
		}

		pOut.println("</div>");
		
		//pOut.println("<div id=\"wrapper\" width=\"100%\" height=\"100%\"><iframe src=\"\" name=\"results\" frameborder=\"0\" seamless width=\"100%\" height=\"100%\"/></div>");
		//pOut.println("<iframe src=\"\" name=\"results_"+pPoemRecord.id()+"\" frameborder=\"0\" seamless width=\"100%\"></iframe>");
		//pOut.println("<iframe id=\"links\" name=\"links_frame\" frameborder=\"0\" src=\"\" width=\"100%\"></iframe>");
		pOut.println("</div>");
		
	}
	
	public Path resultsDir() {
		return mResultsDir;
	}
	
	public void genContent() throws IOException {
		PrintWriter out=mWrapper.insert(MID);
		for(PoemRecord rec: mPoems) {
			addContent(out, rec);
		}
	}
	
	@Override
	public void close() throws IOException {
		mWrapper.close();
		mDeferred.resolve(null);
	}
	
	public Promise<Void,Void,Ii<String,String>> promise() {
		return mDeferred;
	}
}
