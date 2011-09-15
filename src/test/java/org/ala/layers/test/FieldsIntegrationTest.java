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
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/fields.json", "{\"name\":\"Vegetation types - native\",\"id\":\"cl617\",\"type\":\"c\",\"enabled\":true,\"indb\":true,\"spid\":\"617\"}"));
    }
    
    @Test
    public void testFieldsDbJson() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/fieldsdb.json", "{\"name\":\"Vegetation types - native\",\"id\":\"cl617\",\"type\":\"c\",\"enabled\":true,\"indb\":true,\"spid\":\"617\"}"));
    }
    
//    This is extremely slow to execute (lots of objects!)
//    @Test
//    public void testFieldJson() {
//        assertTrue(IntegrationTestUtil.loadURLAssertText( "http://localhost:8080/layers-index/field/cl617.json", "{\"field\":{\"name\":\"Vegetation types - native\""  ));
//    }
    
    @Test
    public void testFieldJson() {
        assertTrue(IntegrationTestUtil.loadURLAssertText( "http://localhost:8080/layers-index/field/cl907.json", "{\"name\":\"Hunter Areas Of Interest\",\"id\":\"cl907\",\"type\":\"c\",\"enabled\":true,\"objects\":[{\"name\":" ));
    }
}
