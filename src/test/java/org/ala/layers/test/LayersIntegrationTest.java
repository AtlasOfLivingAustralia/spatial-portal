package org.ala.layers.test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jac24n
 */
public class LayersIntegrationTest {
    
    public LayersIntegrationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testLayersJson() {
        assertTrue(loadURLAssertText("http://localhost:8080/layers-index/layers.json", "test"));
    }
    
    @Test
    public void testLayersXml() {
        assertTrue(loadURLAssertText("http://localhost:8080/layers-index/layers.xml", "test"));
    }
    
    @Test
    public void testLayerJson() {
        assertTrue(loadURLAssertText("http://localhost:8080/layers-index/layer/761.json", "test"));
    }
    
    @Test
    public void testLayerXml() {
        assertTrue(loadURLAssertText("http://localhost:8080/layers-index/layer/761.xml", "test"));
    }
    
    /**
     * Helper method to load url and check result.
     * @param url
     * @param text
     * @return 
     */
    public boolean loadURLAssertText(String target_url, String return_text){
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
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
            return false;           
        }
    }
}
