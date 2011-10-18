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
import java.util.ArrayList;
import net.sf.json.JSONException;
import org.ala.spatial.data.Facet;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.BiocacheQuery;
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
    public static String getWktFromURI(String layer) {
        String feature_text = null;//DEFAULT_AREA;

        String json = readGeoJSON(layer);

        return feature_text = wktFromJSON(json);
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
     * only MULTIPOLYGON output.
     *
     * extracts 'shape_area' if available and assigns it to storedSize.
     *
     * @param json
     * @return
     */
    static public String wktFromJSON(String json) {
        try {
            StringBuilder sb = new StringBuilder();
            boolean isPolygon = json.contains("\"type\":\"Polygon\"");
            sb.append("MULTIPOLYGON(");
            if (isPolygon) {
                sb.append("("); //for conversion Polygon to Multipolygon.
            }
            int pos = json.indexOf("coordinates") + "coordinates".length() + 3;
            int end = json.indexOf("}", pos);
            char c = json.charAt(pos);
            char prev_c = ' ';
            char next_c;
            pos++;
            while (pos < end) {
                next_c = json.charAt(pos);
                //lbrace to lbracket, next character is not a number
                if (c == '[') {
                    if (next_c != '-' && (next_c < '0' || next_c > '9')) {
                        sb.append('(');
                    }
                    //rbrace to rbracket, prev character was not a number
                } else if (c == ']') {
                    if (prev_c < '0' || prev_c > '9') {
                        sb.append(')');
                    }
                    //comma to space, prev character was a number
                } else if (c == ',' && prev_c >= '0' && prev_c <= '9') {
                    sb.append(' ');
                    //keep the original value
                } else {
                    sb.append(c);
                }
                prev_c = c;
                c = next_c;
                pos++;
            }
            sb.append(")");
            if (isPolygon) {
                sb.append(")"); //for conversion Polygon to Multipolygon.
            }
            return sb.toString();
        } catch (JSONException e) {
            return "none";
        }
    }

    static public double convertPixelsToMeters(int pixels, double latitude, int zoom) {
        return ((Math.cos(latitude * Math.PI / 180.0) * 2 * Math.PI * 6378137) / (256 * Math.pow(2, zoom))) * pixels;
    }

    static public double calculateArea(String wkt) {
        double sumarea = 0;

        //GEOMETRYCOLLECTION
        String areaWorking = wkt;
        ArrayList<String> stringsList = new ArrayList<String>();
        if (areaWorking.startsWith("GEOMETRYCOLLECTION")) {
            //split out polygons and multipolygons
            areaWorking = areaWorking.replace("GEOMETRYCOLLECTION", "");

            int posStart, posEnd, p1, p2;
            ;
            p1 = areaWorking.indexOf("POLYGON", 0);
            p2 = areaWorking.indexOf("MULTIPOLYGON", 0);
            if (p1 < 0) {
                posStart = p2;
            } else if (p2 < 0) {
                posStart = p1;
            } else {
                posStart = Math.min(p1, p2);
            }
            p1 = areaWorking.indexOf("POLYGON", posStart + 10);
            p2 = areaWorking.indexOf("MULTIPOLYGON", posStart + 10);
            while (p1 > 0 || p2 > 0) {
                if (p1 < 0) {
                    posEnd = p2;
                } else if (p2 < 0) {
                    posEnd = p1;
                } else {
                    posEnd = Math.min(p1, p2);
                }

                stringsList.add(areaWorking.substring(posStart, posEnd - 1));
                posStart = posEnd;
                p1 = areaWorking.indexOf("POLYGON", posStart + 10);
                p2 = areaWorking.indexOf("MULTIPOLYGON", posStart + 10);
            }
            stringsList.add(areaWorking.substring(posStart, areaWorking.length()));
        } else {
            stringsList.add(areaWorking);
        }

        for (String w : stringsList) {
            if (w.contains("ENVELOPE")) {
                continue;
            }
            try {
                String[] areas = w.split("\\),\\(");
                double shapearea = 0;

                for (String area : areas) {
                    area = StringUtils.replace(area, "MULTIPOLYGON((", "");
                    area = StringUtils.replace(area, "POLYGON(", "");
                    area = StringUtils.replace(area, ")", "");
                    area = StringUtils.replace(area, "(", "");

                    String[] areaarr = area.split(",");

                    // check if it's the 'world' bbox
                    boolean isWorld = true;
                    for (int i = 0; i < areaarr.length - 1; i++) {
                        String[] darea = areaarr[i].split(" ");
                        if ((Double.parseDouble(darea[0]) < -174
                                && Double.parseDouble(darea[1]) < -84)
                                || (Double.parseDouble(darea[0]) < -174
                                && Double.parseDouble(darea[1]) > 84)
                                || (Double.parseDouble(darea[0]) > 174
                                && Double.parseDouble(darea[1]) > 84)
                                || (Double.parseDouble(darea[0]) > 174
                                && Double.parseDouble(darea[1]) < -84)) {
                            //return 510000000;
                        } else {
                            isWorld = false;
                            break;
                        }
                    }
                    //if (isWorld) return (510000000 * 1000 * 1000 * 1L);
                    if (isWorld) {
                        return 510000000000000L;
                    }

                    double totalarea = 0.0;
                    String d = areaarr[0];
                    for (int f = 1; f < areaarr.length - 2; ++f) {
                        totalarea += Mh(d, areaarr[f], areaarr[f + 1]);
                    }

                    shapearea += totalarea * 6378137 * 6378137;
                }

                sumarea += Math.abs(shapearea);

            } catch (Exception e) {
                System.out.println("Error in calculateArea");
                e.printStackTrace(System.out);
            }
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

    static public String createCircleJs(double longitude, double latitude, double radius) {
        boolean belowMinus180 = false;
        double[][] points = new double[360][];
        for (int i = 0; i < 360; i++) {
            points[i] = computeOffset(latitude, 0, radius, i);
            if (points[i][0] + longitude < -180) {
                belowMinus180 = true;
            }
        }

        //longitude translation
        double dist = ((belowMinus180) ? 360 : 0) + longitude;

        StringBuilder s = new StringBuilder();
        s.append("POLYGON((");
        for (int i = 0; i < 360; i++) {
            s.append(points[i][0] + dist).append(" ").append(points[i][1]).append(",");
            //if (i < 359) {
            //    s.append(",");
            //}
        }
        // append the first point to close the circle
        s.append(points[0][0] + dist).append(" ").append(points[0][1]);
        s.append("))");

        return s.toString();
    }

    static double[] computeOffset(double lat, double lng, double radius, int angle) {
        double b = radius / 6378137.0;
        double c = angle * (Math.PI / 180.0);
        double e = lat * (Math.PI / 180.0);
        double d = Math.cos(b);
        b = Math.sin(b);
        double f = Math.sin(e);
        e = Math.cos(e);
        double g = d * f + b * e * Math.cos(c);

        double x = (lng * (Math.PI / 180.0) + Math.atan2(b * e * Math.sin(c), d - f * g)) / (Math.PI / 180.0);
        double y = Math.asin(g) / (Math.PI / 180.0);

        double[] pt = {x, y};

        return pt;
    }

    /**
     * Util function to add line breaks to a string - it breaks on whole word
     * @param message The text to perform the break on
     * @param length The interval to add a line break to
     * @return 
     */
    public static String breakString(String message, int length) {
        StringBuffer newMessage = new StringBuffer();
        //buffer of last word (used to split lines by whole word
        StringBuffer lastWord = new StringBuffer();
        for (int i = 0; i < message.length(); i++) {
            if (i % length == 0 && i != 0) {
                if (lastWord.length() > 0) {
                    newMessage.delete(newMessage.length() - lastWord.length(), newMessage.length());
                    newMessage.append("\n");
                    newMessage.append(lastWord);
                    newMessage.append(message.charAt(i));
                    lastWord = new StringBuffer();
                } else {
                    newMessage.append("\n");
                }
            } else {
                //reset lastWord stringbuffer when in hits a space
                if (message.charAt(i) == ' ') {
                    lastWord = new StringBuffer();
                } else {
                    lastWord.append(message.charAt(i));
                }
                newMessage.append(message.charAt(i));
            }
        }
        return newMessage.toString();
    }
}
