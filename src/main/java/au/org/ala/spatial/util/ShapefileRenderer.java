/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.util;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.referencing.CRS;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author ajay
 */
public class ShapefileRenderer {

    private static Logger logger = Logger.getLogger(ShapefileRenderer.class);

    public static String generateShapeImage(String filename, String outputdir, String column, String userFilter) {

        final FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
        DataStore dataStore;
        try {
            dataStore = new ShapefileDataStore(new URL("file://" + filename));
            //dataStore = FileDataStoreFinder.getDataStore(new File(filename));
            if (dataStore == null) {
                logger.debug("Opps, datastore is null for " + filename);
            }
            String typename = dataStore.getTypeNames()[0];
            SimpleFeatureSource source = dataStore.getFeatureSource(typename);
            SimpleFeatureCollection features = source.getFeatures();
            SimpleFeatureType schema = features.getSchema();


            //Filter filter = CQL.toFilter("Arctic");
            SimpleFeatureCollection sff = null;
            if (!column.equals("none") && !userFilter.equals("none")) {
                //Filter filter = ff.like(ff.property(column), userFilter);

                Filter filter;
                ArrayList<Filter> filters = new ArrayList<Filter>();
                if ("fid".equalsIgnoreCase(column)) {
                    Set<FeatureId> fids = new HashSet<FeatureId>();
                    for (String flt : userFilter.split(",")) {
                        fids.add(ff.featureId(flt));
                    }
                    filter = ff.id(fids);
                } else {
                    for (String flt : userFilter.split(",")) {
                        filters.add(ff.like(ff.property(column), flt));
                    }
                    filter = ff.or(filters);
                }

                sff = source.getFeatures(filter);
                SimpleFeatureIterator fif = sff.features();
                while (fif.hasNext()) {
                    SimpleFeature f = fif.next();
                    logger.debug(" > " + f.getID() + " - " + f.getAttribute("ECOREGION") + " - " + f.getAttribute("PROVINCE"));
                }
            }


            String wkt4326 = "GEOGCS[" + "\"WGS 84\"," + "  DATUM[" + "    \"WGS_1984\","
                    + "    SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],"
                    + "    TOWGS84[0,0,0,0,0,0,0]," + "    AUTHORITY[\"EPSG\",\"6326\"]],"
                    + "  PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],"
                    + "  UNIT[\"DMSH\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9108\"]],"
                    + "  AXIS[\"Lat\",NORTH]," + "  AXIS[\"Long\",EAST],"
                    + "  AUTHORITY[\"EPSG\",\"4326\"]]";
            String wkt900913 = "PROJCS[\"WGS84 / Google Mercator\", "
                    + "  GEOGCS[\"WGS 84\", "
                    + "   DATUM[\"World Geodetic System 1984\", "
                    + "   SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], "
                    + "  AUTHORITY[\"EPSG\",\"6326\"]], "
                    + " PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], "
                    + " UNIT[\"degree\", 0.017453292519943295], "
                    + " AXIS[\"Longitude\", EAST], "
                    + " AXIS[\"Latitude\", NORTH], "
                    + " AUTHORITY[\"EPSG\",\"4326\"]], "
                    + " PROJECTION[\"Mercator_1SP\"], "
                    + " PARAMETER[\"semi_minor\", 6378137.0], "
                    + " PARAMETER[\"latitude_of_origin\", 0.0],"
                    + " PARAMETER[\"central_meridian\", 0.0], "
                    + " PARAMETER[\"scale_factor\", 1.0], "
                    + " PARAMETER[\"false_easting\", 0.0], "
                    + " PARAMETER[\"false_northing\", 0.0], "
                    + " UNIT[\"m\", 1.0], "
                    + " AXIS[\"x\", EAST], "
                    + " AXIS[\"y\", NORTH], "
                    + " AUTHORITY[\"EPSG\",\"3857\"]] ";
            CoordinateReferenceSystem wgsCRS = CRS.parseWKT(wkt4326);
            CoordinateReferenceSystem googleCRS = CRS.parseWKT(wkt900913);


            StyleBuilder styleBuilder = new StyleBuilder();
            StyleFactory styleFactory = styleBuilder.getStyleFactory();
            PolygonSymbolizer polygon = styleFactory.createPolygonSymbolizer();
            polygon.setStroke(styleFactory.createStroke(ff.literal(Color.BLACK), ff.literal(1)));
            polygon.setFill(styleFactory.createFill(ff.literal(Color.WHITE)));
            //polygon.setFill(styleFactory.createFill(ff.like(ff.property("PROVINCE"), "Arctic").getExpression(), ff.literal(Color.CYAN)));
            Rule ruleP = styleFactory.createRule();
            ruleP.symbolizers().add(polygon);
            FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{ruleP});
            Style style = styleFactory.createStyle();
            style.featureTypeStyles().add(fts);

            Style styleSelected = null;


            if (sff != null && sff.size() > 0) {
                PolygonSymbolizer polygonselected = styleFactory.createPolygonSymbolizer();
                polygonselected.setStroke(styleFactory.createStroke(ff.literal(Color.BLACK), ff.literal(1)));
                polygonselected.setFill(styleFactory.createFill(ff.literal(Color.CYAN)));
                Rule rulePS = styleFactory.createRule();
                rulePS.symbolizers().add(polygonselected);
                FeatureTypeStyle ftssel = styleFactory.createFeatureTypeStyle(new Rule[]{rulePS});
                styleSelected = styleFactory.createStyle();
                styleSelected.featureTypeStyles().add(ftssel);
            }


            int imageWidth = 1000;
            int imageHeight = 600;
            //logger.debug("\n\nDrawing Image");
            MapContext map = new DefaultMapContext();
            //CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
            map.setCoordinateReferenceSystem(wgsCRS);
            map.addLayer(features, style);
            if (styleSelected != null) {
                map.addLayer(sff, styleSelected);
            }
            ReferencedEnvelope mapBounds = map.getLayerBounds();
            GTRenderer renderer = new StreamingRenderer();
            renderer.setContext(map);
            //logger.debug("mapBounds: " + mapBounds);
            double heightToWidth = mapBounds.getSpan(1) / mapBounds.getSpan(0);
            //Rectangle imageBounds = new Rectangle(0, 0, imageWidth, (int) Math.round(imageWidth * heightToWidth));
            Rectangle imageBounds = new Rectangle(0, 0, imageWidth, imageHeight);
            BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D gr = image.createGraphics();
            gr.setPaint(Color.WHITE);
            gr.fill(imageBounds);
            renderer.paint(gr, imageBounds, mapBounds);
            ImageIO.write(image, "jpeg", new File(outputdir + typename + ".jpg"));

            return outputdir + typename + ".jpg";

        } catch (Exception ex) {
            logger.error("error generating shape image", ex);
        }

        return "";

    }

    public static void main(String[] args) {

        String shapepath = "";
        String shapeout = "";
        String column = "";
        String filters = "";

        if (args.length > 3) {
            shapepath = args[0];
            shapeout = args[1];
            column = args[2];
            //filters = args[3];
            for (int i = 3; i < args.length; i++) {
                filters += args[i];
                if (i != (args.length - 1)) {
                    filters += ",";
                }
            }
        } else {
            logger.debug("\n\nUsage:\n\tShapefileRenderer </path/to/shapefile> </path/to/output/directory/> <filter_attribute | \"none\"> <filter_value_1 filter_value_2 ... filter_value_n | \"none\">\n\n");
            System.exit(1);
        }

//        logger.debug(shapepath);
//        logger.debug(shapeout);
//        logger.debug(column);
//        logger.debug(filters);

        //logger.debug(generateShapeImage("/Users/ajay/Downloads/MEOW_zip/MEOW2/meow_ecos.shp", "/Users/ajay/Downloads/MEOW_zip/", "ECOREGION", "Baltic Sea"));
        //logger.debug(generateShapeImage(args[0],args[1],args[2],args[3]));
        logger.debug(generateShapeImage(shapepath, shapeout, column, filters));
    }
}
