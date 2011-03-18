package org.ala.spatial.util;

import java.util.ArrayList;

/**
 * ComplexRegion is a collection of SimpleRegion, expect POLYGONs for now.
 * 
 * treat as a shape file, overlapping regions cancel out presence.
 * 
 * TODO: clockwise/anticlockwise identification
 * 
 * @author Adam Collins
 */
public class ComplexRegion extends SimpleRegion {

    static SimpleRegion parseComplexRegion(String[] polygons) {
        ComplexRegion cr = new ComplexRegion();

        for (String s : polygons) {
            cr.addPolygon(SimpleRegion.parseSimpleRegion(s).getPoints());
        }

        /* speed up for polygons with lots of points */
        double[][] bb = cr.getBoundingBox();
        int width = (int) ((bb[1][0] - bb[0][0]) * 3);
        int height = (int) ((bb[1][1] - bb[0][1]) * 3);
        if (width > 200) {
            width = 200;
        }
        if (height > 200) {
            height = 200;
        }
        if (width > 3 && height > 3) {
            cr.useMask(width, height, 100);
        }

        return cr;
    }
    /**
     * list of SimpleRegion members
     */
    ArrayList<SimpleRegion> simpleregions;
    /**
     * bounding box for all, see SimpleRegion boundingbox.
     */
    double[][] boundingbox_all;
    /**
     * value assigned
     */
    int value;
    /**
     * array for speeding up isWithin
     */
    byte[][] mask;
    Object[][] maskDepth;
    /**
     * mask height
     */
    int mask_height;
    /**
     * mask width
     */
    int mask_width;
    /**
     * mask multiplier for longitude inputs
     */
    double mask_long_multiplier;
    /**
     * mask mulitplier for latitude inputs
     */
    double mask_lat_multiplier;

    /**
     * Constructor for empty ComplexRegion
     */
    public ComplexRegion() {
        super();

        simpleregions = new ArrayList();
        boundingbox_all = new double[2][2];
        value = -1;
        mask = null;
    }

    /**
     * sets integer value stored
     * @param value_ as int
     */
    public void setValue(int value_) {
        value = value_;
    }

    /**
     * gets integer value stored
     * @return int
     */
    public int getValue() {
        return value;
    }

    /**
     * gets the bounding box for shapes in this ComplexRegion
     *
     * @return bounding box for ComplexRegion as double [][]
     */
    @Override
    public double[][] getBoundingBox() {
        return boundingbox_all;
    }

    /**
     * adds a new polygon
     *
     * note: if a mask is in use must call <code>useMask</code> again
     * @param points_ points = double[n][2]
     * where
     * 	n is number of points
     *  [][0] is longitude
     *  [][1] is latitude
     */
    public void addPolygon(double[][] points_) {
        //fix extents
        for (int i = 0; i < points_.length; i++) {
            //adjust to -360 and 360
            while (points_[i][0] < -360) {
                points_[i][0] += 360;
            }
            while (points_[i][0] > 360) {
                points_[i][0] -= 360;
            }
            while (points_[i][1] < -360) {
                points_[i][1] += 360;
            }
            while (points_[i][1] > 360) {
                points_[i][1] -= 360;
            }
        }

        SimpleRegion sr = new SimpleRegion();
        sr.setPolygon(points_);

        simpleregions.add(sr);

        /* update boundingbox_all */
        double[][] bb = sr.getBoundingBox();
        if (simpleregions.size() == 1 || boundingbox_all[0][0] > bb[0][0]) {
            boundingbox_all[0][0] = bb[0][0];
        }
        if (simpleregions.size() == 1 || boundingbox_all[1][0] < bb[1][0]) {
            boundingbox_all[1][0] = bb[1][0];
        }
        if (simpleregions.size() == 1 || boundingbox_all[0][1] > bb[0][1]) {
            boundingbox_all[0][1] = bb[0][1];
        }
        if (simpleregions.size() == 1 || boundingbox_all[1][1] < bb[1][1]) {
            boundingbox_all[1][1] = bb[1][1];
        }

        bounding_box = boundingbox_all;
    }

