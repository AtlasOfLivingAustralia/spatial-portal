package org.ala.rest;

import java.util.Map;
import org.geoserver.rest.MapResource;



/**
 *
 * @author angus
 */
public class FeatureServiceResource extends MapResource {//ReflectiveResource {

   
//    @Override
//    protected Object handleObjectGet() throws Exception {
//       String layer = getRequest().getAttributes().get("layer").toString();
//       String key = getRequest().getAttributes().get("type").toString();
//
//       return new GazetteerFeature(layer,key);
//
//    }

    @Override
    public Map getMap() throws Exception {
       String layer = getRequest().getAttributes().get("layer").toString();
       String key = getRequest().getAttributes().get("type").toString();

       return new GazetteerFeature(layer,key).getMap();

    }
}