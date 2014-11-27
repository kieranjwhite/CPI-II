package com.hourglassapps.cpi_ii.lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;

import com.hourglassapps.util.Log;

public class LuceneVisitor implements FileVisitor<Path> {
	private final static String TAG=LuceneVisitor.class.getName();
	private final IndexWriter mIndexWriter;
	private final FileReaderFactory mPath2Reader;

	public LuceneVisitor(IndexWriter pIndexWriter, FileReaderFactory pPath2Reader) {
		mIndexWriter=pIndexWriter;
		mPath2Reader=pPath2Reader;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path arg0, IOException arg1)
			throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path arg0,
			BasicFileAttributes arg1) throws IOException {
		return FileVisitResult.CONTINUE;
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
	@Override
	public FileVisitResult visitFile(Path pPath, BasicFileAttributes arg1)
			throws IOException {
		try {
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

					// Add the contents of the file to a field named "contents".  Specify a Reader,
					// so that the text of the file is tokenized and indexed, but not stored.
					// Note that FileReader expects the file to be in UTF-8 encoding.
					// If that's not the case searching for special characters will fail.
					doc.add(new TextField("contents", reader));

					if (mIndexWriter.getConfig().getOpenMode() == OpenMode.CREATE) {
						// New index, so we just add the document (no old document can be there):
						System.out.println("adding " + pPath);
						mIndexWriter.addDocument(doc);
					} else {
						// Existing index (an old copy of this document may have been indexed) so 
						// we use updateDocument instead to replace the old one matching the exact 
						// path, if present:
						System.out.println("updating " + pPath);
						mIndexWriter.updateDocument(new Term("path", pPath.toString()), doc);
					}

				}
			}
		} catch(IOException e) {
			Log.e(TAG, e);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path pFile, IOException e)
			throws IOException {
		Log.e(TAG, e, "Unreadable file: "+pFile.toString());
		return FileVisitResult.CONTINUE;
	}
}
