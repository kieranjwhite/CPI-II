package com.hourglassapps.cpi_ii.report;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.WeakHashMap;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.hourglassapps.cpi_ii.lucene.DocSpan;
import com.hourglassapps.util.Cache;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Rtu;

public final class TitlePathConverter implements Converter<Result,String> {
	private final Path mParent;
	private final ConcreteThrower<Exception> mThrower;
	private final JsonStringEncoder mEncoder=JsonStringEncoder.getInstance();
	/*
	private final Cache<String,String> mEncodingCache=new Cache<>(new Converter<String,String>(){

		@Override
		public String convert(String pIn) {
			return new String(mEncoder.quoteAsString(pIn));
		}
		
	});
	*/
	private final static Converter<DocSpan,String> DOCSPAN_TO_STRING=new Converter<DocSpan,String>(){

		@Override
		public String convert(DocSpan pIn) {
			return "{s:"+pIn.startOffset()+",e:"+pIn.endOffset()+"}";
		}
		
	};
	
	public TitlePathConverter(ConcreteThrower<Exception> pThrower) throws IOException {
		mThrower=pThrower;
		Path parent=MainReporter.DOCUMENT_DIR.getParent();
		if(parent==null) {
			parent=Paths.get(".");
		}
		mParent=parent.toRealPath();
	}

	private Path relativize(Path pIn) {
		return mParent.relativize(pIn);
	}
	
	private String toRelURL(Path pPath) {
		String s=pPath.toString();
		return s.replaceAll(File.separator, "/"); /* Since this is a relative path, we have control over all its constituent chars 
												   * so we know there are no other troublesome chars to worry about.
												   */
	}
	
	@Override
	public String convert(Result pResult) {
		String title=pResult.title();
		//String jsonified=mEncodingCache.get(title);
		String jsonified=new String(mEncoder.quoteAsString(title));
		try {
			return "{t:\""+jsonified+"\","+
					"p:\""+toRelURL(relativize(pResult.path().toRealPath()))+"\","+
					"s:["+listSpans(pResult.matches())+"]"+
					"},";
		} catch(IOException e) {
			mThrower.ctch(e);
		}
		return null;
	}

	private String listSpans(SortedSet<DocSpan> matches) {
		return Rtu.join(DOCSPAN_TO_STRING, matches, ",");
	}
	
}