    /**
     * adds a new polygon
     *
     * note: if a mask is in use must call <code>useMask</code> again
     * @param points_ points = double[n][2]
     * where
     * 	n is number of points
     *  [][0] is longitude
     *  [][1] is latitude
     */
    public void addPolygon(float[][] points_) {
        //fix extents
        for (int i = 0; i < points_.length; i++) {
            //adjust to -360 and 360
            while (points_[i][0] < -360) {
                points_[i][0] += 360;
            }
            while (points_[i][0] > 360) {
                points_[i][0] -= 360;
            }
            while (points_[i][1] < -360) {
                points_[i][1] += 360;
            }
            while (points_[i][1] > 360) {
                points_[i][1] -= 360;
            }
        }

        SimpleRegion sr = new SimpleRegion();
        sr.setPolygon(points_);

        simpleregions.add(sr);

        /* update boundingbox_all */
        double[][] bb = sr.getBoundingBox();
        if (simpleregions.size() == 1 || boundingbox_all[0][0] > bb[0][0]) {
            boundingbox_all[0][0] = bb[0][0];
        }
        if (simpleregions.size() == 1 || boundingbox_all[1][0] < bb[1][0]) {
            boundingbox_all[1][0] = bb[1][0];
        }
        if (simpleregions.size() == 1 || boundingbox_all[0][1] > bb[0][1]) {
            boundingbox_all[0][1] = bb[0][1];
        }
        if (simpleregions.size() == 1 || boundingbox_all[1][1] < bb[1][1]) {
            boundingbox_all[1][1] = bb[1][1];
        }

        bounding_box = boundingbox_all;
    }

