package com.hourglassapps.cpi_ii.report.blacklist;

import java.net.MalformedURLException;
import java.net.URL;

import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Thrower;

class URLCanonicaliser implements Converter<URL,URL>, Thrower {
	private ConcreteThrower<MalformedURLException> mThrower=new ConcreteThrower<>();

	@Override
	public URL convert(URL url) {
		try {
			return new URL("http", url.getAuthority().toLowerCase(), url.getPort(), url.getFile());
		} catch(MalformedURLException e) {
			mThrower.ctch(e);
			return null;
		}
	}

	@Override
	public void close() throws Exception {
		mThrower.close();
	}

	@Override
	public <E extends Exception> void throwCaught(Class<E> pCatchable)
			throws Throwable {
		mThrower.throwCaught(pCatchable);
	}
	
	
}