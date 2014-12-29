package com.hourglassapps.cpi_ii.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hourglassapps.cpi_ii.report.LineGenerator.Line;
import com.hourglassapps.cpi_ii.report.LineGenerator.LineType;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Filter;
import com.hourglassapps.util.Rtu;

public class QueryGenerator implements Converter<Line,List<String>> {
	private final Filter<Line> mOneWordLineSpotter;
	
	public QueryGenerator(Filter<Line> pOneWordLineSpotter) {
		mOneWordLineSpotter=pOneWordLineSpotter;
	}
	
	private String join(Line pFst, Line pSnd) {
		if(pFst!=null && pFst.type()!=LineType.TITLE && pSnd!=null && pSnd.type()!=LineType.TITLE) {
			return (pFst.cleaned()+' '+pSnd.cleaned()).trim();
		}
		return "";
	}

	@Override
	public List<String> convert(Line pIn) {
		if(pIn.type()==LineType.TITLE) {
			Set<String> body=new HashSet<>();
			body.add(pIn.cleaned());
			Line l=pIn.next();
			while(l!=null) {
				if(l.type()!=LineType.BODY) {
					continue;
				}
				body.addAll(convert(l));
				l=l.next();
			}
			return new ArrayList<String>(body);
		} else {
			if(mOneWordLineSpotter.accept(pIn)) {
				List<String> phrases=new ArrayList<>();
				//single word line
				Line prev=pIn.prev();
				String clauseA=join(prev, pIn);

				Line next=pIn.next();
				String clauseB=join(pIn, next);

				if(!"".equals(clauseA)) {
					phrases.add(clauseA);
				} 
				if(!"".equals(clauseB)) {
					phrases.add(clauseB);
				} 
				return phrases;
			} else {
				if(pIn.cleaned().length()==0) {
					return Collections.emptyList();
				} else {
					return Collections.singletonList(pIn.cleaned());
				}
			}
		}
	}
}