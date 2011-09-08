package org.ala.layers.test;

import org.ala.layers.test.util.IntegrationTestUtil;
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
 * This module tests the following paths (FieldsService) 
 *   /fields
 *   /fieldsdb
 *   /field/{id}
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
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/fields.json", "{\"fields\":[{\"name\":\"Vegetation types - native\""));
    }
    
    @Test
    public void testFieldsXml() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/fields.xml", "<list><field><id>cl617</id><name>Vegetation types - native</name>"));
    }
    
    @Test
    public void testFieldsDbJson() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/fieldsdb.json", "{\"fields\":[{\"name\":\"Vegetation types - native\""));
    }
    
    @Test
    public void testFieldsDbXml() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/fieldsdb.xml", "<list><field><id>cl617</id><name>Vegetation types - native</name>"));
    }
    
    @Test
    public void testFieldJson() {
        assertTrue(IntegrationTestUtil.loadURLAssertText( "http://localhost:8080/layers-index/field/cl617.json", "{\"field\":{\"name\":\"Vegetation types - native\""  ));
    }
        
    @Test
    public void testFieldXml() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/field/cl617.xml", "<field><id>cl617</id><name>Vegetation types - native"));
    }            
}
