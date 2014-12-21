package com.hourglassapps.serialise;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.hourglassapps.cpi_ii.PoemRecord;
import com.hourglassapps.cpi_ii.Record;
import com.hourglassapps.util.ConcreteThrower;
import com.hourglassapps.util.IOIterator;
import com.hourglassapps.util.Log;
import com.hourglassapps.util.ThrowableIterator;
import com.hourglassapps.util.ThrowingIterable;

public class PoemRecordXMLParser implements ThrowingIterable<PoemRecord>, AutoCloseable {
	private final static String TAG=PoemRecordXMLParser.class.getName();
	private final Reader mPreprocessor;
	
	private final List<PoemRecord> mRecords;
	
	public PoemRecordXMLParser(Reader pPreprocessor) throws IOException, ParseException {
		mPreprocessor=pPreprocessor;
		List<PoemRecord> records=new ArrayList<PoemRecord>();
		
		try {
			DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
			DocumentBuilder builder=factory.newDocumentBuilder();
			Document doc=builder.parse(new InputSource(pPreprocessor));
			
			XPathFactory xPathFactory=XPathFactory.newInstance();
			XPath xpath=xPathFactory.newXPath();
			XPathExpression expr=xpath.compile("/eprints/eprint");
			NodeList eprints=(NodeList)expr.evaluate(doc, XPathConstants.NODESET);
			int numNodes=eprints.getLength();
			for(int n=0; n<numNodes; n++) {
				Node node=eprints.item(n);
				if(node.getNodeType()==Node.ELEMENT_NODE) {
					Element eprint=(Element)node;
					String eprintId=getContent(eprint, "eprintid"),
							date=getContent(eprint, "date"),
							language=getContent(eprint, "language"),
							numStanzas=getContent(eprint, "no_of_stanzas"),
							title=getContent(eprint, "title"), 
							poemText3=getContent(eprint, "poem_text_3"), 
							refrainText3=getContent(eprint, "refrain_text_3");
					PoemRecord r=new PoemRecord();
					r.setEprintid(Long.parseLong(eprintId));
					r.setDate(date);
					r.setLanguage(language);
					r.setNo_of_stanzas(numStanzas);
					r.setTitle(title);
					r.setPoem_text_3(poemText3);
					r.setRefrain_text_3(refrainText3);
					
					records.add(r);
				}
			}
		} catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
			throw new ParseException(e);
		}
		mRecords=records;
	}

	private String getContent(Element pEprint, String pElement) {
		NodeList titles=pEprint.getElementsByTagName(pElement);
		if(titles.getLength()>0) {
			Element el=(Element)titles.item(0);
			NodeList chars=el.getChildNodes();
			if(chars.getLength()>0) {
				return chars.item(0).getNodeValue().trim();
			}
		}
		return null;
	}
	
	@Override
	public IOIterator<PoemRecord> throwableIterator() {
		final ConcreteThrower<IOException> thrower=new ConcreteThrower<IOException>();
		final Iterator<PoemRecord> it=mRecords.iterator();
		return new IOIterator<PoemRecord>() {

			@Override
			public boolean hasNext() {
				if(thrower.fallThrough()) {
					return false;
				}
				return it.hasNext();
			}

			@Override
			public PoemRecord next() {
				return it.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public <E extends Exception> void throwCaught(Class<E> pCatchable)
					throws Throwable {
				thrower.throwCaught(null);
			}

			@Override
			public void close() throws IOException {
				thrower.close();
			}
			
		};
	}
	
	@Override
	public Iterator<PoemRecord> iterator() {
		return throwableIterator();
	}

	@Override
	public void close() throws IOException {
		mPreprocessor.close();
	}

}
