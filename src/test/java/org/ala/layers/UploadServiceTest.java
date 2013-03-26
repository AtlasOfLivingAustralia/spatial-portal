package org.ala.layers;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Assert;

public class UploadServiceTest {

    @Test
    @Ignore
    public void testUploadWkt() throws Exception {
        String json = "{\"wkt\": \"POLYGON((125.5185546875 -21.446934836644,128.2431640625 -21.446934836644,128.2431640625 -19.138942324356,125.5185546875 -19.138942324356,125.5185546875 -21.446934836644))\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/upload/wkt");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.matches("\\{\"pid\":\\d+\\}"));
    }

    @Test
    @Ignore
    public void testUploadKml() throws Exception {
        String json = "{\"kml\": \"<Polygon><outerBoundaryIs><LinearRing><coordinates>125.5185546875,-21.446934836644001 128.2431640625,-21.446934836644001 128.2431640625,-19.138942324356002 125.5185546875,-19.138942324356002 125.5185546875,-21.446934836644001</coordinates></LinearRing></outerBoundaryIs></Polygon>\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";

        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/upload/kml");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.matches("\\{\"pid\":\\d+\\}"));
    }

    @Test
    @Ignore
    public void testUploadShp() throws Exception {
        String json = "{\"zippedShpFileUrl\": \"http://localhost:8080/layers-service/includes/Australia's%20River%20Basins%201997.zip\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/upload/shp");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.matches("\\{\"pid\":\\d+\\}"));
    }
    
    @Test
    @Ignore
    public void testUploadGeojson() throws Exception {
        String json = "{\"geojson\": \"{\\\"type\\\":\\\"Polygon\\\",\\\"coordinates\\\":[[[125.5185546875,-21.446934836644001],[128.2431640625,-21.446934836644001],[128.2431640625,-19.138942324356002],[125.5185546875,-19.138942324356002],[125.5185546875,-21.446934836644001]]]}\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/upload/geojson");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.matches("\\{\"pid\":\\d+\\}"));
    }
    
    @Test
    @Ignore
    public void testMalformedJSON() throws Exception{
        String json = "#%#%#";        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/upload/shp");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.matches("\\{\"error\":\\.+\\}")); 
    }
    
    @Test
    @Ignore
    public void testInvalidJSON() throws Exception{
        // Using a list here when we need a map
        String json = "[1, 2, 3]";        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/upload/shp");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.matches("\\{\"error\":\\.+\\}")); 
    }
    
    @Test
    public void testInvalidWkt() throws Exception {
        String json = "{\"wkt\": \"lalala\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/upload/wkt");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.matches("\\{\"error\":\\.+\\}"));
    }
    
    //Invalid kml
    
    //Invalid geojson
    
    //Invalid shapefile - not a zip
    
    //Invalid shapefile - no .shp file
    
    //Invalid shapefile - corrupt .shp file
    
    //Invalid pid

    @Test
    @Ignore
    public void testGetGeoJson() throws Exception {
        HttpClient httpClient = new HttpClient();
        GetMethod get = new GetMethod("http://localhost:8080/layers-service/uploaded/1/geojson");

        
        int returnCode = httpClient.executeMethod(get);
        Assert.assertEquals(200, returnCode);
        
        String responseText = get.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.equals("{\"wkt\":\"POLYGON((125.5185546875 -21.446934836644,128.2431640625 -21.446934836644,128.2431640625 -19.138942324356,125.5185546875 -19.138942324356,125.5185546875 -21.446934836644))\"}")); 
    }

    @Test
    public void testGetKml() throws Exception {
        HttpClient httpClient = new HttpClient();
        GetMethod get = new GetMethod("http://localhost:8080/layers-service/uploaded/1/kml");

        
        int returnCode = httpClient.executeMethod(get);
        Assert.assertEquals(200, returnCode);
        
        String responseText = get.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.equals("{\"kml\":\"<Polygon><outerBoundaryIs><LinearRing><coordinates>125.5185546875,-21.446934836644001 128.2431640625,-21.446934836644001 128.2431640625,-19.138942324356002 125.5185546875,-19.138942324356002 125.5185546875,-21.446934836644001</coordinates></LinearRing></outerBoundaryIs></Polygon>\"}")); 
        
    }

    @Test
    @Ignore
    public void testGetShp() throws Exception {

    }

    @Test
    @Ignore
    public void testGetWkt() throws Exception {
        HttpClient httpClient = new HttpClient();
        GetMethod get = new GetMethod("http://localhost:8080/layers-service/uploaded/1/wkt");

        
        int returnCode = httpClient.executeMethod(get);
        Assert.assertEquals(200, returnCode);
        
        String responseText = get.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.equals("{\"wkt\":\"POLYGON((125.5185546875 -21.446934836644,128.2431640625 -21.446934836644,128.2431640625 -19.138942324356,125.5185546875 -19.138942324356,125.5185546875 -21.446934836644))\"}"));
    }

    @Test
    @Ignore
    public void testDelete() throws Exception {
        // Create layer
        
        
        // Delete layer
        
        
        // Check layer pid now invalid
        
        String json = "{\"zippedShpFileUrl\": \"http://localhost:8080/layers-service/includes/Australia's%20River%20Basins%201997.zip\", \"name\": \"test\", \"description\": \"test\", \"userid\": \"chris\"}";
        
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod("http://localhost:8080/layers-service/upload/shp");

        post.setRequestHeader("Content-Type", "application/json");
        post.setRequestBody(json);
        
        int returnCode = httpClient.executeMethod(post);
        Assert.assertEquals(200, returnCode);
        
        String responseText = post.getResponseBodyAsString();
        System.out.println(responseText);
        Assert.assertTrue(responseText.matches("\\{\"pid\":\\d+\\}"));
    }

    @Test
    @Ignore
    public void testPointIntersection() throws Exception {
        
    }

}
