/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.util;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.legend.Facet;
import au.org.ala.legend.LegendObject;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.WKTReducedDTO;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.zkoss.zk.ui.Executions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.*;


/**
 * @author Adam
 */
public final class Util {
    private static final Logger LOGGER = Logger.getLogger(Util.class);
    private static int currentColourIdx = 0;

    private Util() {
        //to hide public constructor
    }

    public static String createCircle(double x, double y, final double radius, int sides) {

        try {
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

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
            MathTransform transform = CRS.findMathTransform(wgsCRS, googleCRS);
            Point point = geometryFactory.createPoint(new Coordinate(y, x));
            Geometry geom = JTS.transform(point, transform);
            Point gPoint = geometryFactory.createPoint(new Coordinate(geom.getCoordinate()));

            LOGGER.debug("Google point:" + gPoint.getCoordinate().x + "," + gPoint.getCoordinate().y);

            MathTransform reverseTransform = CRS.findMathTransform(googleCRS, wgsCRS);

            Coordinate[] coords = new Coordinate[sides + 1];
            for (int i = 0; i < sides; i++) {
                double angle = ((double) i / (double) sides) * Math.PI * 2.0;
                double dx = Math.cos(angle) * radius;
                double dy = Math.sin(angle) * radius;
                geom = JTS.transform(geometryFactory.createPoint(new Coordinate(gPoint.getCoordinate().x + dx, gPoint.getCoordinate().y + dy)), reverseTransform);
                coords[i] = new Coordinate(geom.getCoordinate().y, geom.getCoordinate().x);
            }
            coords[sides] = coords[0];

            LinearRing ring = geometryFactory.createLinearRing(coords);
            Polygon polygon = geometryFactory.createPolygon(ring, null);

            WKTWriter writer = new WKTWriter();
            String wkt = writer.write(polygon);
            return wkt.replaceAll(StringConstants.POLYGON + " ", StringConstants.POLYGON).replaceAll(", ", ",");

        } catch (Exception e) {
            LOGGER.debug("Circle fail!");
            return StringConstants.NONE;
        }

    }

    /**
     * transform json string with geometries into wkt.
     * <p/>
     * only MULTIPOLYGON output.
     * <p/>
     * extracts 'shape_area' if available and assigns it to storedSize.
     *
     * @param json
     * @return
     */
    public static String wktFromJSON(String json) {
        try {
            StringBuilder sb = new StringBuilder();
            boolean isPolygon = json.contains("\"type\":\"Polygon\"");
            sb.append("MULTIPOLYGON(");
            if (isPolygon) {
                //for conversion Polygon to Multipolygon.
                sb.append("(");
            }
            int pos = json.indexOf(StringConstants.COORDINATES) + StringConstants.COORDINATES.length() + 3;
            int end = json.indexOf('}', pos);
            char c = json.charAt(pos);
            char prevC = ' ';
            char nextC;
            pos++;
            while (pos < end) {
                nextC = json.charAt(pos);
                //lbrace to lbracket, next character is not a number
                if (c == '[') {
                    if (nextC != '-' && (nextC < '0' || nextC > '9')) {
                        sb.append('(');
                    }
                    //rbrace to rbracket, prev character was not a number
                } else if (c == ']') {
                    if (prevC < '0' || prevC > '9') {
                        sb.append(')');
                    }
                    //comma to space, prev character was a number
                } else if (c == ',' && prevC >= '0' && prevC <= '9') {
                    sb.append(' ');
                    //keep the original value
                } else {
                    sb.append(c);
                }
                prevC = c;
                c = nextC;
                pos++;
            }
            sb.append(")");
            if (isPolygon) {
                //for conversion Polygon to Multipolygon.
                sb.append(")");
            }
            return sb.toString();
        } catch (Exception e) {
            return StringConstants.NONE;
        }
    }

    public static double calculateArea(String wkt) {
        double sumarea = 0;

        //GEOMETRYCOLLECTION
        String areaWorking = wkt;
        List<String> stringsList = new ArrayList<String>();
        if (areaWorking.startsWith(StringConstants.GEOMETRYCOLLECTION)) {
            //split out polygons and multipolygons
            areaWorking = areaWorking.replace(StringConstants.GEOMETRYCOLLECTION, "");

            int posStart, posEnd, p1, p2;
            p1 = areaWorking.indexOf(StringConstants.POLYGON, 0);
            p2 = areaWorking.indexOf(StringConstants.MULTIPOLYGON, 0);
            if (p1 < 0) {
                posStart = p2;
            } else if (p2 < 0) {
                posStart = p1;
            } else {
                posStart = Math.min(p1, p2);
            }
            p1 = areaWorking.indexOf(StringConstants.POLYGON, posStart + 10);
            p2 = areaWorking.indexOf(StringConstants.MULTIPOLYGON, posStart + 10);
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
                p1 = areaWorking.indexOf(StringConstants.POLYGON, posStart + 10);
                p2 = areaWorking.indexOf(StringConstants.MULTIPOLYGON, posStart + 10);
            }
            stringsList.add(areaWorking.substring(posStart, areaWorking.length()));
        } else {
            stringsList.add(areaWorking);
        }

