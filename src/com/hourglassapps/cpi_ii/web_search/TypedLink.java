package com.hourglassapps.cpi_ii.web_search;

import java.net.URL;

import org.apache.commons.io.FilenameUtils;

import com.hourglassapps.util.Typed;

public class TypedLink implements Typed<URL> {
	private final URL mLink;
	
	public TypedLink(URL pLink) {
		mLink=pLink;
	}

	@Override
	public String extension() {
		String path=mLink.getPath();
		if("/".equals(path)) {
			return ".html";
		} else {
			String extension=FilenameUtils.getExtension(mLink.getPath());
			if("".equals(extension)) {
				return "";
			} else {
				return "."+extension;
			}
		}
	}

	@Override
	public URL get() {
		return mLink;
	}


}
