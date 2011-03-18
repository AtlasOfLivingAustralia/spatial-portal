package org.ala.spatial.util;

import java.io.File;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.ala.spatial.util.SpatialLogger;

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
	 * HTTP request type PUT
	 */
	public static final int PUT = 0;
	
	/**
	 * HTTP request type POST
	 */
	public static final int POST = 1;

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
    
    /**
     * sends a PUT or POST call to a URL using authentication and including a file upload
     * 
     * @param type one of UploadSpatialResource.PUT for a PUT call or UploadSpatialResource.POST for a POST call
     * @param url URL for PUT/POST call
     * @param username account username for authentication
     * @param password account password for authentication
     * @param resourcepath local path to file to upload, null for no file to upload
     * @param contenttype file MIME content type
     * @return server response status code as String or empty String if unsuccessful
     */
    public static String httpCall(int type, String url, String username, String password, String resourcepath, String contenttype) {
    	String output = "";

        HttpClient client = new HttpClient();
        client.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        
        
        RequestEntity entity = null;        
        if (resourcepath != null) {
	        File input = new File(resourcepath);	
	        entity = new FileRequestEntity(input, contenttype);
        }
        
        HttpMethod call = null;;        
        if (type == PUT) {
        	PutMethod put = new PutMethod(url);
        	put.setDoAuthentication(true);
        	if (entity != null) {
    	        put.setRequestEntity(entity); 
            }
        	call = put;
        } else if (type == POST) {
        	PostMethod post = new PostMethod(url);
        	if (entity != null) {
    	        post.setRequestEntity(entity); 
            }
        	call = post;
        } else {
        	SpatialLogger.log("UploadSpatialResource","invalid type: " + type);
        	return output;
        }
        
        // Execute the request 
        try {
            int result = client.executeMethod(call);
            
            output += result;             
        } catch (Exception e) {
        	SpatialLogger.log("UploadSpatialResource","failed upload to: " + url);
        } finally {
            // Release current connection to the connection pool once you are done 
            call.releaseConnection();
        }

        return output;
    }
}
