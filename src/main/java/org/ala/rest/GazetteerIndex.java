package org.ala.rest;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.apache.commons.lang.exception.ExceptionUtils;
import org.geotools.util.logging.Logging;

/***
 * Builds the Gazetter index based on the gazetter config and layers/features in Geoserver
 * @author Angus
 */
public class GazetteerIndex implements InitializingBean {

    private static final Logger logger = Logging.getLogger("org.ala.rest.GazetteerIndex");

    /***
     * The Gazetteer index is built here if one does not exist already
     */
    @Override
    public void afterPropertiesSet() {
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

            //Initialize lucene index
            File featureIndexDir = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-index");
            File classIndexDir = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-class-index");
            if (featureIndexDir.exists()) {
                return;//FileUtils.forceDelete(file);
            } else {
                FileUtils.forceMkdir(featureIndexDir);
                FileUtils.forceMkdir(classIndexDir);

                StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
                IndexWriter featureIndex = new IndexWriter(FSDirectory.open(featureIndexDir), analyzer /*Version.LUCENE_CURRENT)*/, true, IndexWriter.MaxFieldLength.UNLIMITED);
                IndexWriter classIndex = new IndexWriter(FSDirectory.open(classIndexDir), analyzer, true, IndexWriter.MaxFieldLength.UNLIMITED);

                for (String layerName : gc.getLayerNames()) {
                    LayerInfo layerInfo = catalog.getLayerByName(layerName);

                    ResourceInfo layerResource = layerInfo.getResource();
                    StoreInfo layerStore = layerResource.getStore();
                    Map params = layerStore.getConnectionParameters();//layerInfo.getResource().getStore().getConnectionParameters();

                    dataStore = DataStoreUtils.acquireDataStore(params, sc);//DataStoreFinder.getDataStore(params);
                    Set classNames = new HashSet();
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

                            if (gc.getIdAttribute2Name(layerName).compareTo("") != 0) {
                                String idAttribute1 = feature.getProperty(gc.getIdAttribute1Name(layerName)).getValue().toString();
                                String idAttribute2 = feature.getProperty(gc.getIdAttribute2Name(layerName)).getValue().toString();
                                featureDoc.add(new Field("idAttribute1", idAttribute1, Store.YES, Index.ANALYZED));
                                featureDoc.add(new Field("idAttribute2", idAttribute2, Store.YES, Index.ANALYZED));
                                featureDoc.add(new Field("id", idAttribute1 + " " + idAttribute2, Store.YES, Index.ANALYZED));
                                logger.finer("Indexed layer " + layerName + " idAttribute1: " + idAttribute1 + " idAttribute2: " + idAttribute2);
                            } else {
                                if (feature.getProperty(gc.getIdAttribute1Name(layerName)).getValue() != null) {
                                    String idAttribute1 = feature.getProperty(gc.getIdAttribute1Name(layerName)).getValue().toString();
                                    featureDoc.add(new Field("idAttribute1", idAttribute1, Store.YES, Index.ANALYZED));
                                    featureDoc.add(new Field("id", idAttribute1, Store.YES, Index.ANALYZED));
                                    logger.finer("Indexed layer " + layerName + " idAttribute1: " + idAttribute1);
                                } else {
                                    logger.severe("Null value retrieved for idAttribute1");
                                }
                            }

                            if (feature.getProperty(gc.getNameAttributeName(layerName)).getValue() != null) {
                                featureDoc.add(new Field("name", feature.getProperty(gc.getNameAttributeName(layerName)).getValue().toString().toLowerCase(), Store.YES, Index.ANALYZED));
                            }

                            if (!gc.getClassAttributeName(layerName).contentEquals("none")) {
                                //building a set of classes for this layer - indexed separately
                                if (feature.getProperty(gc.getClassAttributeName(layerName)).getValue() != null) {
                                    classNames.add(feature.getProperty(gc.getClassAttributeName(layerName)).getValue().toString());
                                }
                            }

                            //GJ: changed from type to layerName - it was confusing
                            featureDoc.add(new Field("layerName", layerName, Store.YES, Index.ANALYZED));

                            //Add all the other feature properties to the index as well but not for searching
                            String geomName = feature.getDefaultGeometryProperty().getName().toString();
                            for (Property property : feature.getProperties()) {
                                if ((descriptionAttributes.contains(property.getName().toString())) && (property.getValue() != null)) { //&& (!(property.getName().toString().contentEquals(geomName)))) {
                                    featureDoc.add(new Field(property.getName().toString(), property.getValue().toString(), Store.YES, Index.NO));
                                }
                            }

                            featureIndex.addDocument(featureDoc);
                            //System.out.println(".");
                        }
                        features.close();

                    }
                    dataStore.dispose();

                    //indexing classes with layer name

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
                featureIndex.close();
                classIndex.close();
            }
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
