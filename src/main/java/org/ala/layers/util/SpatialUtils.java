/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.layers.util;

import org.apache.log4j.Logger;

/**
 * @author ajay
 */
public class SpatialUtils {

    private static Logger logger = Logger.getLogger(SpatialUtils.class);

    static int map_zoom = 21;
    static int map_offset = 268435456; // half the Earth's circumference at zoom level 21
    static double map_radius = map_offset / Math.PI;
    static double meters_per_pixel = 78271.5170; //at zoom level 1
    static int current_zoom = 0;

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

    static String convertGeoToPoints(String geometry) {
        if (geometry == null) {
            return "";
        }
        geometry = geometry.replace(" ", ":");
        geometry = geometry.replace("MULTIPOLYGON(((", "");
        geometry = geometry.replace("POLYGON((", "");
        while (geometry.contains(")")) {
            geometry = geometry.replace(")", "");
        }

        //for case of more than one polygon
        while (geometry.contains(",((")) {
            geometry = geometry.replace(",((", "S");
        }
        while (geometry.contains(",(")) {
            geometry = geometry.replace(",(", "S");
        }
        return geometry;
    }
}
