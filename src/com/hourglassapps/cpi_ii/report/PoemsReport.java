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
import com.hourglassapps.cpi_ii.report.LineGenerator.Line;
import com.hourglassapps.cpi_ii.report.LineGenerator.LineType;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.FileWrapper;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.Rtu;
import com.hourglassapps.util.Thrower;
import com.hourglassapps.util.URLUtils;

public class PoemsReport implements AutoCloseable {
	private final static String TAG=PoemsReport.class.getName();
	
	private final static String CSS="poem.css";
	private final static String FORWARD_HTML="fwd.html";
	private final static String RESULTS_HTML="result_list.html";
	private final static String BLACKLIST_JS="blacklist.js";
	private final static String POEMS_JS="poems.js";
	private final static String RESULTS_JS="result_list.js";
	private final static String WHEN_JS="when.js";
	private final static String JX_JS="jx_V3.01.A.js";
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
	private final Deferred<Void,Void,Ii<Line,String>> mDeferred=new DeferredObject<>();
	private final static Cleaner CLEANER=new Cleaner();
	private final Converter<Line,String> mQueryToFilename;
	private final Path mResultsDir;
	private final Thrower mThrower;
	
	public PoemsReport(Path pDest, Converter<Line,String> pQueryToFilename, Thrower pThrower) throws IOException {
		mThrower=pThrower;
		
		copy(CSS, pDest);
		copy(POEMS_JS, pDest);
		copy(FORWARD_HTML, pDest);
		copy(RESULTS_HTML, pDest);
		copy(RESULTS_JS, pDest);
		copy(BLACKLIST_JS, pDest);
		copy(WHEN_JS, pDest);
		copy(JX_JS, pDest);
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
	
	private String linkAndNotify(long pId, Line pLine) throws IOException {
		String key=mQueryToFilename.convert(pLine);
		Log.i(TAG, "file: "+key);
		if(key!=null) {
			mDeferred.notify(new Ii<Line,String>(pLine,key));
			return href(pId, pLine.text() ,key);
		}
		return pLine.text();

	}
	
	private String href(long pId, String pLine, String pLink) {
		return "<a href=\"fwd.html#"+pLink+"\" target=\"results_"+pId+"\">"+pLine+"</a>";
	}
	
	private void addContent(PrintWriter pOut, PoemRecord pPoemRecord) throws IOException {
		long id=pPoemRecord.id();
		LineGenerator builder=new LineGenerator(id, CLEANER);
		if(pPoemRecord.getTitle()!=null) {
			builder.addLine(pPoemRecord.getTitle(), LineType.TITLE);
		}
		List<String> ref=pPoemRecord.refrain();
		if(ref.size()>0) {
			for(String l: ref) {
				builder.addLine(l, LineType.BODY);
			}
		}
		
		List<StanzaText> stanzas=pPoemRecord.stanzas();
		if(stanzas.size()>0) {
			for(StanzaText s: stanzas) {
				List<String> stanza=s.lines();
				if(stanza.size()>0) {
					for(String l: stanza) {
						builder.addLine(l, LineType.BODY);
					}
				}
				
			}
		}	
		
		int lineNum=0;
		pOut.println("<div data-role=\"page\" id=\"e"+pPoemRecord.id()+"\" data-theme=\"a\">");
		pOut.println("<div class=\"poem\">");
		if(pPoemRecord.getTitle()!=null) {
			Line title=builder.line(lineNum++);
			pOut.println(TITLE_TAGS.fst()+linkAndNotify(id, title)+TITLE_TAGS.snd());
		}
		if(ref.size()>0) {
			pOut.println(STANZA_TAGS.fst()+"Ref."+STANZA_TAGS.snd());
			pOut.println("<p>");
			for(String l: ref) {
				Line line=builder.line(lineNum++);
				pOut.println(linkAndNotify(id, line)+"<br>");
			}
			pOut.println("</p>");
		}
		
		if(stanzas.size()>0) {
			for(StanzaText s: stanzas) {
				pOut.println(STANZA_TAGS.fst()+s.name()+STANZA_TAGS.snd());
				List<String> stanza=s.lines();
				if(stanza.size()>0) {
					pOut.println("<p>");
					for(String l: stanza) {
						Line line=builder.line(lineNum++);
						pOut.println(linkAndNotify(id, line)+"<br>");
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
	
	public void genContent() throws Exception {
		PrintWriter out=mWrapper.insert(MID);
		for(PoemRecord rec: mPoems) {
			try {
				mThrower.throwCaught(Exception.class);
			} catch(Throwable t) {
				assert t instanceof Exception;
				throw (Exception)t;
			}
			addContent(out, rec);
		}
	}
	
	@Override
	public void close() throws IOException {
		mWrapper.close();
		mDeferred.resolve(null);
	}
	
	public Promise<Void,Void,Ii<Line,String>> promise() {
		return mDeferred;
	}
}
