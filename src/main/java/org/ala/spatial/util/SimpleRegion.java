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
    double[][] lines; //for polygons
    /**
     * for point/grid to polygon intersection method
     *
     * polygon edges with sorted longitude
     * lines_long = double [n][2]
     * where
     * 	n is number of edges
     * 	value at [0] is minimum longitude
     * 	value at [1] is maximum longitude
     */
    double[][] lines_long; //for polygons, lines longitude sorted
    /**
     * for point/grid to polygon intersection method
     *
     * polygon edges with sorted latitude
     * lines_lat = double [n][2]
     * where
     * 	n is number of edges
     * 	value at [0] is minimum latitude
     * 	value at [1] is maximum latitude
     */
    double[][] lines_lat; //for polygons, lines latitude sorted
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

            //fix extents
            for (i = 0; i < points.length; i++) {
                //adjust to -360 and 360
                while (points[i][0] < -360) {
                    points[i][0] += 360;
                }
                while (points[i][0] > 360) {
                    points[i][0] -= 360;
                }
                while (points[i][1] < -360) {
                    points[i][1] += 360;
                }
                while (points[i][1] > 360) {
                    points[i][1] -= 360;
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

            /* intersection method precalculated data */
            lines = new double[points.length - 1][2];
            lines_long = new double[points.length - 1][2];
            lines_lat = new double[points.length - 1][2];
            for (i = 0; i < points.length - 1; i++) {
                lines[i][0] = (points[i][1] - points[i + 1][1])
                        / (points[i][0] - points[i + 1][0]);						//slope
                lines[i][1] = points[i][1] - lines[i][0] * points[i][0];		//intercept
                lines_long[i][0] = Math.min(points[i][0], points[i + 1][0]);	//min longitude on line
                lines_long[i][1] = Math.max(points[i][0], points[i + 1][0]);	//max longitude on line
                lines_lat[i][0] = Math.min(points[i][1], points[i + 1][1]);	//min latitude on line
                lines_lat[i][1] = Math.max(points[i][1], points[i + 1][1]);	//max latitude on line
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
     * if on point or on line returns true
     *
     * @param longitude
     * @param latitude
     * @return true iff longitude and latitude point is within polygon
     */
    private boolean isWithinPolygon(double longitude, double latitude) {
        /* bounding box test */
        if (longitude <= bounding_box[1][0] && longitude >= bounding_box[0][0]
                && latitude <= bounding_box[1][1] && latitude >= bounding_box[0][1]) {

            double y;
            int segment;

            //initial segment
            if (points[0][0] > longitude) {
                segment = 1;
            } else {
                segment = -1;
            }

            int i;
            int len = points.length;
            int new_segment;
            int score = 0;

            for (i = 1; i < len; i++) {
                /* determine new segment */
                if (points[i][0] < longitude) {
                    new_segment = -1;
                } else {
                    /* point on point */
                    if (points[i][0] == longitude && points[i][1] == latitude) {
                        return true;
                    }
                    new_segment = 1;
                }

                /* do nothing if segment is the same */
                if (segment != new_segment) {
                    segment = new_segment;

                    //longtiude crossing
                    y = lines[i - 1][0] * longitude + lines[i - 1][1];

                    if (y > latitude) {
                        score++;
                    } else if (y == latitude) {
                        return true;
                    }
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

        double maxlong = Math.max(longitude1, longitude2);
        double minlong = Math.min(longitude1, longitude2);
        double maxlat = Math.max(latitude1, latitude2);
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
     * determines overlap with a grid for POLYGON
     *
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
    int[][] getOverlapGridCells_Polygon(double longitude1, double latitude1, double longitude2, double latitude2, int width, int height, byte[][] three_state_map) {

        double xstep = Math.abs(longitude2 - longitude1) / (double) width;
        double ystep = Math.abs(latitude2 - latitude1) / (double) height;

        double maxlong = Math.max(longitude1, longitude2);
        double minlong = Math.min(longitude1, longitude2);
        double maxlat = Math.max(latitude1, latitude2);
        double minlat = Math.min(latitude1, latitude2);

        //setup for bounding box (TODO: should xstep be -1 or is it handled correctly?)
        int xstart = (int) Math.floor((bounding_box[0][0] - minlong) / xstep);
        int ystart = (int) Math.floor((bounding_box[0][1] - minlat) / ystep);
        int xend = (int) Math.ceil((bounding_box[1][0] - minlong) / xstep);
        int yend = (int) Math.ceil((bounding_box[1][1] - minlat) / ystep);
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
        if (xstart > width || xend < 0 || ystart > height || yend < 0) {
            //outside of bounding box, do nothing
            return null;
        }

        // fill data with coordinates
        int out_width = xend - xstart;
        int out_height = yend - ystart;
        int[][] data = new int[out_width * out_height][2];
        int j, i, p = 0;
        boolean inside;
        boolean cross;
        boolean shapeinside;
        int len = lines.length;

        for (j = ystart; j < yend; j++) {
            for (i = xstart; i < xend; i++) {
                /* test for this grid box */
                double long1 = i * xstep + minlong;
                double long2 = (i + 1) * xstep + minlong;
                double lat1 = j * ystep + minlat;
                double lat2 = (j + 1) * ystep + minlat;

                inside = false;
                cross = false;

                /* box contained within shape */
                if (long1 <= bounding_box[0][0] && bounding_box[1][0] <= long2
                        && lat1 <= bounding_box[0][1] && bounding_box[1][1] <= lat2) {
                    // setup for partial containment results
                    shapeinside = true;
                    cross = true;
                    inside = true;
                } else {
                    shapeinside = false;
                }

                /* any lines cross */
                double q;
                int k = 0;
                if (!shapeinside) {
                    for (k = 0; k < len; k++) {
                        if (lines_long[k][0] <= long2 && lines_long[k][1] >= long1
                                && lines_lat[k][0] <= lat2 && lines_lat[k][1] >= lat1) {
                            // is any gridcell line crossed by this polygon border?

                            //vertical polygon line k cross test
                            if (Double.isInfinite(lines[k][0])) {
                                /* grid top line or grid bottom line pass across
                                 * (long1->long2 cross lines_long[k][0&1])
                                 * and between ends of
                                 * (lines_lat[k][0&1])
                                 */
                                if (long1 <= lines_long[k][1]
                                        && long2 >= lines_long[k][0]
                                        && ((lat1 <= lines_lat[k][1]
                                        && lat1 >= lines_lat[k][0])
                                        || (lat1 <= lines_lat[k][1]
                                        && lat1 >= lines_lat[k][0]))) {
                                    cross = true;
                                    break;
                                }
                            } else {
                                //non-vertical polygonline k cross test

                                /* q is y-intercept of grid LHS edge with
                                 * polygon line k.  Cross=true if q is between
                                 * ends of polylinek lat and grid (lat1&2)
                                 */
                                q = lines[k][0] * long1 + lines[k][1];
                                if (lines_lat[k][0] <= q && q <= lines_lat[k][1]
                                        && lat1 <= q && q <= lat2) {
                                    cross = true;
                                    break;
                                }
                                /* q is y-intercept of grid RHS edge with
                                 * polygon line k.  Cross=true if q is between
                                 * ends of polylinek lat and grid (lat1&2)
                                 */
                                q = lines[k][0] * long2 + lines[k][1];
                                if (lines_lat[k][0] <= q && q <= lines_lat[k][1]
                                        && lat1 <= q && q <= lat2) {
                                    cross = true;
                                    break;
                                }

                                /* different test if polygon line horizontal,
                                 * i.e. slope == 0
                                 */
                                if (lines[k][0] == 0) {
                                    /* cross=true when lat==lat && long's
                                     * overlap
                                     */
                                    if ((lines_lat[k][0] == lat1
                                            || lines_lat[k][0] == lat1)
                                            && (lines_long[k][0] <= long2
                                            && lines_long[k][1] >= long1)) {
                                        cross = true;
                                        break;
                                    }
                                } else {
                                    /* q is x-intercept of grid BOTTOM edge with
                                     * polygon line k.  Cross=true if q is between
                                     * ends of polylinek longs and grid (long1&2)
                                     */
                                    q = (lat1 - lines[k][1]) / lines[k][0];
                                    if (lines_long[k][0] <= q && q <= lines_long[k][1]
                                            && long1 <= q && q <= long2) {
                                        cross = true;
                                        break;
                                    }
                                    /* q is x-intercept of grid TOP edge with
                                     * polygon line k.  Cross=true if q is between
                                     * ends of polylinek longs and grid (long1&2)
                                     */
                                    q = (lat2 - lines[k][1]) / lines[k][0];
                                    if (lines_long[k][0] <= q && q <= lines_long[k][1]
                                            && long1 <= q && q <= long2) {
                                        cross = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (!cross) {
                        /* first point inside, for zero cross & therefore
                         * box contained within shape
                         */
                        if (isWithinPolygon(long1, lat1)) {
                            inside = true;
                        } else {
                            inside = false;
                        }
                    }
                }
                if (three_state_map != null) {
                    if (cross) {
                        three_state_map[j][i] = SimpleRegion.GI_PARTIALLY_PRESENT;
                    } else if (inside) {
                        three_state_map[j][i] = SimpleRegion.GI_FULLY_PRESENT;
                    } else {
                        three_state_map[j][i] = SimpleRegion.GI_ABSENCE;
                    }
                }

                //record if intersecting
                if (cross || inside) {
                    data[p][0] = i;
                    data[p][1] = j;
                    p++;
                }
            }
        }

        //output only required range
        return java.util.Arrays.copyOfRange(data, 0, p);
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
}
