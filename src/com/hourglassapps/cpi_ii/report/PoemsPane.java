package com.hourglassapps.cpi_ii.report;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.hourglassapps.util.Converter;
import com.hourglassapps.util.Ii;
import com.hourglassapps.util.Rtu;

public class PoemsPane implements AutoCloseable {
	private final Path mPath;
	private final static String FIRST_HALF="poems_start";
	private final static String SECOND_HALF="poems_end";
	private final Writer mOut;
	private final Converter<Ii<Long,String>,String> mIdTitleToRelURL;
	
	public PoemsPane(Path pDest, Converter<Ii<Long,String>,String> pIdTitleToRelURL) throws IOException {
		mPath=pDest;
		if(Files.exists(mPath)) {
			Files.delete(mPath);
		}
		OutputStream outStream=new BufferedOutputStream(new FileOutputStream(mPath.toFile()));
		try(InputStream in=PoemsPane.class.getResourceAsStream(FIRST_HALF)) {
			Rtu.copyFile(in, outStream);
		}
		mOut=new PrintWriter(outStream);
		mIdTitleToRelURL=pIdTitleToRelURL;
	}


	
	public void add(Ii<Long, String> pIdTitle) throws IOException {
		mOut.write("<a href=\""+mIdTitleToRelURL.convert(pIdTitle)+"\" data-role=\"button\"><div align=\"left\">"+pIdTitle.snd()+"</div></a>\n");
	}

	@Override
	public void close() throws IOException {
		mOut.close();
		try(
				OutputStream outStream=new BufferedOutputStream(new FileOutputStream(mPath.toFile(), true));
				InputStream in=PoemsPane.class.getResourceAsStream(SECOND_HALF);
				) {
			Rtu.copyFile(in, outStream);
		}
	}
}
