/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 *
 * @author Adam
 */
public class Util {

    /**
     * get Active Area as WKT string, from a layer name and feature class
     *
     * @param layer name of layer as String
     * @param classification value of feature classification
     * @param register_shape true to register the shape with alaspatial shape register
     * @return
     */
    public static String getWktFromURI(String layer, boolean register_shape) {
        String feature_text = null;//DEFAULT_AREA;

        if (!register_shape) {
            String json = readGeoJSON(layer);
            return feature_text = wktFromJSON(json);
        }

        try {
            String uri = layer;
            String gaz = "gazetteer/";
            int i1 = uri.indexOf(gaz);
            int i2 = uri.indexOf("/", i1 + gaz.length() + 1);
            int i3 = uri.lastIndexOf(".json");
            String table = uri.substring(i1 + gaz.length(), i2);
            String value = uri.substring(i2 + 1, i3);
            //test if available in alaspatial
            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(CommonData.satServer + "/alaspatial/species/shape/lookup");
            get.addParameter("table", table);
            get.addParameter("value", value);
            get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("register table and value with alaspatial: " + slist);

            if (slist != null && result == 200) {
                feature_text = "LAYER(" + layer + "," + slist + ")";

                return feature_text;
            }
        } catch (Exception e) {
            System.out.println("no alaspatial shape for layer: " + layer);
            e.printStackTrace();
        }
        try {
            //class_name is same as layer name
            String json = readGeoJSON(layer);
            feature_text = wktFromJSON(json);

            if (!register_shape) {
                return feature_text;
            }

            //register wkt with alaspatial and use LAYER(layer name, id)
            HttpClient client = new HttpClient();
            //GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            PostMethod get = new PostMethod(CommonData.satServer + "/alaspatial/species/shape/register");
            get.addParameter("area", feature_text);
            get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("register wkt shape with alaspatial: " + slist);

            feature_text = "LAYER(" + layer + "," + slist + ")";
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("SelectionController.getLayerGeoJsonAsWkt(" + layer + "): " + feature_text);
        return feature_text;
    }

    static public String createCircle(double x, double y, final double RADIUS) {
        return createCircle(x, y, RADIUS, 50);

    }

    static public String createCircle(double x, double y, final double RADIUS, int sides) {

        try {
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

//            CoordinateReferenceSystem dataCRS = CRS.decode("EPSG:4326");
//            CoordinateReferenceSystem googleCRS = CRS.decode("EPSG:900913");
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
            MathTransform transform = CRS.findMathTransform(wgsCRS, googleCRS);
            Point point = geometryFactory.createPoint(new Coordinate(y, x));
            Geometry geom = JTS.transform(point, transform);
            Point gPoint = geometryFactory.createPoint(new Coordinate(geom.getCoordinate()));

            System.out.println("Google point:" + gPoint.getCoordinate().x + "," + gPoint.getCoordinate().y);

            MathTransform reverseTransform = CRS.findMathTransform(googleCRS, wgsCRS);
            final int SIDES = sides;
            Coordinate coords[] = new Coordinate[SIDES + 1];
            for (int i = 0; i < SIDES; i++) {
                double angle = ((double) i / (double) SIDES) * Math.PI * 2.0;
                double dx = Math.cos(angle) * RADIUS;
                double dy = Math.sin(angle) * RADIUS;
                geom = JTS.transform(geometryFactory.createPoint(new Coordinate((double) gPoint.getCoordinate().x + dx, (double) gPoint.getCoordinate().y + dy)), reverseTransform);
                coords[i] = new Coordinate(geom.getCoordinate().y, geom.getCoordinate().x);
            }
            coords[SIDES] = coords[0];

            LinearRing ring = geometryFactory.createLinearRing(coords);
            Polygon polygon = geometryFactory.createPolygon(ring, null);


            //Geometry polyGeom = JTS.transform(coords,reverseTransform);
            WKTWriter writer = new WKTWriter();
            String wkt = writer.write(polygon);
            return wkt.replaceAll("POLYGON ", "POLYGON").replaceAll(", ", ",");

        } catch (Exception e) {
            System.out.println("Circle fail!");
            return "none";
        }

    }

    static private String readGeoJSON(String feature) {
        StringBuffer content = new StringBuffer();

        try {
            // Construct data

            // Send data
            URL url = new URL(feature);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                content.append(line);
            }
            conn.disconnect();
        } catch (Exception e) {
        }
        return content.toString();
    }

