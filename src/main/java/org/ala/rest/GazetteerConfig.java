package org.ala.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/***
 * Configuration class for Gazetteer.  Reads the gazetteer.xml to get Geoserver layer names and fields for use in the Gazetteer.
 * @author Angus
 */
public class GazetteerConfig {

    private static final Logger logger = Logging.getLogger("org.ala.rest.GazetteerConfig");
    Document configDoc;

    public GazetteerConfig() {
        //open the gazetteer.xml
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true); // never forget this!
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            configDoc = builder.parse(new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer.xml"));


        } catch (Exception e) {
            logger.severe("Failed to initialize Gazetteer");
            logger.severe(ExceptionUtils.getFullStackTrace(e));
        }

    }

    /**
     * Gets a list of geoserver layer names used by the gazetteer
     * @return a List of the geoserver layer names
     */
    public List<String> getLayerNames() {
        List<String> layerNames = new ArrayList();
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//layer/name/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;

            for (int i = 0; i < nodes.getLength(); i++) {
                layerNames.add(nodes.item(i).getNodeValue());
            }
        } catch (Exception e) {
            logger.severe("An error has occurred getting the layer names");
            logger.severe(ExceptionUtils.getFullStackTrace(e));
        }
        return layerNames;
    }

    /**
     * Gets a list of geoserver layer names that are used for a default search
     * @return a list of Default geoserver layers
     */
    public List<String> getDefaultLayerNames() {
        List<String> defaultLayerNames = new ArrayList();
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//layer[defaultLayer=\"true\"]/name/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;

            for (int i = 0; i < nodes.getLength(); i++) {
                defaultLayerNames.add(nodes.item(i).getNodeValue());
            }
        } catch (Exception e) {
            logger.severe("An error has occurred getting default layer names");
            logger.severe(ExceptionUtils.getFullStackTrace(e));
        }
        return defaultLayerNames;
    }

    /**
     * Get the layer alias out of the config file
     * @return layer alias (or empty string if none present)
     */
    public String getLayerAlias(String layerName) {
        String alias = "";
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//layer[name=\"" + layerName + "\"]/layerAlias/text()");
            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            alias = (String) nodes.item(0).getNodeValue();
            return alias;
        } catch (Exception e) {
            logger.info("No layer alias defined for " + layerName);
            return "";
        }
    }

    /**
     * Returns a layer name given a layer alias
     * @param alias
     * @return
     */
    public String getNameFromAlias(String alias) {
        String name = "";

        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//layer[layerAlias=\"" + alias + "\"]/name/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            if (nodes.getLength() != 1) {
                logger.finer("Unable to find alias for " + alias);
            } else {
                name = nodes.item(0).getNodeValue();
                logger.finer("Layer " + name + " found matching alias " + alias);
            }
            return name;
        } catch (Exception e) {
            logger.severe("An error has occurred getting layer name from alias");
            logger.severe(ExceptionUtils.getFullStackTrace(e));
            return "";
        }
    }

    /**
     * Helper method to check to see if a layer name has been defined
     * @param name
     * @return
     */
    public boolean layerNameExists(String name){
        if (getLayerNames().contains(name))
            return true;
        else
            logger.finer("Layer name does not exist for " + name);
            return false;
    }

//    /**
//     * Helper method to check to see if a layer (or alias) has been defined
//     * @param name
//     * @return
//     */
//    public boolean layerExists(String name){
//        if (getLayerNames().contains(name))
//            return true;
//        else if (getNameFromAlias(name).compareTo("") != 0)
//            return true;
//        else
//            logger.finer("Layer name or alias does not exist for " + name);
//            return false;
//    }

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
            XPathExpression expr = xpath.compile("//layer[name='" + layerName + "']/descriptionAttributes/descriptionAttribute/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;

            for (int i = 0; i < nodes.getLength(); i++) {
                descriptionAttributes.add(nodes.item(i).getNodeValue());
            }

        } catch (Exception e) {
            logger.severe("An error has occurred getting description attributes");
            logger.severe(ExceptionUtils.getFullStackTrace(e));
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

        } catch (Exception e) {
            logger.severe(ExceptionUtils.getFullStackTrace(e));
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
        String idAttribute2 = "";
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//layer[name='" + layerName + "']/idAttribute2/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            if (nodes.getLength() > 0) {
                idAttribute2 = nodes.item(0).getNodeValue();
            }
        } catch (Exception e) {
            logger.severe(ExceptionUtils.getFullStackTrace(e));
        }
        return idAttribute2;
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
            XPathExpression expr = xpath.compile("//layer[name='" + layerName + "']/nameAttribute/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            idAttribute = nodes.item(0).getNodeValue();

        } catch (Exception e) {
            logger.severe(ExceptionUtils.getFullStackTrace(e));
        }
        return idAttribute;
    }

    /***
     * Gets the gazetteer class attribute for the layer
     * @param layerName
     * @return the name of the layers class attribute
     */
    public String getClassAttributeName(String layerName) {
        //using xpath to query
        String classAttribute = "none";
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//layer[name='" + layerName + "']/classAttribute/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            classAttribute = nodes.item(0).getNodeValue();

        } catch (Exception e) {
            //this fails pretty hard when the attribute doesn't exist
            return "none";
        }
        return classAttribute;
    }

    /**
     * Checks to see if a layer is a default layer
     * @param layerName
     * @return
     */
    public boolean isDefaultLayer(String layerName){
    boolean defaultLayer = false;
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//layer[name='" + layerName + "']/defaultLayer/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            defaultLayer = Boolean.valueOf(nodes.item(0).getNodeValue()).booleanValue();
            return defaultLayer;
        } catch (Exception e) {
            return false;

        }
    }
    /**
     * Checks to see if the layer is name searchable
     * @param layerName
     * @return
     */
    public boolean isNameSearchable(String layerName){
    boolean nameSearch = false;
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//layer[name='" + layerName + "']/nameSearch/text()");

            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            nameSearch = Boolean.valueOf(nodes.item(0).getNodeValue()).booleanValue();
            return nameSearch;
        } catch (Exception e) {
            return false;

        }
    }
}
