package org.ala.layers.util;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

public class SpatialConversionUtils {

    // This code was adapted from the webportal class org.ala.spatial.util.ShapefileUtils, method loadShapeFile
    public static String shapefileToWKT(File shpfile) {
        try {

            FileDataStore store = FileDataStoreFinder.getDataStore(shpfile);

            System.out.println("Loading shapefile. Reading content:");
            System.out.println(store.getTypeNames()[0]);

            SimpleFeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0]);

            SimpleFeatureCollection featureCollection = featureSource.getFeatures();
            SimpleFeatureIterator it = featureCollection.features();
            
            List<String> wktStrings = new ArrayList<String>();
            
            
            while (it.hasNext()) {
                SimpleFeature feature = (SimpleFeature) it.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                WKTWriter wkt = new WKTWriter();

                String wktString = wkt.write(geom);

                wktStrings.add(wktString);
            }

            featureCollection.close(it);
            
            if (wktStrings.size() > 1) {
                return "GEOMETRYCOLLECTION(" + StringUtils.join(wktStrings, ",") + ")";
            } else {
                return wktStrings.get(0);
            }

        } catch (Exception e) {
            System.out.println("Unable to load shapefile: ");
            e.printStackTrace(System.out);
        }

        return null;
    }

    public static void saveShapefile(File shpfile, String wktString) {
        try {
            String wkttype = "POLYGON";
            if (wktString.contains("GEOMETRYCOLLECTION") || wktString.contains("MULTIPOLYGON")) {
                wkttype = "GEOMETRYCOLLECTION";
            }
            final SimpleFeatureType TYPE = createFeatureType(wkttype);

            FeatureCollection collection = FeatureCollections.newCollection();
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

            WKTReader wkt = new WKTReader();
            Geometry geom = wkt.read(wktString);
            featureBuilder.add(geom);

            SimpleFeature feature = featureBuilder.buildFeature(null);
            collection.add(feature);

            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put("url", shpfile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);

            ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(TYPE);

            newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

            Transaction transaction = new DefaultTransaction("create");

            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();

                } catch (Exception problem) {
                    problem.printStackTrace();
                    transaction.rollback();

                } finally {
                    transaction.close();
                }
            }

            System.out.println("Active Area shapefile written to: " + shpfile.getAbsolutePath());

        } catch (Exception e) {
            System.out.println("Unable to save shapefile: ");
            e.printStackTrace(System.out);
        }
    }

    private static SimpleFeatureType createFeatureType(String type) {

        // DataUtilities.createType("ActiveArea", "area:Polygon:srid=4326",
        // "name:String");

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("ActiveArea");
        builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference
                                                    // system

        // add attributes in order
        if ("GEOMETRYCOLLECTION".equalsIgnoreCase(type)) {
            builder.add("area", MultiPolygon.class);
        } else {
            builder.add("area", Polygon.class);
        }
        builder.length(15).add("name", String.class); // <- 15 chars width for
                                                      // name field

        // build the type
        final SimpleFeatureType ActiveArea = builder.buildFeatureType();

        return ActiveArea;
    }
    
    public static String wktToGeoJSON(String wkt) {
//        GeoJs
//        WKTReader wktReader = new WKTReader();
//        Geometry geom = wktReader.read(wkt);
        return null;
        
    }
}
