/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.webservice;

import au.org.emii.portal.config.Settings;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Wrapper around the the jdom and jersey libraries to reduce code duplication
 * @author geoff
 */
public class XmlWebServiceJerseyImpl implements XmlWebService {

    private Settings settings = null;

    /**
     * The xml reply parsed into a document - gets set automatically
     * when something is requeste by xpath
     */
    private Document document = null;
    /**
     * Raw text of the reply
     */
    private String reply = null;
    /**
     * Mime type of the reply
     */
    private String mimeType = null;
    /**
     * True if a reply has been received
     */
    private boolean replyReceived = false;
    /**
     * Log4j instance
     */
    private Logger logger = Logger.getLogger(getClass());
    /**
     * The uri to make requests to
     */
    private String uri = null;

    /**
     * Keep reference to client response so we can call close() on it
     */
    private ClientResponse response =  null;

    /**
     * Reset all status flags on initialisation
     */
    public XmlWebServiceJerseyImpl() {
        reset();
    }

    /**
     * Make a service request - no parameters
     * @param uri
     */
    @Override
    public void makeRequest(String uri) {
        reset();
        // hold onto the uri for debugging purposes
        this.uri = uri;
        WebResource webResource = prepareQuery();
        processResponse(webResource);
    }

    /**
     * Prepare a webresource instance to start the query
     * @return
     */
    private WebResource prepareQuery() {
        Client client = Client.create();
        return client.resource(uri);
    }

    /**
     * Set the reply flags after a response has been made
     * @param response
     */
    private void setReplyFlags() {
        replyReceived = true;
        mimeType = response.getHeaders().getFirst("Content-Type");
        reply = response.getEntity(String.class);
    }

    /**
     * Make a service request with parameters
     * @param uri
     * @param queryParams
     */
    @Override
    public void makeRequest(String uri, MultivaluedMap queryParams) {
        reset();
        // hold onto the uri for debugging purposes
        this.uri = uri;
        WebResource webResource = prepareQuery();

        processResponse(webResource, queryParams);
    }