    /**
     * transform json string with geometries into wkt.
     *
     * extracts 'shape_area' if available and assigns it to storedSize.
     *
     * @param json
     * @return
     */
    static public String wktFromJSON(String json) {
        try {
            JSONObject obj = JSONObject.fromObject(json);
            JSONArray geometries = obj.getJSONArray("geometries");
            String wkt = "";
            for (int i = 0; i < geometries.size(); i++) {
                String coords = geometries.getJSONObject(i).getString("coordinates");

                if (geometries.getJSONObject(i).getString("type").equalsIgnoreCase("multipolygon")) {
                    wkt += coords.replace("]]],[[[", "))*((").replace("]],[[", "))*((").replace("],[", "*").replace(",", " ").replace("*", ",").replace("[[[[", "MULTIPOLYGON(((").replace("]]]]", ")))");

                } else {
                    wkt += coords.replace("],[", "*").replace(",", " ").replace("*", ",").replace("[[[", "POLYGON((").replace("]]]", "))").replace("],[", "),(");
                }

                wkt = wkt.replace(")))MULTIPOLYGON(", ")),");
            }
            return wkt;
        } catch (JSONException e) {
            return "none";
        }
    }

    static public double convertPixelsToMeters(int pixels, double latitude, int zoom) {
        return ((Math.cos(latitude * Math.PI / 180.0) * 2 * Math.PI * 6378137) / (256 * Math.pow(2, zoom))) * pixels;
    }

    static public double calculateArea(String wkt) {
        double sumarea = 0;
        
        try {
            String [] areas = wkt.split("\\),\\(");

            for(String area : areas) {
                area = StringUtils.replace(area, "MULTIPOLYGON((", "");
                area = StringUtils.replace(area, "POLYGON((", "");
                area = StringUtils.replace(area, ")", "");
                area = StringUtils.replace(area, "(", "");

                String[] areaarr = area.split(",");

                double totalarea = 0.0;
                String d = areaarr[0];
                for (int f = 1; f < areaarr.length - 2; ++f) {
                    totalarea += Mh(d, areaarr[f], areaarr[f + 1]);
                }

                sumarea += totalarea * 6378137 * 6378137;
            }

        } catch (Exception e) {
            System.out.println("Error in calculateArea");
            e.printStackTrace(System.out);
        }

        return sumarea;
    }

    static private double Mh(String a, String b, String c) {
        return Nh(a, b, c) * hi(a, b, c);
    }

    static private double Nh(String a, String b, String c) {
        String[] poly = {a, b, c, a};
        double[] area = new double[3];
        int i = 0;
        double j = 0.0;
        for (i = 0; i < 3; ++i) {
            area[i] = vd(poly[i], poly[i + 1]);
            j += area[i];
        }
        j /= 2;
        double f = Math.tan(j / 2);
        for (i = 0; i < 3; ++i) {
            f *= Math.tan((j - area[i]) / 2);
        }
        return 4 * Math.atan(Math.sqrt(Math.abs(f)));
    }

    static private double hi(String a, String b, String c) {
        String[] d = {a, b, c};

        int i = 0;
        double[][] bb = new double[3][3];
        for (i = 0; i < 3; ++i) {
            String[] coords = d[i].split(" ");
            double lng = Double.parseDouble(coords[0]);
            double lat = Double.parseDouble(coords[1]);

            double y = Uc(lat);
            double x = Uc(lng);

            bb[i][0] = Math.cos(y) * Math.cos(x);
            bb[i][1] = Math.cos(y) * Math.sin(x);
            bb[i][2] = Math.sin(y);
        }

        return (bb[0][0] * bb[1][1] * bb[2][2] + bb[1][0] * bb[2][1] * bb[0][2] + bb[2][0] * bb[0][1] * bb[1][2] - bb[0][0] * bb[2][1] * bb[1][2] - bb[1][0] * bb[0][1] * bb[2][2] - bb[2][0] * bb[1][1] * bb[0][2] > 0) ? 1 : -1;
    }

    static private double vd(String a, String b) {
        String[] coords1 = a.split(" ");
        double lng1 = Double.parseDouble(coords1[0]);
        double lat1 = Double.parseDouble(coords1[1]);

        String[] coords2 = b.split(" ");
        double lng2 = Double.parseDouble(coords2[0]);
        double lat2 = Double.parseDouble(coords2[1]);

        double c = Uc(lat1);
        double d = Uc(lat2);

        return 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((c - d) / 2), 2) + Math.cos(c) * Math.cos(d) * Math.pow(Math.sin((Uc(lng1) - Uc(lng2)) / 2), 2)));
    }

    static private double Uc(double a) {
        return a * (Math.PI / 180);
    }
}
