package org.ala.layers.test;

import org.ala.layers.test.util.IntegrationTestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This module tests the following paths (LayersService) 
 *   /layers
 *   /layer/{id}
 *   /layers/grids
 *   /layers/shapes
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
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/layers.json", "test"));
    }
    
    @Test
    public void testLayersXml() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/layers.xml", "test"));
    }
    
    @Test
    public void testLayerJson() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/layer/761.json", "test"));
    }
    
    @Test
    public void testLayerXml() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/layer/761.xml", "test"));
    }

}
