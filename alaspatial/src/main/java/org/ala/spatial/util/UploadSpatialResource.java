package org.ala.spatial.util;

import java.io.File;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;

/**
 * UploadSpatialResource helps with loading any dynamically generated
 * spatial data into geoserver.
 *
 * Main code from: 
 * http://svn.apache.org/viewvc/httpcomponents/oac.hc3x/trunk/src/examples/BasicAuthenticationExample.java?view=log
 * 
 * @author ajay
 */
public class UploadSpatialResource {

    /**
     * Contructor for UploadSpatialResource
     */
    public UploadSpatialResource() {
        super();
    }

    public static String loadResource(String url, String extra, String username, String password, String resourcepath) {
        String output = "";

        HttpClient client = new HttpClient();

        client.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        File input = new File(resourcepath);

        PutMethod put = new PutMethod(url);
        put.setDoAuthentication(true);

        //put.addRequestHeader("Content-type", "application/zip");
        
        // Request content will be retrieved directly 
        // from the input stream 
        RequestEntity entity = new FileRequestEntity(input, "application/zip");
        put.setRequestEntity(entity); 
        
        // Execute the request 
        try {
            int result = client.executeMethod(put);
            
            // get the status code
            System.out.println("Response status code: " + result);
            
            // Display response
            System.out.println("Response body: ");
            System.out.println(put.getResponseBodyAsString());

            output += result; 
            
        } catch (Exception e) {
            System.out.println("Something went wrong with UploadSpatialResource");
            e.printStackTrace(System.out);
        } finally {
            // Release current connection to the connection pool once you are done 
            put.releaseConnection();
        }

        return output;


    }

}
