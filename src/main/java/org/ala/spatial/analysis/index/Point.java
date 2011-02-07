/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

/**
 *
 * @author Adam
 */
/**
 * points object for housing longitude, latitude and a sorted records index
 *
 * not required for use elsewhere
 *
 * @author adam
 *
 */
public class Point {

    /**
     * longitude as double
     */
    public double longitude;
    /**
     * latitude as double
     */
    public double latitude;
    /**
     * sorted records index of this object
     */
    public int idx;

    /**
     * constructor for Point
     * @param longitude_ longitude as double
     * @param latitude_ latitude as double
     * @param idx_ sorted records index of this object
     */
    public Point(double longitude_, double latitude_, int idx_) {
        longitude = longitude_;
        latitude = latitude_;
        idx = idx_;
    }
}
