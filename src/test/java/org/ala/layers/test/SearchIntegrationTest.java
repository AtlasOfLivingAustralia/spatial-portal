package org.ala.layers.test;

import org.ala.layers.test.util.IntegrationTestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * TODO
 * 
 * @author jac24n
 */
public class SearchIntegrationTest {
    
    public SearchIntegrationTest() {
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
    public void testSearchJson() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/search?q=canberra", "[{\"name\":\"Canberra Central, SUB, ACT001006\",\"id\":\"ACT001006\",\"description\""));
    }

}
