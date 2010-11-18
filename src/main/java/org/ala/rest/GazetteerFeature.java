package org.ala.rest;

import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.queryParser.ParseException;
import org.geotools.data.DataStore;

import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

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
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 *
 *@author angus
 */
public class GazetteerFeature {

    private static final Logger logger = Logging.getLogger("org.ala.rest.GazetteerFeature");
    String id;
    String name;
    Map properties;
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

                            this.name = feature.getProperty(gc.getNameAttributeName(layerName)).getValue().toString();
                            logger.info("Feature Name is : " + this.name);

                            //Construct a geoJSON reperesntation of the geometry uing GeoJSONBuilder
                            //logger.info("Feature geom is " + feature.getDefaultGeometryProperty().getValue().toString());

                            StringWriter w = new StringWriter();
                            GeoJSONBuilder geoJson = new GeoJSONBuilder(w);
                            geoJson.writeGeom((Geometry) feature.getDefaultGeometryProperty().getValue());
                            this.geometries.add(w.toString());

                            //Add all the feature properties to the geojson properties object
                            Collection<Property> featureProperties = feature.getProperties();
                            String geomName = feature.getDefaultGeometryProperty().getName().toString();
                            this.properties = new HashMap();
                            for (Property property : featureProperties) {
                                //logger.info("GazetteerFeature: " + property.toString());
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

    /**
     * Use lucene to get idAttribute1 and (maybe) idAttribute2
     * @param id
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private Document luceneSearch(String id) throws IOException, ParseException {
        Document doc = null;
        File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-index");
        IndexSearcher is = new IndexSearcher(FSDirectory.open(file));
        QueryParser qp = new QueryParser(Version.LUCENE_CURRENT, "id", new StandardAnalyzer(Version.LUCENE_CURRENT));
        Query nameQuery = qp.parse(id);

        TopDocs topDocs = is.search(nameQuery, 1);

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            doc = is.doc(scoreDoc.doc);
        }
        return doc;
    }

    public Map getJSONMap() {
        Map map = new HashMap();
        map.put("type", "GeometryCollection");
        map.put("id", this.id);
        map.put("name", this.name);
        map.put("properties", this.properties);
        map.put("geometries", this.geometries);
        return map;
    }
}
