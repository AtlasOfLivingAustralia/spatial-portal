package org.ala.spatial.util;

import java.io.Serializable;
import java.util.HashMap;

/**
 * SimpleRegion enables point to shape intersections, where the shape
 * is stored within SimpleRegion as a circle, bounding box or polygon.
 *
 * Other utilities include shape presence on a defined grid;
 * fully present, partially present, absent.
 *
 * @author Adam Collins
 */
public class SimpleRegion extends Object implements Serializable {

    static final long serialVersionUID = -5509351896749940566L;
    /**
     * shape type not declared
     */
    public static final int UNDEFINED = 0;
    /**
     * shape type bounding box; upper, lower, left, right
     */
    public static final int BOUNDING_BOX = 1;
    /**
     * shape type circle; point and radius
     */
    public static final int CIRCLE = 2;
    /**
     * shape type polygon; list of points as longitude, latitude pairs
     * last point == first point
     */
    public static final int POLYGON = 3;
    /**
     * UNDEFINED state for grid intersection output
     *
     * can be considered ABSENCE
     */
    public static final int GI_UNDEFINED = 0;
    /**
     * PARTiALLy PRESENT state for grid intersection output
     */
    public static final int GI_PARTIALLY_PRESENT = 1;
    /**
     * FULLY PRESENT state for grid intersection output
     */
    public static final int GI_FULLY_PRESENT = 2;
    /**
     * ABSENCE state for grid intersection output
     */
    public static final int GI_ABSENCE = 0;
    /**
     * assigned shape type
     */
    int type;
    /**
     * points store
     * BOUNDING_BOX = double [2][2]
     * CIRCLE = double [1][2]
     * POLYGON, n points (start = end) = double[n][2]
     */
    double[][] points;
    /**
     * for point/grid to polygon intersection method
     *
     * polygon edges as lines of the form <code>y = a*x + b</code>
     * lines = double [n][2]
     * where
     * 	n is number of edges
     * 	value at [0] is <code>a</code>
     * 	value at [1] is <code>b</code>
     */
    double[][] lines2;
    /**
     * bounding box for types BOUNDING_BOX and POLYGON
     *
     * bounding_box = double [2][2]
     * where
     * 	[0][0] = minimum longitude
     *  [0][1] = minimum latitude
     *  [1][0] = maximum longitude
     *  [1][1] = maximum latitude
     */
    double[][] bounding_box; //for polygons
    /**
     * radius for type CIRCLE in m
     *
     */
    double radius;
    /**
     * misc attributes
     */
    HashMap<String, Object> attributes;

    /**
     * Constructor for a SimpleRegion with no shape
     */
    public SimpleRegion() {
        type = UNDEFINED;
    }

    /**
     * gets number of points for type POLYGON
     *
     * note: first point = last point
     *
     * @return number of points as int
     */
    public int getNumberOfPoints() {
        return points.length;
    }

    /**
     * gets the bounding box for types POLYGON and BOUNDING_BOX
     *
     * @return bounding box as double[2][2]
     * with [][0] longitude and [][1] latitude
     * minimum values at [0][], maximum values at [1][0]
     */
    public double[][] getBoundingBox() {
        return bounding_box;
    }

    /**
     * defines the SimpleRegion as type BOUNDING_BOX
     *
     * @param longitude1
     * @param latitude1
     * @param longitude2
     * @param latitude2
     */
    public void setBox(double longitude1, double latitude1, double longitude2, double latitude2) {
        type = BOUNDING_BOX;
        points = new double[2][2];
        points[0][0] = Math.min(longitude1, longitude2);
        points[0][1] = Math.min(latitude1, latitude2);
        points[1][0] = Math.max(longitude1, longitude2);
        points[1][1] = Math.max(latitude1, latitude2);

        for (int i = 0; i < points.length; i++) {
            //fix at -180 and 180
            if (points[i][0] < -180) {
                points[i][0] = -180;
            }
            if (points[i][0] > 180) {
                points[i][0] = 180;
            }
            while (points[i][1] < -180) {
                points[i][1] = -180;
            }
            while (points[i][1] > 180) {
                points[i][1] = 180;
            }
        }

        bounding_box = points;
    }

    /**
     * defines the SimpleRegion as type UNDEFINED
     */
    public void setNone() {
        type = UNDEFINED;
    }

