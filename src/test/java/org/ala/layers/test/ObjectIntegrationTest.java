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
public class ObjectIntegrationTest {
    
    public ObjectIntegrationTest() {
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
    public void testObjectsJson() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/objects/617.json");
            URLConnection connection = url.openConnection();
            InputStream inStream = connection.getInputStream();
            BufferedReader input =
            new BufferedReader(new InputStreamReader(inStream));
            
            String line = "";
            while ((line = input.readLine()) != null){
                System.out.println(line);
                // @todo put in output check
                assertTrue(true);
            }
        }catch (Exception e){
            e.printStackTrace();
            fail("unable to retrieve /objects/617.json");
        }
    }
    
    @Test
    public void testObjectsXml() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/objects/617.xml");
            URLConnection connection = url.openConnection();
            InputStream inStream = connection.getInputStream();
            BufferedReader input =
            new BufferedReader(new InputStreamReader(inStream));
            
            String line = "";
            while ((line = input.readLine()) != null){
                System.out.println(line);
                // @todo put in output check
                assertTrue(true);
            }
        }catch (Exception e){
            e.printStackTrace();
            fail("unable to retrieve /objects/617.xml");
        }
    }    

    @Test
    public void testObjectJson() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/object/3746567.json");
            URLConnection connection = url.openConnection();
            InputStream inStream = connection.getInputStream();
            BufferedReader input =
            new BufferedReader(new InputStreamReader(inStream));
            
            String line = "";
            while ((line = input.readLine()) != null){
                System.out.println(line);
                // @todo put in output check
                assertTrue(true);
            }
        }catch (Exception e){
            e.printStackTrace();
            fail("unable to retrieve /object/3746567.json");
        }
    }
    
    @Test
    public void testObjectXml() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/object/3746567.xml");
            URLConnection connection = url.openConnection();
            InputStream inStream = connection.getInputStream();
            BufferedReader input =
            new BufferedReader(new InputStreamReader(inStream));
            
            String line = "";
            while ((line = input.readLine()) != null){
                System.out.println(line);
                // @todo put in output check
                assertTrue(true);
            }
        }catch (Exception e){
            e.printStackTrace();
            fail("unable to retrieve /object/3746567.xml");
        }
    }    
    
}
