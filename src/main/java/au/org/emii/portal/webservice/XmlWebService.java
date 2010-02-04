/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.webservice;

import com.sun.jersey.api.client.Client;
import javax.ws.rs.core.MultivaluedMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author geoff
 */
public interface XmlWebService {

        public String getXmlMimeType();

        /**
         * Make a service request - no parameters
         * @param uri
         */
        public void makeRequest(String uri);

        /**
         * Make a service request with parameters
         * @param uri
         * @param queryParams
         */
        public void makeRequest(String uri, MultivaluedMap queryParams);

        /**
         * Make a request using the supplied client and no parameters.  Using a
         * passed in client allows access to restricted services by preserving
         * http sessions created by the remote server through the use of cookies
         * @param client
         * @param uri
         */
        public void makeRequest(Client client, String uri);

        /**
         * Make a request using the supplied client and parameters.  Using a
         * passed in client allows access to restricted services by preserving
         * http sessions created by the remote server through the use of cookies
         * @param client
         * @param uri
         * @param queryParams
         */
        public void makeRequest(Client client, String uri, MultivaluedMap queryParams);

        /**
         * return the boolean value at the given xpath
         * @param xpathString
         * @return
         */
        public boolean parseBoolean(String xpathString);

        /**
         * return the boolean value at the given xpath
         * @param node root node
         * @param xpathString xpath relative to node
         * @return
         */
        public boolean parseBoolean(Node node, String xpathString);

        /**
         * return the double value at the given xpath
         * @param xpathString
         * @return
         */
        public double parseDouble(String xpathString);

        /**
         * return the double value at the given xpath
         * @param node root node
         * @param xpathString xpath relative to node
         * @return
         */
        public double parseDouble(Node node, String xpathString);

        /**
         * return the node at the given xpath
         * @param xpathString
         * @return
         */
        public Node parseNode(String xpathString);

        /**
         * return the node at the given xpath
         * @param node root node
         * @param xpathString xpath relative to node
         * @return
         */
        public Node parseNode(Node node, String xpathString);

        /**
         * return the nodeset at the given xpath
         * @param xpathString
         * @return
         */
        public NodeList parseNodeList(String xpathString);

        /**
         * return the nodeset at the given xpath
         * @param node root node
         * @param xpathString xpath relative to node
         * @return
         */
        public NodeList parseNodeList(Node node, String xpathString);

        /**
         * return the string value at the given xpath
         * @param xpathString
         * @return
         */
        public String parseString(String xpathString);

        /**
         * return the string value at the given xpath
         * @param node root node
         * @param xpathString xpath relative to node
         * @return
         */
        public String parseString(Node node, String xpathString);

        /**
         * Set the string that will designate an xml mime type, usually
         * "application/xml".  If the mime type of the http request to the
         * service uri matches the string passed in here, then the response
         * will be treated as xml
         * @param xmlMimeType
         */
        public void setXmlMimeType(String xmlMimeType);

        /**
         * Check that response is of type xml, as indicated by the its mime type
         * @return
         */
        public boolean isResponseXml();

        /**
         * Get the raw service response - useful for debugging and writing error
         * messages
         * @return
         */
        public String getRawResponse();

        /**
         * Close any resources we are holding - must be called after the result
         * is no longer required or clients using ApacheHttpClient will freeze
         * on subsequent requests
         */
        public void close();

}
