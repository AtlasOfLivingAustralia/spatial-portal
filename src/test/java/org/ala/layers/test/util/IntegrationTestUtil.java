/*
 */
package org.ala.layers.test.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author jac24n
 */
public class IntegrationTestUtil {
     
    /**
     * Helper method to load url and check result.
     * @param url
     * @param text
     * @return 
     */
    public static boolean loadURLAssertText(String target_url, String return_text){
        try{
            URL url = new URL(target_url);
            URLConnection connection = url.openConnection();
            InputStream inStream = connection.getInputStream();
            BufferedReader input =
            new BufferedReader(new InputStreamReader(inStream));
            
            String line = "";
            StringBuilder sb = new StringBuilder();
            while ((line = input.readLine()) != null){
                sb.append(line);
            }
            if (sb.toString().contains(return_text) == true){
                return true;
            }
            else{
                System.out.println("Unable to match: " + return_text);
                return false;
            }
        } catch (java.io.IOException e){
            System.out.println("IO Exception occurred tring to open: " + target_url);
            return false;           
        }
    }  
}
