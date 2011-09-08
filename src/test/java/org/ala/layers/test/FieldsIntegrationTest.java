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
public class FieldsIntegrationTest {
    
    public FieldsIntegrationTest() {
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
    public void testFieldsJson() {
        assertTrue(loadURLAssertText("http://localhost:8080/layers-index/fields.json", "{\"fields\":[{\"name\":\"Vegetation types - native\""));
    }
    
    @Test
    public void testFieldsXml() {
        assertTrue(loadURLAssertText("http://localhost:8080/layers-index/fields.xml", "<list><field><id>cl617</id><name>Vegetation types - native</name>"));
    }
    
    @Test
    public void testFieldsDbJson() {
        assertTrue(loadURLAssertText("http://localhost:8080/layers-index/fieldsdb.json", "{\"fields\":[{\"name\":\"Vegetation types - native\""));
    }
    
    @Test
    public void testFieldsDbXml() {
        assertTrue(loadURLAssertText("http://localhost:8080/layers-index/fieldsdb.xml", "<list><field><id>cl617</id><name>Vegetation types - native</name>"));
    }
    
    @Test
    public void testFieldJson() {
        assertTrue(loadURLAssertText( "http://localhost:8080/layers-index/field/cl617.json", "{\"field\":{\"name\":\"Vegetation types - native\""  ));
    }
        
    @Test
    public void testFieldXml() {
        assertTrue(loadURLAssertText("http://localhost:8080/layers-index/field/cl617.xml", "<field><id>cl617</id><name>Vegetation types - native"));
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
