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
import com.vividsolutions.jts.operation.distance.DistanceOp;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
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
import org.geotools.measure.Measure;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.vfny.geoserver.util.DataStoreUtils;

/***
 *
 * @author angus
 */
@XStreamAlias("search")
public class ClosestFeatureSearch {

    private static final Logger logger = Logging.getLogger("org.ala.rest.ClosestFeatureSearch");
    @XStreamAlias("results")
    ArrayList<ClosestFeatureSearchResultItem> results;
    @XStreamAlias("xmlns:xlink")
    @XStreamAsAttribute
    String xlink = "http://www.w3.org/1999/xlink";

    /**
     * Constructor to perform a search across the default layers
     * @param lon
     * @param lat
     */
    public ClosestFeatureSearch(String lon, String lat, int radius, int count) {
        this(lon, lat, radius, count, "");
    }

    /**
     * Performs a closest feature search across a single layer (if layer is empty string, it searches across all default layers).
     * @param lon
     * @param lat
     * @param layerName
     */
    public ClosestFeatureSearch(String lon, String lat, int radius, int count, String[] layers) {
        results = new ArrayList<ClosestFeatureSearchResultItem>();
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
                search(catalog, layer, sc, lon, lat, radius, count, gc);
            }

        } else {
            for (String layerName : layers) {
                logger.finer("Searching specific layer " + layerName);
                search(catalog, layerName, sc, lon, lat, radius, count, gc);
            }
        }
    }

    /**
     * Performs a closest feature search across a single layer (if layer is empty string, it searches across all default layers).
     * @param lon
     * @param lat
     * @param layerName
     */
    public ClosestFeatureSearch(String lon, String lat, int radius, int count, String layerName) {
        results = new ArrayList<ClosestFeatureSearchResultItem>();
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
                search(catalog, layer, sc, lon, lat, radius, count, gc);
            }

        } else {
            logger.finer("Searching specific layer " + layerName);
            search(catalog, layerName, sc, lon, lat, radius, count, gc);
        }
    }

    /**
     * Refactored method to perform a closest feature search
     *
     * @param catalog
     * @param layerName
     * @param sc
     * @param lon
     * @param lat
     * @param gc
     */
    private void search(Catalog catalog, String layerName, ServletContext sc, String lon, String lat, int radius, int count, GazetteerConfig gc) {

        try {
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
            Coordinate coord = new Coordinate(new Double(lon).doubleValue(), new Double(lat).doubleValue());
            Point p = geometryFactory.createPoint(coord);

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
            String cqlFilter = "WITHIN(the_geom," + polygonWKT + ")";
            logger.finer("Running cql filter: " + cqlFilter);
            features = layer.getFeatures(CQL.toFilter(cqlFilter)).features();
            logger.finer("Number of candidate results is " + layer.getFeatures(CQL.toFilter(cqlFilter)).size());
            Feature nearestFeature = null;
            if (features.hasNext()) {

                ArrayList<ClosestFeatureSearchResultItem> matches = new ArrayList<ClosestFeatureSearchResultItem>();
                while (features.hasNext()) {
                    double nearestDistance = 9e9;
                    double nearestBearing = 0;
                    SimpleFeature f = (SimpleFeature) features.next();
                    Geometry geom = (Geometry) f.getDefaultGeometry();
                    DistanceOp op = new DistanceOp(p, geom);
                    Coordinate[] co = op.closestPoints();
                    Measure m = DefaultGeographicCRS.WGS84.distance(new double[]{co[0].x, co[0].y,}, new double[]{co[1].x, co[1].y,});
                    nearestFeature = f;
                    nearestDistance = m.doubleValue();
                    logger.finer("Coordinate 1 is " + new Double(co[0].x).toString() + ", " + new Double(co[0].y).toString());
                    logger.finer("Coordinate 2 is " + new Double(co[1].x).toString() + ", " + new Double(co[1].y).toString());
                    nearestBearing = calcBearing(co);
                    logger.finer("Bearing is " + new Double(nearestBearing).toString());

                    String id1 = nearestFeature.getProperty(gc.getIdAttribute1Name(layerName)).getValue().toString();
                    String name = "";
                    if (gc.getNameAttributeName(layerName).compareTo("") != 0) {
                        name = nearestFeature.getProperty(gc.getNameAttributeName(layerName)).getValue().toString();
                    } else {
                        name = nearestFeature.getProperty(gc.getIdAttribute1Name(layerName)).getValue().toString();
                    }
                    if (gc.getIdAttribute2Name(layerName).compareTo("") != 0) {
                        String id2 = nearestFeature.getProperty(gc.getIdAttribute2Name(layerName)).getValue().toString();
                        matches.add(new ClosestFeatureSearchResultItem(layerName, name, id1, id2, strRoundDouble(new Double(nearestDistance)), strRoundDouble(new Double(nearestBearing))));
                    } else {
                        matches.add(new ClosestFeatureSearchResultItem(layerName, name, id1, strRoundDouble(new Double(nearestDistance)), strRoundDouble(new Double(nearestBearing))));
                    }
                }

                //sort the matches by distance
                Collections.sort(matches, new DistanceComparator());
                int counter = 0;

                //grab count entries to display
                for (ClosestFeatureSearchResultItem closestFeatureSearchResultItem : matches) {
                    if (counter >= count)
                        break;
                    logger.finer("Result found feature name " + closestFeatureSearchResultItem.name + " distance " + closestFeatureSearchResultItem.distance + " bearing " + closestFeatureSearchResultItem.bearing);
                    results.add(closestFeatureSearchResultItem);
                    counter++;
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

    /**
     * Calculates the bearing between two points
     * @param coords
     * @return
     */
    private double calcBearing(Coordinate[] coords) {
        //this uses the bearing algorithm detailed on http://www.movable-type.co.uk/scripts/latlong.html
        double lat1 = Math.toRadians(coords[0].y);
        double lon1 = Math.toRadians(coords[0].x);
        double lat2 = Math.toRadians(coords[1].y);
        double lon2 = Math.toRadians(coords[1].x);
        return ((Math.toDegrees(Math.atan2(Math.sin(lon2-lon1)*Math.cos(lat2), Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(lon2-lon1))))+360)%360;
    }

    /**
     * Returns a string representation of a Double to four decimal places
     * @param inValue
     * @return
     */
    public String strRoundDouble(Double inValue){
        DecimalFormat fourDec = new DecimalFormat("0.0000");
        fourDec.setGroupingUsed(false);
        return fourDec.format(inValue.doubleValue());
    }

    /**
     * This class is responsible for comparing distances of ClosestFeatureSearchResultItems
     * This is used to get the nearest points
     */
    class DistanceComparator implements Comparator {

        public int compare(Object feature1, Object feature2) {

            double feature1_distance = new Double(((ClosestFeatureSearchResultItem) feature1).distance).doubleValue();
            double feature2_distance = new Double(((ClosestFeatureSearchResultItem) feature2).distance).doubleValue();

            if (feature1_distance > feature2_distance) {
                return 1;
            } else if (feature1_distance < feature2_distance) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
