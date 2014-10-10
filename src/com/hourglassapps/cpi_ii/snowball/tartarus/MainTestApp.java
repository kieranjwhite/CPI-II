
package com.hourglassapps.cpi_ii.snowball.tartarus;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.FileOutputStream;

import com.hourglassapps.cpi_ii.latin.LatinStemmer;
import com.hourglassapps.util.Log;

public class MainTestApp {
	private static void usage()
	{
		System.err.println("Usage: TestApp <term>");
		System.err.println("Usage: TestApp <input file> <output file>");
	}

	private static void stemAndShow(LatinStemmer pStemmer, String pTerm, PrintWriter pOut) {
		pStemmer.setCurrent(pTerm);
		pOut.print(pTerm+"\t");
		pStemmer.stem(LatinStemmer.PartOfSpeech.NOUN);
		pOut.print(pStemmer.getCurrent()+"\t");
		pStemmer.setCurrent(pTerm);
		pStemmer.stem(LatinStemmer.PartOfSpeech.VERB);
		pOut.println(pStemmer.getCurrent());
	}
	
	public static void main(String [] args) throws Throwable {
		if (args.length < 1) {
			usage();
			return;
		}

		LatinStemmer stemmer = new LatinStemmer();

		if(args.length==1) {
			try(PrintWriter out=new PrintWriter(new OutputStreamWriter(System.out))) {
				stemAndShow(stemmer, args[0].toLowerCase(), out);				
			}
		} else {

			Reader reader=null;
			try {
				if("-".equals(args[0])) {
					reader=new InputStreamReader(System.in);
				} else {
					reader = new InputStreamReader(new FileInputStream(args[0]));
					reader = new BufferedReader(reader);
				}
				StringBuffer input = new StringBuffer();

				OutputStream outstream;

				if (args.length < 3) {
					if("-".equals(args[1])) {
						outstream = System.out;							
					} else {
						outstream = new FileOutputStream(args[1]);							
					}
				} else {
					usage();
					return;
				}

				//Writer output=null;
				try(PrintWriter out=new PrintWriter(new BufferedWriter(new OutputStreamWriter(outstream)))) {
					int character;
					while ((character = reader.read()) != -1) {
						char ch = (char) character;
						if (Character.isWhitespace((char) ch)) {
							if (input.length() > 0) {
								stemAndShow(stemmer, input.toString(), out);
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
}
