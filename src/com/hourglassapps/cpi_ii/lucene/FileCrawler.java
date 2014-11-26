package com.hourglassapps.cpi_ii.lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;

public class FileCrawler {
	private final FileReaderFactory mPath2Reader;

	public FileCrawler(FileReaderFactory pPath2Reader) {
		mPath2Reader=pPath2Reader;
		//new InputStreamReader(fis, StandardCharsets.UTF_8)
	}

	/**
	 * kw 25/11/2014: copied from org.apache.lucene.demo.IndexFiles
	 * 
	 * Indexes the given file using the given writer, or if a directory is given,
	 * recurses over files and directories found under the given directory.
	 * 
	 * NOTE: This method indexes one document per input file.  This is slow.  For good
	 * throughput, put multiple documents into your input file(s).  An example of this is
	 * in the benchmark module, which can create "line doc" files, one document per line,
	 * using the
	 * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
	 * >WriteLineDocTask</a>.
	 *  
	 * @param pIndexWriter Writer to the index where the given file/dir info will be stored
	 * @param pPath The file to index, or the directory to recurse into to find files to index
	 * @throws IOException If there is a low-level I/O error
	 */
	public void indexDocs(IndexWriter pIndexWriter, Path pPath)
			throws IOException {
		// do not try to index files that cannot be read
		if (Files.isReadable(pPath)) {
			if (Files.isDirectory(pPath)) {
				try(DirectoryStream<Path> dir=Files.newDirectoryStream(pPath)) {
					for(Path p: dir) {
						indexDocs(pIndexWriter, p);
					}
				}
			} else {
				Reader inner=mPath2Reader.reader(pPath);
				if(inner!=null) {
					try(Reader reader=new BufferedReader(inner)) {

						// make a new, empty document
						Document doc = new Document();

						// Add the path of the file as a field named "path".  Use a
						// field that is indexed (i.e. searchable), but don't tokenize 
						// the field into separate words and don't index term frequency
						// or positional information:
						Field pathField = new StringField("path", pPath.toString(), Field.Store.YES);
						doc.add(pathField);

						// Add the last modified date of the file a field named "modified".
						// Use a LongField that is indexed (i.e. efficiently filterable with
						// NumericRangeFilter).  This indexes to milli-second resolution, which
						// is often too fine.  You could instead create a number based on
						// year/month/day/hour/minutes/seconds, down the resolution you require.
						// For example the long value 2011021714 would mean
						// February 17, 2011, 2-3 PM.
						doc.add(new LongField("modified", Files.getLastModifiedTime(pPath).toMillis(), Field.Store.NO));

						// Add the contents of the file to a field named "contents".  Specify a Reader,
						// so that the text of the file is tokenized and indexed, but not stored.
						// Note that FileReader expects the file to be in UTF-8 encoding.
						// If that's not the case searching for special characters will fail.
						doc.add(new TextField("contents", reader));

						if (pIndexWriter.getConfig().getOpenMode() == OpenMode.CREATE) {
							// New index, so we just add the document (no old document can be there):
							System.out.println("adding " + pPath);
							pIndexWriter.addDocument(doc);
						} else {
							// Existing index (an old copy of this document may have been indexed) so 
							// we use updateDocument instead to replace the old one matching the exact 
							// path, if present:
							System.out.println("updating " + pPath);
							pIndexWriter.updateDocument(new Term("path", pPath.toString()), doc);
						}

					}
				}
			}
		}
	}
}
