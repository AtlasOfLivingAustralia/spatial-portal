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
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/object/3742602", "{\"name\":\"Australian Capital Territory\",\"id\":\"Australian Capital Territory\",\"description\":\"Australian Capital Territory, Territory\",\"pid\":\"3742602\"}"));
    }          
}