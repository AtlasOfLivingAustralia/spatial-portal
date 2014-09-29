package au.org.ala.spatial.util;

import au.org.ala.spatial.StringConstants;
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
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ajay
 */
public final class ShapefileUtils {

    private static final Logger LOGGER = Logger.getLogger(ShapefileUtils.class);

    private ShapefileUtils() {
        //to hide public constructor
    }

    public static Map loadShapefile(File shpfile) {
        try {

            FileDataStore store = FileDataStoreFinder.getDataStore(shpfile);

            LOGGER.debug("Loading shapefile. Reading content:");
            LOGGER.debug(store.getTypeNames()[0]);

            FeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0]);

            FeatureCollection featureCollection = featureSource.getFeatures();
            FeatureIterator it = featureCollection.features();
            Map shape = new HashMap();
            StringBuilder sb = new StringBuilder();
            StringBuilder sbGeometryCollection = new StringBuilder();
            boolean isGeometryCollection = false;
            while (it.hasNext()) {

                SimpleFeature feature = (SimpleFeature) it.next();

                Geometry geom = (Geometry) feature.getDefaultGeometry();
                WKTWriter wkt = new WKTWriter();

                String wktString = wkt.write(geom);

                wktString = wktString.replaceAll(", ", ",");
                boolean valid = true;
                boolean multipolygon = false;
                boolean polygon = false;
                boolean geometrycollection = false;
                if (wktString.startsWith(StringConstants.MULTIPOLYGON + " ")) {
                    wktString = wktString.substring((StringConstants.MULTIPOLYGON + " (").length(), wktString.length() - 1);
                    multipolygon = true;
                } else if (wktString.startsWith(StringConstants.POLYGON + " ")) {
                    wktString = wktString.substring((StringConstants.POLYGON + " ").length());
                    polygon = true;
                } else if (wktString.startsWith(StringConstants.GEOMETRYCOLLECTION + " (")) {
                    wktString = wktString.substring((StringConstants.GEOMETRYCOLLECTION + " (").length(), wktString.length() - 1);
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
                        sbGeometryCollection.append(StringConstants.MULTIPOLYGON).append("(").append(wktString).append(")");
                    } else if (polygon) {
                        sbGeometryCollection.append(StringConstants.POLYGON).append(wktString);
                    } else if (geometrycollection) {
                        sbGeometryCollection.append(wktString);
                    }
                }
            }

            if (!isGeometryCollection) {
                sb.append(")");
                shape.put(StringConstants.WKT, StringConstants.MULTIPOLYGON + "(" + sb);
            } else {
                sbGeometryCollection.append(")");
                shape.put(StringConstants.WKT, StringConstants.GEOMETRYCOLLECTION + "(" + sbGeometryCollection);
            }

            it.close();

            return shape;

        } catch (Exception e) {
            LOGGER.error("Unable to load shapefile: ", e);
        }

        return null;
    }

    public static void saveShapefile(File shpfile, String wktString) {
        try {
            final SimpleFeatureType type = createFeatureType();

            List<SimpleFeature> features = new ArrayList<SimpleFeature>();
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);

            WKTReader wkt = new WKTReader();
            Geometry geom = wkt.read(wktString);

            if (geom instanceof GeometryCollection) {
                GeometryCollection gc = (GeometryCollection) geom;
                for (int i = 0; i < gc.getNumGeometries(); i++) {
                    Geometry g = gc.getGeometryN(i);
                    if (g instanceof Polygon) {
                        g = new GeometryBuilder().multiPolygon((Polygon) g);
                    }
                    featureBuilder.add(g);

                    SimpleFeature feature = featureBuilder.buildFeature(null);
                    features.add(feature);
                }
            } else {
                Geometry g = geom;
                if (g instanceof Polygon) {
                    g = new GeometryBuilder().multiPolygon((Polygon) g);
                }

                featureBuilder.add(g);

                SimpleFeature feature = featureBuilder.buildFeature(null);
                features.add(feature);
            }

            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put("url", shpfile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);

            ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(type);

            newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

            Transaction transaction = new DefaultTransaction("create");

            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                DefaultFeatureCollection collection = new DefaultFeatureCollection();
                collection.addAll(features);
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();

                } catch (Exception problem) {
                    LOGGER.error("error pricessing shape file: " + shpfile.getAbsolutePath(), problem);
                    transaction.rollback();

                } finally {
                    transaction.close();
                }
            }

            LOGGER.debug("Active Area shapefile written to: " + shpfile.getAbsolutePath());

        } catch (Exception e) {
            LOGGER.error("Unable to save shapefile: " + shpfile.getAbsolutePath(), e);
        }
    }

    private static SimpleFeatureType createFeatureType() {

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("ActiveArea");
        builder.setCRS(DefaultGeographicCRS.WGS84);

        builder.add("the_geom", MultiPolygon.class);

        // build the type
        return builder.buildFeatureType();
    }
}
