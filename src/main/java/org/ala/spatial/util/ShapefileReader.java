package org.ala.spatial.util;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

/**
 *
 * @author ajay
 */
public class ShapefileReader {

    public static Map loadShapefile(File shpfile) {
        try {
            FileDataStore store = FileDataStoreFinder.getDataStore(shpfile);

            System.out.println("Loading shapefile. Reading content:");
            System.out.println(store.getTypeNames()[0]);

            FeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0]);

            FeatureCollection featureCollection = featureSource.getFeatures();
            FeatureIterator it = featureCollection.features();
            Map shape = new HashMap(); 
            while (it.hasNext()) {
                //System.out.println("======================================");
                //System.out.println("Feature: ");
                SimpleFeature feature = (SimpleFeature) it.next();
                //System.out.println(feature.getID());
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                WKTWriter wkt = new WKTWriter();
                //System.out.println(wkt.writeFormatted(geom));
                //addWKTLayer(wkt.write(geom), feature.getID());
                shape.put("id", feature.getID());
                shape.put("wkt", wkt.write(geom));
                break;
            }
            featureCollection.close(it);

            return shape; 

        } catch (Exception e) {
            System.out.println("Unable to load shapefile: ");
            e.printStackTrace(System.out);
        }

        return null; 
    }

    public static void main(String[] args) {
        System.out.println("Loading shapefile");
        File shpfile = new File("/Users/ajay/projects/tmp/uploads/SinglePolygon/SinglePolygon.shp"); 
        loadShapefile(shpfile);
    }

}