    /**
     * defines the SimpleRegion as type CIRCLE
     *
     * @param longitude
     * @param latitude
     * @param radius_ radius of the circle in m
     */
    public void setCircle(double longitude, double latitude, double radius_) {
        type = CIRCLE;
        points = new double[1][2];
        points[0][0] = longitude;
        points[0][1] = latitude;
        radius = radius_;
    }

    /**
     * defines the SimpleRegion as type POLYGON
     *
     * @param points_ array of points as longitude and latiude
     * in double [n][2] where n is the number of points
     */
    public void setPolygon(double[][] points_) {
        if (points_ != null && points_.length > 1) {
            type = POLYGON;
            int i;

            for (i = 0; i < points_.length; i++) {
                //fix at -180 and 180
                if (points_[i][0] < -180) {
                    points_[i][0] = -180;
                }
                if (points_[i][0] > 180) {
                    points_[i][0] = 180;
                }
                while (points_[i][1] < -180) {
                    points_[i][1] = -180;
                }
                while (points_[i][1] > 180) {
                    points_[i][1] = 180;
                }
            }

            /* copy and ensure last point == first point */
            int len = points_.length - 1;
            if (points_[0][0] != points_[len][0] || points_[0][1] != points_[len][1]) {
                points = new double[points_.length + 1][2];
                for (i = 0; i < points_.length; i++) {
                    points[i][0] = points_[i][0];
                    points[i][1] = points_[i][1];
                }
                points[points_.length][0] = points_[0][0];
                points[points_.length][1] = points_[0][1];
            } else {
                points = points_.clone();
            }

            /* bounding box setup */
            bounding_box = new double[2][2];
            bounding_box[0][0] = points[0][0];
            bounding_box[0][1] = points[0][1];
            bounding_box[1][0] = points[0][0];
            bounding_box[1][1] = points[0][1];
            for (i = 1; i < points.length; i++) {
                if (bounding_box[0][0] > points[i][0]) {
                    bounding_box[0][0] = points[i][0];
                }
                if (bounding_box[1][0] < points[i][0]) {
                    bounding_box[1][0] = points[i][0];
                }
                if (bounding_box[0][1] > points[i][1]) {
                    bounding_box[0][1] = points[i][1];
                }
                if (bounding_box[1][1] < points[i][1]) {
                    bounding_box[1][1] = points[i][1];
                }
            }

            // intersection method precalculated data
            lines2 = new double[points.length][2];   //lines[0][] is not used
            for (i = 0; i < points.length - 1; i++) {
                lines2[i + 1][0] = (points[i][1] - points[i + 1][1])
                        / (points[i][0] - points[i + 1][0]);				//slope
                lines2[i + 1][1] = points[i][1] - lines2[i + 1][0] * points[i][0];		//intercept
            }
        }
    }

