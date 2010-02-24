package org.ala.rest;

import org.geoserver.rest.ReflectiveResource;

public class GazetteerSearchResource extends ReflectiveResource {
   
   @Override
   protected Object handleObjectGet() throws Exception {
       //Map<String, Object> map = getRequest().getAttributes().get("q");
       String stringValue = getRequest().getAttributes().get("q").toString().split("=")[1]; //map.get("q");
       System.out.println(stringValue);
       
       return new GazetteerSearch(stringValue);
	}
}
