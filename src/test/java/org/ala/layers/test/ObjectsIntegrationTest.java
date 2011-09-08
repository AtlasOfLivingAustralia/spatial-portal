package org.ala.layers.test;

import org.ala.layers.test.util.IntegrationTestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This module tests the following paths (ObjectsService) 
 *   /objects/{id}
 *   /object/{pid}
 * 
 * @author jac24n
 */
public class ObjectsIntegrationTest {
    
    public ObjectsIntegrationTest() {
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
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/objects/22.json", "test"));
    }

    @Test
    public void testObjectsXml() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/objects/22.xml", "test"));
    }

    @Test
    public void testObjectJson() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/object/5085887.json", "test"));
    }

    @Test
    public void testObjectXml() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/object/5085887.xml", "test"));
    }
           
}
