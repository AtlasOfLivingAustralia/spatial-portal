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
        try{
            URL url = new URL("http://localhost:8080/layers-index/layers.json");
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
            fail("unable to retrieve /layers.json");
        }
    }
    
    @Test
    public void testLayersXml() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/layers.xml");
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
            fail("unable to retrieve /layers.xml");
        }
    }

    @Test
    public void testLayersGridsJson() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/layers/grids.json");
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
            fail("unable to retrieve /layers/grids.json");
        }
    }
    
    @Test
    public void testLayersGridsXml() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/layers/grids.xml");
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
            fail("unable to retrieve /layers/grids.xml");
        }
    }
        @Test
    public void testLayersShapesJson() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/layers/shapes.json");
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
            fail("unable to retrieve /layers/shapes.json");
        }
    }
    
    @Test
    public void testLayersShapesXml() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/layers/shapes.xml");
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
            fail("unable to retrieve /layers/shapes.xml");
        }
    }
    
    @Test
    public void testLayerJson() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/layer/761.json");
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
            fail("unable to retrieve /layer/761.json");
        }
    }
    
    @Test
    public void testLayerXml() {
        try{
            URL url = new URL("http://localhost:8080/layers-index/layer/761.xml");
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
            fail("unable to retrieve /layer/761.xml");
        }
    }    
}
