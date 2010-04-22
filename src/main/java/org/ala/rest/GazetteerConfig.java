package org.ala.rest;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

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
        }

    
       
    }

    public String getIdAttributeName(String layerName) {
        //returns the gazetteer identifier for the layer
        //using xpath to query
        String idAttribute = "none";
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr
             = xpath.compile("//layer[name='" + layerName + "']/idAttribute/text()");

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


    public String getNameAttributeName(String layerName) {
        //returns the gazetteer display name for the layer
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