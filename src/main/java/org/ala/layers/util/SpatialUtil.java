/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.layers.util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Adam
 */
public class SpatialUtil {

    static HashMap<Double, double[]> commonGridLatitudeArea = new HashMap<Double, double[]>();

    //calculateArea from FilteringResultsWCController
    static public double calculateArea(double[][] areaarr) {
        try {
            double totalarea = 0.0;
            double[] d = areaarr[0];
            for (int f = 1; f < areaarr.length - 2; ++f) {
                totalarea += Mh(d, areaarr[f], areaarr[f + 1]);
            }

            totalarea = Math.abs(totalarea * 6378137 * 6378137);

            //return as sq km
            return totalarea / 1000.0 / 1000.0;

        } catch (Exception e) {
            System.out.println("Error in calculateArea");
            e.printStackTrace(System.out);
        }

        return 0;
    }

    static private double Mh(double[] a, double[] b, double[] c) {
        return Nh(a, b, c) * hi(a, b, c);
    }

    static private double Nh(double[] a, double[] b, double[] c) {
        double[][] poly = {a, b, c, a};
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

    static private double hi(double[] a, double[] b, double[] c) {
        double[][] d = {a, b, c};

        int i = 0;
        double[][] bb = new double[3][3];
        for (i = 0; i < 3; ++i) {
            double lng = d[i][0];
            double lat = d[i][1];

            double y = Uc(lat);
            double x = Uc(lng);

            bb[i][0] = Math.cos(y) * Math.cos(x);
            bb[i][1] = Math.cos(y) * Math.sin(x);
            bb[i][2] = Math.sin(y);
        }

        return (bb[0][0] * bb[1][1] * bb[2][2] + bb[1][0] * bb[2][1] * bb[0][2] + bb[2][0] * bb[0][1] * bb[1][2] - bb[0][0] * bb[2][1] * bb[1][2] - bb[1][0] * bb[0][1] * bb[2][2] - bb[2][0] * bb[1][1] * bb[0][2] > 0) ? 1 : -1;
    }

    static private double vd(double[] a, double[] b) {
        double lng1 = a[0];
        double lat1 = a[1];

        double lng2 = b[0];
        double lat2 = b[1];

        double c = Uc(lat1);
        double d = Uc(lat2);

        return 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((c - d) / 2), 2) + Math.cos(c) * Math.cos(d) * Math.pow(Math.sin((Uc(lng1) - Uc(lng2)) / 2), 2)));
    }

    static private double Uc(double a) {
        return a * (Math.PI / 180);
    }

    static public double calculateArea(String wkt) {


        double sumarea = 0;

        //GEOMETRYCOLLECTION
        String areaWorking = wkt;
        ArrayList<String> stringsList = new ArrayList<String>();
        if (areaWorking.startsWith("GEOMETRYCOLLECTION")) {
            //split out polygons and multipolygons
            areaWorking = areaWorking.replace(", ", ",");
            areaWorking = areaWorking.replace(") ", ")");
            areaWorking = areaWorking.replace(" )", ")");
            areaWorking = areaWorking.replace(" (", "(");
            areaWorking = areaWorking.replace("( ", "(");

            int posStart, posEnd, p1, p2;
            p1 = areaWorking.indexOf("POLYGON", 0);
            p2 = areaWorking.indexOf("MULTIPOLYGON", 0);
            if (p1 < 0) {
                posStart = p2;
            } else if (p2 < 0) {
                posStart = p1;
            } else {
                posStart = Math.min(p1, p2);
            }
            String endString = null;
            if (posStart == p1) {
                endString = "))";
            } else {
                endString = ")))";
            }
            posEnd = areaWorking.indexOf(endString, posStart);
            while (posStart > 0 && posEnd > 0) {
                //split multipolygons
                if (endString.length() == 3) {
                    for (String s : areaWorking.substring(posStart, posEnd - 1).split("\\)\\),\\(\\(")) {
                        stringsList.add(s);
                    }
                } else {
                    stringsList.add(areaWorking.substring(posStart, posEnd - 1));
                }

                posStart = posEnd;
                p1 = areaWorking.indexOf("POLYGON", posStart);
                p2 = areaWorking.indexOf("MULTIPOLYGON", posStart);
                if (p1 < 0) {
                    posStart = p2;
                } else if (p2 < 0) {
                    posStart = p1;
                } else {
                    posStart = Math.min(p1, p2);
                }
                if (posStart == p1) {
                    endString = "))";
                } else {
                    endString = ")))";
                }
                posEnd = areaWorking.indexOf(endString, posStart);
            }
            if (posStart >= 0) {
                stringsList.add(areaWorking.substring(posStart, areaWorking.length()));
            }
        } else if (areaWorking.startsWith("MULTIPOLYGON")) {
            //split
            for (String s : areaWorking.split("\\)\\),\\(\\(")) {
                stringsList.add(s);
            }
        } else if (areaWorking.startsWith("POLYGON")) {
            stringsList.add(areaWorking);
        }

        for (String w : stringsList) {
            if (w.contains("ENVELOPE")) {
                continue;
            }

            String[] areas = w.split("\\),\\(");
            double shapearea = 0;

            for (String area : areas) {
                area = area.replace("MULTIPOLYGON", "");
                area = area.replace("POLYGON", "");
                area = area.replace(")", "");
                area = area.replace("(", "");

                String[] areaarr = area.split(",");
                // Trim any leading or trailing whitespace off the coordinate pairs.
                for (int i = 0; i < areaarr.length - 1; i++) {
                    areaarr[i] = areaarr[i].trim();
                }

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

    static private int map_zoom = 21;
    static private int map_offset = 268435456; // half the Earth's circumference at zoom level 21
    static private double map_radius = map_offset / Math.PI;
    static double meters_per_pixel = 78271.5170; //at zoom level 1

    static public int convertLngToPixel(double lng) {
        return (int) Math.round(map_offset + map_radius * lng * Math.PI / 180);
    }

    static public double convertPixelToLng(int px) {
        return (px - map_offset) / map_radius * 180 / Math.PI;
    }

    static public int convertLatToPixel(double lat) {
        return (int) Math.round(map_offset - map_radius
                * Math.log((1 + Math.sin(lat * Math.PI / 180))
                / (1 - Math.sin(lat * Math.PI / 180))) / 2);
    }

    static public double convertPixelToLat(int px) {
        return Math.asin((Math.pow(Math.E, ((map_offset - px) / map_radius * 2)) - 1) / (1 + Math.pow(Math.E, ((map_offset - px) / map_radius * 2)))) * 180 / Math.PI;
    }

    static public double convertMetersToPixels(double meters, double latitude, int zoom) {
        return meters / ((Math.cos(latitude * Math.PI / 180.0) * 2 * Math.PI * 6378137) / (256 * Math.pow(2, zoom)));
    }

    static public double convertPixelsToMeters(int pixels, double latitude, int zoom) {
        return ((Math.cos(latitude * Math.PI / 180.0) * 2 * Math.PI * 6378137) / (256 * Math.pow(2, zoom))) * pixels;
    }

    static public double convertMetersToLng(double meters) {
        return meters / 20037508.342789244 * 180;
    }

    static public double convertMetersToLat(double meters) {
        return 180.0 / Math.PI * (2 * Math.atan(Math.exp(meters / 20037508.342789244 * Math.PI)) - Math.PI / 2.0);
    }

    static public int planeDistance(double lat1, double lng1, double lat2, double lng2, int zoom) {
        // Given a pair of lat/long coordinates and a map zoom level, returns
        // the distance between the two points in pixels

        int x1 = convertLngToPixel(lng1);
        int y1 = convertLatToPixel(lat1);

        int x2 = convertLngToPixel(lng2);
        int y2 = convertLatToPixel(lat2);

        int distance = (int) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));

        return distance >> (map_zoom - zoom);
    }

    public static double cellArea(double resolution, double latitude) {
        double[] areas = commonGridLatitudeArea.get(resolution);

        if (areas == null) {
            areas = buildCommonGridLatitudeArea(resolution);
            commonGridLatitudeArea.put(resolution, areas);
        }

        return areas[(int) (Math.floor(Math.abs(latitude / resolution)) * resolution)];
    }

    static double[] buildCommonGridLatitudeArea(double resolution) {
        int parts = (int) Math.ceil(90 / resolution);
        double[] areas = new double[parts];

        for (int i = 0; i < parts; i++) {
            double minx = 0;
            double maxx = resolution;
            double miny = resolution * i;
            double maxy = miny + resolution;

            areas[i] = SpatialUtil.calculateArea(new double[][]{{minx, miny}, {minx, maxy}, {maxx, maxy}, {maxx, miny}, {minx, miny}});
        }

        return areas;
    }
}
