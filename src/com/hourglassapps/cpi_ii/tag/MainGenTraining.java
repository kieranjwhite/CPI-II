package com.hourglassapps.cpi_ii.tag;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.Reader;

import com.hourglassapps.cpi_ii.tag.stempel.Compile;
import com.hourglassapps.util.Log;

public class MainGenTraining {
	private final static String TAG=MainGenTraining.class.getName();
	
	public static void main(String[] pArgs) throws IOException {
		if (pArgs.length < 1) {
			usage();
			return;
		}

		BufferedReader reader=null;
		try {
			if("-".equals(pArgs[0])) {
				reader=new BufferedReader(new InputStreamReader(System.in));
			} else {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(pArgs[0])));
			}
	
			PipedOutputStream outstream=new PipedOutputStream();

			if (pArgs.length < 3) {
				final Reader pipe=new InputStreamReader(new PipedInputStream(outstream));
				final String outFilename=pArgs[1];
				Thread compiler=new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							Compile.compileFromReader(pipe, new String[]{ "latin", outFilename});
						} catch (IOException e) {
							Log.e(TAG, e);
						}
					}});
				compiler.start();
			} else {
				usage();
				return;
			}
			
			try(PrintWriter writer=new PrintWriter(outstream)) {
				String lemma, lastLemma, variant;
				lemma=reader.readLine();
				lastLemma=null;
				StringBuffer outLine = new StringBuffer();
				boolean first=true;
				while(lemma!=null) {
					variant=reader.readLine();
					assert(variant!=null);
					if(lemma.equals(lastLemma)) {
						outLine.append(' ').append(variant);
					} else {
						if(first) {
							first=false;
						} else {
							writer.println(outLine);
						}
						outLine.delete(0, outLine.length());
						outLine.append(lemma).append(' ').append(variant);
						lastLemma=lemma;
					}
					
					lemma=reader.readLine();
				}
				writer.println(outLine);
			}
		} finally {
			if(reader!=null) {
				reader.close();
			}
		}


	}

		private static void usage() {
			System.out.println("Args: -input <IN_FILE> -out <OUT_FILE>");
		}
}
