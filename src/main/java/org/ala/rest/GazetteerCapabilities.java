/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

/**
 *
 * @author jac24n
 */
public class GazetteerCapabilities {

    private static final Logger logger = Logging.getLogger("org.ala.rest.GazetteerCapabilities");
    //Map layersMap = new HashMap();
    ArrayList<Map> layers = new ArrayList<Map>();
    GazetteerConfig gc = new GazetteerConfig();
    GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
    ServletContext sc = GeoServerExtensions.bean(ServletContext.class);
    Catalog catalog = gs.getCatalog();

    public GazetteerCapabilities() {

        ArrayList<String> layerNames = (ArrayList<String>) gc.getLayerNames();


        for (String layerName : layerNames) {
            LayerInfo layerInfo = catalog.getLayerByName(layerName);
            //only care about enabled layers
            if (layerInfo.enabled()) {
                Map layerMap = new HashMap();
                logger.finer("fetching layer properties for " + layerName);
                layerMap.put("layer_name", layerInfo.getName());
                layerMap.put("alias", gc.getLayerAlias(layerName));
                layerMap.put("default", new Boolean(gc.isDefaultLayer(layerName)).toString());
                layers.add(layerMap);
            }
        }
    }

    public Map getJSONMap() {
        Map map = new HashMap();
        map.put("name", "ALA Gazetteer");
        map.put("geoserver_catalog_id", catalog.getId());
        map.put("layers", layers);
        return map;
    }
}
