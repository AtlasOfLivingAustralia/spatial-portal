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
       String id1 = getRequest().getAttributes().get("id1").toString();
       String id2 = null;
       if (getRequest().getAttributes().containsKey("id2"))
           id2 = getRequest().getAttributes().get("id2").toString();
       
       return new GazetteerFeature(layer,id1,id2).getJSONMap();
    }
}