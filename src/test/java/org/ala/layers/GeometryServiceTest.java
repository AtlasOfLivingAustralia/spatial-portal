package org.ala.layers;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This class is to help with debugging of the geometry service. The tests are all ignored so that they do not get run as part of the build process.
 * @author ChrisF
 *
 */
public class GeometryServiceTest {

    @Test
    @Ignore
    public void testgeometryWkt() throws Exception {
        String json = "{\"wkt\": \"POLYGON((125.5185546875 -21.446934836644,128.2431640625 -21.446934836644,128.2431640625 -19.138942324356,125.5185546875 -19.138942324356,125.5185546875 -21.446934836644))\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/geometry/wkt");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.matches("\\{\"id\":\\d+\\}"));
    }

    @Test
    @Ignore
    public void testgeometryGeojson() throws Exception {
        String json = "{\"geojson\": \"{\\\"type\\\":\\\"Polygon\\\",\\\"coordinates\\\":[[[125.5185546875,-21.446934836644001],[128.2431640625,-21.446934836644001],[128.2431640625,-19.138942324356002],[125.5185546875,-19.138942324356002],[125.5185546875,-21.446934836644001]]]}\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/geometry/geojson");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.matches("\\{\"id\":\\d+\\}"));
    }
    
    @Test
    @Ignore
    public void testMalformedJSON() throws Exception{
        String json = "#%#%#";        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/geometry/wkt");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.equals("{\"error\":\"Malformed request. Expecting a JSON object.\"}")); 
    }
    
    @Test
    @Ignore
    public void testInvalidJSON() throws Exception{
        // Using a list here when we need a map
        String json = "[1, 2, 3]";        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/geometry/wkt");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.equals("{\"error\":\"Malformed request. Expecting a JSON object.\"}")); 
    }
    
    @Test
    @Ignore
    public void testMalformedWkt() throws Exception {
        String json = "{\"wkt\": \"lalala\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/geometry/wkt");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.equals("{\"error\":\"Malformed WKT.\"}"));
    }
    
    @Test
    @Ignore
    public void testMalformedGeoJSON() throws Exception {
        String json = "{\"geojson\": \"lalala\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/geometry/geojson");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.equals("{\"error\":\"Malformed GeoJSON geometry. Note that only GeoJSON geometries can be supplied here. Features and FeatureCollections cannot.\"}"));
    }
    
    //Invalid pid

    @Test
    @Ignore
    public void testGetGeoJson() throws Exception {
        HttpClient httpClient = new HttpClient();
        GetMethod get = new GetMethod("http://localhost:8080/layers-service/geometry/1/geojson");

        
        int returnCode = httpClient.executeMethod(get);
        Assert.assertEquals(200, returnCode);
        
        String responseText = get.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.equals("{\"geojson\":\"{\\\"type\\\":\\\"Polygon\\\",\\\"coordinates\\\":[[[125.5185546875,-21.446934836644001],[128.2431640625,-21.446934836644001],[128.2431640625,-19.138942324356002],[125.5185546875,-19.138942324356002],[125.5185546875,-21.446934836644001]]]}\",\"id\":1,\"name\":\"test\",\"description\":\"test\",\"user_id\":\"chris\",\"time_added\":\"1970-01-01T09:41+1000\"}")); 
    }

    @Test
    @Ignore
    public void testGetWkt() throws Exception {
        HttpClient httpClient = new HttpClient();
        GetMethod get = new GetMethod("http://localhost:8080/layers-service/geometry/1/wkt");

        
        int returnCode = httpClient.executeMethod(get);
        Assert.assertEquals(200, returnCode);
        
        String responseText = get.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.equals("{\"wkt\":\"POLYGON((125.5185546875 -21.446934836644,128.2431640625 -21.446934836644,128.2431640625 -19.138942324356,125.5185546875 -19.138942324356,125.5185546875 -21.446934836644))\",\"id\":1,\"name\":\"test\",\"description\":\"test\",\"user_id\":\"chris\",\"time_added\":\"1970-01-01T09:41+1000\"}"));
    }

    @Test
    @Ignore
    public void testDelete() throws Exception {
        // Create layer
        String json = "{\"wkt\": \"POLYGON((125.5185546875 -21.446934836644,128.2431640625 -21.446934836644,128.2431640625 -19.138942324356,125.5185546875 -19.138942324356,125.5185546875 -21.446934836644))\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/geometry/wkt");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        String responseText = post.getResponseBodyAsString();
        String pid = responseText.replace("{", "").replace("}", "").split(":")[1];
        
        // Delete layer
        DeleteMethod delete = new DeleteMethod("http://localhost:8080/layers-service/geometry/delete/" + pid);
        int returnCode2 = httpClient.executeMethod(delete);
        Assert.assertEquals(200, returnCode2);
        String responseText2 = delete.getResponseBodyAsString();
        Assert.assertTrue(responseText2.equals("{\"deleted\":true}"));
        
        // Check layer pid now invalid
        GetMethod get = new GetMethod("http://localhost:8080/layers-service/geometry/" + pid + "/wkt");

        
        int returnCode3 = httpClient.executeMethod(get);
        Assert.assertEquals(200, returnCode3);
        
        String responseText3 = get.getResponseBodyAsString();
        Assert.assertTrue(responseText3.matches("\\{\"error\":\"Invalid geometry ID: \\d+\"\\}"));
        
    }

    @Test
    @Ignore
    public void testPointRadiusIntersection() throws Exception {
        HttpClient httpClient = new HttpClient();
        GetMethod get = new GetMethod("http://localhost:8080/layers-service/geometry/intersect_point_radius");
        get.setQueryString("latitude=-20&longitude=127&radius=10");
        
        int returnCode = httpClient.executeMethod(get);
        Assert.assertEquals(200, returnCode);
        
        String responseText = get.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.matches("\\{\"intersecting_ids\":\\[(\\d+,)*\\d+\\]\\}"));
    }
    
    @Test
    @Ignore
    public void testAreaIntersectionWkt() throws Exception {
        String json = "{\"wkt\": \"POLYGON((126.44140625 -20.297236602241,127.056640625 -20.297236602241,127.056640625 -19.636380658043,126.44140625 -19.636380658043,126.44140625 -20.297236602241))\"}";
        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/geometry/intersect_area/wkt");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.matches("\\{\"intersecting_ids\":\\[(\\d+,)*\\d+\\]\\}"));
    }
    
    @Test
    @Ignore
    public void testGeometryCollection() throws Exception {
        String json = "{\"wkt\": \"GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(4 6,7 10))\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/geometry/wkt");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.equals("{\"error\":\"Invalid geometry. See web service documentation for a discussion of valid geometries.\"}"));        
    }
    
    @Test
    @Ignore
    public void testInvalidGeometry() throws Exception {
        String json = "{\"wkt\": \"POLYGON((0 0,10 10,0 10,10 0,0 0))\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/geometry/wkt");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.equals("{\"error\":\"Invalid geometry. See web service documentation for a discussion of valid geometries.\"}"));    
    }
    
