package org.ala.rest;

import org.geoserver.test.GeoServerTestSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

public class GazetteerSearchResourceTest extends GeoServerTestSupport {

     public void testGetAsXML() throws Exception {
     //make the request, parsing the result as a dom
     Document dom = getAsDOM( "/rest/result.xml" );

     //print out the result
     print(dom);

     //make assertions
     Node message = getFirstElementByTagName( dom, "message");
     assertNotNull(message);
     assertEquals( "Search Result", message.getFirstChild().getNodeValue() );
   }

    public void testGetAsJSON() throws Exception {
     //make the request, parsing the result into a json object
     JSON json = getAsJSON( "/rest/result.json");

     //print out the result
     print(json);

     //make assertions
     assertTrue( json instanceof JSONObject );
     JSONObject hello = ((JSONObject) json).getJSONObject( "org.ala.rest.GazetteerSearch" );
     assertEquals( "Search Result", hello.get( "message" ) );
   } 
}
