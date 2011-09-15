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
public class ShapesIntegrationTest {
    
    public ShapesIntegrationTest() {
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
    public void testShapeKml() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/shape/kml/3742602", "<MultiGeometry><Polygon><outerBoundaryIs><LinearRing><coordinates>149.126878000000147,-35.128149999999948 149.127542000000062,-35.129535999999973 149.127564000000007,-35.129581999999971 149.128356999999937,-35.129230999999947 149.130492000000004,-35.130618999999967 149.132384000000116,-35.130159999999989 149.135621000000015,-35.129371999999989 149.137577999999962,-35.13000599999998 149.137618999999972,-35.130018999999891 149.137436999999977,-35.131007999999895 149.138763000000154,-35.132388999999932 149.137483000000088,-35.134440999999924 149.137405999999942,-35.136785999999972 149.137405000000058,-35.136810999999909 149.138580999999931,-35.136968999999908 149.138595000000123,-35.136969999999906 149.139267000000132,-35.137813999999935"));
    }
    
    @Test
    public void testShapeWkt() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/shape/wkt/3742602", "MULTIPOLYGON(((149.126878 -35.1281499999999,149.127542 -35.129536,149.127564 -35.129582,149.128357 -35.1292309999999,149.130492 -35.130619,149.132384 -35.13016,149.135621 -35.129372,149.137578 -35.130006,149.137619 -35.1300189999999,149.137437 -35.1310079999999,149.138763 -35.1323889999999,149.137483 -35.1344409999999,149.137406 -35.136786,149.137405 -35.1368109999999"));
    }
            
    @Test
    public void testShapeGeojson() {
        assertTrue(IntegrationTestUtil.loadURLAssertText("http://localhost:8080/layers-index/shape/geojson/3742602", "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[149.126878000000147,-35.128149999999948],[149.127542000000062,-35.129535999999973],[149.127564000000007,-35.129581999999971],[149.128356999999937,-35.129230999999947],[149.130492000000004,-35.130618999999967],[149.132384000000116,-35.130159999999989],[149.135621000000015,-35.129371999999989],[149.137577999999962"));
    }
}
