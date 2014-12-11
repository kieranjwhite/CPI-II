package com.hourglassapps.cpi_ii.report;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Ii;

public final class TitlePathConverter implements Converter<Ii<String,Path>,String> {
	private final Path mParent;
	private final ConcreteThrower<Exception> mThrower;
	private final JsonStringEncoder mEncoder=JsonStringEncoder.getInstance();
	private final Set<SoftReference<String>> mTitles=new HashSet<>();
	private final Map<String,String> mTitleToJsonString=new WeakHashMap<>();
	
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
	
	@Override
	public String convert(Ii<String,Path> pTitlePath) {
		String title=pTitlePath.fst();
		String jsonified;
		if(mTitleToJsonString.containsKey(title)) {
			jsonified=mTitleToJsonString.get(title);
		} else {
			jsonified=new String(mEncoder.quoteAsString(title));
			mTitles.add(new SoftReference<String>(jsonified));
			mTitleToJsonString.put(title, jsonified);
		}
		try {
			return "{t:\""+jsonified+"\","
					+ "p:\""+relativize(pTitlePath.snd().toRealPath()).toString()+"\"},";
		} catch(IOException e) {
			mThrower.ctch(e);
		}
		return null;
	}
	
}