    /**
     * gets points of a polygon only
     *
     * @return points of this object if it is a polygon as double[][]
     * otherwise returns null.
     */
    public double[][] getPoints() {
        return points;
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
    public boolean isWithin(double longitude, double latitude) {
        switch (type) {
            case 0:
                /* no region defined, must be within this absence of a boundary */
                return true;
            case 1:
                /* return for bounding box */
                return (longitude <= points[1][0] && longitude >= points[0][0]
                        && latitude <= points[1][1] && latitude >= points[0][1]);
            case 2:
                /* TODO: fix to use radius units m not degrees */
                double x = longitude - points[0][0];
                double y = latitude - points[0][1];
                return Math.sqrt(x * x + y * y) <= radius;
            case 3:
                /* determine for Polygon */
                return isWithinPolygon(longitude, latitude);
        }
        return false;
    }

    /**
     * returns true when point is within the polygon
     *
     * method:
     * treat as segments with target long in the middle:
     *
     *
     *	              __-1__|___1_
     *			    |
     *
     *
     * iterate through points and count number of latitude axis crossing where
     * crossing is > latitude.
     *
     * point is inside of area when number of crossings is odd;
     *
     * point is on a polygon edge return true
     *
     * @param longitude
     * @param latitude
     * @return true iff longitude and latitude point is on edge or within polygon
     */
    private boolean isWithinPolygon(double longitude, double latitude) {
        // bounding box test
        if (longitude <= bounding_box[1][0] && longitude >= bounding_box[0][0]
                && latitude <= bounding_box[1][1] && latitude >= bounding_box[0][1]) {

            //initial segment
            boolean segment = points[0][0] > longitude;

            double y;
            int i;
            int len = points.length;
            int score = 0;

            for (i = 1; i < len; i++) {
                // is it in a new segment?
                if ((points[i][0] > longitude) != segment) {
                    //lat value at line crossing > target point
                    y = lines2[i][0] * longitude + lines2[i][1];
                    if (y > latitude) {
                        score++;
                    } else if (y == latitude) {
                        //line crossing
                        return true;
                    }

                    segment = !segment;
                } else if (points[i][0] == longitude && points[i][1] == latitude) {
                    //point on point
                    return true;
                }
            }
            return (score % 2 != 0);
        }
        return false;		//not within bounding box
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
    public int[][] getOverlapGridCells(double longitude1, double latitude1, double longitude2, double latitude2, int width, int height, byte[][] three_state_map) {

        switch (type) {
            case 0:
                return null;
            case 1:
                return getOverlapGridCells_Box(longitude1, latitude1, longitude2, latitude2, width, height, bounding_box, three_state_map);
            case 2:
                return null; /* TODO: circle grid */
            case 3:
                return getOverlapGridCells_Polygon(longitude1, latitude1, longitude2, latitude2, width, height, three_state_map);
        }
        return null;
    }

    /**
     * determines overlap with a grid for a BOUNDING_BOX
     *
     * @param longitude1
     * @param latitude1
     * @param longitude2
     * @param latitude2
     * @param xres number of longitude segements as int
     * @param yres number of latitude segments as int
     * @param bb bounding box as double[2][2] with [][0] as longitude, [][1] as latitude,
     * [0][] as minimum values, [1][] as maximum values
     * @return (x,y) as double [][2] for each grid cell at least partially falling
     * within the specified region of the specified resolution beginning at 0,0
     * for minimum longitude and latitude through to xres,yres for maximums
     */
    public int[][] getOverlapGridCells_Box(double longitude1, double latitude1,
            double longitude2, double latitude2, int width, int height, double[][] bb, byte[][] three_state_map) {

        double xstep = Math.abs(longitude2 - longitude1) / (double) width;
        double ystep = Math.abs(latitude2 - latitude1) / (double) height;

        //double maxlong = Math.max(longitude1, longitude2);
        double minlong = Math.min(longitude1, longitude2);
        //double maxlat = Math.max(latitude1, latitude2);
        double minlat = Math.min(latitude1, latitude2);

        //setup minimums from bounding box (TODO: should this have -1 on steps?)
        int xstart = (int) Math.floor((bb[0][0] - minlong) / xstep);
        int ystart = (int) Math.floor((bb[0][1] - minlat) / ystep);
        int xend = (int) Math.ceil((bb[1][0] - minlong) / xstep);
        int yend = (int) Math.ceil((bb[1][1] - minlat) / ystep);
        if (xstart < 0) {
            xstart = 0;
        }
        if (ystart < 0) {
            ystart = 0;
        }
        if (xend > width) {
            xend = width;
        }
        if (yend > height) {
            yend = height;
        }

        // fill data with cell coordinates
        int out_width = xend - xstart;
        int out_height = yend - ystart;
        int[][] data = new int[out_width * out_height][2];
        int j, i, p = 0;
        if (three_state_map == null) {
            for (j = ystart; j < yend; j++) {
                for (i = xstart; i < xend; i++) {
                    data[p][0] = i;
                    data[p][1] = j;
                    p++;
                }
            }
        } else {
            for (j = ystart; j < yend; j++) {
                for (i = xstart; i < xend; i++) {
                    data[p][0] = i;
                    data[p][1] = j;
                    three_state_map[j][i] = SimpleRegion.GI_FULLY_PRESENT;
                    p++;
                }
            }
            //set three state map edges to partially present
            for (j = ystart; j < yend; j++) {
                three_state_map[j][xstart] = SimpleRegion.GI_PARTIALLY_PRESENT;
                three_state_map[j][xend - 1] = SimpleRegion.GI_PARTIALLY_PRESENT;
            }
            for (i = xstart; i < xend; i++) {
                three_state_map[ystart][i] = SimpleRegion.GI_PARTIALLY_PRESENT;
                three_state_map[yend - 1][i] = SimpleRegion.GI_PARTIALLY_PRESENT;
            }

            //no need to set SimpleRegion.GI_ABSENCE
        }
        return data;
    }

    /**
     * defines a region by a points string, POLYGON only
     *
     * TODO: define better format for parsing, including BOUNDING_BOX and CIRCLE
     *
     * @param pointsString points separated by ',' with longitude and latitude separated by ':'
     * @return SimpleRegion object
     */
    public static SimpleRegion parseSimpleRegion(String pointsString) {
        if (pointsString.equalsIgnoreCase("none")) {
            return null;
        }
        SimpleRegion simpleregion = new SimpleRegion();
        String[] pairs = pointsString.split(",");

        double[][] points = new double[pairs.length][2];
        for (int i = 0; i < pairs.length; i++) {
            String[] longlat = pairs[i].split(":");
            if (longlat.length == 2) {
                try {
                    points[i][0] = Double.parseDouble(longlat[0]);
                    points[i][1] = Double.parseDouble(longlat[1]);
                } catch (Exception e) {
                    //TODO: alert failure
                }
            } else {
                //TODO: alert failure
            }
        }

        //test for box
        //  get min/max long/lat
        //  each point has only one identical lat or long to previous point
        //  4 or 5 points (start and end points may be identical)
        if (((points.length == 4 && (points[0][0] != points[3][0] || points[0][1] != points[3][1]))
                || (points.length == 5 && points[0][0] == points[4][0]
                && points[0][1] == points[4][1]))) {

            //get min/max long/lat
            double minlong = 0, minlat = 0, maxlong = 0, maxlat = 0;
            for (int i = 0; i < points.length; i++) {
                if (i == 0 || minlong > points[i][0]) {
                    minlong = points[i][0];
                }
                if (i == 0 || maxlong < points[i][0]) {
                    maxlong = points[i][0];
                }
                if (i == 0 || minlat > points[i][1]) {
                    minlat = points[i][1];
                }
                if (i == 0 || maxlat < points[i][1]) {
                    maxlat = points[i][1];
                }
            }

            //  each point has only one identical lat or long to previous point
            int prev_idx = 3;
            int i = 0;
            for (i = 0; i < 4; i++) {
                if ((points[i][0] == points[prev_idx][0])
                        == (points[i][1] == points[prev_idx][1])) {
                    break;
                }
                prev_idx = i;
            }
            //it is a box if no 'break' occurred
            if (i == 4) {
                simpleregion.setBox(minlong, minlat, maxlong, maxlat);
                return simpleregion;
            }
        }
        simpleregion.setPolygon(points);
        return simpleregion;
    }

    public double getWidth() {
        return bounding_box[1][0] - bounding_box[0][0];
    }

    public double getHeight() {
        return bounding_box[1][1] - bounding_box[0][1];
    }

    public int getType() {
        return type;
    }

    public void setAttribute(String name, Object value) {
        if (attributes == null) {
            attributes = new HashMap<String, Object>();
        }
        attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        if (attributes != null) {
            return attributes.get(name);
        }
        return null;
    }

    /**
     * determines overlap with a grid for POLYGON
     *
     * when <code>three_state_map</code> is not null populate it with one of:
     * 	GI_UNDEFINED
     * 	GI_PARTIALLY_PRESENT
     * 	GI_FULLY_PRESENT
     * 	GI_ABSENCE
     *
     * 1. Get 3state mask and fill edge passes as 'partial'.
     *  then
     * 3. Test 0,0 then progress across vert raster until finding cells[][] entry
     * 4. Repeat from (3).
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
    public int[][] getOverlapGridCells_Polygon(double longitude1, double latitude1, double longitude2, double latitude2, int width, int height, byte[][] three_state_map) {
        int i, j;
        if (three_state_map == null) {
            three_state_map = new byte[width][height];
        }

        double divx = (longitude2 - longitude1) / width;
        double divy = (latitude2 - latitude1) / height;

        //to cells
        int x, y, xend, yend;
        for (j = 1; j < points.length; j++) {
            x = (int) ((points[j - 1][0] - longitude1) / divx);
            y = (int) ((points[j - 1][1] - latitude1) / divy);
            xend = (int) ((points[j][0] - longitude1) / divx);
            yend = (int) ((points[j][1] - latitude1) / divy);

            //determine cell exit side and value
            double xDirection = (points[j - 1][0] < points[j][0]) ? 1 : -1;
            double yDirection = (points[j - 1][1] < points[j][1]) ? 1 : -1;

            if (x == xend && y == yend) {
                if (y >= 0 && y < three_state_map.length && x >= 0 && x < three_state_map[0].length) {
                    three_state_map[y][x] = GI_PARTIALLY_PRESENT;
                }
            } else if ((points[j - 1][0] - points[j][0]) == 0) {
                //vertical line
                while (y != yend) {
                    if (y >= 0 && y < three_state_map.length && x >= 0 && x < three_state_map[0].length) {
                        three_state_map[y][x] = GI_PARTIALLY_PRESENT;
                    }
                    y += yDirection;
                }
                if (y >= 0 && y < three_state_map.length && x >= 0 && x < three_state_map[0].length) {
                    three_state_map[y][x] = GI_PARTIALLY_PRESENT;
                }
            } else {
                double slope = (points[j - 1][1] - points[j][1]) / (points[j - 1][0] - points[j][0]);
                double intercept = points[j - 1][1] - slope * points[j - 1][0];

                while (x != xend || y != yend) {
                    //current cell
                    if (y >= 0 && y < three_state_map.length && x >= 0 && x < three_state_map[0].length) {
                        three_state_map[y][x] = GI_PARTIALLY_PRESENT;
                    }

                    //edges of the current cell
                    double cellTopLat = (y + 1) * divy + latitude1;
                    double cellBottomLat = y * divy + latitude1;
                    double cellLeftLong = x * divx + longitude1;
                    double cellRightLong = (x + 1) * divx + longitude1;

                    double ix, iy;

                    //check rhs
                    double xdist = 1, ydist = 1;

                    if (x != xend) {
                        if (xDirection > 0) {
                            iy = slope * cellRightLong + intercept;
                        } else {
                            //check lhs
                            iy = slope * cellLeftLong + intercept;
                        }
                        if (iy <= cellTopLat && iy >= cellBottomLat) {
                            x += xDirection;
                            continue;
                        } else {
                            xdist = Math.min(Math.abs(cellTopLat - iy), Math.abs(iy - cellBottomLat));   //for numerical error
                        }
                    }

                    if (y != yend) {
                        if (yDirection > 0) {
                            //check top
                            ix = (cellTopLat - intercept) / slope;
                        } else {
                            //check bottom
                            ix = (cellBottomLat - intercept) / slope;
                        }
                        if (ix <= cellRightLong && ix >= cellLeftLong) {
                            y += yDirection;
                            continue;
                        } else {
                            ydist = Math.min(Math.abs(cellRightLong - ix), Math.abs(ix - cellLeftLong));   //for numerical error
                        }
                    }

                    //no exit found, pick nearest
                    if (xdist < ydist && x != xend) {
                        x += xDirection;
                    } else if (xdist > ydist && y != yend) {
                        y += yDirection;
                    } else {
                        //undecided, may be through the point, no harm in setting 'partial'.
                        x += xDirection;
                        if (y >= 0 && y < three_state_map.length && x >= 0 && x < three_state_map[0].length) {
                            three_state_map[y][x] = GI_PARTIALLY_PRESENT;
                        }
                        x -= xDirection;
                        y += yDirection;
                        if (y >= 0 && y < three_state_map.length && x >= 0 && x < three_state_map[0].length) {
                            three_state_map[y][x] = GI_PARTIALLY_PRESENT;
                        }
                        x += xDirection;
                    }
                }
                if (y >= 0 && y < three_state_map.length && x >= 0 && x < three_state_map[0].length) {
                    three_state_map[y][x] = GI_PARTIALLY_PRESENT;
                }
            }
        }

        //do raster check
        int[][] data = new int[width * height][2];
        int p = 0;
        for (j = 0; j < three_state_map[0].length; j++) {
            for (i = 0; i < three_state_map.length; i++) {
                if (three_state_map[i][j] == GI_PARTIALLY_PRESENT) {
                    //if it is partially present, do nothing
                } else if ((j == 0 || three_state_map[i][j - 1] == GI_PARTIALLY_PRESENT)) {
                    if (isWithin(j * divx + divx / 2 + longitude1, i * divy + divy / 2 + latitude1)) {
                        //if the previous was partially present or absent, test
                        three_state_map[i][j] = GI_FULLY_PRESENT;
                    } //else absent
                } else {
                    //if the previous was fully present, repeat
                    //if the previous was absent, repeat
                    three_state_map[i][j] = three_state_map[i][j - 1];
                }

                //apply to cells;
                if (three_state_map[i][j] != GI_UNDEFINED) {   //undefined == absence
                    data[p][0] = j;
                    data[p][1] = i;
                    p++;
                }
            }
        }
        return java.util.Arrays.copyOfRange(data, 0, p);
    }
}
