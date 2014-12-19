
package com.hourglassapps.cpi_ii.stem;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.FileOutputStream;

import org.apache.lucene.analysis.core.WhitespaceTokenizer;

import com.hourglassapps.cpi_ii.latin.LatinAnalyzer;
import com.hourglassapps.cpi_ii.latin.LatinStemmer;
import com.hourglassapps.cpi_ii.stem.StemRecorderFilter.Factory;
import com.hourglassapps.util.Log;

public class MainStemTest {
	private final static Factory STEMMER_FACTORY=LatinAnalyzer.STEMPEL_RECORDER_FACTORY;
	
	private static void usage()
	{
		System.err.println("Usage: TestApp <term>");
		System.err.println("Usage: TestApp <input file> <output file>");
	}

	private static void stemAndShow(String pTerm, PrintWriter pOut) throws IOException {
		try(StemRecorderFilter filter=STEMMER_FACTORY.inst(new WhitespaceTokenizer(
				new StringReader(pTerm)))) {
			pOut.print(filter.stem());
		}
	}
	
	protected void invoke(String[] pArgs) throws Throwable {
		if (pArgs.length < 1) {
			usage();
			return;
		}

		if(pArgs.length==1) {
			try(PrintWriter out=new PrintWriter(new OutputStreamWriter(System.out))) {
				stemAndShow(pArgs[0].toLowerCase(), out);
				out.println();
			}
		} else {

			Reader reader=null;
			try {
				if("-".equals(pArgs[0])) {
					reader=new InputStreamReader(System.in);
				} else {
					reader = new InputStreamReader(new FileInputStream(pArgs[0]));
					reader = new BufferedReader(reader);
				}
				StringBuffer input = new StringBuffer();

				OutputStream outstream;

				if (pArgs.length < 3) {
					if("-".equals(pArgs[1])) {
						outstream = System.out;							
					} else {
						outstream = new FileOutputStream(pArgs[1]);							
					}
				} else {
					usage();
					return;
				}

				//Writer output=null;
				try(PrintWriter out=new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(outstream)))) {
					int character;
					while ((character = reader.read()) != -1) {
						char ch = (char) character;
						if (Character.isWhitespace(ch)) {
							if (input.length() > 0) {
								stemAndShow(input.toString(), out);
								out.print(ch);
								input.delete(0, input.length());
							}
						} else {
							input.append(Character.toLowerCase(ch));
						}
					}
					out.flush();
				}

			} finally {
				if(reader!=null) {
					reader.close();
				}
			}	
		}		
	}
	
	public static void main(String [] pArgs) throws Throwable {
		new MainStemTest().invoke(pArgs);
	}
}
