package org.ala.rest;

import java.util.Map;
import org.geoserver.rest.MapResource;



/**
 * This class handles requests for gazetteer features and returns a Map which
 * is serialized as GeoJSON
 * @author angus
 */
public class FeatureServiceResource extends MapResource {//ReflectiveResource {

    @Override
    public Map getMap() throws Exception {
       String layer = getRequest().getAttributes().get("layer").toString();
       String key = getRequest().getAttributes().get("type").toString();

       return new GazetteerFeature(layer,key).getJSONMap();

    }
}