package org.ala.rest;

import java.util.ArrayList;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureIterator;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;

import org.vfny.geoserver.util.DataStoreUtils;

/***
 *
 * @author angus
 */
@XStreamAlias("search")
public class PointSearch {

    private static final Logger logger = Logging.getLogger("org.ala.rest.PointSearch");
    @XStreamAlias("results")
    ArrayList<SearchResultItem> results;
    @XStreamAlias("xmlns:xlink")
    @XStreamAsAttribute
    String xlink = "http://www.w3.org/1999/xlink";

    /***
     *
     * @return a HashMap representation of the resource - which will be serialized into xml/json
     */
    public Map getMap() {
        HashMap resultsMap = new HashMap();
        resultsMap.put("results", this.results);
        return resultsMap;
    }


    /**
     * Performs a point search across a single layer (if layer is empty string, it searches across all default layers).
     * @param lon
     * @param lat
     * @param layerName
     */
    public PointSearch(String lon, String lat, String[] layers) {
        results = new ArrayList<SearchResultItem>();
        GazetteerConfig gc = new GazetteerConfig();
        GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
        ServletContext sc = GeoServerExtensions.bean(ServletContext.class);
        Catalog catalog = gs.getCatalog();
        logger.info("Point search parameters " + lon + "," + lat + "," + layers);

        //here we want to search default layers
        if (layers.length == 0) {
            logger.finer("No actual layers here, searching default layers");
            ArrayList<String> defaultLayers = (ArrayList<String>) gc.getDefaultLayerNames();
            for (String layer : defaultLayers) {
                search(catalog, layer, sc, lon, lat, gc);
            }

        } else {
            for (String layerName : layers) {
                logger.finer("Searching specific layer " + layerName);
                search(catalog, layerName, sc, lon, lat, gc);
            }
        }
    }

        /**
     * Performs a point search across a single layer (if layer is empty string, it searches across all default layers).
     * @param lon
     * @param lat
     * @param layerName
     */
    public PointSearch(String lon, String lat, String layerName) {
        results = new ArrayList<SearchResultItem>();
        GazetteerConfig gc = new GazetteerConfig();
        GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
        ServletContext sc = GeoServerExtensions.bean(ServletContext.class);
        Catalog catalog = gs.getCatalog();
        logger.info("Point search parameters " + lon + "," + lat + "," + layerName);

        //here we want to search default layers
        if (layerName.compareTo("") == 0) {
            logger.finer("Searching default layers");
            ArrayList<String> defaultLayers = (ArrayList<String>) gc.getDefaultLayerNames();
            for (String layer : defaultLayers) {
                search(catalog, layer, sc, lon, lat, gc);
            }

        } else {
            logger.finer("Searching specific layer " + layerName);
            search(catalog, layerName, sc, lon, lat, gc);
        }
    }

    /**
     * Constructor to perform a search across the default layers
     * @param lon
     * @param lat
     */
    public PointSearch(String lon, String lat) {
        this(lon, lat, "");
    }

    /**
     * Refactored method to perform a layer based point search
     *
     * @param catalog
     * @param layerName
     * @param sc
     * @param lon
     * @param lat
     * @param gc
     */
    private void search(Catalog catalog, String layerName, ServletContext sc, String lon, String lat, GazetteerConfig gc) {
        try {
            
            if (!gc.layerNameExists(layerName)){
                logger.finer("layer " + layerName + " does not exist - trying aliases.");
                layerName = gc.getNameFromAlias(layerName);
                if (layerName.compareTo("") == 0){
                    logger.finer("no aliases found for layer, giving up");
                    return;
                }
            }
            
            LayerInfo layerInfo = catalog.getLayerByName(layerName);

            Map params = layerInfo.getResource().getStore().getConnectionParameters();
            DataStore dataStore = DataStoreUtils.acquireDataStore(params, sc); //DataStoreFinder.getDataStore(params);
            FeatureSource layer = dataStore.getFeatureSource(layerName);
            logger.info("Searching " + layerName);
            FeatureIterator features = layer.getFeatures(CQL.toFilter("CONTAINS(the_geom,POINT(" + lon + " " + lat + "))")).features();
            if (features.hasNext()) {
                logger.info("Found one!!!");
                while (features.hasNext()) {
                    Feature feature = (Feature) features.next();
                    String id = feature.getProperty(gc.getIdAttribute1Name(layerName)).getValue().toString();
                    results.add(new SearchResultItem(layerName, id));
                }
            }
            features.close();
        } catch (IOException e1) {
            logger.severe("IOException thrown in point search");
            logger.severe(ExceptionUtils.getFullStackTrace(e1));
        } catch (Exception e2) {
            logger.severe("Exception thrown in point search");
            logger.severe(ExceptionUtils.getFullStackTrace(e2));
        }
    }
}
