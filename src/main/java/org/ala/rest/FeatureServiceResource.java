package org.ala.rest;

import org.geoserver.rest.ReflectiveResource;



/**
 *
 * @author angus
 */
public class FeatureServiceResource extends ReflectiveResource {

   
    @Override
    protected Object handleObjectGet() throws Exception {
       String layer = getRequest().getAttributes().get("layer").toString();
       String key = getRequest().getAttributes().get("type").toString();



       return new GazetteerFeature(layer,key);

    }
}