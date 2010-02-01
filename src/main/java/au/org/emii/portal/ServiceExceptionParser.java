package au.org.emii.portal;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Parse an OGC Service exception xml document 
 * @author geoff
 *
 */
public class ServiceExceptionParser {
	
	private final static String REASON_XPATH = "/ServiceExceptionReport/ServiceException/text()";
	
	public static String parseAndGetReason(InputStream is) {
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		DocumentBuilder documentBuilder = null;
		Document document = null;
		String reason = "Unable to understand reason";
		try {
			documentBuilder = domFactory.newDocumentBuilder();
			document = documentBuilder.parse(is);
			
			XPathExpression expr = null;
			expr = xpath.compile(REASON_XPATH);
			reason = expr.evaluate(document);
			
		}
		catch (ParserConfigurationException e) {
			reason = e.getMessage();
		} 
		catch (SAXException e) {
			reason = e.getMessage();
		} 
		catch (IOException e) {
			reason = e.getMessage();
		} 
		catch (XPathExpressionException e) {
			reason = e.getMessage();
		}
		
		return reason;
	}
}
