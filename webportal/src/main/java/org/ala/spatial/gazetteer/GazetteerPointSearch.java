
package org.ala.spatial.gazetteer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.http.HttpEntity;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 *
 * @author Angus
 */
public class GazetteerPointSearch {


    /***
     * Given a lon,lat and layer - queries the gaz for a polygon
     * @param lon longitude
     * @param lat latitude
     * @param layer geoserver layer to search
     * @return returns a link to a geojson feature in the gaz
     */
    public static String PointSearch(String lon, String lat, String layer, String geoserver) {
        //HttpHost targetHost = new HttpHost("localhost", 8080); // "ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com"
       // DefaultHttpClient httpclient = new DefaultHttpClient();
       // BasicHttpContext localcontext = new BasicHttpContext();


        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(false);

        String featureURL = "none";

        try {

            //Read in the xml response
            DocumentBuilder builder = domFactory.newDocumentBuilder();
           
            String uri = geoserver + "/gazetteer/" + layer + "/latlon/" + lat + "," + lon;
            System.out.println(uri);
            HttpGet get = new HttpGet(uri);
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(get);
            HttpEntity entity = response.getEntity();

            Document resultDoc = builder.parse(entity.getContent());

            //Get a list of links (to features) from the xml
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
           
            XPathExpression linksExpr = xpath.compile("//search/results/result/@*");

            NodeList links = (NodeList) linksExpr.evaluate(resultDoc, XPathConstants.NODESET);

            featureURL= geoserver + links.item(0).getNodeValue();
            

        }
        catch (Exception e1) {
            //FIXME: log something
            System.out.println(e1.getMessage());
        }
        return featureURL;
        }
}
