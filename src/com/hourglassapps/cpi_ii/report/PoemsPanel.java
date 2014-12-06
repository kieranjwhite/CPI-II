package com.hourglassapps.cpi_ii.report;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.hourglassapps.cpi_ii.PoemRecord;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.FileWrapper;
import com.hourglassapps.util.Ii;

public class PoemsPanel implements AutoCloseable {
	private final static String START="poems_start";
	private final static String MID="poems_mid";
	private final static String END="poems_end";
	private final Writer mOut;
	private final FileWrapper mWrapper;
	private final List<PoemRecord> mPoems=new ArrayList<>();
	
	public PoemsPanel(Path pDest) throws IOException {
		mWrapper=new FileWrapper(PoemsPanel.class, START, END, pDest);
		mOut=mWrapper.writer();
	}

	public void addTitle(PoemRecord pRec) throws IOException {
		Ii<Long,String> idTitle=new Ii<>(pRec.id(), pRec.getTitle());
		mOut.write("<a href=\"#e"+pRec.id()+"\" data-role=\"button\"><div align=\"left\">"+
				idTitle.snd()+"</div></a>\n");
		mPoems.add(pRec);
	}

	private void addContent(PrintWriter pOut, PoemRecord pPoemRecord) {
		pOut.println("<div data-role=\"page\" id=\"e"+pPoemRecord.id()+"\" data-theme=\"a\">");
		pOut.println("<div class=\"poem\">");
		for(String l: pPoemRecord.lines()) {
			pOut.println(l);
		}
		pOut.println("</div>");
		//pOut.println("<iframe id=\"links\" name=\"links_frame\" frameboder=\"0\" src=\"\" width=\"100%\"></iframe>");
		pOut.println("</div>");
	}
	
	@Override
	public void close() throws IOException {
		try{
			PrintWriter out=mWrapper.insert(MID);
			for(PoemRecord rec: mPoems) {
				addContent(out, rec);
			}
		} finally {
			mWrapper.close();
		}
	}
}