    /**
     * Attempt to parse the document field from the raw string reply
     */
    private void parseReplyXml() {
        if (replyReceived == true) {
            if (isResponseXml()) {
                try {
                    // looks good - got some xml
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true); // never forget this!
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    byte[] currentXMLBytes = reply.getBytes();
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(currentXMLBytes);
                    document = db.parse(byteArrayInputStream);

                } catch (SAXException ex) {
                    logger.error("error parsing xml", ex);
                } catch (IOException ex) {
                    logger.error("error parsing xml", ex);
                } catch (ParserConfigurationException ex) {
                    logger.error("error parsing xml", ex);
                }
            } else {
                logger.debug("web service reply is not an xml document");
            }
        } else {
            throw new RuntimeException(
                    "attempt to read web service xml result before query was sent.  URI='" + uri + "'");
        }
    }

    /**
     * lookup the requested xpath from the given node and return the result
     * @param root Root node for query - use document or pass in a Node instance
     * @param xpathString relative xpath to return
     * @param type QName of reply type to return
     * @return instance of type at xpathString or null if nothing matches
     */
    private Object parseXpath(Node root, String xpathString, QName type) {
        Object object = null;

        // blow up if there is no context node
        if (root == null) {
            throw new RuntimeException("attempted to search against null xml node");
        }


        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        try {
            XPathExpression expr = xpath.compile(xpathString);
            object = expr.evaluate(root, type);

        } catch (XPathExpressionException ex) {
            logger.error("error parsing xpath", ex);
        }

        return object;
    }

    /**
     * lookup the requested xpath from the root document node and return the result
     * @param xpathString xpath relative to document root node to return
     * @param type QName of reply type to return
     * @return instance of type at xpathString or null if nothing matches
     */
    private Object parseXpath(String xpathString, QName type) {

        Object object = null;

        // lazy init document (but it may fail so second check)
        if (document == null) {
            parseReplyXml();
        }

        if (document != null) {
            object = parseXpath(document, xpathString, type);
        }


        return object;
    }

    /**
     * return the string value at the given xpath or null if the string is
     * empty or not present
     * @param xpathString
     * @return
     */
    @Override
    public String parseString(String xpathString) {
        String string = (String) parseXpath(xpathString, XPathConstants.STRING);
        return (string != null && string.equals("")) ? null : string;
    }

    /**
     * return the string value at the given xpath or null if the string is
     * empty or not present
     * @param node root node
     * @param xpathString xpath relative to node
     * @return
     */
    @Override
    public String parseString(Node node, String xpathString) {
        String string = (String) parseXpath(node, xpathString, XPathConstants.STRING);
        return (string != null && string.equals("")) ? null : string;
    }

    /**
     * return the boolean value at the given xpath
     * @param xpathString
     * @return
     */
    @Override
    public boolean parseBoolean(String xpathString) {
        return ((Boolean) parseXpath(xpathString, XPathConstants.BOOLEAN)).booleanValue();
    }

    /**
     * return the boolean value at the given xpath
     * @param node root node
     * @param xpathString xpath relative to node
     * @return
     */
    @Override
    public boolean parseBoolean(Node node, String xpathString) {
        return ((Boolean) parseXpath(node, xpathString, XPathConstants.BOOLEAN)).booleanValue();
    }

    /**
     * return the double value at the given xpath
     * @param xpathString
     * @return
     */
    @Override
    public double parseDouble(String xpathString) {
        return ((Double) parseXpath(xpathString, XPathConstants.NUMBER)).doubleValue();
    }

    /**
     * return the double value at the given xpath
     * @param node root node
     * @param xpathString xpath relative to node
     * @return
     */
    @Override
    public double parseDouble(Node node, String xpathString) {
        return ((Double) parseXpath(node, xpathString, XPathConstants.NUMBER)).doubleValue();
    }

    /**
     * return the node at the given xpath
     * @param xpathString
     * @return
     */
    @Override
    public Node parseNode(String xpathString) {
        return (Node) parseXpath(xpathString, XPathConstants.NODE);
    }

    /**
     * return the node at the given xpath
     * @param node root node
     * @param xpathString xpath relative to node
     * @return
     */
    @Override
    public Node parseNode(Node node, String xpathString) {
        return (Node) parseXpath(node, xpathString, XPathConstants.NODE);
    }

    /**
     * return the nodeset at the given xpath
     * @param xpathString
     * @return
     */
    @Override
    public NodeList parseNodeList(String xpathString) {
        return (NodeList) parseXpath(xpathString, XPathConstants.NODESET);
    }

    /**
     * return the nodeset at the given xpath
     * @param node root node
     * @param xpathString xpath relative to node
     * @return
     */
    @Override
    public NodeList parseNodeList(Node node, String xpathString) {
        return (NodeList) parseXpath(node, xpathString, XPathConstants.NODESET);
    }

    /**
     * Reset object so it can be reused
     */
    private void reset() {
        document = null;
        reply = null;
        mimeType = null;
        replyReceived = false;
    }

    @Override
    public boolean isResponseXml() {
        // check mimetype for null as well - some of my mock servers
        // dont bother to set it to html on error sometimes
        return mimeType != null && mimeType.equals(settings.getXmlMimeType());
    }

    @Override
    public String getRawResponse() {
        return reply;
    }

    @Override
    public void makeRequest(Client client, String uri) {
        reset();
        // hold onto the uri for debugging purposes
        this.uri = uri;
        processResponse(client.resource(uri));

        
    }

    @Override
    public void makeRequest(Client client, String uri, MultivaluedMap queryParams) {
        reset();
        // hold onto the uri for debugging purposes
        this.uri = uri;
        processResponse(client.resource(uri), queryParams);
    }

    private void processResponse(WebResource webResource, MultivaluedMap queryParams) {
        logger.debug("requesting " + uri + " params="+ queryParams.toString());
        try {
            response = webResource.queryParams(queryParams).get(ClientResponse.class);
            setReplyFlags();
        } catch (ClientHandlerException e) {
            logger.error(String.format(
                    "Error connecting to uri '%s' - malformed service URI?", uri), e);
        }
        
    }

    private void processResponse(WebResource webResource) {
        logger.debug("requesting " + uri);
        response = webResource.get(ClientResponse.class);
        setReplyFlags();
    }

    @Override
    public void close() {
        if (response != null) {
            response.close();
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String getXmlMimeType() {
        return settings.getXmlMimeType();
    }


}
