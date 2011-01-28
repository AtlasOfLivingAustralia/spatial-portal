package org.ala.rest;

import com.vividsolutions.jts.geom.Geometry;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.geotools.data.DataStore;

import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;

import org.geoserver.config.GeoServer;

import org.geoserver.platform.GeoServerExtensions;
import org.geotools.filter.text.cql2.CQL;
import org.opengis.feature.Property;


import org.vfny.geoserver.util.DataStoreUtils;
import org.geoserver.wfs.response.GeoJSONBuilder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.GeometryType;
import org.opengis.geometry.BoundingBox;

/**
 *
 *@author angus
 */
public class GazetteerFeature {

    private static final Logger logger = Logging.getLogger("org.ala.rest.GazetteerFeature");
    String id;
    String name;
    Map advanced_properties; //detailed feature metadata
    Map<String,String> properties;    //basic feature metadata (to displayed in UI)
    List<String> geometries = new ArrayList<String>();

    /**
     * Instantiates a gazetteer feature given an id string and layer name
     * @param layerName
     * @param idAttribute1
     * @throws IOException
     * @throws Exception
     */
    public GazetteerFeature(String layerName, String idAttribute1) throws IOException, Exception {

        logger.finer("param: layerName: " + layerName);
        logger.finer("param: idAttribute1: " + idAttribute1);

        GazetteerConfig gc = new GazetteerConfig();

        GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
        ServletContext sc = GeoServerExtensions.bean(ServletContext.class);
        Catalog catalog = gs.getCatalog();

        //check to see if layer exists, if not check to see if alias exists ...
        if (!gc.layerNameExists(layerName)) {
            logger.finer("layer " + layerName + " does not exist - trying aliases.");
            layerName = gc.getNameFromAlias(layerName);
            if (layerName.compareTo("") == 0) {
                logger.finer("no aliases found for layer, giving up");
                return;
            }
        }

        //Use search to find matches (provides case insensitive matching)
        String[] layers = {layerName};
        String searchTerms = idAttribute1;

        Search search = new Search(searchTerms, layers, "id");
        ArrayList<SearchResultItem> results = search.getResults();
        if (results.size() == 0) {
            logger.severe("No results returned - this isn't great");
            throw new Exception("No results returned - this isn't great");
        }
        logger.finer("Number of hits is " + results.size());

        SearchResultItem searchResultItem = results.get(0);

        //grab the first result (highest hit)
        logger.finer("Match idAttribute1: " + searchResultItem.idAttribute1);

        idAttribute1 = searchResultItem.idAttribute1;

        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        Map params = layerInfo.getResource().getStore().getConnectionParameters();

        DataStore dataStore = DataStoreUtils.acquireDataStore(params, sc);

        try {

            if (dataStore == null) {
                throw new Exception("Could not find datastore for this layer");

            } else {
                FeatureSource layer = dataStore.getFeatureSource(layerName);
                String cql = gc.getIdAttribute1Name(layerName) + "='" + idAttribute1.replace('_', ' ') + "'";
                logger.finer("cql: " + cql);

                FeatureIterator features = layer.getFeatures(CQL.toFilter(cql)).features();

                try {
                    if (features.hasNext()) {
                        while (features.hasNext()) {
                            Feature feature = (Feature) features.next();
                            String layerAlias = "";
                            layerAlias = gc.getLayerAlias(layerName);
                            if (layerAlias.compareTo("") == 0){
                                layerAlias = layerName;
                            }
                            logger.finer("Layer Alias is : " + layerAlias);

                            this.id = layerAlias + "/" + feature.getProperty(gc.getIdAttribute1Name(layerName)).getValue().toString().replace(" ", "_");
                            logger.info("Feature ID is : " + this.id);

                            this.properties = new HashMap<String,String>();
                            this.properties.put("Feature_ID", this.id);

                            this.name = feature.getProperty(gc.getNameAttributeName(layerName)).getValue().toString();
                            logger.info("Feature Name is : " + this.name);
                            this.properties.put("Feature_Name", this.name);

                            //Construct a geoJSON representation of the geometry using GeoJSONBuilder
                            //logger.info("Feature geom is " + feature.getDefaultGeometryProperty().getValue().toString());

                            StringWriter w = new StringWriter();
                            GeoJSONBuilder geoJson = new GeoJSONBuilder(w);
                            geoJson.writeGeom((Geometry) feature.getDefaultGeometryProperty().getValue());


                            BoundingBox bb = feature.getBounds();

                            Double minx = new Double(bb.getMinX());
                            Double miny = new Double(bb.getMinY());
                            Double maxx = new Double(bb.getMaxX());
                            Double maxy = new Double(bb.getMaxY());

                            if (maxx.compareTo(minx) == 0 && maxy.compareTo(miny) == 0){
                                String point = "(" + strRoundDouble(miny) + "," + strRoundDouble(minx) + ")";
                                this.properties.put("Point", point);
                                logger.finer("Point is: " + point);
                            }
                            else{
                                String boundingBox = "((" + strRoundDouble(miny) + "," + strRoundDouble(minx) + "),(" + strRoundDouble(maxy) + "," + strRoundDouble(maxx) + "))";
                                this.properties.put("Bounding_Box", boundingBox);
                                logger.finer("Bounding box is: " + boundingBox);
                            }
			                //Get Metadata link from config
                            this.properties.put("Layer_Metadata", gc.getMetadataPath(layerName));
                            logger.finer("Layer metadata url is " + gc.getBaseURL() + "/layers/" + layerName);
                            this.geometries.add(w.toString());

                            //Add all the feature properties to the geojson properties object
                            Collection<Property> featureProperties = feature.getProperties();
                            String geomName = feature.getDefaultGeometryProperty().getName().toString();
                            this.advanced_properties = new HashMap();
                            for (Property property : featureProperties) {
                                if ((property.getName() != null) && (property.getValue() != null) && (!(property.getName().toString().contentEquals(geomName)))) {
                                    this.properties.put(property.getName().toString(), property.getValue().toString());
                                }
                            }
                        }
                    } else {
                        throw new Exception("Could not find feature");
                    }
                } finally {
                    features.close();
                }

            }
        } finally {
            dataStore.dispose();

        }
    }

    public Map getJSONMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", "GeometryCollection");
        map.put("id", this.id);
        map.put("name", this.name);
        map.put("properties", this.properties);
        map.put("geometries", this.geometries);
        return map;
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
}

