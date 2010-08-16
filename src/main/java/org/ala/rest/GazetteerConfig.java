package org.ala.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/***
 * Configuration class for Gazetteer.  Reads the gazetteer.xml to get Geoserver layer names and fields for use in the Gazetteer.
 * @author Angus
 */
public class GazetteerConfig {
    Document configDoc;
    public GazetteerConfig() {
        //open the gazetteer.xml
        try
        {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true); // never forget this!
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            configDoc = builder.parse(new File(GeoserverDataDirectory.getGeoserverDataDirectory(),"gazetteer.xml"));

            
        }
        catch(Exception e)
        {
            //FIXME: gazetteer is probably not going to work
            System.out.println("Failed to initialize Gazetteer");
            e.printStackTrace();
        }

    
       
    }

    /**
     * Gets a list of geoserver layer names used by the gazetteer
     * @return a List of the geoserver layer names
     */
    public List<String> getLayerNames()
    {
        List<String> layerNames = new ArrayList();
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//layer/name/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
           
            for(int i=0;i<nodes.getLength();i++) {
                layerNames.add(nodes.item(i).getNodeValue());
            }
        }
        catch(Exception e)
        {
            //FIXME
            
        }
        return layerNames;
    }

    /***
     * Get a list of attributes to use in the feature description for the given layer
     * @param layerName 
     * @return
     */
    public List<String> getDescriptionAttributes(String layerName) {
        //using xpath to query
        List<String> descriptionAttributes = new ArrayList<String>();
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr
             = xpath.compile("//layer[name='" + layerName + "']/descriptionAttributes/descriptionAttribute/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
     
            for(int i=0;i<nodes.getLength();i++) {
                descriptionAttributes.add(nodes.item(i).getNodeValue());
            }

        }
        catch(Exception e)
        {
            //FIXME
        }
        return descriptionAttributes;
    }

    /***
     * Gets the first gazetteer identifier for the layer
     * @param layerName
     * @return the name of the id attribute
     */
    public String getIdAttribute1Name(String layerName) {
        //using xpath to query
        String idAttribute = "none";
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//layer[name='" + layerName + "']/idAttribute1/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            idAttribute = nodes.item(0).getNodeValue();

        }
        catch(Exception e)
        {
            //FIXME
        }
        return idAttribute;
    }

     /***
     * Gets the second gazetteer identifier for the layer
     * @param layerName
     * @return the name of the id attribute
     */
    public String getIdAttribute2Name(String layerName) {
        //using xpath to query
        String idAttribute = "none";
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//layer[name='" + layerName + "']/idAttribute2/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            idAttribute = nodes.item(0).getNodeValue();

        }
        catch(Exception e)
        {
            //FIXME
        }
        return idAttribute;
    }

    /***
     * Gets the gazetteer display name for the layer
     * @param layerName
     * @return the name of the features name attribute
     */
    public String getNameAttributeName(String layerName) {
        //using xpath to query
        String idAttribute = "none";
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr
             = xpath.compile("//layer[name='" + layerName + "']/nameAttribute/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            idAttribute = nodes.item(0).getNodeValue();

        }
        catch(Exception e)
        {
            //FIXME
        }
        return idAttribute;
    }
}