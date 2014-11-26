package com.hourglassapps.cpi_ii.web_search;

import java.net.URL;

import org.apache.commons.io.FilenameUtils;

import com.hourglassapps.util.Typed;

public class TypedLink implements Typed<URL> {
	public final static char FILE_EXTENSION_DELMITER='.';
	private final static int MAX_EXTENSION_LENGTH=64;
	private final URL mLink;
	
	public TypedLink(URL pLink) {
		mLink=pLink;
	}

	@Override
	public String extension() {
		String path=mLink.getPath();
		String extension=FilenameUtils.getExtension(path);
		if(extension.length()>MAX_EXTENSION_LENGTH) {
			return "";
		}
		if("".equals(extension)) {
			return "";
		} else {
			return FILE_EXTENSION_DELMITER+extension;
		}
	}

	@Override
	public URL get() {
		return mLink;
	}


}
