package org.ala.layers.test;

import org.ala.layers.test.util.IntegrationTestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This module tests the following paths (IntersectService) 
 *  /intersect/{ids}/{lat}/{lng}
 *  /intersect/test/{ids}/{lat}/{lng}
 * 
 *  /intersect/batch
 *
 * @author jac24n
 */
public class IntersectIntegrationTest {
    
    public IntersectIntegrationTest() {
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
    public void testIntersectJson() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/intersect/cl22/-29.911/132.769.json", "test"));
    }
    
    @Test
    public void testIntersectXml() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/intersect/cl22/-29.911/132.769.xml", "test"));
    }
    
}
