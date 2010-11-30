package org.ala.rest;

import java.util.ArrayList;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTWriter;
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
import org.geotools.geometry.jts.JTSFactoryFinder;
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

    /**
     * Constructor to perform a search across the default layers
     * @param lon
     * @param lat
     */
    public PointSearch(String lon, String lat, int radius) {
        this(lon, lat, radius, "");
    }

    /**
     * Performs a point search across a single layer (if layer is empty string, it searches across all default layers).
     * @param lon
     * @param lat
     * @param layerName
     */
    public PointSearch(String lon, String lat, int radius, String[] layers) {
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
                search(catalog, layer, sc, lon, lat, radius, gc);
            }

        } else {
            for (String layerName : layers) {
                logger.finer("Searching specific layer " + layerName);
                search(catalog, layerName, sc, lon, lat, radius, gc);
            }
        }
    }

    /**
     * Performs a point search across a single layer (if layer is empty string, it searches across all default layers).
     * @param lon
     * @param lat
     * @param layerName
     */
    public PointSearch(String lon, String lat, int radius, String layerName) {
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
                search(catalog, layer, sc, lon, lat, radius, gc);
            }

        } else {
            logger.finer("Searching specific layer " + layerName);
            search(catalog, layerName, sc, lon, lat, radius, gc);
        }
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
    private void search(Catalog catalog, String layerName, ServletContext sc, String lon, String lat, int radius, GazetteerConfig gc) {
        
        try {

            if (!gc.layerNameExists(layerName)) {
                logger.finer("layer " + layerName + " does not exist - trying aliases.");
                layerName = gc.getNameFromAlias(layerName);
                if (layerName.compareTo("") == 0) {
                    logger.finer("no aliases found for layer, giving up");
                    return;
                }
            }

            LayerInfo layerInfo = catalog.getLayerByName(layerName);

            Map params = layerInfo.getResource().getStore().getConnectionParameters();
            DataStore dataStore = DataStoreUtils.acquireDataStore(params, sc); //DataStoreFinder.getDataStore(params);
            FeatureSource layer = dataStore.getFeatureSource(layerName);
            logger.info("Searching " + layerName);
            FeatureIterator features = null;
            if (radius == 0) {
                logger.finer("No radius has been specified");
                features = layer.getFeatures(CQL.toFilter("CONTAINS(the_geom,POINT(" + lon + " " + lat + "))")).features();
            } else {

                GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
                Coordinate coord = new Coordinate(new Double(lon).doubleValue(), new Double(lat).doubleValue());
                Point p = geometryFactory.createPoint(coord);

                //divide radius by 111.12 to get kilometres
                double radiusInDegrees = radius / 111.12;

                logger.finer("radius " + radius + " (km) in degrees is " + radiusInDegrees);

                Geometry polygon = p.buffer(radiusInDegrees);

                Coordinate[] coordinates = polygon.getCoordinates();
                logger.finer("Number of coordinates returned is " + coordinates.length);
                logger.finer("numpoints: " + polygon.getNumPoints());

                //wkt writer - well worth using
                WKTWriter wkt = new WKTWriter();
                String polygonWKT = wkt.write(polygon);

                logger.finer("polygonWKT is " + polygonWKT);

                //perform intersect operation to find out what layers the circle intersects with
                String cqlFilter = "INTERSECT(the_geom," + polygonWKT + ")";
                logger.finer("Running cql filter: " + cqlFilter);
                features = layer.getFeatures(CQL.toFilter(cqlFilter)).features();
            }
            if (features.hasNext()) {
                logger.info("Found one!!!");
                while (features.hasNext()) {
                    Feature feature = (Feature) features.next();
                    String id1 = feature.getProperty(gc.getIdAttribute1Name(layerName)).getValue().toString();
                    String id2 = feature.getProperty(gc.getIdAttribute2Name(layerName)).getValue().toString();
                    results.add(new SearchResultItem(layerName, id1, id2, new Float("1.0")));
                }
            }
            features.close();
            dataStore.dispose();
        } catch (IOException e1) {
            logger.severe("IOException thrown in point search");
            logger.severe(ExceptionUtils.getFullStackTrace(e1));
        } catch (Exception e2) {
            logger.severe("Exception thrown in point search");
            logger.severe(ExceptionUtils.getFullStackTrace(e2));
        }
       
    }

    /***
     *
     * @return a HashMap representation of the resource - which will be serialized into xml/json
     */
    public Map getMap() {
        HashMap resultsMap = new HashMap();
        resultsMap.put("results", this.results);
        return resultsMap;
    }
}
