package au.org.ala.spatial.util;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import org.apache.log4j.Logger;
import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ajay
 */
public class ShapefileUtils {

    private static Logger logger = Logger.getLogger(ShapefileUtils.class);

    public static Map loadShapefile(File shpfile) {
        try {

            FileDataStore store = FileDataStoreFinder.getDataStore(shpfile);

            logger.debug("Loading shapefile. Reading content:");
            logger.debug(store.getTypeNames()[0]);

            FeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0]);

            FeatureCollection featureCollection = featureSource.getFeatures();
            FeatureIterator it = featureCollection.features();
            Map shape = new HashMap();
            StringBuilder sb = new StringBuilder();
            StringBuilder sbGeometryCollection = new StringBuilder();
            boolean isGeometryCollection = false;
            while (it.hasNext()) {
                //logger.debug("======================================");
                //logger.debug("Feature: ");
                SimpleFeature feature = (SimpleFeature) it.next();
                //logger.debug(feature.getID());
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                WKTWriter wkt = new WKTWriter();

                String wktString = wkt.write(geom);
//                wktString = wktString.replaceAll(", ", ",").replace(",(", ",POLYGON(").replace("),", "),");
                wktString = wktString.replaceAll(", ", ",");
                boolean valid = true;
                boolean multipolygon = false;
                boolean polygon = false;
                boolean geometrycollection = false;
                if (wktString.startsWith("MULTIPOLYGON ")) {
                    wktString = wktString.substring("MULTIPOLYGON (".length(), wktString.length() - 1);
                    multipolygon = true;
                } else if (wktString.startsWith("POLYGON ")) {
                    wktString = wktString.substring("POLYGON ".length());
                    polygon = true;
                } else if (wktString.startsWith("GEOMETRYCOLLECTION (")) {
                    wktString = wktString.substring("GEOMETRYCOLLECTION (".length(), wktString.length() - 1);
                    geometrycollection = true;
                    isGeometryCollection = true;
                } else {
                    valid = false;
                }
                if (valid) {
                    if (sb.length() > 0) {
                        sb.append(",");
                        sbGeometryCollection.append(",");
                    }
                    sb.append(wktString);

                    if (multipolygon) {
                        sbGeometryCollection.append("MULTIPOLYGON(").append(wktString).append(")");
                    } else if (polygon) {
                        sbGeometryCollection.append("POLYGON").append(wktString);
                    } else if (geometrycollection) {
                        sbGeometryCollection.append(wktString);
                    }
                }

//                if (wktString.indexOf(",POLYGON") > -1) {
//                    wktString = wktString.replace("MULTIPOLYGON (((", "GEOMETRYCOLLECTION(POLYGON((");
//                } else {
//                    wktString = wktString.replace("MULTIPOLYGON (((", "POLYGON((").replace(")))", "))");
//                }


                //logger.debug(wkt.writeFormatted(geom));
                //addWKTLayer(wkt.write(geom), feature.getID());
//                shape.put("id", feature.getID());
//                shape.put("wkt", wktString);

//                break;
            }
//            shape.put("id", feature.getID());
            if (!isGeometryCollection) {
                sb.append(")");
                shape.put("wkt", "MULTIPOLYGON(" + sb);
            } else {
                sbGeometryCollection.append(")");
                shape.put("wkt", "GEOMETRYCOLLECTION(" + sbGeometryCollection);
            }

            it.close();

            return shape;

        } catch (Exception e) {
            logger.error("Unable to load shapefile: ", e);
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

            DefaultFeatureCollection collection = new DefaultFeatureCollection();
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

            WKTReader wkt = new WKTReader();
            Geometry geom = wkt.read(wktString);

            if (geom instanceof GeometryCollection) {
                GeometryCollection gc = (GeometryCollection) geom;
                for (int i = 0; i < gc.getNumGeometries(); i++) {
                    featureBuilder.add(gc.getGeometryN(i));

                    SimpleFeature feature = featureBuilder.buildFeature(null);
                    collection.add(feature);
                }
            } else {
                featureBuilder.add(geom);

                SimpleFeature feature = featureBuilder.buildFeature(null);
                collection.add(feature);
            }

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
                    logger.error("error pricessing shape file: " + shpfile.getAbsolutePath(), problem);
                    transaction.rollback();

                } finally {
                    transaction.close();
                }
            }

            logger.debug("Active Area shapefile written to: " + shpfile.getAbsolutePath());

        } catch (Exception e) {
            logger.error("Unable to save shapefile: " + shpfile.getAbsolutePath(), e);
        }
    }

    private static SimpleFeatureType createFeatureType(String type) {

        // DataUtilities.createType("ActiveArea", "area:Polygon:srid=4326", "name:String");

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("ActiveArea");
        builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference system

        // add attributes in order
        if ("GEOMETRYCOLLECTION".equalsIgnoreCase(type)) {
            builder.add("area", MultiPolygon.class);
        } else {
            builder.add("area", Polygon.class);
        }
        builder.length(15).add("name", String.class); // <- 15 chars width for name field

        // build the type
        final SimpleFeatureType ActiveArea = builder.buildFeatureType();

        return ActiveArea;
    }

    public static void main(String[] args) {
//        logger.debug("Loading shapefile");
//        //File shpfile = new File("/Users/ajay/projects/tmp/uploads/SinglePolygon/SinglePolygon.shp");
//        File shpfile = new File("/Users/ajay/Downloads/My_Area_1_Shapefile/My_Area_1_Shapefile.shp");
//        loadShapefile(shpfile);

//        try {
//            logger.debug("Saving shapefile");
//            File shpfile = new File("/Users/ajay/projects/tmp/uploads/ActiveArea/ActiveArea.shp");
//            //String wktString = "POLYGON((128.63867187521 -23.275665408794,128.63867187521 -29.031198581005,137.25195312487 -28.56908637161,138.8339843748 -24.561114535569,134.61523437497 -20.83211850342,130.83593750012 -21.160338084068,128.63867187521 -23.275665408794))";
//            String wktfile = "/Users/ajay/Downloads/Australian_Capital_Territory_WKT.txt";
//            String wktString = FileUtils.readFileToString(new File(wktfile));
//            saveShapefile(shpfile, wktString.toString());
//        } catch (Exception e) {
//            logger.debug("Error reading wkt file");
//            e.printStackTrace(System.out);
//        }

    }
}
