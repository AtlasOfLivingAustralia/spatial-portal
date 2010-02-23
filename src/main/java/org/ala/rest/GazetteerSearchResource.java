package org.ala.rest;

import org.geoserver.rest.ReflectiveResource;

public class GazetteerSearchResource extends ReflectiveResource {

   @Override
   protected Object handleObjectGet() throws Exception {
	return new GazetteerSearch( "Search Result" );   
	}
}
