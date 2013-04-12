package org.ala.layers.util;

import java.io.File;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
import org.geotools.kml.KMLConfiguration;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

public class SpatialConversionUtils {
    
//    /** log4j logger */
//    private static final Logger logger = Logger.getLogger(SpatialConversionUtils.class);
//
//    // This code was adapted from the webportal class org.ala.spatial.util.ShapefileUtils, method loadShapeFile
//    public static String shapefileToWKT(File shpfile) {
//        try {
//
//            FileDataStore store = FileDataStoreFinder.getDataStore(shpfile);
//
//            System.out.println("Loading shapefile. Reading content:");
//            System.out.println(store.getTypeNames()[0]);
//
//            SimpleFeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0]);
//
//            SimpleFeatureCollection featureCollection = featureSource.getFeatures();
//            SimpleFeatureIterator it = featureCollection.features();
//            
//            List<String> wktStrings = new ArrayList<String>();
//            
//            
//            while (it.hasNext()) {
//                SimpleFeature feature = (SimpleFeature) it.next();
//                Geometry geom = (Geometry) feature.getDefaultGeometry();
//                WKTWriter wkt = new WKTWriter();
//
//                String wktString = wkt.write(geom);
//
//                wktStrings.add(wktString);
//            }
//
//            featureCollection.close(it);
//            
//            if (wktStrings.size() > 1) {
//                return "GEOMETRYCOLLECTION(" + StringUtils.join(wktStrings, ",") + ")";
//            } else {
//                return wktStrings.get(0);
//            }
//
//        } catch (Exception e) {
//            System.out.println("Unable to load shapefile: ");
//            e.printStackTrace(System.out);
//        }
//
//        return null;
//    }
//
//    public static File saveShapefile(File shpfile, String wktString) {
//        try {
//            String wkttype = "POLYGON";
//            if (wktString.contains("GEOMETRYCOLLECTION") || wktString.contains("MULTIPOLYGON")) {
//                wkttype = "GEOMETRYCOLLECTION";
//            }
//            final SimpleFeatureType TYPE = createFeatureType(wkttype);
//
//            FeatureCollection collection = FeatureCollections.newCollection();
//            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
//
//            WKTReader wkt = new WKTReader();
//            Geometry geom = wkt.read(wktString);
//            featureBuilder.add(geom);
//
//            SimpleFeature feature = featureBuilder.buildFeature(null);
//            collection.add(feature);
//
//            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
//
//            Map<String, Serializable> params = new HashMap<String, Serializable>();
//            params.put("url", shpfile.toURI().toURL());
//            params.put("create spatial index", Boolean.TRUE);
//
//            ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
//            newDataStore.createSchema(TYPE);
//
//            newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
//
//            Transaction transaction = new DefaultTransaction("create");
//
//            String typeName = newDataStore.getTypeNames()[0];
//            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
//
//            if (featureSource instanceof SimpleFeatureStore) {
//                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
//
//                featureStore.setTransaction(transaction);
//                try {
//                    featureStore.addFeatures(collection);
//                    transaction.commit();
//
//                } catch (Exception problem) {
//                    problem.printStackTrace();
//                    transaction.rollback();
//
//                } finally {
//                    transaction.close();
//                }
//            }
//
//            System.out.println("Active Area shapefile written to: " + shpfile.getAbsolutePath());
//            return shpfile;
//        } catch (Exception e) {
//            System.out.println("Unable to save shapefile: ");
//            e.printStackTrace(System.out);
//            return null;
//        }
//    }
//
//    private static SimpleFeatureType createFeatureType(String type) {
//
//        // DataUtilities.createType("ActiveArea", "area:Polygon:srid=4326",
//        // "name:String");
//
//        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
//        builder.setName("ActiveArea");
//        builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference
//                                                    // system
//
//        // add attributes in order
//        if ("GEOMETRYCOLLECTION".equalsIgnoreCase(type)) {
//            builder.add("area", MultiPolygon.class);
//        } else {
//            builder.add("area", Polygon.class);
//        }
//        builder.length(15).add("name", String.class); // <- 15 chars width for
//                                                      // name field
//
//        // build the type
//        final SimpleFeatureType ActiveArea = builder.buildFeatureType();
//
//        return ActiveArea;
//    }
//    
//    // This method is taken from the spatial portal class org.ala.spatial.analysis.web.AreaUploadShapefile.
//    public static String getKMLPolygonAsWKT(String kmldata) {
//        try {
//            Parser parser = new Parser(new KMLConfiguration());
//            SimpleFeature f = (SimpleFeature) parser.parse(new StringReader(kmldata));
//            Collection placemarks = (Collection) f.getAttribute("Feature");
//
//            Geometry g = null;
//            SimpleFeature sf = null;
//
//            //for <Placemark>
//            if (placemarks.size() > 0 && placemarks.size() > 0) {
//                sf = (SimpleFeature) placemarks.iterator().next();
//                g = (Geometry) sf.getAttribute("Geometry");
//            }
//
//            //for <Folder><Placemark>
//            if (g == null && sf != null) {
//                placemarks = (Collection) sf.getAttribute("Feature");
//                if (placemarks != null && placemarks.size() > 0) {
//                    g = (Geometry) ((SimpleFeature) placemarks.iterator().next()).getAttribute("Geometry");
//                }
//            }
//
//            if (g != null) {
//                WKTWriter wr = new WKTWriter();
//                String wkt = wr.write(g);
//                return wkt;
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace(System.out);
//        }
//
//        return null;
//    }
//    
//    public static String getGeoJsonAsWKT(String geoJson) {
//        
//    }
}
