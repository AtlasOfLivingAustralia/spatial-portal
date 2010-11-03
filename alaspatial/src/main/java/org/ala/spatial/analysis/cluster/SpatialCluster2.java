/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.cluster;

/**
 *
 * http://forum.mapaplace.com/discussion/3/server-side-marker-clustering-python-source-code/
 * 
 * @author ajay
 */
public class SpatialCluster2 {

    private double minLng = 110;
    private double minLat = -34;
    private double maxLng = 160;
    private double maxLat = -9;
    private int width = 500;
    private int height = 400;

    private int convertLngToPixel(double lng) {
        //return int(round(math.fabs(lng - self.topLeftLng) / self.lngDelta  * self.mapWidth))
        double lngDelta = Math.abs(maxLng - minLng);
        return (int) Math.round(Math.abs(lng - minLng) / lngDelta * width);
    }

    private int convertLatToPixel(double lat) {
        //return int(round(math.fabs(lat - self.topLeftLat) / self.latDelta  * self.mapHeight))
        double latDelta = Math.abs(minLat - maxLat);
        return (int) Math.round(Math.abs(lat - minLat) / latDelta * height);
    }

    private void clip() {

    }
}
