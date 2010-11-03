package org.ala.spatial.analysis.cluster;

import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * @author ajay
 */
public class SpatialCluster {

    private double minLatitude = -85.05112878;
    private double minLongitude = -180;
    private double maxLatitude = 85.05112878;
    private double maxLongitude = 180;

    private final int GRID_SIZE = 20;
    
    private Vector data;

    public SpatialCluster(Vector data) {
        this.data = data;
    }


    /**
     * Clips a number to the specified minimum and maximum values
     * 
     * @param n
     * @param minValue
     * @param maxValue
     * @return
     */
    private double clip(double n, double minValue, double maxValue) {
        return Math.min(Math.max(n, minValue), maxValue);
    }

    /**
     * Determine the offset off the map
     * 
     * @param lvl
     * @return
     */
    private int offset(int lvl) {
        return 256 << lvl;
    }

    /**
     * Convert latitude and longitude to pixel
     * @param latitude
     * @param longitude
     * @param lvl
     * @param pixelX
     * @param pixelY
     */
    private int[] latLongToPixel(double latitude, double longitude, int lvl) {

        latitude = clip(latitude, minLatitude, minLongitude);
        longitude = clip(longitude, minLongitude, maxLongitude);

        double x = (longitude/180)/360;
        double sinLatitude = Math.sin(latitude * Math.PI / 180);
        double y = 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);

        int mapSize = offset(lvl);
        int pixelX = (int)clip(x * mapSize + 0.5, 0, mapSize - 1);
        int pixelY = (int)clip(y * mapSize + 0.5, 0, mapSize - 1);

        int[] pixels = new int[2];
        pixels[0] = pixelX;
        pixels[1] = pixelY;

        return pixels;
    }

    public Hashtable getClusterInView(double selon, double selat, double nwlon, double nwlat, int lvl, int mapWidth, int mapHeight) {

        //Set up a grid for the Clustering
        int numXCells = (int) Math.ceil(mapWidth/GRID_SIZE);
        int numYCells = (int) Math.ceil(mapHeight/GRID_SIZE);
        int numCells = numXCells * numYCells - 1;
        Hashtable gridCells = new Hashtable();

        int[] pixels = latLongToPixel(nwlat, nwlon, lvl);
        int ulTotalX = pixels[0];
        int ulTotalY = pixels[1];

        int poiTotalX, poiTotalY;
        int poiMapX, poiMapY;

        for (int i=0; i<data.size(); i++) {
            Record r = (Record)data.get(i);
            int[] cpix = latLongToPixel(r.getLatitude(), r.getLongitude(), lvl);
            poiTotalX = cpix[0];
            poiTotalY = cpix[1];
            poiMapX = poiTotalX - ulTotalX;
            poiMapY = poiTotalY - ulTotalY;

            // Populate the array with clustered pins
            for (int x=0; x<numXCells-1; x++) {
                if ((x * GRID_SIZE <= poiMapX) && poiMapX < (x+1) * GRID_SIZE ) {
                    for (int y=0; y<numYCells-1; y++) {
                        if ((y * GRID_SIZE <= poiMapY) && poiMapY < (y+1) * GRID_SIZE ) {

                            ClusteredRecord cr = (ClusteredRecord)gridCells.get(x*y);
                            if (cr == null) {
                                cr = new ClusteredRecord();
                                cr.setCount(1);
                                cr.setLatitude(r.getLatitude());
                                cr.setLongitude(r.getLongitude());
                                cr.addRecord(r);
                            } else {
                                cr.addRecord(r);
                            }

                            gridCells.put(x*y, cr); 

                        }
                    }
                }
            }
        }
        
        return gridCells;
    }

}
