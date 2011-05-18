package org.ala.spatial.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Two or more simple regions that may overlap.
 *
 * Intersections and overlaps must be in all members
 *
 * @author Adam Collins
 */
public class OrRegion extends SimpleRegion implements Serializable {

    /**
     * member regions for AND operations
     */
    ArrayList<SimpleRegion> orRegions;

    /**
     * Constructor for a SimpleRegion with no shape
     */
    public OrRegion() {
        type = UNDEFINED;
    }

    /**
     * gets number of points for type POLYGON
     *
     * note: first point = last point
     *
     * @return number of points as int
     */
    @Override
    public int getNumberOfPoints() {
        int count = 0;
        for(SimpleRegion sr : orRegions) {
            count += sr.getNumberOfPoints();
        }
        return count;
    }

    /**
     * gets the bounding box for types POLYGON and BOUNDING_BOX
     *
     * @return bounding box as double[2][2]
     * with [][0] longitude and [][1] latitude
     * minimum values at [0][], maximum values at [1][0]
     */
    @Override
    public double[][] getBoundingBox() {
        if(bounding_box == null) {
            for(SimpleRegion sr : orRegions) {
                double [][] bb = sr.getBoundingBox();
                if(bounding_box == null) {
                    bounding_box = bb.clone();
                } else {
                    //limit bounding box to interior limits
                    bounding_box[0][0] = Math.max(bounding_box[0][0],bb[0][0]);
                    bounding_box[0][1] = Math.max(bounding_box[0][1],bb[0][1]);
                    bounding_box[1][0] = Math.min(bounding_box[1][0],bb[1][0]);
                    bounding_box[1][1] = Math.min(bounding_box[1][1],bb[1][1]);
                }
            }
        }
        return bounding_box;
    }
    
    /**
     * defines the SimpleRegion as type POLYGON
     *
     * @param points_ array of points as longitude and latiude
     * in double [n][2] where n is the number of points
     */
    public void setSimpleRegions(ArrayList<SimpleRegion> regions) {
        orRegions = regions;
        getBoundingBox();
    }

    /**
     * gets points of a polygon only
     *
     * @return points of this object if it is a polygon as double[][]
     * otherwise returns null.
     */
    @Override
    public float[][] getPoints() {
        //of no use to AndRegion
        return null;
    }

    /**
     * returns true when the point provided is within the SimpleRegion
     *
     * note: type UNDEFINED implies no boundary, always returns true.
     *
     * @param longitude
     * @param latitude
     * @return true iff point is within or on the edge of this SimpleRegion
     */
    @Override
    public boolean isWithin(double longitude, double latitude) {
        for(SimpleRegion sr : orRegions) {
            if(sr.isWithin(longitude, latitude)){
                return true;
            }
        }
        return false;
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
     * for minimum longitude and latitude through to xres,yres for maximums
     */
    @Override
    public int[][] getOverlapGridCells(double longitude1, double latitude1, double longitude2, double latitude2, int width, int height, byte[][] three_state_map) {
        if(getWidth() <= 0 || getHeight() <= 0){
            return null;
        }
        int [][] cells = null;
        for(SimpleRegion sr : orRegions) {
            byte [][] map = null;
            if(three_state_map != null){
                map = new byte[three_state_map.length][three_state_map[0].length];
            }
            int [][] new_cells = sr.getOverlapGridCells(longitude1, latitude1, longitude2, latitude2, width, height, map);
            if(map != null) {
                //if first time through cells == null
                if(cells == null){
                    for(int i=0;i<map.length;i++){
                        for(int j=0;j<map[i].length;j++){
                            three_state_map[i][j] = map[i][j];
                        }
                    }
                } else {
                    for(int i=0;i<map.length;i++){
                        for(int j=0;j<map[i].length;j++){
                            if(map[i][j] == GI_FULLY_PRESENT || three_state_map[i][j] == GI_FULLY_PRESENT) {
                                three_state_map[i][j] = GI_FULLY_PRESENT;
                            } else if(map[i][j] == GI_PARTIALLY_PRESENT){
                                three_state_map[i][j] = GI_PARTIALLY_PRESENT;
                            }
                        }
                    }
                }
            }

            if(cells == null) {
                cells = new_cells;
            } else {
                //TODO: cells + new_cells merge with OR, it will still work without this
            }
        }

        return cells;
    }

    /**
     * defines a region by a points string, POLYGON only
     *
     * TODO: define better format for parsing, including BOUNDING_BOX and CIRCLE
     *
     * @param pointsString points separated by ',' with longitude and latitude separated by ':'
     * @return SimpleRegion object
     */
    public static OrRegion parseSimpleRegion(String pointsString) {
        return null;
    }

    @Override
    public double getWidth() {
        return bounding_box[1][0] - bounding_box[0][0];
    }

    @Override
    public double getHeight() {
        return bounding_box[1][1] - bounding_box[0][1];
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (attributes == null) {
            attributes = new HashMap<String, Object>();
        }
        attributes.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        if (attributes != null) {
            return attributes.get(name);
        }
        return null;
    }
}
