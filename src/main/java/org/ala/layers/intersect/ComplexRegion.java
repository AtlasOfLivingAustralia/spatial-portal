/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/

package org.ala.layers.intersect;

import java.util.ArrayList;

/**
 * ComplexRegion is a collection of SimpleRegion, expect POLYGONs for now.
 * <p/>
 * treat as a shape file, overlapping regions cancel out presence.
 * <p/>
 * TODO: clockwise/anticlockwise identification
 *
 * @author Adam Collins
 */
public class ComplexRegion extends SimpleRegion {

    static public SimpleRegion parseComplexRegion(String[] polygons) {
        ComplexRegion cr = new ComplexRegion();

        for (String s : polygons) {
            cr.addPolygon(SimpleRegion.parseSimpleRegion(s));
        }

        cr.useMask(-1, -1, -1);

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
     * maintain mapping for simpleregions belonging to the same polygon
     */
    ArrayList<Integer> polygons;

    /**
     * Constructor for empty ComplexRegion
     */
    public ComplexRegion() {
        super();

        simpleregions = new ArrayList();
        boundingbox_all = new double[2][2];
        value = -1;
        mask = null;
        polygons = new ArrayList();
    }

    /**
     * sets integer value stored
     *
     * @param value_ as int
     */
    public void setValue(int value_) {
        value = value_;
    }

    /**
     * gets integer value stored
     *
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
     * <p/>
     * note: if a mask is in use must call <code>useMask</code> again
     *
     * @param points_ points = double[n][2]
     *                where
     *                n is number of points
     *                [][0] is longitude
     *                [][1] is latitude
     */
    public void addPolygon(SimpleRegion sr) {
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
     * <p/>
     * uses <code>mask</code> when available
     * <p/>
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

        short[] countsIn = new short[polygons.get(polygons.size() - 1) + 1];
        //int count_in = 0;       //count of regions overlapping the point
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
                        countsIn[polygons.get(d[i])]++;
                    }
                }
                /* true iif within an odd number of regions for any polygon*/
                for (int i = 0; i < countsIn.length; i++) {
                    if (countsIn[i] % 2 == 1) {
                        return true;
                    }
                }
            }

        }

        /* check for all SimpleRegions */
        for (int i = 0; i < simpleregions.size(); i++) {
            if (simpleregions.get(i).isWithin(longitude, latitude)) {
                countsIn[polygons.get(i)]++;
            }
        }

        /* true iif within an odd number of regions for any polygon*/
        for (int i = 0; i < countsIn.length; i++) {
            if (countsIn[i] % 2 == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * builds a grid (mask) to speed up isWithin.
     * <p/>
     * TODO: split out shapes with large numbers of points in GI_PARTIALLY_PRESENT grid cells.
     * <p/>
     * TODO: automatic(best) height/width specification
     *
     * @param width
     * @param height
     */
    public void useMask(int width, int height, int depthThreashold) {
        //calculate defaults for -1 inputs
        double[][] bb = getBoundingBox();
        int length = 0;
        for (SimpleRegion sr : simpleregions) {
            length += sr.getNumberOfPoints();
        }
        int w = (int) ((bb[1][0] - bb[0][0]) * 3);
        int h = (int) ((bb[1][1] - bb[0][1]) * 3);
        if (length > 5000) {
            w = 200;
            h = 200;
        }
        if (w > 200) {
            w = 200;
        }
        if (h > 200) {
            h = 200;
        }
        if (width == -1) {
            width = w;
        }
        if (height == -1) {
            height = h;
        }
        if (depthThreashold == -1) {
            depthThreashold = 100;
        }
        if (width < 3 || height < 3) {
            return;
        }

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

        /* temp mask for current Polygon */
        byte[][] shapemask = new byte[height][width];

        /* temp mask for current SimpleRegion */
        byte[][] shapemaskregion = new byte[height][width];

        int k = 0;
        while (k < simpleregions.size()) {
            int p = k;
            for (; k < simpleregions.size()
                    && (p == k || polygons.get(k - 1) == polygons.get(k)); k++) {

                SimpleRegion sr = simpleregions.get(k);
                sr.getOverlapGridCells(boundingbox_all[0][0], boundingbox_all[0][1], boundingbox_all[1][0], boundingbox_all[1][1], width, height, shapemaskregion);

                //shapemaskregion into shapemask
                for (i = 0; i < height; i++) {
                    for (j = 0; j < width; j++) {
                        if (shapemaskregion[i][j] == SimpleRegion.GI_PARTIALLY_PRESENT
                                || shapemask[i][j] == SimpleRegion.GI_PARTIALLY_PRESENT) {
                            shapemask[i][j] = SimpleRegion.GI_PARTIALLY_PRESENT;                //partially inside
                            if (md != null) {
                                if (md[i][j] == null) {
                                    md[i][j] = new ArrayList<Integer>();
                                }
                                md[i][j].add(k);
                            }
                        } else if (shapemaskregion[i][j] == SimpleRegion.GI_FULLY_PRESENT) {
                            if (shapemask[i][j] == SimpleRegion.GI_FULLY_PRESENT) {
                                shapemask[i][j] = SimpleRegion.GI_ABSENCE;      //completely inside
                            } else {
                                shapemask[i][j] = SimpleRegion.GI_FULLY_PRESENT;//completely outside (inside of a cutout region)
                            }
                            if (md != null) {
                                if (md[i][j] == null) {
                                    md[i][j] = new ArrayList<Integer>();
                                }
                                md[i][j].add(k);
                            }
                        }

                        /* reset shapemaskregion for next part */
                        shapemaskregion[i][j] = 0;
                    }
                }
            }

            //shapemask into mask
            for (i = 0; i < height; i++) {
                for (j = 0; j < width; j++) {
                    if (shapemask[i][j] == SimpleRegion.GI_FULLY_PRESENT
                            || mask[i][j] == SimpleRegion.GI_FULLY_PRESENT) {
                        mask[i][j] = SimpleRegion.GI_FULLY_PRESENT;                //partially inside
                    } else if (shapemask[i][j] == SimpleRegion.GI_PARTIALLY_PRESENT) {
                        mask[i][j] = SimpleRegion.GI_PARTIALLY_PRESENT;
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
                    if (md[i][j] != null && mask[i][j] == SimpleRegion.GI_PARTIALLY_PRESENT) {
                        int[] d = new int[md[i][j].size()];
                        for (k = 0; k < d.length; k++) {
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
     * <p/>
     * for type POLYGON
     * when <code>three_state_map</code> is not null populate it with one of:
     * GI_UNDEFINED
     * GI_PARTIALLY_PRESENT
     * GI_FULLY_PRESENT
     * GI_ABSENCE
     *
     * @param longitude1
     * @param latitude1
     * @param longitude2
     * @param latitude2
     * @param xres       number of longitude segements as int
     * @param yres       number of latitude segments as int
     * @return (x, y) as double [][2] for each grid cell at least partially falling
     * within the specified region of the specified resolution beginning at 0,0
     * for minimum longitude and latitude through to xres,yres for maximums     *
     */
    @Override
    public int[][] getOverlapGridCells(double longitude1, double latitude1, double longitude2, double latitude2, int width, int height, byte[][] three_state_map, boolean noCellsReturned) {
        int i, j;
        // if no threestate map exists, create one
        if (three_state_map == null) {
            three_state_map = new byte[height][width];
        }

        byte[][] tmpMask = new byte[height][width];

        int k = 0;
        while (k < simpleregions.size()) {
            int p = k;
            for (; k < simpleregions.size()
                    && (p == k || polygons.get(k - 1).equals(polygons.get(k))); k++) {

                SimpleRegion sr = simpleregions.get(k);
                sr.getOverlapGridCells_Acc(longitude1, latitude1, longitude2, latitude2, width, height, tmpMask);
            }

            fillAccMask(p, k - 1, longitude1, latitude1, longitude2, latitude2, width, height, tmpMask, true);

            //tmpMask into mask
            for (i = 0; i < height; i++) {
                for (j = 0; j < width; j++) {
                    if (tmpMask[i][j] == 2 || three_state_map[i][j] == 2) {
                        three_state_map[i][j] = 2;
                    } else if (tmpMask[i][j] == 1) {
                        three_state_map[i][j] = 1;
                    }

                    /* reset shapemask for next part */
                    tmpMask[i][j] = 0;
                }
            }
        }

        boolean cellsReturned = !noCellsReturned;
        if (cellsReturned) {
            int[][] data = new int[width * height][2];
            int p = 0;
            for (i = 0; i < height; i++) {
                for (j = 0; j < width; j++) {
                    if (three_state_map[i][j] != GI_UNDEFINED) {   //undefined == absence
                        data[p][0] = j;
                        data[p][1] = i;
                        p++;
                    }
                }
            }
            data = java.util.Arrays.copyOf(data, p);
        }


        return null;
    }

    @Override
    public boolean isWithin_EPSG900913(double longitude, double latitude) {
        short[] countsIn = new short[polygons.get(polygons.size() - 1) + 1];
         /* check for all SimpleRegions */
        for (int i = 0; i < simpleregions.size(); i++) {
            if (simpleregions.get(i).isWithin_EPSG900913(longitude, latitude)) {
                countsIn[polygons.get(i)]++;
            }
        }

        /* true iif within an odd number of regions for any polygon*/
        for (int i = 0; i < countsIn.length; i++) {
            if (countsIn[i] % 2 == 1) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int[][] getOverlapGridCells_EPSG900913(double longitude1, double latitude1, double longitude2, double latitude2, int width, int height, byte[][] three_state_map, boolean noCellsReturned) {
        int i, j;

        int[][] output = null;
        byte[][] mask = three_state_map;

        // if no threestate map exists, create one
        if (mask == null) {
            mask = new byte[height][width];
            three_state_map = mask;
        }

        byte[][] tmpMask = new byte[height][width];

        int k = 0;
        while (k < simpleregions.size()) {
            int p = k;
            for (; k < simpleregions.size()
                    && (p == k || polygons.get(k - 1).equals(polygons.get(k))); k++) {

                SimpleRegion sr = simpleregions.get(k);
                sr.getOverlapGridCells_Acc_EPSG900913(longitude1, latitude1, longitude2, latitude2, width, height, tmpMask);
            }

            fillAccMask_EPSG900913(k, p - 1, longitude1, latitude1, longitude2, latitude2, width, height, tmpMask, true);

            //tmpMask into mask
            for (i = 0; i < height; i++) {
                for (j = 0; j < width; j++) {
                    if (tmpMask[i][j] == 2 || mask[i][j] == 2) {
                        mask[i][j] = 2;
                    } else if (tmpMask[i][j] == 1) {
                        mask[i][j] = 1;
                    }

                    /* reset shapemask for next part */
                    tmpMask[i][j] = 0;
                }
            }
        }

        boolean cellsReturned = !noCellsReturned;
        if (cellsReturned) {
            int[][] data = new int[width * height][2];
            int p = 0;
            for (i = 0; i < height; i++) {
                for (j = 0; j < width; j++) {
                    if (mask[i][j] != GI_UNDEFINED) {   //undefined == absence
                        data[p][0] = j;
                        data[p][1] = i;
                        p++;
                    }
                }
            }
            data = java.util.Arrays.copyOf(data, p);
            return data;
        }

        return null;
    }

    void addSet(ArrayList<SimpleRegion> simpleRegions) {
        int nextSetNumber = (polygons.size() > 0) ? polygons.get(polygons.size() - 1) + 1 : 0;

        for (int i = 0; i < simpleRegions.size(); i++) {
            addPolygon(simpleRegions.get(i));
            polygons.add(nextSetNumber);
        }
    }

    public int[][] fillAccMask(int startPolygon, int endPolygon, double longitude1, double latitude1, double longitude2, double latitude2, int width, int height, byte[][] three_state_map, boolean noCellsReturned) {
        double divx = (longitude2 - longitude1) / width;
        double divy = (latitude2 - latitude1) / height;

        int i, j;
        //do raster check
        int[][] data = null;
        boolean cellsReturned = !noCellsReturned;
        if (cellsReturned) {
            data = new int[width * height][2];
        }
        int p = 0;
        for (j = 0; j < three_state_map[0].length; j++) {
            for (i = 0; i < three_state_map.length; i++) {
                if (three_state_map[i][j] == GI_PARTIALLY_PRESENT) {
                    //if it is partially present, do nothing
                } else if ((j == 0 || three_state_map[i][j - 1] == GI_PARTIALLY_PRESENT)) {
                    if (i > 0
                            && (three_state_map[i - 1][j] == GI_FULLY_PRESENT
                            || three_state_map[i - 1][j] == GI_ABSENCE)) {
                        //use same as LHS
                        three_state_map[i][j] = three_state_map[i - 1][j];
                    } else {
                        int count = 0;
                        for (int k = startPolygon; k <= endPolygon; k++) {
                            count += isWithin(j * divx + divx / 2 + longitude1, i * divy + divy / 2 + latitude1) ? 1 : 0;
                        }
                        if (count % 2 == 1) {
                            //if the previous was partially present, test
                            three_state_map[i][j] = GI_FULLY_PRESENT;
                        } //else absent
                    } //else absent
                } else {
                    //if the previous was fully present, repeat
                    //if the previous was absent, repeat
                    three_state_map[i][j] = three_state_map[i][j - 1];
                }

                //apply to cells;
                if (cellsReturned && three_state_map[i][j] != GI_UNDEFINED) {   //undefined == absence
                    data[p][0] = j;
                    data[p][1] = i;
                    p++;
                }
            }
        }
        if (data != null) {
            data = java.util.Arrays.copyOf(data, p);
        }
        return data;
    }

    public int[][] fillAccMask_EPSG900913(int startPolygon, int endPolygon, double longitude1, double latitude1, double longitude2, double latitude2, int width, int height, byte[][] three_state_map, boolean noCellsReturned) {
        double divx = (longitude2 - longitude1) / width;
        double divy = (latitude2 - latitude1) / height;

        int i, j;
        //do raster check
        int[][] data = null;
        boolean cellsReturned = !noCellsReturned;
        if (cellsReturned) {
            data = new int[width * height][2];
        }
        int p = 0;
        for (j = 0; j < three_state_map[0].length; j++) {
            for (i = 0; i < three_state_map.length; i++) {
                if (three_state_map[i][j] == GI_PARTIALLY_PRESENT) {
                    //if it is partially present, do nothing
                } else if ((j == 0 || three_state_map[i][j - 1] == GI_PARTIALLY_PRESENT)) {
                    if (i > 0
                            && (three_state_map[i - 1][j] == GI_FULLY_PRESENT
                            || three_state_map[i - 1][j] == GI_ABSENCE)) {
                        //use same as LHS
                        three_state_map[i][j] = three_state_map[i - 1][j];
                    } else {
                        int count = 0;
                        for (int k = startPolygon; k <= endPolygon; k++) {
                            count += isWithin_EPSG900913(j * divx + divx / 2 + longitude1, i * divy + divy / 2 + latitude1) ? 1 : 0;
                        }
                        if (count % 2 == 1) {
                            //if the previous was partially present, test
                            three_state_map[i][j] = GI_FULLY_PRESENT;
                        } //else absent
                    } //else absent
                } else {
                    //if the previous was fully present, repeat
                    //if the previous was absent, repeat
                    three_state_map[i][j] = three_state_map[i][j - 1];
                }

                //apply to cells;
                if (cellsReturned && three_state_map[i][j] != GI_UNDEFINED) {   //undefined == absence
                    data[p][0] = j;
                    data[p][1] = i;
                    p++;
                }
            }
        }
        if (data != null) {
            data = java.util.Arrays.copyOf(data, p);
        }
        return data;
    }

}
