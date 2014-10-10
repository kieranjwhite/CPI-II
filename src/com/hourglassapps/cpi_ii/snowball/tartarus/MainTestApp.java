
package com.hourglassapps.cpi_ii.snowball.tartarus;

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

	public static void main(String [] args) throws Throwable {
		if (args.length < 1) {
			usage();
			return;
		}

		SnowballProgram stemmer = new LatinStemmer();

		if(args.length==1) {
			stemmer.setCurrent(args[0]);
			stemmer.stem();
			System.out.println(stemmer.getCurrent());
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

				Writer output=null;
				try {
					output = new OutputStreamWriter(outstream);
					output = new BufferedWriter(output);

					int repeat = 1;
					if (args.length > 4) {
						repeat = Integer.parseInt(args[4]);
					}

					int character;
					while ((character = reader.read()) != -1) {
						char ch = (char) character;
						if (Character.isWhitespace((char) ch)) {
							if (input.length() > 0) {
								stemmer.setCurrent(input.toString());
								for (int i = repeat; i != 0; i--) {
									stemmer.stem();
								}
								output.write(stemmer.getCurrent());
								output.write('\n');
								input.delete(0, input.length());
							}
						} else {
							input.append(Character.toLowerCase(ch));
						}
					}
					output.flush();
				} finally {
					if(output!=null) {
						output.close();
					}
				}

			} finally {
				if(reader!=null) {
					reader.close();
				}
			}
		}
	}
}
