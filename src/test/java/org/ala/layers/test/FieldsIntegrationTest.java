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
        try{
            URL url = new URL("http://localhost:8080/layers-index/fields.json");
            URLConnection connection = url.openConnection();
            InputStream inStream = connection.getInputStream();
            BufferedReader input =
            new BufferedReader(new InputStreamReader(inStream));
            
            String line = "";
            while ((line = input.readLine()) != null){
                System.out.println(line);
                if (line.contains("{\"fields\":[{\"name\":\"Vegetation types - native\"")){                    
                    assertTrue(true);
                    break;
                }
                else{
                    fail("Unexpected output.");
                }
            }   
        }catch (Exception e){
            e.printStackTrace();
            fail("exception has been thrown");
        }
    }
    
    @Test
    public void testFieldsXml() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/fields.xml");
            URLConnection connection = url.openConnection();
            InputStream inStream = connection.getInputStream();
            BufferedReader input =
            new BufferedReader(new InputStreamReader(inStream));
            
            String line = "";
            while ((line = input.readLine()) != null){
                System.out.println(line);
                if (line.contains("<list><field><id>cl617</id><name>Vegetation types - native</name>")){                    
                    assertTrue(true);
                    break;
                }
                else{
                    fail("Unexpected output.");
                }
            }   
        }catch (Exception e){
            e.printStackTrace();
            fail("exception has been thrown");
        }
    }

    @Test
    public void testFieldsDbJson() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/fieldsdb.json");
            URLConnection connection = url.openConnection();
            InputStream inStream = connection.getInputStream();
            BufferedReader input =
            new BufferedReader(new InputStreamReader(inStream));
            
            String line = "";
            while ((line = input.readLine()) != null){
                System.out.println(line);
                if (line.contains("{\"fields\":[{\"name\":\"Vegetation types - native\"")){                    
                    assertTrue(true);
                    break;
                }
                else{
                    fail("Unexpected output.");
                }
            }   
        }catch (Exception e){
            e.printStackTrace();
            fail("unable to retrieve /layers/fieldsdb.json");
        }
    }
    
    @Test
    public void testFieldsDbXml() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/fieldsdb.xml");
            URLConnection connection = url.openConnection();
            InputStream inStream = connection.getInputStream();
            BufferedReader input =
            new BufferedReader(new InputStreamReader(inStream));
            
            String line = "";
            while ((line = input.readLine()) != null){
                System.out.println(line);
                if (line.contains("<list><field><id>cl617</id><name>Vegetation types - native</name>")){                    
                    assertTrue(true);
                    break;
                }
                else{
                    fail("Unexpected output.");
                }
            }   
        }catch (Exception e){
            e.printStackTrace();
            fail("unable to retrieve /layers/fieldsdb.xml");
        }
    }
    
    @Test
    public void testFieldJson() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/field/cl617.json");
            URLConnection connection = url.openConnection();
            InputStream inStream = connection.getInputStream();
            BufferedReader input =
            new BufferedReader(new InputStreamReader(inStream));
            
            String line = "";
            while ((line = input.readLine()) != null){
                System.out.println(line);
                if (line.contains("{\"field\":{\"name\":\"Vegetation types - native\"")){                    
                    assertTrue(true);
                    break;
                }
                else{
                    fail("Unexpected output.");
                }
            }   
        }catch (Exception e){
            e.printStackTrace();
            fail("unable to retrieve /layers/cl617.json");
        }
    }
      
    @Test
    public void testFieldXml() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/field/cl617.xml");
            URLConnection connection = url.openConnection();
            InputStream inStream = connection.getInputStream();
            BufferedReader input =
            new BufferedReader(new InputStreamReader(inStream));
            
            String line = "";
            while ((line = input.readLine()) != null){
                System.out.println(line);
                if (line.contains("<field><id>cl617</id><name>Vegetation types - native")){                    
                    assertTrue(true);
                    break;
                }
                else{
                    fail("Unexpected output.");
                }
            }   
        }catch (Exception e){
            e.printStackTrace();
            fail("unable to retrieve /layers/cl617.xml");
        }
    }    
}