        for (String w : stringsList) {
            if (w.contains(StringConstants.ENVELOPE)) {
                continue;
            }
            try {
                String[] areas = w.split("\\),\\(");
                double shapearea = 0;

                for (String area : areas) {
                    area = StringUtils.replace(area, " (", "");
                    area = StringUtils.replace(area, ", ", ",");
                    area = StringUtils.replace(area, StringConstants.MULTIPOLYGON, "");
                    area = StringUtils.replace(area, StringConstants.POLYGON, "");
                    area = StringUtils.replace(area, ")", "");
                    area = StringUtils.replace(area, "(", "");

                    String[] areaarr = area.split(",");

                    // check if it's the 'world' bbox
                    boolean isWorld = true;
                    for (int i = 0; i < areaarr.length - 1 && isWorld; i++) {
                        String[] darea = areaarr[i].split(" ");
                        if (!((Double.parseDouble(darea[0]) < -174
                                && Double.parseDouble(darea[1]) < -84)
                                || (Double.parseDouble(darea[0]) < -174
                                && Double.parseDouble(darea[1]) > 84)
                                || (Double.parseDouble(darea[0]) > 174
                                && Double.parseDouble(darea[1]) > 84)
                                || (Double.parseDouble(darea[0]) > 174
                                && Double.parseDouble(darea[1]) < -84))) {
                            isWorld = false;
                        }
                    }
                    //use world area
                    if (isWorld) {
                        return 510000000000000L;
                    }

                    double totalarea = 0.0;
                    String d = areaarr[0];
                    for (int f = 1; f < areaarr.length - 2; ++f) {
                        totalarea += mh(d, areaarr[f], areaarr[f + 1]);
                    }

                    shapearea += totalarea * 6378137 * 6378137;
                }

                sumarea += Math.abs(shapearea);

            } catch (Exception e) {
                LOGGER.error("Error in calculateArea", e);
            }
        }

