/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletContext;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.vfny.geoserver.util.DataStoreUtils;

/**
 * Represents a list of features (as hyperlinks) for a given layer
 * @author angus
 */
public class FeatureList {

    private static final Logger logger = Logging.getLogger("org.ala.rest.FeatureList");
    List<String> links = new ArrayList();
    private static int PAGE_SIZE = 100;
    private int total_features;
    private int page;
    private String layerName;
    private GazetteerConfig gc = new GazetteerConfig();
    private String format = "";

//    public FeatureList(String layerName,int page, String format) throws IOException, Exception {
//        this(layerName, page);
//        this.format = format;
//    }

    public FeatureList(String layerName) throws IOException, Exception {
        this(layerName, 1);
    }

    public FeatureList(String layerName, int page) throws IOException, Exception {
        logger.finer("param: layerName: " + layerName);
        this.page = page;
        this.layerName = layerName;
        

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
                FeatureIterator features = layer.getFeatures().features(); //FIXME: this could be a lot faster if we didn't request the_geom?.
                //Feature[] features = (Feature[])layer.getFeatures().t;

                String idAttribute1Name = gc.getIdAttribute1Name(layerName);
                String idAttribute2Name = gc.getIdAttribute2Name(layerName);
                total_features = layer.getCount(Query.ALL);
                if (features.hasNext()) {
                    int featureNumber = 0;
                    System.out.println("PAGE: " + page );
                    while (features.hasNext()) {   
                        Feature feature = features.next(); //TODO: Really dumb way to do paging - but FeatureSource doesn't seem to allow anything else
                        if (featureNumber == PAGE_SIZE * (page)) {
                            System.out.println("Finished page");
                            break;
                        }
                        if (featureNumber >= PAGE_SIZE * (page-1)) { 
                            String link = gc.getBaseURL() + "/" + layerName + "/" + feature.getProperty(idAttribute1Name).getValue().toString();
                            if (idAttribute2Name.compareTo("") != 0) {
                                link += "/" + feature.getProperty(idAttribute2Name).getValue().toString();
                            }
                            link += ".json";
                            links.add(link.replace(" ", "_"));
                        }  
                        featureNumber++;
                    }
                } else {
                    throw new Exception("Could not find feature");
                }


            }
        } finally {
            dataStore.dispose();

        }
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Map getMap() {
        Map map = new HashMap();
        map.put("features", this.links);
        if (total_features > page * PAGE_SIZE) {
            int nextPage = page + 1;
            String next = String.valueOf(nextPage);
            if (!format.isEmpty())
                    next = String.valueOf(nextPage) + "." + format;

            map.put("next", gc.getBaseURL() + "/" + layerName + "/features/" + next );
        }
        return map;
    }
}


