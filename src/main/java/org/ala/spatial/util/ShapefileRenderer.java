/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
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
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author ajay
 */
public class ShapefileRenderer {

    public static String generateShapeImage(String filename, String outputdir, String column, String userFilter) {

        //String filename = "/Users/ajay/Downloads/MEOW_zip/MEOW2/meow_ecos.shp";

        final FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
        DataStore dataStore;
        try {
            dataStore = new ShapefileDataStore(new URL("file://"+filename));
            //dataStore = FileDataStoreFinder.getDataStore(new File(filename));
            if (dataStore==null) {
                System.out.println("Opps, datastore is null for " + filename);
            }
            String typename = dataStore.getTypeNames()[0];
            SimpleFeatureSource source = dataStore.getFeatureSource(typename);
            SimpleFeatureCollection features = source.getFeatures();
            SimpleFeatureType schema = features.getSchema();


            //Filter filter = CQL.toFilter("Arctic");
            SimpleFeatureCollection sff = null;
            if (!column.equals("none") && !userFilter.equals("none")) {
                //Filter filter = ff.like(ff.property(column), userFilter);

                ArrayList<Filter> filters = new ArrayList<Filter>();
                for (String flt : userFilter.split(",")) {
                    filters.add(ff.like(ff.property(column), flt));
                }
                Filter filter = ff.or(filters);
                
                sff = source.getFeatures(filter);
                SimpleFeatureIterator fif = sff.features();
                while (fif.hasNext()) {
                    SimpleFeature f = fif.next();
                    //System.out.println(" > " + f.getID() + " - " + f.getAttribute("ECOREGION") + " - " + f.getAttribute("PROVINCE"));
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
                    + " AUTHORITY[\"EPSG\",\"900913\"]] ";
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
            int imageHeight = 700;
            //System.out.println("\n\nDrawing Image");
            MapContext map = new DefaultMapContext();
            //CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
            map.setCoordinateReferenceSystem(wgsCRS);
            map.addLayer(features, style);
            if (styleSelected != null) {
                map.addLayer(sff, styleSelected);
            }
            GTRenderer renderer = new StreamingRenderer();
            renderer.setContext(map);
            ReferencedEnvelope mapBounds = map.getLayerBounds();
            //System.out.println("mapBounds: " + mapBounds);
            double heightToWidth = mapBounds.getSpan(1) / mapBounds.getSpan(0);
            //Rectangle imageBounds = new Rectangle(0, 0, imageWidth, (int) Math.round(imageWidth * heightToWidth));
            Rectangle imageBounds = new Rectangle(0, 0, imageWidth, imageHeight);
            BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D gr = image.createGraphics();
            gr.setPaint(Color.WHITE);
            gr.fill(imageBounds);
            renderer.paint(gr, imageBounds, mapBounds);
            ImageIO.write(image, "jpeg", new File(outputdir+typename+"_" + userFilter + ".jpg"));

            return outputdir+typename+"_" + userFilter + ".jpg";

            //} catch (ParserConfigurationException ex) {
            //    Logger.getLogger(ShapefileRenderer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAuthorityCodeException ex) {
            Logger.getLogger(ShapefileRenderer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FactoryException ex) {
            Logger.getLogger(ShapefileRenderer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ShapefileRenderer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ShapefileRenderer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "";

    }

    public static void main(String[] args) {


//        if (args.length > 0) {
//            System.out.println("Found args:");
//            for (int i=0;i<args.length;i++) {
//                System.out.println("\t"+args[i]);
//            }
//        }

        String shapepath = "";
        String shapeout = "";
        String column = "";
        String filters = "";

        if (args.length > 3) {
            shapepath = args[0];
            shapeout = args[1];
            column = args[2];
            //filters = args[3];
            for (int i=3; i<args.length; i++) {
                filters += args[i];
                if (i != (args.length-1)) {
                    filters += " ";
                }
            }
        }

//        System.out.println(shapepath);
//        System.out.println(shapeout);
//        System.out.println(column);
//        System.out.println(filters);

//        if (args.length != 4) {
//            System.out.println("\n\nUsage:\n\tShapefileRenderer </path/to/shapefile> </path/to/output/directory/> <filter_attribute | \"none\"> <filter_value | \"none\">\n\n");
//            System.exit(1);
//        }

        //System.out.println(generateShapeImage("/Users/ajay/Downloads/MEOW_zip/MEOW2/meow_ecos.shp", "/Users/ajay/Downloads/MEOW_zip/", "PROVINCE", "Arctic"));
        //System.out.println(generateShapeImage(args[0],args[1],args[2],args[3]));
        System.out.println(generateShapeImage(shapepath,shapeout,column,filters));
    }
}