        return sumarea;
    }

    private static double mh(String a, String b, String c) {
        return nh(a, b, c) * hi(a, b, c);
    }

    private static double nh(String a, String b, String c) {
        String[] poly = {a, b, c, a};
        double[] area = new double[3];
        int i;
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

    private static double hi(String a, String b, String c) {
        String[] d = {a, b, c};

        int i;
        double[][] bb = new double[3][3];
        for (i = 0; i < 3; ++i) {
            String[] coords = d[i].split(" ");
            double lng = Double.parseDouble(coords[0]);
            double lat = Double.parseDouble(coords[1]);

            double y = uc(lat);
            double x = uc(lng);

            bb[i][0] = Math.cos(y) * Math.cos(x);
            bb[i][1] = Math.cos(y) * Math.sin(x);
            bb[i][2] = Math.sin(y);
        }

        return (bb[0][0] * bb[1][1] * bb[2][2] + bb[1][0] * bb[2][1] * bb[0][2] + bb[2][0] * bb[0][1] * bb[1][2] - bb[0][0] * bb[2][1] * bb[1][2] - bb[1][0] * bb[0][1] * bb[2][2] - bb[2][0] * bb[1][1] * bb[0][2] > 0) ? 1 : -1;
    }

    private static double vd(String a, String b) {
        String[] coords1 = a.split(" ");
        double lng1 = Double.parseDouble(coords1[0]);
        double lat1 = Double.parseDouble(coords1[1]);

        String[] coords2 = b.split(" ");
        double lng2 = Double.parseDouble(coords2[0]);
        double lat2 = Double.parseDouble(coords2[1]);

        double c = uc(lat1);
        double d = uc(lat2);

        return 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((c - d) / 2), 2) + Math.cos(c) * Math.cos(d) * Math.pow(Math.sin((uc(lng1) - uc(lng2)) / 2), 2)));
    }

    private static double uc(double a) {
        return a * (Math.PI / 180);
    }

    public static String createCircleJs(double longitude, double latitude, double radius) {
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
        s.append(StringConstants.POLYGON).append("((");
        for (int i = 0; i < 360; i++) {
            s.append(points[i][0] + dist).append(" ").append(points[i][1]).append(",");
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

        return new double[]{x, y};
    }

    /**
     * Util function to add line breaks to a string - it breaks on whole word
     *
     * @param message The text to perform the break on
     * @param length  The interval to add a line break to
     * @return
     */
    public static String breakString(String message, int length) {
        StringBuilder newMessage = new StringBuilder();
        //buffer of last word (used to split lines by whole word
        StringBuilder lastWord = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            if (i % length == 0 && i != 0) {
                if (lastWord.length() > 0) {
                    newMessage.delete(newMessage.length() - lastWord.length(), newMessage.length());
                    newMessage.append("\n");
                    newMessage.append(lastWord);
                    newMessage.append(message.charAt(i));
                    lastWord = new StringBuilder();
                } else {
                    newMessage.append("\n");
                }
            } else {
                //reset lastWord stringbuffer when in hits a space
                if (message.charAt(i) == ' ') {
                    lastWord = new StringBuilder();
                } else {
                    lastWord.append(message.charAt(i));
                }
                newMessage.append(message.charAt(i));
            }
        }
        return newMessage.toString();
    }

    public static String readUrl(String feature) {
        StringBuilder content = new StringBuilder();

        HttpURLConnection conn = null;
        try {
            // Send data
            URL url = new URL(feature);
            conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = rd.readLine()) != null) {
                content.append(line).append("\n");
            }

        } catch (Exception e) {
            LOGGER.error("failed to read from: " + feature, e);
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    LOGGER.error("failed to close url: " + feature, e);
                }
            }
        }
        return content.toString();
    }

    public static Facet getFacetForObject(String value, String id) {
        //test facet
        Facet f = new Facet(id, "\"" + value + "\"", true);

        //test if this facet is in biocache with some matches
        List<Facet> facets = new ArrayList<Facet>();
        facets.add(f);
        if (new BiocacheQuery(null, null, null, facets, false, null).getOccurrenceCount() > 0) {
            return f;
        }

        return null;
    }

    public static String getStringValue(String startAt, String tag, String json) {
        String typeStart = "\"" + tag + "\":\"";
        String typeEnd = "\"";
        int beginning = startAt == null ? 0 : json.indexOf(startAt) + startAt.length();
        int start = json.indexOf(typeStart, beginning) + typeStart.length();
        int end = json.indexOf(typeEnd, start);
        return json.substring(start, end);
    }

    public static int findInArray(String lookFor, String[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(lookFor)) {
                return i;
            }
        }
        return -1;
    }

    public static int nextColour() {
        int colour = LegendObject.colours[currentColourIdx % LegendObject.colours.length];

        currentColourIdx++;

        return colour;
    }


    public static List<Map.Entry<String, String>> getQueryParameters(String params) {
        if (params == null || params.length() == 0) {
            return new ArrayList();
        }

        List<Map.Entry<String, String>> list = new ArrayList<Map.Entry<String, String>>();
        for (String s : params.split("&")) {
            String[] keyvalue = s.split("=");
            if (keyvalue.length >= 2) {
                String key = keyvalue[0];
                String value = keyvalue[1];
                try {
                    value = URLDecoder.decode(value, StringConstants.UTF_8);
                } catch (Exception e) {
                    LOGGER.error("error decoding to UTF-8: " + value, e);
                }
                list.add(new HashMap.SimpleEntry<String, String>(key, value));
            }
        }

        return list;
    }

    public static String getUserEmail() {
        String useremail = null;
        try {
            if (Executions.getCurrent().getUserPrincipal() != null) {
                Principal principal = Executions.getCurrent().getUserPrincipal();
                if (principal instanceof AttributePrincipal) {
                    AttributePrincipal ap = (AttributePrincipal) principal;
                    useremail = (String) ap.getAttributes().get("email");
                } else {
                    useremail = principal.getName();
                }
            }
        } catch (Exception e) {
            LOGGER.error("no user available", e);
        }

        if (useremail == null) {
            return "guest@ala.org.au";
        }

        return useremail;
    }

    public static boolean isLoggedIn() {
        return getUserEmail() != null;
    }

    public static WKTReducedDTO reduceWKT(String originalWKT) {
        String wkt = originalWKT;
        String reducedBy = "No reduction.";
        if (wkt == null) {
            return new WKTReducedDTO(null, null, "Invalid WKT.");
        }
        try {
            WKTReader wktReader;
            com.vividsolutions.jts.geom.Geometry g = null;

            //reduction attempts, 3 decimal places, 2, 1, .2 increments, .5 increments,
            // and finally, convert to 1/1.001 increments (expected to be larger) then back to .5 increments (expected to be smaller)
            // to make the WKT string shorter.
            double[] reductionValues = {1000, 100, 10, 5, 2};
            int attempt = 0;
            while (wkt.length() > Integer.parseInt(CommonData.getSettings().getProperty("max_q_wkt_length")) && attempt < reductionValues.length) {
                if (g == null) {
                    wktReader = new WKTReader();
                    g = wktReader.read(wkt);
                }

                int startLength = wkt.length();

                //reduce to decimal places
                g = com.vividsolutions.jts.precision.GeometryPrecisionReducer.reduce(g, new PrecisionModel(reductionValues[attempt]));

                //stop if something is wrong and use previous iteration WKT
                String newwkt = g.toString();
                if (g == null || newwkt == null || newwkt.length() < 100 || !(new IsValidOp(g).isValid())) {
                    break;
                }

                wkt = newwkt;
                LOGGER.info("reduced WKT from string length " + startLength + " to " + wkt.length());

                reducedBy = String.format("Reduced to resolution %f decimal degrees. \r\nWKT character length " + startLength + " to " + wkt.length(), 1 / reductionValues[attempt]);

                attempt++;
            }
            LOGGER.info("user WKT of length: " + wkt.length());
        } catch (Exception e) {
            LOGGER.error("failed to reduce WKT size", e);
        }

        //webportal (for some reason) does not like these spaces in WKT
        return new WKTReducedDTO(originalWKT, wkt.replace(" (", "(").replace(", ", ","), reducedBy);
    }

    public static String readUrlPost(String url, NameValuePair[] params) {
        try {
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(url);

            post.setRequestBody(params);

            int result = client.executeMethod(post);

            // Get the response
            if (result == 200) {
                return post.getResponseBodyAsString();
            }
        } catch (Exception e) {
            LOGGER.error("failed to read url: " + url, e);
        }
        return null;
    }

    public static String[] getDistributionOrChecklist(String spcode) {
        try {
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append("/distribution/").append(spcode);

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.getLayersServer() + sbProcessUrl.toString());
            LOGGER.debug(CommonData.getLayersServer() + sbProcessUrl.toString());
            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);
            int result = client.executeMethod(get);
            if (result == 200) {
                String txt = get.getResponseBodyAsString();
                JSONParser jp = new JSONParser();
                JSONObject jo = (JSONObject) jp.parse(txt);
                if (jo == null) {
                    return new String[0];
                } else {
                    String[] output = new String[14];

                    String scientific = jo.containsKey(StringConstants.SCIENTIFIC) ? jo.get(StringConstants.SCIENTIFIC).toString() : "";
                    String auth = jo.containsKey(StringConstants.AUTHORITY) ? jo.get(StringConstants.AUTHORITY).toString() : "";
                    String common = jo.containsKey(StringConstants.COMMON_NAM) ? jo.get(StringConstants.COMMON_NAM).toString() : "";
                    String family = jo.containsKey(StringConstants.FAMILY) ? jo.get(StringConstants.FAMILY).toString() : "";
                    String genus = jo.containsKey(StringConstants.GENUS) ? jo.get(StringConstants.GENUS).toString() : "";
                    String name = jo.containsKey(StringConstants.SPECIFIC_N) ? jo.get(StringConstants.SPECIFIC_N).toString() : "";
                    String min = jo.containsKey(StringConstants.MIN_DEPTH) ? jo.get(StringConstants.MIN_DEPTH).toString() : "";
                    String max = jo.containsKey(StringConstants.MAX_DEPTH) ? jo.get(StringConstants.MAX_DEPTH).toString() : "";

                    String md = jo.containsKey(StringConstants.METADATA_U) ? jo.get(StringConstants.METADATA_U).toString() : "";
                    String lsid = jo.containsKey(StringConstants.LSID) ? jo.get(StringConstants.LSID).toString() : "";
                    String areaName = jo.containsKey(StringConstants.AREA_NAME) ? jo.get(StringConstants.AREA_NAME).toString() : "";
                    String areaKm = jo.containsKey(StringConstants.AREA_KM) ? jo.get(StringConstants.AREA_KM).toString() : "";
                    String dataResourceId = jo.containsKey(StringConstants.DATA_RESOURCE_UID) ? jo.get(StringConstants.DATA_RESOURCE_UID).toString() : "";

                    output[0] = spcode;
                    output[1] = scientific;
                    output[2] = auth;
                    output[3] = common;
                    output[4] = family;
                    output[5] = genus;
                    output[6] = name;
                    output[7] = min;
                    output[8] = max;
                    output[9] = md;
                    output[10] = lsid;
                    output[11] = areaName;
                    output[12] = areaKm;
                    output[13] = dataResourceId;

                    return output;
                }
            }
        } catch (Exception e) {
            LOGGER.error("error building distributions list", e);
        }
        return new String[0];
    }

    /**
     * Generates data for rendering of distributions table.
     *
     * @param type
     * @param wkt
     * @param lsids
     * @param geomIdx
     * @return
     */
    public static String[] getDistributionsOrChecklists(String type, String wkt, String lsids, String geomIdx) {
        try {
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append("/").append(type);

            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(CommonData.getLayersServer() + sbProcessUrl.toString());
            LOGGER.debug(CommonData.getLayersServer() + sbProcessUrl.toString());
            if (wkt != null) {
                post.addParameter(StringConstants.WKT, wkt);
            }
            if (lsids != null) {
                post.addParameter(StringConstants.LSIDS, lsids);
            }
            if (geomIdx != null) {
                post.addParameter(StringConstants.GEOM_IDX, geomIdx);
            }
            post.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);
            int result = client.executeMethod(post);
            if (result == 200) {
                String txt = post.getResponseBodyAsString();
                JSONParser jp = new JSONParser();
                JSONArray ja = (JSONArray) jp.parse(txt);
                if (ja == null || ja.isEmpty()) {
                    return new String[0];
                } else {
                    String[] lines = new String[ja.size() + 1];
                    lines[0] = "SPCODE,SCIENTIFIC_NAME,AUTHORITY_FULL,COMMON_NAME,FAMILY,GENUS_NAME,SPECIFIC_NAME,MIN_DEPTH,MAX_DEPTH,METADATA_URL,LSID,AREA_NAME,AREA_SQ_KM";
                    for (int i = 0; i < ja.size(); i++) {
                        JSONObject jo = (JSONObject) ja.get(i);
                        String spcode = jo.containsKey(StringConstants.SPCODE) ? jo.get(StringConstants.SPCODE).toString() : "";
                        String scientific = jo.containsKey(StringConstants.SCIENTIFIC) ? jo.get(StringConstants.SCIENTIFIC).toString() : "";
                        String auth = jo.containsKey(StringConstants.AUTHORITY) ? jo.get(StringConstants.AUTHORITY).toString() : "";
                        String common = jo.containsKey(StringConstants.COMMON_NAM) ? jo.get(StringConstants.COMMON_NAM).toString() : "";
                        String family = jo.containsKey(StringConstants.FAMILY) ? jo.get(StringConstants.FAMILY).toString() : "";
                        String genus = jo.containsKey(StringConstants.GENUS) ? jo.get(StringConstants.GENUS).toString() : "";
                        String name = jo.containsKey(StringConstants.SPECIFIC_N) ? jo.get(StringConstants.SPECIFIC_N).toString() : "";
                        String min = jo.containsKey(StringConstants.MIN_DEPTH) ? jo.get(StringConstants.MIN_DEPTH).toString() : "";
                        String max = jo.containsKey(StringConstants.MAX_DEPTH) ? jo.get(StringConstants.MAX_DEPTH).toString() : "";

                        String md = jo.containsKey(StringConstants.METADATA_U) ? jo.get(StringConstants.METADATA_U).toString() : "";
                        String lsid = jo.containsKey(StringConstants.LSID) ? jo.get(StringConstants.LSID).toString() : "";
                        String areaName = jo.containsKey(StringConstants.AREA_NAME) ? jo.get(StringConstants.AREA_NAME).toString() : "";
                        String areaKm = jo.containsKey(StringConstants.AREA_KM) ? jo.get(StringConstants.AREA_KM).toString() : "";
                        String dataResourceUid = jo.containsKey(StringConstants.DATA_RESOURCE_UID) ? jo.get(StringConstants.DATA_RESOURCE_UID).toString() : "";

                        lines[i + 1] = spcode + "," + wrap(scientific) + "," + wrap(auth) + "," + wrap(common) + ","
                                + wrap(family) + "," + wrap(genus) + "," + wrap(name) + "," + min + "," + max
                                + "," + wrap(md) + "," + wrap(lsid) + "," + wrap(areaName) + "," + wrap(areaKm)
                                + "," + wrap(dataResourceUid);
                    }

                    return lines;
                }
            }
        } catch (Exception e) {
            LOGGER.error("error building distribution or checklist csv", e);
        }
        return new String[0];
    }

    public static String wrap(String s) {
        return "\"" + s.replace("\"", "\"\"").replace("\\", "\\\\") + "\"";
    }

    public static String[] getAreaChecklists(String[] records) {
        String[] lines = null;
        try {
            if (records != null && records.length > 0) {
                String[][] data = new String[records.length - 1][];
                // header
                for (int i = 1; i < records.length; i++) {
                    CSVReader csv = new CSVReader(new StringReader(records[i]));
                    data[i - 1] = csv.readNext();
                    csv.close();
                }
                java.util.Arrays.sort(data, new Comparator<String[]>() {
                    @Override
                    public int compare(String[] o1, String[] o2) {
                        // compare WMS urls
                        return CommonData.getSpeciesChecklistWMSFromSpcode(o1[0])[1].compareTo(CommonData.getSpeciesChecklistWMSFromSpcode(o2[0])[1]);
                    }
                });

                lines = new String[records.length];
                lines[0] = lines[0] = "SPCODE,SCIENTIFIC_NAME,AUTHORITY_FULL,COMMON_NAME,FAMILY,GENUS_NAME,SPECIFIC_NAME,MIN_DEPTH,MAX_DEPTH,METADATA_URL,LSID,AREA_NAME,AREA_SQ_KM,SPECIES_COUNT";
                int len = 1;
                int thisCount = 0;
                for (int i = 0; i < data.length; i++) {
                    thisCount++;
                    if (i == data.length - 1 || !CommonData.getSpeciesChecklistWMSFromSpcode(data[i][0])[1].equals(CommonData.getSpeciesChecklistWMSFromSpcode(data[i + 1][0])[1])) {
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < data[i].length; j++) {
                            if (j > 0) {
                                sb.append(",");
                            }
                            if (j == 0 || (j >= 9 && j != 10)) {
                                sb.append(Util.wrap(data[i][j]));
                            }
                        }
                        sb.append(",").append(thisCount);
                        lines[len] = sb.toString();
                        len++;
                        thisCount = 0;
                    }
                }
                lines = java.util.Arrays.copyOf(lines, len);
            }
        } catch (Exception e) {
            LOGGER.error("error building species checklist", e);
            lines = null;
        }
        return lines;
    }

    public static String getMetadataHtmlForDistributionOrChecklist(String spcode, String[] row, String layerName) {
        if (CommonData.getSpeciesDistributionWMSFromSpcode(spcode) != null && CommonData.getSpeciesDistributionWMSFromSpcode(spcode)[0] != null) {
            return getMetadataHtmlForExpertDistribution(Util.getDistributionOrChecklist(spcode));
        } else {
            return getMetadataHtmlForAreaChecklist(spcode, layerName);
        }
    }

    public static String getMetadataHtmlForExpertDistribution(String[] row) {
        if (row.length == 0) {
            return null;
        }

        String scientificName = row[1];
        String commonName = row[3];
        String familyName = row[4];
        String minDepth = row[7];
        String maxDepth = row[8];
        String metadataLink = row[9];
        String lsid = row[10];
        String area = row[12];
        String dataResourceUid = row[13];

        String[] distributionCollectoryDetails = getDistributionCollectoryDetails(dataResourceUid);
        String websiteUrl = distributionCollectoryDetails[0];
        String citation = distributionCollectoryDetails[1];
        String logoUrl = distributionCollectoryDetails[2];

        String speciesPageUrl = CommonData.getBieServer() + "/species/" + lsid;

        String html = "Expert Distribution\n";
        html += "<table class='md_table'>";
        html += "<tr class='md_grey-bg'><td class='md_th'>Scientific name: </td><td class='md_spacer'/><td class='md_value'><a target='_blank' href='" + speciesPageUrl + "'>" + scientificName
                + "</a></td></tr>";
        html += "<tr><td class='md_th'>Common name: </td><td class='md_spacer'/><td class='md_value'>" + commonName + "</td></tr>";
        html += "<tr class='md_grey-bg'><td class='md_th'>Family name: </td><td class='md_spacer'/><td class='md_value'>" + familyName + "</td></tr>";
        String lastClass = "";
        if (row[7] != null && row[7].length() > 0) {
            html += "<tr class='" + lastClass + "'><td class='md_th'>Min depth: </td><td class='md_spacer'/><td class='md_value'>" + minDepth + "</td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        }
        if (row[8] != null && row[8].length() > 0) {
            html += "<tr class='" + lastClass + "'><td class='md_th'>Max depth: </td><td class='md_spacer'/><td class='md_value'>" + maxDepth + "</td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        }
        if (row[9] != null && row[9].length() > 0) {
            html += "<tr class='" + lastClass + "'><td class='md_th'>Metadata link: </td><td class='md_spacer'/><td class='md_value'><a target='_blank' href='" + metadataLink + "'>" + metadataLink
                    + "</a></td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        }
        if (row[12] != null && row[12].length() > 0) {
            html += "<tr class='" + lastClass + "'><td class='md_th'>Area sq km: </td><td class='md_spacer'/><td class='md_value'>" + area + "</td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        }
        html += "<tr class='" + lastClass + "'><td class='md_th'>Source website: </td><td class='md_spacer'/><td class='md_value'><a target='_blank' href='" + websiteUrl + "'>" + websiteUrl
                + "</a></td></tr>";
        lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";

        if (citation != null) {
            html += "<tr class='" + lastClass + "'><td class='md_th'>Citation: </td><td class='md_spacer'/><td class='md_value'>" + citation + "</td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        }

        if (logoUrl != null) {
            html += "<tr class='" + lastClass + "'><td class='md_th'></td><td class='md_spacer'/><td class='md_value'><img src=\"" + logoUrl + "\"/></td></tr>";
        }

        html += "</table>";

        return html;
    }

    // Fetches the website url, citation and logo url from the data resource
    // associated with an expert distribution.
    public static String[] getDistributionCollectoryDetails(String dataResourceUid) {
        try {
            String url = CommonData.collectoryServer + "/dataResource/" + dataResourceUid;

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);
            LOGGER.debug(url);
            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);
            int result = client.executeMethod(get);
            if (result == 200) {
                String txt = get.getResponseBodyAsString();
                JSONParser jp = new JSONParser();
                JSONObject jo = (JSONObject) jp.parse(txt);
                if (jo == null) {
                    return new String[0];
                } else {
                    String[] output = new String[13];
                    String websiteUrl = jo.containsKey("websiteUrl") ? jo.get("websiteUrl").toString() : "";
                    String citation = (jo.containsKey("citation") && jo.containsValue("citation")) ? jo.get("citation").toString() : null;
                    String logoUrl = null;

                    if (jo.containsKey("logoRef")) {
                        JSONObject logoObject = (JSONObject) jo.get("logoRef");
                        if (logoObject.containsKey("uri")) {
                            logoUrl = logoObject.get("uri").toString();
                        }
                    }

                    output[0] = websiteUrl;
                    output[1] = citation;
                    output[2] = logoUrl;

                    return output;
                }
            }
        } catch (Exception e) {
            LOGGER.error("error fetching collectory details for distributions area", e);
        }
        return new String[0];
    }

    public static String getMetadataHtmlForAreaChecklist(String spcode, String layerName) {
        if (spcode == null) {
            return null;
        }

        try {
            int count = CommonData.getSpeciesChecklistCountByWMS(CommonData.getSpeciesChecklistWMSFromSpcode(spcode)[1]);

            String url = CommonData.getLayersServer() + "/checklist/" + spcode;
            String jsontxt = Util.readUrl(url);
            if (jsontxt == null || jsontxt.length() == 0) {
                return null;
            }

            JSONParser jp = new JSONParser();
            JSONObject jo = (JSONObject) jp.parse(jsontxt);

            String html = "Checklist area\n";
            html += "<table class='md_table'>";

            String lastClass = "";

            if (layerName != null && jo.containsKey(StringConstants.GEOM_IDX)) {
                html += "<tr class='" + lastClass + "'><td class='md_th'>Number of scientific names: </td><td class='md_spacer'/><td class='md_value'><a href='#' onClick='openAreaChecklist(\""
                        + jo.get(StringConstants.GEOM_IDX) + "\")'>" + count + "</a></td></tr>";
            } else {
                html += "<tr class='" + lastClass + "'><td class='md_th'>Number of scientific names: </td><td class='md_spacer'/><td class='md_value'>" + count + "</td></tr>";
            }

            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";

            if (jo != null && jo.containsKey(StringConstants.METADATA_U)) {
                html += "<tr class='" + lastClass + "'><td class='md_th'>Metadata link: </td><td class='md_spacer'/><td class='md_value'><a target='_blank' href='" + jo.get(StringConstants.METADATA_U) + "'>"
                        + jo.get(StringConstants.METADATA_U) + "</a></td></tr>";
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            }
            if (jo != null && jo.containsKey(StringConstants.AREA_NAME)) {
                html += "<tr class='" + lastClass + "'><td class='md_th'>Area name: </td><td class='md_spacer'/><td class='md_value'>" + jo.get(StringConstants.AREA_NAME) + "</td></tr>";
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            }
            if (jo != null && jo.containsKey(StringConstants.AREA_KM)) {
                html += "<tr class='" + lastClass + "'><td class='md_th'>Area sq km: </td><td class='md_spacer'/><td class='md_value'>" + jo.get(StringConstants.AREA_KM) + "</td></tr>";
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            }

            try {
                if (jo != null && jo.containsKey(StringConstants.PID) && jo.containsKey(StringConstants.AREA_NAME)) {
                    String fid;
                    fid = Util.getStringValue(null, StringConstants.FID, Util.readUrl(CommonData.getLayersServer() + "/object/" + jo.get(StringConstants.PID)));

                    String spid = Util.getStringValue("\"id\":\"" + fid + "\"", "spid", Util.readUrl(CommonData.getLayersServer() + "/fields"));
                    if (spid != null) {
                        String layerInfoUrl = CommonData.getLayersServer() + "/layers/view/more/" + spid;
                        html += "<tr class='" + lastClass + "'><td class='md_th'>More about this area: </td><td class='md_spacer'/><td class='md_value'><a target='_blank' href='" + layerInfoUrl
                                + "'>" + layerInfoUrl + "</a></td></tr>";

                    }
                }
            } catch (Exception e) {
                LOGGER.error("error building metadata HTML", e);
            }

            html += "</table>";

            return html;
        } catch (Exception e) {
            LOGGER.error("error building html metadata for distributions area spcode=" + spcode, e);
        }

        return null;
    }

    public static String fixWkt(String wkt) {

        if (wkt == null || !(wkt.startsWith("POLYGON") || wkt.startsWith("MULTIPOLYGON"))) {
            return wkt;
        }

        String newWkt = wkt;
        try {
            WKTReader wktReader = new WKTReader();
            com.vividsolutions.jts.geom.Geometry g = wktReader.read(wkt);
            //NC 20130319: Ensure that the WKT is valid according to the WKT standards.

            //if outside -180 to 180, cut and fit
            Envelope env = g.getEnvelopeInternal();
            if (env.getMinX() < -180 || env.getMaxX() > 180) {
                int minx = -180;
                while (minx > env.getMinX()) {
                    minx -= 360;
                }
                int maxx = 180;
                while (maxx < env.getMaxX()) {
                    maxx += 360;
                }

                //divide, translate and rejoin
                Geometry newGeometry = null;
                for (int i = minx; i < maxx; i += 360) {
                    Geometry cutter = wktReader.read("POLYGON((" + i + " -90," + i + " 90," + (i + 360) + " 90," + (i + 360) + " -90," + i + " -90))");

                    Geometry part = cutter.intersection(g);

                    //offset cutter
                    if (i != -180) {
                        AffineTransformation at = AffineTransformation.translationInstance(-180 - i, 0);

                        part.apply(at);
                    }

                    if (part.getArea() > 0) {
                        if (newGeometry == null) {
                            newGeometry = part;
                        } else {
                            newGeometry = newGeometry.union(part);
                        }
                    }
                }

                newWkt = newGeometry.toText();
            }

            IsValidOp op = new IsValidOp(g);
            if (!op.isValid()) {
                //this will fix some issues
                g = g.buffer(0);
                op = new IsValidOp(g);
            }
            if (!op.isValid()) {
                //give up?
            } else if (g.isRectangle()) {
                //NC 20130319: When the shape is a rectangle ensure that the points a specified in the correct order.
                //get the new WKT for the rectangle will possibly need to change the order.

                com.vividsolutions.jts.geom.Envelope envelope = g.getEnvelopeInternal();
                newWkt = StringConstants.POLYGON + "(("
                        + envelope.getMinX() + " " + envelope.getMinY() + ","
                        + envelope.getMaxX() + " " + envelope.getMinY() + ","
                        + envelope.getMaxX() + " " + envelope.getMaxY() + ","
                        + envelope.getMinX() + " " + envelope.getMaxY() + ","
                        + envelope.getMinX() + " " + envelope.getMinY() + "))";
            }
        } catch (ParseException parseException) {
            LOGGER.error("error fixing WKT", parseException);
        }

        return newWkt;
    }

    public static List<Double> getBoundingBox(String wkt) {
        try {
            WKTReader wktReader = new WKTReader();
            com.vividsolutions.jts.geom.Geometry g = wktReader.read(wkt);

            List<Double> bbox = new ArrayList<Double>();
            bbox.add(g.getEnvelopeInternal().getMinX());
            bbox.add(g.getEnvelopeInternal().getMinY());
            bbox.add(g.getEnvelopeInternal().getMaxX());
            bbox.add(g.getEnvelopeInternal().getMaxY());

            return bbox;
        } catch (Exception e) {
            return Util.getBoundingBox(CommonData.WORLD_WKT);
        }
    }
}