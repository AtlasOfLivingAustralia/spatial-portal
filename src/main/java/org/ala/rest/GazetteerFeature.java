package org.ala.rest;

import com.vividsolutions.jts.geom.Geometry;
import java.io.IOException;
import java.io.StringWriter;
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

/**
 *
 *@author angus
 */
public class GazetteerFeature {

    private static final Logger logger = Logging.getLogger("org.ala.rest.GazetteerFeature");
    String id;
    String name;
    Map advanced_properties; //detailed feature metadata
    Map basic_properties;    //basic feature metadata (to displayed in UI)
    List<String> geometries = new ArrayList();

    public GazetteerFeature(String layerName, String idAttribute1) throws IOException, Exception {
        this(layerName, idAttribute1, "");
    }

    /**
     * Instantiates a gazetteer feature given an id string and layer name
     * @param layerName
     * @param id
     * @throws IOException
     * @throws Exception
     */
    public GazetteerFeature(String layerName, String idAttribute1, String idAttribute2) throws IOException, Exception {

        logger.finer("param: layerName: " + layerName);
        logger.finer("param: idAttribute1: " + idAttribute1);
        logger.finer("param: idAttribute2: " + idAttribute2);

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
        if (idAttribute2.compareTo("") != 0) {
            searchTerms += " " + idAttribute2;
        }

        Search search = new Search(searchTerms, layers);
        ArrayList<SearchResultItem> results = search.getResults();
        if (results.size() == 0) {
            logger.severe("No results returned - this isn't great");
            throw new Exception("No results returned - this isn't great");
        }
        logger.finer("Number of hits is " + results.size());
        if (results.size() > 1) {
            logger.severe("Too many features match this id, please be more specific.");
            throw new Exception("Too many features match this id, please be more specific.");
        }

        SearchResultItem searchResultItem = results.get(0);

        //grab the first result (highest hit)
        logger.finer("Match idAttribute1: " + searchResultItem.idAttribute1);
        logger.finer("Match idAttribute2: " + searchResultItem.idAttribute2);
        idAttribute1 = searchResultItem.idAttribute1;
        if (searchResultItem.idAttribute2 == null) {
            idAttribute2 = "";
        } else {
            idAttribute2 = searchResultItem.idAttribute2;
        }

        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        Map params = layerInfo.getResource().getStore().getConnectionParameters();

        DataStore dataStore = DataStoreUtils.acquireDataStore(params, sc);

        try {

            if (dataStore == null) {
                throw new Exception("Could not find datastore for this layer");

            } else {
                FeatureSource layer = dataStore.getFeatureSource(layerName);
                //if table has a second id attribute ...
                String cql = gc.getIdAttribute1Name(layerName) + "='" + idAttribute1.replace('_', ' ') + "'";
                if (idAttribute2.compareTo("") != 0) {
                    cql = cql + " AND " + gc.getIdAttribute2Name(layerName) + "='" + idAttribute2.replace('_', ' ') + "'";
                }
                logger.finer("cql: " + cql);

                FeatureIterator features = layer.getFeatures(CQL.toFilter(cql)).features();

                try {
                    if (features.hasNext()) {
                        while (features.hasNext()) {
                            Feature feature = (Feature) features.next();

                            this.id = layerName + "/" + feature.getProperty(gc.getIdAttribute1Name(layerName)).getValue().toString();
                            if (gc.getIdAttribute2Name(layerName).compareTo("") != 0) {
                                this.id += "/" + feature.getProperty(gc.getIdAttribute2Name(layerName)).getValue().toString();
                            }
                            logger.info("Feature ID is : " + this.id);

                            this.basic_properties = new HashMap();
                            this.basic_properties.put("Feature_ID", this.id);

                            this.name = feature.getProperty(gc.getNameAttributeName(layerName)).getValue().toString();
                            logger.info("Feature Name is : " + this.name);
                            this.basic_properties.put("Feature_Name", this.name);

                            //Construct a geoJSON reperesntation of the geometry uing GeoJSONBuilder
                            //logger.info("Feature geom is " + feature.getDefaultGeometryProperty().getValue().toString());

                            StringWriter w = new StringWriter();
                            GeoJSONBuilder geoJson = new GeoJSONBuilder(w);
                            geoJson.writeGeom((Geometry) feature.getDefaultGeometryProperty().getValue());
                            this.geometries.add(w.toString());

                            //Add all the feature properties to the geojson properties object
                            Collection<Property> featureProperties = feature.getProperties();
                            String geomName = feature.getDefaultGeometryProperty().getName().toString();
                            this.advanced_properties = new HashMap();
                            for (Property property : featureProperties) {
                                //logger.info("GazetteerFeature: " + property.toString());
                                if ((property.getName() != null) && (property.getValue() != null) && (!(property.getName().toString().contentEquals(geomName)))) {
                                    this.advanced_properties.put(property.getName().toString(), property.getValue().toString());
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
        Map map = new HashMap();
        map.put("type", "GeometryCollection");
        map.put("id", this.id);
        map.put("name", this.name);
        map.put("properties", this.basic_properties);
        map.put("advanced_properties", this.advanced_properties);
        map.put("geometries", this.geometries);
        return map;
    }
}
