package com.hourglassapps.cpi_ii.report;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.hourglassapps.cpi_ii.report.LineGenerator.Line;
import com.hourglassapps.cpi_ii.report.LineGenerator.LineType;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Filter;
import com.hourglassapps.util.Rtu;

public class QueryGenerator implements Converter<Line,String> {
	private final Filter<Line> mOneWordLineSpotter;
	
	public QueryGenerator(Filter<Line> pOneWordLineSpotter) {
		mOneWordLineSpotter=pOneWordLineSpotter;
	}
	
	private String join(Line pFst, Line pSnd) {
		String adjoined="";
		if(pFst!=null && pFst.type()!=LineType.TITLE) {
			adjoined+=pFst.cleaned()+' ';
		}
		if(pSnd!=null && pSnd.type()!=LineType.TITLE) {
			adjoined+=pSnd.cleaned();
		}
		return adjoined.trim();
	}

	@Override
	public String convert(Line pIn) {
		if(pIn.type()==LineType.TITLE) {
			Set<String> body=new HashSet<>();
			body.add("\""+pIn.cleaned()+"\"");
			Line l=pIn.next();
			while(l!=null) {
				if(l.type()!=LineType.BODY) {
					continue;
				}
				body.add(convert(l));
				l=l.next();
			}
			return Rtu.join(new ArrayList<>(body), " ");
		} else {
			if(mOneWordLineSpotter.accept(pIn)) {
				//single word line
				Line prev=pIn.prev();
				String clauseA=join(prev, pIn);

				Line next=pIn.next();
				String clauseB=join(pIn, next);

				String clauses="";
				if(!"".equals(clauseA)) {
					clauses+="\""+clauseA+"\" ";
				} 
				if(!"".equals(clauseB)) {
					clauses+="\""+clauseB+"\"";
				} 
				return clauses.trim();
			} else {
				return "\""+pIn.cleaned()+"\"";
			}
		}
	}
}