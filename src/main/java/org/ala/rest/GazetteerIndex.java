package org.ala.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.springframework.beans.factory.InitializingBean;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.vfny.geoserver.util.DataStoreUtils;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.geotools.util.logging.Logging;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/***
 * Builds the Gazetter index based on the gazetteer config and layers/features in Geoserver
 * @author Angus
 */
public class GazetteerIndex implements InitializingBean {

    private static final Logger logger = Logging.getLogger("org.ala.rest.GazetteerIndex");

    /***
     * The Gazetteer index is built here if one does not exist already
     */
    @Override
    public void afterPropertiesSet() {
        try {

            //Initialize lucene index
            File featureIndexDir = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-index");
            File classIndexDir = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-class-index");
            if (featureIndexDir.exists()) {
                return;
            } else {
                //create a new Thread and perform indexing in background
                // Create and start the thread
                Thread thread = new IndexThread();
                thread.start();
            }
        } catch (Exception e) {
            logger.severe("An error occurred generating index.");
        }

    }
}

class IndexThread extends Thread {
    // This method is called when the thread runs

    private static final Logger logger = Logging.getLogger("org.ala.rest.IndexThread");

    public void run() {
        //Get geoserver catalog from Geoserver config
        GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
        Catalog catalog = gs.getCatalog();

        ServletContext sc = GeoServerExtensions.bean(ServletContext.class);
        GazetteerConfig gc = GeoServerExtensions.bean(GazetteerConfig.class);

        for (String layerName : gc.getLayerNames()) {
            String layerAlias = gc.getLayerAlias(layerName);
            if (layerAlias.compareTo("") != 0) {
                logger.info("Layer alias for " + layerName + " is " + layerAlias);
            }
        }

        DataStore dataStore = null;
        FeatureIterator features = null;

        for (String layerName : gc.getDefaultLayerNames()) {
            logger.info("default layer detected:" + layerName);
        }
        try {
            File featureIndexDir = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-index");
            File classIndexDir = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-class-index");
            FileUtils.forceMkdir(featureIndexDir);
            FileUtils.forceMkdir(classIndexDir);

            StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
            IndexWriter featureIndex = new IndexWriter(FSDirectory.open(featureIndexDir), analyzer /*Version.LUCENE_CURRENT)*/, true, IndexWriter.MaxFieldLength.UNLIMITED);
            IndexWriter classIndex = new IndexWriter(FSDirectory.open(classIndexDir), analyzer, true, IndexWriter.MaxFieldLength.UNLIMITED);

            for (String layerName : gc.getLayerNames()) {
                LayerInfo layerInfo = catalog.getLayerByName(layerName);

                ResourceInfo layerResource = layerInfo.getResource();
                StoreInfo layerStore = layerResource.getStore();
                Map params = layerStore.getConnectionParameters();

                dataStore = DataStoreUtils.acquireDataStore(params, sc);
                Set classNames = new HashSet();
                Set uniqueFeatureIds = new HashSet();
                Map featureMap = new HashMap();
                if (dataStore == null) {
                    throw new Exception("Could not find datastore for this layer");
                } else {
                    logger.info("Indexing " + layerName);
                    FeatureSource layer = dataStore.getFeatureSource(layerName);
                    features = layer.getFeatures().features();
                    List<String> descriptionAttributes = gc.getDescriptionAttributes(layerName);
                    String idAttribute = gc.getIdAttribute1Name(layerName);

                    while (features.hasNext()) {
                        Feature feature = features.next();
                        Document featureDoc = new Document();
                        String idAttribute1 = null;
                        if (feature.getProperty(gc.getIdAttribute1Name(layerName)).getValue() != null) {
                            idAttribute1 = feature.getProperty(gc.getIdAttribute1Name(layerName)).getValue().toString();
                            featureDoc.add(new Field("idAttribute1", idAttribute1, Store.YES, Index.ANALYZED));
                            featureDoc.add(new Field("id", idAttribute1, Store.YES, Index.ANALYZED));
                            logger.finer("Indexed layer " + layerName + " idAttribute1: " + idAttribute1);
                        } else {
                            logger.severe("Null value retrieved for idAttribute1");
                        }

                        if (feature.getProperty(gc.getNameAttributeName(layerName)).getValue() != null) {
                            featureDoc.add(new Field("name", feature.getProperty(gc.getNameAttributeName(layerName)).getValue().toString(), Store.YES, Index.ANALYZED));
                        }

                        if (!gc.getClassAttributeName(layerName).contentEquals("none")) {
                            //building a set of classes for this layer - indexed separately
                            if (feature.getProperty(gc.getClassAttributeName(layerName)).getValue() != null) {
                                classNames.add(feature.getProperty(gc.getClassAttributeName(layerName)).getValue().toString());
                            }
                        }

                        //add layer name to index
                        featureDoc.add(new Field("layerName", layerName, Store.YES, Index.ANALYZED));

                        //add layer alias to index
                        String layerAlias = gc.getLayerAlias(layerName);

                        featureDoc.add(new Field("layerAlias", layerAlias, Store.YES, Index.ANALYZED));

                        //Go through the description attributes and construct a description string
                        String description = "";
                        for (String descriptionAttribute : descriptionAttributes) {
                            if (feature.getProperty(descriptionAttribute).getValue() == null){
                                logger.severe("Unable to index description attribute " + descriptionAttribute + " for layer " + layerName);
                            }
                            else{
                                description += feature.getProperty(descriptionAttribute).getValue().toString() + ", ";
                            }
                        }

                        if (layerAlias.compareTo("") == 0) {
                            description += "(" + layerName + " " + feature.getDefaultGeometryProperty().getType().getBinding().getSimpleName() + ")";
                        } else {
                            description += "(" + layerAlias + " " + feature.getDefaultGeometryProperty().getType().getBinding().getSimpleName() + ")";
                        }
                        featureDoc.add(new Field("Description", description, Store.YES, Index.NO));
                        logger.finer("Description added to index: " + description);
                        for (Property property : feature.getProperties()) {
                            if ((descriptionAttributes.contains(property.getName().toString())) && (property.getValue() != null)) {
                                featureDoc.add(new Field(property.getName().toString(), property.getValue().toString(), Store.YES, Index.NO));
                            }
                        }

                        featureDoc.add(new Field("Type", feature.getDefaultGeometryProperty().getType().getBinding().getSimpleName(), Store.YES, Index.NO));

                        //AM: relying on the new requirement to have unique idAttribute1
                        uniqueFeatureIds.add(idAttribute1);
                        featureMap.put(idAttribute1, featureDoc);
                    }
                    features.close();

                }
                dataStore.dispose();

                //For some layers, multiple entries are multiple polygons of the same feature - only index once per *feature*
                for (Object id : uniqueFeatureIds) {
                    logger.finer("Found an id:" + (String) id);
                    featureIndex.addDocument((Document) featureMap.get(id));
                }

                //Indexing classes with layer name
                Iterator iter = classNames.iterator();
                StringBuilder sb = new StringBuilder();
                if (iter.hasNext()) {
                    sb.append(iter.next());
                    while (iter.hasNext()) {
                        sb.append(",").append(iter.next());
                    }
                    Document classDoc = new Document();
                    classDoc.add(new Field("layer", layerName, Store.YES, Index.ANALYZED));
                    classDoc.add(new Field(gc.getClassAttributeName(layerName), sb.toString(), Store.YES, Index.NO));
                    classIndex.addDocument(classDoc);
                }
            }

            //go through synonyms and index features
            org.w3c.dom.Document synonymDoc;
            try {
                DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
                domFactory.setNamespaceAware(true);
                DocumentBuilder builder = domFactory.newDocumentBuilder();
                synonymDoc = builder.parse(new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-synonyms.xml"));
                NodeList nList = synonymDoc.getElementsByTagName("layer");
                for (int i = 0; i < nList.getLength(); i++) {

                    Element e = (Element) nList.item(i);
                    String layerName = e.getAttribute("name");
                    logger.log(Level.FINER, "Found synonym for layer: {0}", layerName);

                    NodeList featureNodes = e.getElementsByTagName("feature");

                    for (int j = 0; j < featureNodes.getLength(); j++) {
                        Document featureDoc = new Document();
                        featureDoc.add(new Field("layerName", layerName, Store.YES, Index.ANALYZED));
                        Element featureElement = (Element) featureNodes.item(j);
                        String idAttribute1 = featureElement.getElementsByTagName("idAttribute1").item(0).getTextContent();
                        logger.log(Level.FINER, "idAttribute1 is: {0}", idAttribute1);
                        featureDoc.add(new Field("idAttribute1", idAttribute1, Store.YES, Index.ANALYZED));
                        String synonym = featureElement.getElementsByTagName("synonym").item(0).getTextContent();
                        logger.log(Level.FINER, "synonym is: {0}", synonym);
                        featureDoc.add(new Field("id", synonym, Store.YES, Index.ANALYZED));
                        featureDoc.add(new Field("name", synonym, Store.YES, Index.ANALYZED));
                        featureIndex.addDocument(featureDoc);
                    }
                }
            } catch (FileNotFoundException fnfe) {
                logger.log(Level.SEVERE, "Unable to find synonyms.xml in {0}", GeoserverDataDirectory.getGeoserverDataDirectory());
            } catch (Exception e) {
                logger.severe("Failed to initialize Gazetteer");
                logger.severe(ExceptionUtils.getFullStackTrace(e));
            }

            featureIndex.close();
            classIndex.close();
        } catch (Exception e) {
            logger.severe("An error has occurred getting description attributes");
            logger.severe(ExceptionUtils.getFullStackTrace(e));


        } finally {
            if (features != null) {
                features.close();

            }
            if (dataStore != null) {
                dataStore.dispose();

            }

        }
    }
}
