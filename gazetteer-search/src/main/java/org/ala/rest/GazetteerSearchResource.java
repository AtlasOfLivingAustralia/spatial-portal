package org.ala.rest;

import org.geoserver.rest.ReflectiveResource;

public class GazetteerSearchResource extends ReflectiveResource {
   
   @Override
   protected Object handleObjectGet() throws Exception {
       //hmmm the getAttributes stuff doesn't seem to work as documented 
       String stringValue = getRequest().getAttributes().get("q").toString().split("=")[1]; 
       //System.out.println(stringValue);
       
       return new GazetteerSearch(stringValue);
   }
}