    /**
     * returns true when the point provided is within the ComplexRegion
     *
     * uses <code>mask</code> when available
     *
     * note: type UNDEFINED implies no boundary, always returns true.
     *
     * @param longitude
     * @param latitude
     * @return true iff point is within or on the edge of this ComplexRegion
     */
    @Override
    public boolean isWithin(double longitude, double latitude) {
        if (simpleregions.size() == 1) {
            return simpleregions.get(0).isWithin(longitude, latitude);
        }
        if (boundingbox_all[0][0] > longitude || boundingbox_all[1][0] < longitude
                || boundingbox_all[0][1] > latitude || boundingbox_all[1][1] < latitude) {
            return false;
        }

        int count_in = 0;       //count of regions overlapping the point
        if (mask != null) {
            /* use mask if exists */
            int long1 = (int) Math.floor((longitude - boundingbox_all[0][0]) * mask_long_multiplier);
            int lat1 = (int) Math.floor((latitude - boundingbox_all[0][1]) * mask_lat_multiplier);

            if (long1 == mask[0].length) {
                long1--;
            }
            if (lat1 == mask.length) {
                lat1--;
            }

            if (mask[lat1][long1] == SimpleRegion.GI_FULLY_PRESENT) {
                return true;
            } else if (mask[lat1][long1] == SimpleRegion.GI_UNDEFINED
                    || mask[lat1][long1] == SimpleRegion.GI_ABSENCE) {
                return false;
            }
            //partial, try maskDepth and sum overlaps
            if (maskDepth != null && maskDepth[lat1][long1] != null) {
                int[] d = (int[]) maskDepth[lat1][long1];
                for (int i = 0; i < d.length; i++) {
                    if (simpleregions.get(d[i]).isWithin(longitude, latitude)) {
                        count_in++;
                    }
                }
                /* true iif within an odd number of regions */
                if (count_in % 2 == 1) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        /* check for all SimpleRegions */
        for (SimpleRegion sr : simpleregions) {
            if (sr.isWithin(longitude, latitude)) {
                count_in++;
            }
        }

        /* true iif within an odd number of regions */
        if (count_in % 2 == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * builds a grid (mask) to speed up isWithin.
     *
     * TODO: split out shapes with large numbers of points in GI_PARTIALLY_PRESENT grid cells.
     *
     * TODO: automatic(best) height/width specification
     *
     * @param width
     * @param height
     */
    public void useMask(int width, int height, int depthThreashold) {
        int i, j;

        /* class variables assignment */
        mask_width = width;
        mask_height = height;
        mask_long_multiplier =
                mask_width / (double) (boundingbox_all[1][0] - boundingbox_all[0][0]);
        mask_lat_multiplier =
                mask_height / (double) (boundingbox_all[1][1] - boundingbox_all[0][1]);

        /* end result mask */
        mask = new byte[height][width];
        ArrayList<Integer>[][] md = null;
        if (simpleregions.size() > depthThreashold) {
            //use mask depth as well
            md = new ArrayList[height][width];
        }

        /* temp mask for current SimpleRegion */
        byte[][] shapemask = new byte[height][width];

        for (int k = 0; k < simpleregions.size(); k++) {
            SimpleRegion sr = simpleregions.get(k);
            sr.getOverlapGridCells(boundingbox_all[0][0], boundingbox_all[0][1], boundingbox_all[1][0], boundingbox_all[1][1], width, height, shapemask);

            //shapemask into mask
            for (i = 0; i < height; i++) {
                for (j = 0; j < width; j++) {
                    if (shapemask[i][j] == 1 || mask[i][j] == 1) {
                        mask[i][j] = 1;				//partially inside
                        if (md != null) {
                            if (md[i][j] == null) {
                                md[i][j] = new ArrayList<Integer>();
                            }
                            md[i][j].add(k);
                        }
                    } else if (shapemask[i][j] == 2) {
                        if (mask[i][j] == 2) {
                            mask[i][j] = 3;			//completely inside
                        } else {
                            mask[i][j] = 2;			//completely outside (inside of a cutout region)
                        }
                        if (md != null) {
                            if (md[i][j] == null) {
                                md[i][j] = new ArrayList<Integer>();
                            }
                            md[i][j].add(k);
                        }
                    }

                    /* reset shapemask for next part */
                    shapemask[i][j] = 0;
                }
            }
        }

        //maskDepth to int[]
        if (md != null) {
            maskDepth = new Object[md.length][md[0].length];
            for (i = 0; i < height; i++) {
                for (j = 0; j < width; j++) {
                    if (md[i][j] != null) {
                        int[] d = new int[md[i][j].size()];
                        for (int k = 0; k < d.length; k++) {
                            d[k] = md[i][j].get(k);
                        }
                        maskDepth[i][j] = d;
                    }
                }
            }
        }
    }

    /**
     * determines overlap with a grid
     *
     * for type POLYGON
     * when <code>three_state_map</code> is not null populate it with one of:
     * 	GI_UNDEFINED
     * 	GI_PARTIALLY_PRESENT
     * 	GI_FULLY_PRESENT
     * 	GI_ABSENCE
     *
     * @param longitude1
     * @param latitude1
     * @param longitude2
     * @param latitude2
     * @param xres number of longitude segements as int
     * @param yres number of latitude segments as int
     * @return (x,y) as double [][2] for each grid cell at least partially falling
     * within the specified region of the specified resolution beginning at 0,0
     * for minimum longitude and latitude through to xres,yres for maximums     *
     */
    /*@Override
    public int[][] getOverlapGridCells(double longitude1, double latitude1, double longitude2, double latitude2, int width, int height, byte[][] three_state_map) {
    int i, j;

    //if there are a large number of points, attempt to reduce them
    int points_count = 0;
    for (i = 0; i < simpleregions.size(); i++) {
    points_count += simpleregions.get(i).getNumberOfPoints();
    }
    if (points_count > 10000) {
    ComplexRegion cr = new ComplexRegion();

    int points_remaining = 0;

    for (i = 0; i < simpleregions.size(); i++) {
    double[][] points = simpleregions.get(i).getPoints();
    double[][] tmpPoints = new double[points.length][2];

    //leave a small number of points alone
    int pos = 3;
    for (j = 0; j < pos; j++) {
    tmpPoints[j][0] = points[j][0];
    tmpPoints[j][1] = points[j][1];
    }

    double divx = (longitude2 - longitude1) / width;
    double divy = (latitude2 - latitude1) / height;

    //don't include point j if it is in same cell as j-1
    for (j = pos; j < points.length; j++) {
    if ((int) ((points[j][0] - longitude1) / divx) != (int) ((points[j - 1][0] - longitude1) / divx)
    || (int) ((points[j][1] - latitude1) / divy) != (int) ((points[j - 1][1] - latitude1) / divy)) {
    //need to add j-1 so exit from cell is the same
    if (tmpPoints[pos - 1][0] != points[j - 1][0]
    || tmpPoints[pos - 1][0] != points[j - 1][0]) {
    tmpPoints[pos][0] = points[j - 1][0];
    tmpPoints[pos][1] = points[j - 1][1];
    pos++;
    }

    tmpPoints[pos][0] = points[j][0];
    tmpPoints[pos][1] = points[j][1];
    pos++;
    }
    }

    //shorten
    double[][] newPoints = new double[pos][2];
    for (j = 0; j < pos; j++) {
    newPoints[j][0] = tmpPoints[j][0];
    newPoints[j][1] = tmpPoints[j][1];
    }

    points_remaining += newPoints.length;

    //create simple shape and add
    cr.addPolygon(newPoints);
    }

    //report difference
    SpatialLogger.info("reduced number of points: " + points_count + " to " + points_remaining);

    //return result for reduced points
    return cr.getOverlapGridCellsActual(longitude1, latitude1, longitude2, latitude2, width, height, three_state_map);
    }

    return getOverlapGridCellsActual(longitude1, latitude1, longitude2, latitude2, width, height, three_state_map);
    }*/
    @Override
    public int[][] getOverlapGridCells(double longitude1, double latitude1, double longitude2, double latitude2, int width, int height, byte[][] three_state_map, boolean noCellsReturned) {
        int i, j;

        int[][] output = null;

        byte[][] mask = three_state_map;

        // if no threestate map exists, create one
        if (mask == null) {
            mask = new byte[height][width];
            //set initial values
            for (i = 0; i < height; i++) {
                for (j = 0; j < width; j++) {
                    mask[i][j] = SimpleRegion.GI_UNDEFINED;
                }
            }
        }
        /*byte[][] shapemask = new byte[height][width];

        for (SimpleRegion sr : simpleregions) {
        int[][] cells = sr.getOverlapGridCells(longitude1, latitude1, longitude2, latitude2, width, height, shapemask);

        //merge shapemask into thee_state_map
        //for (i = 0; i < height; i++) {
        //    for (j = 0; j < width; j++) {
        if (cells != null) {
        for (int k = 0; k < cells.length; k++) {
        i = cells[k][1];
        j = cells[k][0];
        if (shapemask[i][j] == SimpleRegion.GI_PARTIALLY_PRESENT
        || mask[i][j] == SimpleRegion.GI_PARTIALLY_PRESENT) {
        //partially inside
        mask[i][j] = SimpleRegion.GI_PARTIALLY_PRESENT;
        } else if (shapemask[i][j] == SimpleRegion.GI_FULLY_PRESENT) {
        if (mask[i][j] == SimpleRegion.GI_FULLY_PRESENT) {
        //completely outside (inside of a cutout region)
        mask[i][j] = SimpleRegion.GI_ABSENCE;
        } else {
        //completely inside
        mask[i][j] = SimpleRegion.GI_FULLY_PRESENT;
        }
        }
        shapemask[i][j] = SimpleRegion.GI_ABSENCE;
        }
        }
        }

        //count cells full or partial
        int count = 0;
        for (i = 0; i < height; i++) {
        for (j = 0; j < width; j++) {
        if (mask[i][j] != SimpleRegion.GI_UNDEFINED
        && mask[i][j] != SimpleRegion.GI_ABSENCE) {
        count++;
        }
        }
        }

        //populate output for cells full or partial
        output = new int[count][2];
        count = 0;
        for (i = 0; i < height; i++) {
        for (j = 0; j < width; j++) {
        if (mask[i][j] != SimpleRegion.GI_UNDEFINED
        && mask[i][j] != SimpleRegion.GI_ABSENCE) {
        output[count][0] = j;
        output[count][1] = i;
        count++;
        }
        }
        }*/

        for (SimpleRegion sr : simpleregions) {
            sr.getOverlapGridCells_Acc(longitude1, latitude1, longitude2, latitude2, width, height, mask);
        }

        int[][] cells = fillAccMask(longitude1, latitude1, longitude2, latitude2, width, height, three_state_map, noCellsReturned);

        return cells;
    }
}
