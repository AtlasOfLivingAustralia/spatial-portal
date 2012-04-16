/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.RendererUtilities;
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
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.Identifier;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Document;

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
            dataStore = new ShapefileDataStore(new URL("file://" + filename));
            //dataStore = FileDataStoreFinder.getDataStore(new File(filename));
            if (dataStore == null) {
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

                Filter filter;
                ArrayList<Filter> filters = new ArrayList<Filter>();
                if ("fid".equalsIgnoreCase(column)) {
                    Set<FeatureId> fids = new HashSet<FeatureId>();
                    for (String flt : userFilter.split(",")) {
                        fids.add( ff.featureId(flt) );
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
                    System.out.println(" > " + f.getID() + " - " + f.getAttribute("ECOREGION") + " - " + f.getAttribute("PROVINCE"));
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
            int imageHeight = 600;
            //System.out.println("\n\nDrawing Image");
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
            //System.out.println("mapBounds: " + mapBounds);
            double heightToWidth = mapBounds.getSpan(1) / mapBounds.getSpan(0);
            //Rectangle imageBounds = new Rectangle(0, 0, imageWidth, (int) Math.round(imageWidth * heightToWidth));
            Rectangle imageBounds = new Rectangle(0, 0, imageWidth, imageHeight);
            BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D gr = image.createGraphics();
            gr.setPaint(Color.WHITE);
            gr.fill(imageBounds);
            renderer.paint(gr, imageBounds, mapBounds);
            ImageIO.write(image, "jpeg", new File(outputdir + typename + "_" + userFilter + ".jpg"));



            //exportSVG(map, mapBounds, new FileOutputStream(outputdir + typename + "_" + userFilter + ".svg"), new Dimension(1000, 600));
            //testsvg(map, mapBounds, imageBounds, sff);
            //testsvg2(features, new FileOutputStream(outputdir + typename + "_" + userFilter + "_google.svg"));
            System.out.println("writing svg to: " + outputdir + typename + "_" + userFilter + "_google.svg");

            return outputdir + typename + "_" + userFilter + ".jpg";

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

//    public static void testsvg(MapContext map, ReferencedEnvelope env, Rectangle imageBounds, SimpleFeatureCollection sff) {
//        try {
//            AffineTransform wst = RendererUtilities.worldToScreenTransform(env, imageBounds);
//
//            System.out.println("*****************************");
//            System.out.println(env.toString());
//            System.out.println(imageBounds.toString());
//            System.out.println("*****************************");
//
//            SimpleFeatureIterator fif = sff.features();
//            while (fif.hasNext()) {
//                SimpleFeature f = fif.next();
//                //System.out.println(" > " + f.getID() + " - " + f.getAttribute("ECOREGION") + " - " + f.getAttribute("PROVINCE"));
//                Geometry geom = (Geometry) f.getDefaultGeometry();
//                System.out.println("type: " + geom.getGeometryType());
//                System.out.println("num.geoms: " + geom.getNumGeometries());
//                System.out.println("num.points: " + geom.getNumPoints());
//
//                //System.out.println("Geom:\n" + geom.toText());
//
//                Coordinate[] coordinates = geom.getCoordinates();
//                System.out.println("coords.len: " + coordinates.length);
//                for (int i = 0; i < coordinates.length; i++) {
//                    System.out.println("*****************************");
//                    System.out.println("orig: " + coordinates[i].x + ", " + coordinates[i].y);
//                    Point2D ps = new DirectPosition2D(coordinates[i].x, coordinates[i].y);
//                    Point2D pd = wst.transform(ps, null);
//                    System.out.println("tran: " + pd.getX() + ", " + pd.getY());
//
//                    Coordinate cd = JTS.transform(coordinates[i], null, new AffineTransform2D(wst));
//                    System.out.println("jtst: " + cd.x + ", " + cd.y);
//                    System.out.println("*****************************");
//                }
//
//
//            }
//
//
//        } catch (Exception e) {
//            System.out.println("Exception in testsvg");
//            e.printStackTrace(System.out);
//        }
//    }
//
////    public static void testsvg2(SimpleFeatureCollection sff, FileOutputStream file) {
////        try {
////            System.out.println("Starting testsvg2");
////            GenerateSVGdocument svg = new GenerateSVGdocument();
////            Document svgDoc = svg.initialiseDocument("1000", "600");
////            svg.setViewBoxAttribute(10.0, 10.0, 1000.0, 600.0);
////
////            Element svgRoot = svgDoc.getDocumentElement();
////
////            Element gElement = svgDoc.createElementNS("http://www.w3.org/2000/svg", "g");
////            gElement.setAttributeNS(null, "id", "vectorLayer");
////
////            svgRoot.appendChild(gElement);
////
////            double midpoint = svg.getMiddleY();
////            //midpoint = 100;
////            midpoint = 50;
////
////            System.out.println("Setting midpoint: " + midpoint);
////
////
////
////            StringBuilder sbPath = new StringBuilder();
////            StringBuilder sbInfo = new StringBuilder();
////            GeneratePolygonElement gpe = new GeneratePolygonElement();
////            SimpleFeatureIterator fif = sff.features();
////            while (fif.hasNext()) {
////                SimpleFeature f = fif.next();
////                sbPath.append("\"").append(f.getID()).append("\":\"").append(gpe.transformFeature(f, f.getID(), svgDoc, gElement, midpoint, 255, 255, 255, 0.5, 0, 0, 0, 1, "1px", null, null, null, null)).append("\"");
////
////                sbInfo.append("\"").append(f.getID()).append("\":").append("{");
////                for (AttributeType at : sff.getSchema().getTypes()) {
////                    if (sff.getSchema().getDescriptor(at.getName()) == null) {
////                        continue;
////                    }
////                    Object obj = f.getAttribute(at.getName());
////                    if (obj != null) {
////                        sbInfo.append(at.getName().toString()).append(":\"").append(String.valueOf(obj)).append("\"");
////                    }
////                    sbInfo.append(",");
////                }
////                sbInfo.deleteCharAt(sbInfo.lastIndexOf(",")).append("}");
////
////                if (fif.hasNext()) {
////                    sbPath.append(",");
////                    sbInfo.append(",");
////                }
////            }
////
////
////            // Prepare the DOM document for writing
////            Source source = new DOMSource(svgDoc);
////
////            // Prepare the output file
////            //File file = new File(filename);
////            Result result = new StreamResult(file);
////
////            // Write the DOM document to the file
////            Transformer xformer = TransformerFactory.newInstance().newTransformer();
////            xformer.transform(source, result);
////
////            System.out.println("var shapefile = {shapes:{"+sbPath.toString()+"},info:{"+sbInfo.toString()+"}};");
////
////
////            System.out.println("Finished testsvg2");
////        } catch (Exception ex) {
////            Logger.getLogger(ShapefileRenderer.class.getName()).log(Level.SEVERE, null, ex);
////        }
////    }
//
//    public static void exportSVG(MapContext map, ReferencedEnvelope env, OutputStream out,
//            Dimension canvasSize) throws IOException, ParserConfigurationException {
//        if (canvasSize == null) {
//            canvasSize = new Dimension(300, 300); // default of 300x300
//        }
//        Document document = null;
//
//        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//        DocumentBuilder db = dbf.newDocumentBuilder();
//
//        // Create an instance of org.w3c.dom.Document
//        document = db.getDOMImplementation().createDocument("http://www.w3.org/2000/svg", "svg", null);
//
//        // Set up the map
//        SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);
//        ctx.setComment("Generated by GeoTools2 with Batik SVG Generator");
//
//        SVGGraphics2D g2d = new SVGGraphics2D(ctx, true);
//
//        g2d.setSVGCanvasSize(canvasSize);
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//        StreamingRenderer renderer = new StreamingRenderer();
//        renderer.setContext(map);
//
//        Rectangle outputArea = new Rectangle(g2d.getSVGCanvasSize());
//        ReferencedEnvelope dataArea = map.getMaxBounds();
//
//        System.out.println("rendering map");
//        renderer.paint(g2d, outputArea, dataArea);
//        System.out.println("writing to file");
//        OutputStreamWriter osw = null;
//        try {
//            osw = new OutputStreamWriter(out, "UTF-8");
//            g2d.stream(osw);
//        } finally {
//            if (osw != null) {
//                osw.close();
//            }
//        }
//
//    }

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
            for (int i = 3; i < args.length; i++) {
                filters += args[i];
                if (i != (args.length - 1)) {
                    filters += " ";
                }
            }
        }

        System.out.println(shapepath);
        System.out.println(shapeout);
        System.out.println(column);
        System.out.println(filters);

        if (args.length != 4) {
            System.out.println("\n\nUsage:\n\tShapefileRenderer </path/to/shapefile> </path/to/output/directory/> <filter_attribute | \"none\"> <filter_value | \"none\">\n\n");
            System.exit(1);
        }

        //System.out.println(generateShapeImage("/Users/ajay/Downloads/MEOW_zip/MEOW2/meow_ecos.shp", "/Users/ajay/Downloads/MEOW_zip/", "ECOREGION", "Baltic Sea"));
        //System.out.println(generateShapeImage(args[0],args[1],args[2],args[3]));
        System.out.println(generateShapeImage(shapepath,shapeout,column,filters));
    }
}