//  @Test
//  @Ignore
//  public void testgeometryKml() throws Exception {
//      String json = "{\"kml\": \"<Polygon><outerBoundaryIs><LinearRing><coordinates>125.5185546875,-21.446934836644001 128.2431640625,-21.446934836644001 128.2431640625,-19.138942324356002 125.5185546875,-19.138942324356002 125.5185546875,-21.446934836644001</coordinates></LinearRing></outerBoundaryIs></Polygon>\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
//
//      HttpClient httpClient = new HttpClient();
//      PostMethod post = new PostMethod("http://localhost:8080/layers-service/geometry/kml");
//
//      post.setRequestHeader("Content-Type", "application/json");
//      post.setRequestBody(json);
//      
//      int returnCode = httpClient.executeMethod(post);
//      Assert.assertEquals(200, returnCode);
//      
//      String responseText = post.getResponseBodyAsString();
//      System.out.println(responseText);
//      Assert.assertTrue(responseText.matches("\\{\"pid\":\\d+\\}"));
//  }

//  @Test
//  @Ignore
//  public void testgeometryShp() throws Exception {
//      String json = "{\"zippedShpFileUrl\": \"http://localhost:8080/layers-service/includes/Australia's%20River%20Basins%201997.zip\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
//      
//      HttpClient httpClient = new HttpClient();
//      PostMethod post = new PostMethod("http://localhost:8080/layers-service/geometry/shp");
//
//      post.setRequestHeader("Content-Type", "application/json");
//      post.setRequestBody(json);
//      
//      int returnCode = httpClient.executeMethod(post);
//      Assert.assertEquals(200, returnCode);
//      
//      String responseText = post.getResponseBodyAsString();
//      System.out.println(responseText);
//      Assert.assertTrue(responseText.matches("\\{\"pid\":\\d+\\}"));
//  }
    
//  @Test
//  @Ignore
//  public void testGetKml() throws Exception {
//      HttpClient httpClient = new HttpClient();
//      GetMethod get = new GetMethod("http://localhost:8080/layers-service/geometryed/1/kml");
//
//      
//      int returnCode = httpClient.executeMethod(get);
//      Assert.assertEquals(200, returnCode);
//      
//      String responseText = get.getResponseBodyAsString();
//      System.out.println(responseText);
//      Assert.assertTrue(responseText.equals("{\"kml\":\"<Polygon><outerBoundaryIs><LinearRing><coordinates>125.5185546875,-21.446934836644001 128.2431640625,-21.446934836644001 128.2431640625,-19.138942324356002 125.5185546875,-19.138942324356002 125.5185546875,-21.446934836644001</coordinates></LinearRing></outerBoundaryIs></Polygon>\"}")); 
//      
//  }
//
//  @Test
//  @Ignore
//  public void testGetShp() throws Exception {
//
//  }
    
    //Invalid kml
    
    //Invalid shapefile - not a zip
    
    //Invalid shapefile - no .shp file
    
    //Invalid shapefile - corrupt .shp file    

}
