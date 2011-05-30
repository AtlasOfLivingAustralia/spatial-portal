package org.ala.spatial.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.spatial.analysis.service.ShapeLookup;

/**
 * SimpleShapeFile is a representation of a Shape File for
 * intersections with points
 *
 * .shp MULTIPOLYGON only
 * .dbf can read values from String and Number columns only
 *
 * TODO: finish serialization
 *
 * @author Adam Collins
 */
public class SimpleShapeFile extends Object implements Serializable {

    static final long serialVersionUID = -9046250209453575076L;
    /**
     * .shp file header contents
     */
    ShapeHeader shapeheader;
    /**
     * .shp file record contents
     */
    ShapeRecords shaperecords;
    /**
     * list of ComplexRegions, one per .shp record
     */
    ArrayList<ComplexRegion> regions;
    /**
     * .dbf contents
     */
    DBF dbf;
    /**
     * for balancing shape files with large numbers of shapes.
     */
    ShapesReference shapesreference;
    /**
     * one dbf column, for use after loading from a file
     */
    short[] singleColumn;
    /**
     * one lookup for a dbf column, for use after loading from a file
     */
    String[] singleLookup;

    /**
     * Constructor for a SimpleShapeFile, requires .dbf and .shp files present
     * on the fileprefix provided.
     *
     * @param fileprefix file path for valid files after appending .shp and .dbf
     */
    public SimpleShapeFile(String fileprefix) {
        //If fileprefix exists as-is it is probably a saved SimpleShapeFile
        if (loadRegion(fileprefix)) {
            //previously saved region loaded
        } else {
            /* read dbf */
            dbf = new DBF(fileprefix + ".dbf");

            /* read shape header */
            shapeheader = new ShapeHeader(fileprefix);

            /* read shape records */
            shaperecords = new ShapeRecords(fileprefix, shapeheader.getShapeType());

            /* get ComplexRegion list from shape records */
            regions = shaperecords.getRegions();

            /* create shapes reference for intersections */
            shapesreference = new ShapesReference(shaperecords);
        }
    }

    /**
     * Constructor for a SimpleShapeFile, requires .dbf and .shp files present
     * on the fileprefix provided.
     *
     * @param fileprefix file path for valid files after appending .shp and .dbf
     */
    SimpleShapeFile(String fileprefix, boolean loadDbf) {
        //If fileprefix exists as-is it is probably a saved SimpleShapeFile
        if (loadRegion(fileprefix)) {
            //previously saved region loaded
        } else {
            /* read dbf */
            if (loadDbf) {
                dbf = new DBF(fileprefix + ".dbf");
            }

            /* read shape header */
            shapeheader = new ShapeHeader(fileprefix);

            /* read shape records */
            shaperecords = new ShapeRecords(fileprefix, shapeheader.getShapeType());

            /* get ComplexRegion list from shape records */
            regions = shaperecords.getRegions();

            /* create shapes reference for intersections */
            if (loadDbf) {
                shapesreference = new ShapesReference(shaperecords);
            }
        }
    }

    public static SimpleRegion readRegions(String shapeFileName) {
        SimpleShapeFile ssf = new SimpleShapeFile(shapeFileName, false);

        if (ssf.regions.size() == 1) {
            return ssf.regions.get(0);
        } else {
            ComplexRegion r = new ComplexRegion();
            for (int i = 0; i < ssf.regions.size(); i++) {
                for (int j = 0; j < ssf.regions.get(i).simpleregions.size(); j++) {
                    r.addPolygon(ssf.regions.get(i).simpleregions.get(j).getPoints());
                }
            }
            return r;
        }

    }

    /**
     * save partial file (enough to reload and use intersect function)
     *
     * @param filename
     */
    public void saveRegion(String filename, int column) {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            shapesreference.sr.records = null; //cleanup
            oos.writeObject(shapesreference);

            singleLookup = getColumnLookup(column);
            oos.writeObject(singleLookup);

            singleColumn = new short[regions.size()];
            for (int i = 0; i < singleColumn.length; i++) {
                singleColumn[i] = (short) java.util.Arrays.binarySearch(singleLookup, dbf.getValue(i, column));
            }
            oos.writeObject(singleColumn);

            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * save partial file (enough to reload and use intersect function)
     *
     * for each unique value in the shape file
     *
     * Note: excludes regions that share the same column value as another.
     *
     * @param filename
     */
    public void saveEachRegion(String filename, int column) {
        int m, i;
        for (m = 0; m < singleLookup.length; m++) {
            for (i = 0; i < regions.size(); i++) {
                if (singleColumn[i] == (short) m) {
                    //check that no other column has this 'm' value
                    int j = 0;
                    for (j = 0; j < singleColumn.length; j++) {
                        if (i != j && singleColumn[i] == singleColumn[j]) {
                            break;
                        }
                    }
                    if (j == singleColumn.length) {
                        try {
                            FileOutputStream fos = new FileOutputStream(filename + "_" + m);
                            BufferedOutputStream bos = new BufferedOutputStream(fos);
                            ObjectOutputStream oos = new ObjectOutputStream(bos);
                            ComplexRegion cr = shapesreference.sr.region.get(i);
                            oos.writeObject(cr);
                            oos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * save partial file (enough to reload and use intersect function)
     *
     * @param filename
     * @return true when successful
     */
    public boolean loadRegion(String filename) {
        if (new File(filename).exists()) {
            try {
                FileInputStream fis = new FileInputStream(filename);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis);
                shapesreference = (ShapesReference) ois.readObject();

                singleLookup = (String[]) ois.readObject();

                singleColumn = (short[]) ois.readObject();
                ois.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * save partial file (enough to reload and use intersect function)
     *
     * @param filename
     */
    static public ComplexRegion loadShapeInRegion(String filename, int idx) {
        ComplexRegion cr = null;
        try {
            FileInputStream fis = new FileInputStream(filename + "_" + idx);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            cr = (ComplexRegion) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cr;
    }

    /**
     * loads a complex region, adds cells (used in GridCutter), save region
     *
     * @param filename
     */
    static public void addCellsToShapeRegion(String filename) {
        SpatialLogger.log("addCellsToShapeRegion(" + filename + ") start");

        int idx = 0;
        LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<String>();
        while (idx < 100000) {
            if (new File(filename + "_" + idx).exists()) {
                lbq.add(filename + "_" + idx);
            }
            idx++;
        }
        CountDownLatch cdl = new CountDownLatch(lbq.size());

        //run these functions at the same time as other build_all requests
        class AddCellsThread extends Thread {

            public CountDownLatch cdl;
            public LinkedBlockingQueue<String> lbq;

            @Override
            public void run() {
                try {
                    setPriority(MIN_PRIORITY);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    while (true) {
                        String filename = lbq.take();
                        int p = filename.lastIndexOf('_');
                        ComplexRegion cr = loadShapeInRegion(filename.substring(0, p), Integer.parseInt(filename.substring(p + 1)));
                        cr.setAttribute("cells", cr.getOverlapGridCells(
                                TabulationSettings.grd_xmin, TabulationSettings.grd_ymin,
                                TabulationSettings.grd_xmax, TabulationSettings.grd_ymax,
                                TabulationSettings.grd_ncols, TabulationSettings.grd_nrows,
                                null));

                        FileOutputStream fos = new FileOutputStream(filename);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(cr);
                        oos.close();
                        cdl.countDown();
                    }
                } catch (InterruptedException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        AddCellsThread[] adc = new AddCellsThread[TabulationSettings.analysis_threads];
        for (int i = 0; i < adc.length; i++) {
            adc[i] = new AddCellsThread();
            adc[i].cdl = cdl;
            adc[i].lbq = lbq;
            adc[i].start();
        }

        try {
            cdl.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //end threads
        for (int i = 0; i < adc.length; i++) {
            adc[i].interrupt();
        }

        SpatialLogger.log("addCellsToShapeRegion(" + filename + ") end");
    }

    static public void main(String[] args) {
        TabulationSettings.load();

        if (args.length > 0) {
            SimpleShapeFile.addCellsToShapeRegion(
                    TabulationSettings.index_path
                    + args[0]);
        }
        /*
        SimpleShapeFile.addCellsToShapeRegion(
        TabulationSettings.index_path
        + "aus2");
        SimpleShapeFile.addCellsToShapeRegion(
        TabulationSettings.index_path
        + "imcra4_pb");
        SimpleShapeFile.addCellsToShapeRegion(
        TabulationSettings.index_path
        + "ibra_reg_shape");
         */
    }

    /**
     * returns a list of column names in the
     * .dbf file
     *
     * @return list of column names as String []
     */
    public String[] listColumns() {
        return dbf.getColumnNames();
    }

    /**
     * returns set of values found in the .dbf file
     * at a column number, zero base.
     *
     * @param column integer representing column whose set of values
     * is to be returned.  see <code>listColumns()</code> for listing
     * column names.
     *
     * @return set of values in the column as String [] sorted
     */
    public String[] getColumnLookup(int column) {
        return dbf.getColumnLookup(column);
    }

    /**
     * returns the position, zero indexed, of the provided
     * column_name from within the .dbf
     *
     * @param column_name
     * @return -1 if not found, otherwise column index number, zero base
     */
    public int getColumnIdx(String column_name) {
        return dbf.getColumnIdx(column_name);
    }

    /**
     * use when created from a shape file
     *
     * identifies the index within a lookup list provided
     * for each provided point, or -1 for not found.
     *
     * @param points double [n][2]
     * where
     * 	n is number of points
     *  [][0] is longitude
     *  [][1] is latitude
     * @param lookup String [], same as output from <code>getColumnLookup(column)</code>
     * @param column .dbf column value to use
     * @return index within a lookup list provided
     * for each provided point, or -1 for not found as int []
     */
    public int[] intersect(double[][] points, String[] lookup, int column) {
        int i;

        //copy, tag and sort points
        PointPos[] p = new PointPos[points.length];
        for (i = 0; i < p.length; i++) {
            p[i] = new PointPos(points[i][0], points[i][1], i);
        }
        java.util.Arrays.sort(p, new Comparator<PointPos>() {

            @Override
            public int compare(PointPos o1, PointPos o2) {
                if (o1.x == o2.x) {
                    return ((o1.y - o2.y) > 0) ? 1 : -1;
                } else {
                    return ((o1.x - o2.x) > 0) ? 1 : -1;
                }
            }
        });

        /* setup for thread count */
        int threadcount = TabulationSettings.analysis_threads + 5;
        ArrayList<Integer> threadstart = new ArrayList(threadcount * 10);
        int step = (int) Math.ceil(points.length / (double) (threadcount * 10));
        if (step % 2 != 0) {
            step++;
        }
        int pos = 0;
        for (i = 0; i < threadcount * 10; i++) {
            threadstart.add(new Integer(pos));
            pos += step;
        }

        LinkedBlockingQueue<Integer> lbq = new LinkedBlockingQueue(threadstart);
        CountDownLatch cdl = new CountDownLatch(lbq.size());

        IntersectionThread[] it = new IntersectionThread[threadcount];

        int[] target = new int[points.length];

        for (i = 0; i < threadcount; i++) {
            it[i] = new IntersectionThread(shapesreference, p, lbq, step, target, cdl);
        }

        //wait for all parts to be finished
        try {
            cdl.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //end threads
        for (i = 0; i < threadcount; i++) {
            it[i].interrupt();
        }

        //transform target from shapes_idx to column_idx
        for (i = 0; i < target.length; i++) {
            String s = dbf.getValue(target[i], column);
            int v = java.util.Arrays.binarySearch(lookup, s);
            if (v < 0) {
                v = -1;
            }
            target[i] = v;
        }

        return target;
    }

    /**
     * use from saved shape file
     *
     * e.g
     *
     * <code>int[] output = (new SimpleShapeFile(filename)).intersect(points, 1);</code>
     *
     * identifies the index within a lookup list provided
     * for each provided point, or -1 for not found.
     *
     * @param points double [n][2]
     * where
     * 	n is number of points
     *  [][0] is longitude
     *  [][1] is latitude
     * @param lookup String [], same as output from <code>getColumnLookup(column)</code>
     * @param column .dbf column value to use
     * @return index within a lookup list provided
     * for each provided point, or -1 for not found as int []
     */
    public int[] intersect(double[][] points, int threadcount) {
        int i;

        //copy, tag and sort points
        PointPos[] p = new PointPos[points.length];
        for (i = 0; i < p.length; i++) {
            p[i] = new PointPos(points[i][0], points[i][1], i);
        }
        java.util.Arrays.sort(p, new Comparator<PointPos>() {

            @Override
            public int compare(PointPos o1, PointPos o2) {
                if (o1.x == o2.x) {
                    return ((o1.y - o2.y) > 0) ? 1 : -1;
                } else {
                    return ((o1.x - o2.x) > 0) ? 1 : -1;
                }
            }
        });


        /* setup for thread count */
        ArrayList<Integer> threadstart = new ArrayList(threadcount * 10);
        int step = (int) Math.ceil(points.length / (double) (threadcount * 10));
        if (step % 2 != 0) {
            step++;
        }
        int pos = 0;
        for (i = 0; i < threadcount * 10; i++) {
            threadstart.add(new Integer(pos));
            pos += step;
        }

        System.out.println("parts count = " + threadstart.size());
        LinkedBlockingQueue<Integer> lbq = new LinkedBlockingQueue(threadstart);
        CountDownLatch cdl = new CountDownLatch(lbq.size());

        IntersectionThread[] it = new IntersectionThread[threadcount];

        int[] target = new int[points.length];

        for (i = 0; i < threadcount; i++) {
            it[i] = new IntersectionThread(shapesreference, p, lbq, step, target, cdl);
        }

        try {
            cdl.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (i = 0; i < threadcount; i++) {
            it[i].interrupt();
        }

        //transoform target from shapes_idx to column_idx
        for (i = 0; i < target.length; i++) {
            if (target[i] >= 0 && target[i] < singleColumn.length) {
                target[i] = singleColumn[target[i]];
            } else {
                target[i] = -1;
            }
        }

        return target;
    }

    /**
     * gets shape header as String
     * @return String
     */
    public String getHeaderString() {
        return shapeheader.toString();
    }

    public String getValueString(int idx) {
        return singleLookup[idx];
    }

    public String[] getColumnLookup() {
        return singleLookup;
    }

    /**
     * for use with SimpleShapeFile constructed from a Shape File
     *
     * generates a list of 'values' per grid cell, for input grid
     * definition, from .dbf column.
     *
     * @param column .dbf column whose sorted set of values is used as index source
     * @param longitude1 bounding box point 1 longitude
     * @param latitude1 bounding box point 1 latitude
     * @param longitude2 bounding box point 2 longitude
     * @param latitude2 bounding box point 2 latitude
     * @param width bounding box width in number of cells
     * @param height bounding box height in number of cells
     * @return list of (cell x, cell y, cell value) as Tile [] for at least partial cell
     * coverage
     */
    public Tile[] getTileList(int column, double longitude1, double latitude1, double longitude2, double latitude2, int width, int height) {
        int i, j, k;

        String[] lookup = getColumnLookup(column);

        Vector<Tile> tiles = new Vector<Tile>();
        byte[][] map;
        int m;

        byte[][] mask = new byte[height][width];

        for (m = 0; m < lookup.length; m++) {
            for (i = 0; i < regions.size(); i++) {
                if (dbf.getValue(i, column).equals(lookup[m])) {
                    map = new byte[height][width];
                    regions.get(i).getOverlapGridCells(longitude1, latitude1, longitude2, latitude2, width, height, map);

                    /* merge on first in basis for partial or complete cells */
                    for (j = 0; j < map.length; j++) {
                        for (k = 0; k < map[j].length; k++) {
                            if (map[j][k] == SimpleRegion.GI_PARTIALLY_PRESENT
                                    || map[j][k] == SimpleRegion.GI_FULLY_PRESENT) {
                                mask[j][k] = 1;			//indicate presence
                            }
                        }
                    }
                }
            }

            /* add to tiles */
            for (i = 0; i < height; i++) {
                for (j = 0; j < width; j++) {
                    if (mask[i][j] > 0) {				//from above indicated presence
                        mask[i][j] = 0;					//reset for next region in loop
                        tiles.add(new Tile((float) m, (height - 1 - i) * width + j));
                    }
                }
            }
        }

        // return as [] instead of Vector
        Tile[] tilesa = new Tile[tiles.size()];
        tiles.toArray(tilesa);
        return tilesa;
    }

    /**
     * for use after loading previously saved SimpleShapeFile
     *
     * generates a list of 'values' per grid cell, for input grid
     * definition, from .dbf column.
     *
     * @param longitude1 bounding box point 1 longitude
     * @param latitude1 bounding box point 1 latitude
     * @param longitude2 bounding box point 2 longitude
     * @param latitude2 bounding box point 2 latitude
     * @param width bounding box width in number of cells
     * @param height bounding box height in number of cells
     * @return list of (cell x, cell y, cell value) as Tile [] for at least partial cell
     * coverage
     */
    public Tile[] getTileList(double longitude1, double latitude1, double longitude2, double latitude2, int width, int height) {
        int i, j, k;

        String[] lookup = singleLookup;

        Vector<Tile> tiles = new Vector<Tile>();
        byte[][] map;
        int m;

        byte[][] mask = new byte[height][width];

        for (m = 0; m < lookup.length; m++) {
            for (i = 0; i < regions.size(); i++) {
                if (singleColumn[i] == (short) m) {
                    map = new byte[height][width];

                    shapesreference.sr.region.get(i).getOverlapGridCells(longitude1, latitude1, longitude2, latitude2, width, height, map);

                    /* merge on first in basis for partial or complete cells */
                    for (j = 0; j < map.length; j++) {
                        for (k = 0; k < map[j].length; k++) {
                            if (map[j][k] == SimpleRegion.GI_PARTIALLY_PRESENT
                                    || map[j][k] == SimpleRegion.GI_FULLY_PRESENT) {
                                mask[j][k] = 1;			//indicate presence
                            }
                        }
                    }
                }
            }

            /* add to tiles */
            for (i = 0; i < height; i++) {
                for (j = 0; j < width; j++) {
                    if (mask[i][j] > 0) {				//from above indicated presence
                        mask[i][j] = 0;					//reset for next region in loop
                        tiles.add(new Tile((float) m, (height - 1 - i) * width + j));
                    }
                }
            }
        }

        // return as [] instead of Vector
        Tile[] tilesa = new Tile[tiles.size()];
        tiles.toArray(tilesa);
        return tilesa;
    }

    /**
     * defines a region by a points string, POLYGON only
     *
     * TODO: define better format for parsing, including BOUNDING_BOX and CIRCLE
     *
     * @param pointsString points separated by ',' with longitude and latitude separated by ':'
     * @return SimpleRegion object
     */
    public static SimpleRegion parseWKT(String pointsString) {
        if (pointsString == null) {
            return null;
        }

        if (pointsString != null && pointsString.startsWith("LAYER")) {
            String s = pointsString.substring(pointsString.lastIndexOf(',') + 1, pointsString.length() - 1);
            return ShapeLookup.getShape(s);
        }

        //GEOMETRYCOLLECTION
        ArrayList<String> stringsList = new ArrayList<String>();
        if (pointsString.startsWith("GEOMETRYCOLLECTION")) {
            //split out polygons and multipolygons
            pointsString = pointsString.replace("GEOMETRYCOLLECTION", "");

            int posStart, posEnd, p1, p2;;
            p1 = pointsString.indexOf("POLYGON", 0);
            p2 = pointsString.indexOf("MULTIPOLYGON", 0);
            if (p1 < 0) {
                posStart = p2;
            } else if (p2 < 0) {
                posStart = p1;
            } else {
                posStart = Math.min(p1, p2);
            }
            
            p1 = pointsString.indexOf("POLYGON", posStart + 10);
            p2 = pointsString.indexOf("MULTIPOLYGON", posStart + 10);
            while (p1 > 0 || p2 > 0) {
                if (p1 < 0) {
                    posEnd = p2;
                } else if (p2 < 0) {
                    posEnd = p1;
                } else {
                    posEnd = Math.min(p1, p2);
                }

                stringsList.add(pointsString.substring(posStart, posEnd-1));
                posStart = posEnd;
                p1 = pointsString.indexOf("POLYGON", posStart + 10);
                p2 = pointsString.indexOf("MULTIPOLYGON", posStart + 10);
            }
            stringsList.add(pointsString.substring(posStart, pointsString.length()));
        } else {
            stringsList.add(pointsString);
        }

        ArrayList<SimpleRegion> regions = new ArrayList<SimpleRegion>();
        for (String ps : stringsList) {
            ps = convertGeoToPoints(ps);

            String[] polygons = ps.split("S");

            //String[] fixedPolygons = fixStringPolygons(polygons);

            //System.out.println("$" + pointsString);

            if (stringsList.size() == 1) {
                if (polygons.length == 1) {
                    return SimpleRegion.parseSimpleRegion(polygons[0]);
                } else {
                    return ComplexRegion.parseComplexRegion(polygons);
                }
            } else {
                if (polygons.length == 1) {
                    regions.add(SimpleRegion.parseSimpleRegion(polygons[0]));
                } else {
                    regions.add(ComplexRegion.parseComplexRegion(polygons));
                }
            }
        }

        OrRegion orRegion = new OrRegion();
        orRegion.setSimpleRegions(regions);
        return orRegion;
    }

    private static ArrayList<double[]> fixStringPolygons(String[] polygons) {
        ArrayList<double[]> fixedPolygons = new ArrayList<double[]>();

        for (int p = 0; p < polygons.length; p++) {
            String[] pairs = polygons[p].split(",");

            //track polygon longtiude extents
            double min = 0;
            double max = 0;

            double[][] points = new double[pairs.length][2];
            for (int i = 0; i < pairs.length; i++) {
                String[] longlat = pairs[i].split(":");
                if (longlat.length == 2) {
                    try {
                        points[i][0] = Double.parseDouble(longlat[0]);
                        points[i][1] = Double.parseDouble(longlat[1]);

                        while (points[i][0] > 360) {
                            points[i][0] -= 360;
                        }
                        while (points[i][0] <= -360) {
                            points[i][0] += 360;
                        }

                        if (i == 0) {
                            min = points[i][0];
                            max = min;
                        } else {
                            if (min < points[i][0]) {
                                min = points[i][0];
                            }
                            if (max < points[i][0]) {
                                max = points[i][0];
                            }
                        }
                    } catch (Exception e) {
                        //TODO: alert failure
                    }
                } else {
                    //TODO: alert failure
                }
            }

            //does it need to be split?
            double xplus = 0;
            if (min <= -180) {
                xplus = 360;
            }
            if (max + xplus > 180) {
                //TODO: split into polygons
            }
        }

        return fixedPolygons;
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

/**
 * represents partial shape file header structure
 *
 * @author adam
 *
 */
class ShapeHeader extends Object implements Serializable {

    static final long serialVersionUID = 1219127870707511387L;

    /* from .shp file header specification */
    int filecode;
    int filelength;
    int version;
    int shapetype;
    double[] boundingbox;

    /* TODO: use appropriately for failed constructor */
    boolean isvalid;

    /**
     * constructor takes shapefile file path, appends .shp itself,
     * and reads in the shape file header values.
     *
     * TODO: any validation
     *
     * @param fileprefix
     */
    public ShapeHeader(String fileprefix) {
        try {
            FileInputStream fis = new FileInputStream(fileprefix + ".shp");
            FileChannel fc = fis.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);		//header will be smaller

            fc.read(buffer);
            buffer.flip();

            buffer.order(ByteOrder.BIG_ENDIAN);
            filecode = buffer.getInt();
            buffer.getInt();
            buffer.getInt();
            buffer.getInt();
            buffer.getInt();
            buffer.getInt();
            filelength = buffer.getInt();

            buffer.order(ByteOrder.LITTLE_ENDIAN);
            version = buffer.getInt();
            shapetype = buffer.getInt();
            boundingbox = new double[8];
            for (int i = 0; i < 8; i++) {
                boundingbox[i] = buffer.getDouble();
            }

            fis.close();

            isvalid = true;
        } catch (Exception e) {
            System.out.println("loading header error: " + fileprefix + ": " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * returns shape file type as indicated in header
     *
     * @return shape file type as int
     */
    public int getShapeType() {
        return shapetype;
    }

    /**
     * format .shp header contents
     * @return
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("\r\nFile Code: \r\n");
        sb.append(String.valueOf(filecode));

        sb.append("\r\nFile Length: \r\n");
        sb.append(String.valueOf(filelength));

        sb.append("\r\nVersion: \r\n");
        sb.append(String.valueOf(version));

        sb.append("\r\nShape Type: \r\n");
        sb.append(String.valueOf(shapetype));

        int i = 0;

        sb.append("\r\nXmin: \r\n");
        sb.append(String.valueOf(boundingbox[i++]));

        sb.append("\r\nYmin: \r\n");
        sb.append(String.valueOf(boundingbox[i++]));

        sb.append("\r\nXmax: \r\n");
        sb.append(String.valueOf(boundingbox[i++]));


        sb.append("\r\nYmax: \r\n");
        sb.append(String.valueOf(boundingbox[i++]));


        sb.append("\r\nZmin: \r\n");
        sb.append(String.valueOf(boundingbox[i++]));

        sb.append("\r\nZmax: \r\n");
        sb.append(String.valueOf(boundingbox[i++]));

        sb.append("\r\nMmin: \r\n");
        sb.append(String.valueOf(boundingbox[i++]));

        sb.append("\r\nMmax: \r\n");
        sb.append(String.valueOf(boundingbox[i++]));

        return sb.toString();
    }

    /**
     * @return true iff header loaded and passed validation
     */
    public boolean isValid() {
        return isvalid;
    }
}

/**
 * collection of shape file records
 *
 * @author adam
 *
 */
class ShapeRecords extends Object implements Serializable {

    static final long serialVersionUID = -8141403235810528840L;
    /**
     * list of ShapeRecord
     */
    ArrayList<ShapeRecord> records;
    /**
     * true if constructor was successful
     */
    boolean isvalid;
    ArrayList<ComplexRegion> region;

    /**
     * constructor creates the shape file from filepath
     * with specified shape type (only 5, MULTIPOLYGON for now)
     *
     * TODO: static
     * TODO: validation
     *
     * @param fileprefix
     * @param shapetype
     */
    public ShapeRecords(String fileprefix, int shapetype) {
        isvalid = false;
        try {
            FileInputStream fis = new FileInputStream(fileprefix + ".shp");
            FileChannel fc = fis.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate((int) fc.size() - 100);

            fc.read(buffer, 100);				//records header starts at 100
            buffer.flip();
            buffer.order(ByteOrder.BIG_ENDIAN);

            records = new ArrayList();

            while (buffer.hasRemaining()) {
                records.add(new ShapeRecord(buffer, shapetype));
            }

            fis.close();

            isvalid = true;
        } catch (Exception e) {
            System.out.println("loading shape records error: " + fileprefix + ": " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * @return true iff records loaded and passed validation
     */
    public boolean isValid() {
        return isvalid;
    }

    /**
     * creates a list of ComplexRegion objects from
     * loaded shape file records	 *
     *
     * @return
     */
    public ArrayList<ComplexRegion> getRegions() {
        if (region == null) {
            /* object for output */
            ArrayList<ComplexRegion> sra = new ArrayList();

            for (int i = 0; i < records.size(); i++) {
                ShapeRecord shr = records.get(i);
                ComplexRegion sr = new ComplexRegion();

                /* add each polygon (list of points) belonging to
                 * this shape record to the new ComplexRegion
                 */
                int points_count = 0;
                for (int j = 0; j < shr.getNumberOfParts(); j++) {
                    sr.addPolygon(shr.getPoints(j));
                    points_count += shr.getPoints(j).length;
                }

                /* speed up for polygons with lots of points */
                if (points_count > 20) {
                    double[][] bb = sr.getBoundingBox();
                    int width = (int) ((bb[1][0] - bb[0][0]) * 3);
                    int height = (int) ((bb[1][1] - bb[0][1]) * 3);
                    if (width > 200) {
                        width = 200;
                    }
                    if (height > 200) {
                        height = 200;
                    }
                    if (width > 3 && height > 3) {
                        sr.useMask(width, height, Integer.MAX_VALUE);
                    }
                    //sr.useMask(100, 50);
                }

                sra.add(sr);
            }
            region = sra;
        }
        return region;
    }

    /**
     * gets number of shape records
     *
     * @return
     */
    public int getNumberOfRecords() {
        return records.size();
    }
}

/**
 * collection of shape file records
 *
 * @author adam
 *
 */
class ShapeRecord extends Object implements Serializable {

    static final long serialVersionUID = -4426292545633280160L;
    int recordnumber;
    int contentlength;
    Shape shape;

    public ShapeRecord(ByteBuffer bb, int shapetype) {
        bb.order(ByteOrder.BIG_ENDIAN);
        recordnumber = bb.getInt();
        contentlength = bb.getInt();

        switch (shapetype) {
            case 5:
                shape = new Polygon(bb);
                break;
            default:
                System.out.println("unknown shape type: " + shapetype);
        }
    }

    /**
     * format .shp record summary
     * @return String
     */
    @Override
    public String toString() {
        if (shape != null) {
            return "Record Number: " + recordnumber + ", Content Length: " + contentlength
                    + shape.toString();
        }

        return "Record Number: " + recordnumber + ", Content Length: " + contentlength;
    }

    /**
     * gets the list of points for a shape part
     *
     * @param part index of shape part
     * @return points as double[][2]
     * where
     * 	[][0] is longitude
     *  [][1] is latitude
     */
    public double[][] getPoints(int part) {
        return shape.getPoints(part);
    }

    /**
     * gets number of parts in this shape
     *
     * @return number of parts as int
     */
    public int getNumberOfParts() {
        return shape.getNumberOfParts();
    }
}

/**
 * empty shape template
 *
 * TODO: abstract
 *
 * @author adam
 *
 */
class Shape extends Object implements Serializable {

    static final long serialVersionUID = 8573677305368105719L;

    /**
     * default constructor
     *
     */
    public Shape() {
    }

    /**
     * returns a list of points for the numbered shape part.
     *
     * @param part index of part to return
     *
     * @return double[][2] containing longitude and latitude
     * pairs where
     * 	[][0] is longitude
     *  [][1] is latitude
     */
    public double[][] getPoints(int part) {
        return null;
    }

    /**
     * returns number of parts in this shape
     *
     * @return number of parts as int
     */
    public int getNumberOfParts() {
        return 0;
    }
}

/**
 * object for shape file POLYGON
 *
 * @author adam
 *
 */
class Polygon extends Shape {

    /**
     * shape file POLYGON record fields
     */
    int shapetype;
    double[] boundingbox;
    int numparts;
    int numpoints;
    int[] parts;
    double[] points;

    /**
     * creates a shape file POLYGON from a ByteBuffer
     *
     * TODO: static
     *
     * @param bb ByteBuffer containing record bytes
     * from a shape file POLYGON record
     */
    public Polygon(ByteBuffer bb) {
        int i;

        bb.order(ByteOrder.LITTLE_ENDIAN);
        shapetype = bb.getInt();

        boundingbox = new double[4];
        for (i = 0; i < 4; i++) {
            boundingbox[i] = bb.getDouble();
        }

        numparts = bb.getInt();

        numpoints = bb.getInt();

        parts = new int[numparts];
        for (i = 0; i < numparts; i++) {
            parts[i] = bb.getInt();
        }

        points = new double[numpoints * 2];			//x,y pairs
        for (i = 0; i < numpoints * 2; i++) {
            points[i] = bb.getDouble();
        }
    }

    /**
     * output .shp POLYGON summary
     * @return String
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("(");
        sb.append(String.valueOf(boundingbox[0]));
        sb.append(", ");
        sb.append(String.valueOf(boundingbox[1]));

        sb.append(") (");
        sb.append(String.valueOf(boundingbox[2]));
        sb.append(", ");
        sb.append(String.valueOf(boundingbox[3]));

        sb.append(") parts=");
        sb.append(String.valueOf(numparts));
        sb.append(" points=");
        sb.append(String.valueOf(numpoints));

        return sb.toString();
    }

    /**
     * returns number of parts in this shape
     *
     * one part == one polygon
     *
     * @return number of parts as int
     */
    @Override
    public int getNumberOfParts() {
        return numparts;
    }

    /**
     * returns a list of points for the numbered shape part.
     *
     * @param part index of part to return
     *
     * @return double[][2] containing longitude and latitude
     * pairs where
     * 	[][0] is longitude
     *  [][1] is latitude
     */
    @Override
    public double[][] getPoints(int part) {
        double[][] output;				//data to return
        int start = parts[part];		//first index of this part

        /* last index of this part */
        int end = numpoints;
        if (part < numparts - 1) {
            end = parts[part + 1];
        }

        /* fill output */
        output = new double[end - start][2];
        for (int i = start; i < end; i++) {
            output[i - start][0] = points[i * 2];
            output[i - start][1] = points[i * 2 + 1];
        }
        return output;
    }
}

/**
 * .dbf file object
 *
 * @author adam
 *
 */
class DBF extends Object implements Serializable {

    static final long serialVersionUID = -1631837349804567374L;
    /**
     * .dbf file header object
     */
    DBFHeader dbfheader;
    /**
     * .dbf records
     */
    DBFRecords dbfrecords;

    /**
     * constructor for new DBF from .dbf filename
     * @param filename path and file name of .dbf file
     */
    public DBF(String filename) {
        /* get file header */
        dbfheader = new DBFHeader(filename);

        /* get records */
        dbfrecords = new DBFRecords(filename, dbfheader);
    }

    /**
     * returns index of a column by column name
     *
     * @param column_name column name as String
     * @return index of column_name as int
     * 	-1 for none
     */
    public int getColumnIdx(String column_name) {
        return dbfheader.getColumnIdx(column_name);
    }

    /**
     * gets the list of column names in column order
     *
     * @return column names as String []
     */
    public String[] getColumnNames() {
        return dbfheader.getColumnNames();
    }

    /**
     * gets the value at a row and column
     * @param row row index as int
     * @param column column index as int
     * @return value as String
     */
    public String getValue(int row, int column) {
        return dbfrecords.getValue(row, column);
    }

    /**
     * lists all values in a specified column as a sorted set
     *
     * @param column index of column values to return
     * @return sorted set of values in the column as String[]
     */
    public String[] getColumnLookup(int column) {
        int i, len;
        TreeSet<String> ts = new TreeSet<String>();

        /* build set */
        len = dbfheader.getNumberOfRecords();
        for (i = 0; i < len; i++) {
            ts.add(getValue(i, column));
        }

        /* convert to sorted [] */
        String[] sa = new String[ts.size()];
        ts.toArray(sa);

        return sa;
    }

    /**
     * lists values that are occur on more than one row
     *
     * @param column index of column values to return
     * @return sorted set of duplicated values in the column as String[]
     */
    public String[] getColumnMultipleValues(int column) {
        int i, len;
        TreeSet<String> ts = new TreeSet<String>();
        TreeSet<String> dup = new TreeSet<String>();

        /* build set */
        len = dbfheader.getNumberOfRecords();
        for (i = 0; i < len; i++) {
            String v = getValue(i, column);
            if (ts.contains(v)) {
                dup.add(v);
            } else {
                ts.add(v);
            }
        }

        /* convert to sorted [] */
        String[] sa = new String[dup.size()];
        dup.toArray(sa);

        return sa;
    }
}

/**
 * .dbf header object
 *
 * @author adam
 *
 */
class DBFHeader extends Object implements Serializable {

    static final long serialVersionUID = -1807390252140128281L;
    /**
     * .dbf header fields (partial)
     */
    int filetype;
    int[] lastupdate;
    int numberofrecords;
    int recordsoffset;
    int recordlength;
    int tableflags;
    /* int codepagemark; */
    /**
     * list of fields/columns in the .dbf header
     */
    ArrayList<DBFField> fields;
    /* TODO: use appropriately for failed constructor */
    boolean isvalid;

    /**
     * constructor for DBFHeader from  a .dbf filename
     * @param filename filename of .dbf file as String
     */
    public DBFHeader(String filename) {

        isvalid = false;
        try {
            int i;

            /* load whole file */
            FileInputStream fis = new FileInputStream(filename);
            FileChannel fc = fis.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate((int) fc.size());
            fc.read(buffer);
            buffer.flip();						//ready to read
            buffer.order(ByteOrder.BIG_ENDIAN);

            filetype = (0xFF & buffer.get());
            lastupdate = new int[3];
            for (i = 0; i < 3; i++) {
                lastupdate[i] = (0xFF & buffer.get());
            }
            numberofrecords = (0xFF & buffer.get()) + 256 * ((0xFF & buffer.get())
                    + 256 * ((0xFF & buffer.get()) + 256 * (0xFF & buffer.get())));
            recordsoffset = (0xFF & buffer.get()) + 256 * (0xFF & buffer.get());
            recordlength = (0xFF & buffer.get()) + 256 * (0xFF & buffer.get());
            for (i = 0; i < 16; i++) {
                buffer.get();
            }
            tableflags = (0xFF & buffer.get());
            /* codepagemark = */ buffer.get();
            buffer.get();
            buffer.get();

            fields = new ArrayList();
            byte nextfsr;
            while ((nextfsr = buffer.get()) != 0x0D) {
                fields.add(new DBFField(nextfsr, buffer));
            }
            /* don't care dbc, skip */

            fis.close();

            isvalid = true;
        } catch (Exception e) {
            System.out.println("loading dbfheader error: " + filename + ": " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * gets field list
     * @return all fields as ArrayList<DBFField>
     */
    public ArrayList<DBFField> getFields() {
        return fields;
    }

    /**
     * gets records offset in .dbf file
     *
     * @return record offset as int
     */
    public int getRecordsOffset() {
        return recordsoffset;
    }

    /**
     * gets the number of records in the .dbf
     * @return number of records as int
     */
    public int getNumberOfRecords() {
        return numberofrecords;
    }

    /**
     * gets the list of column names in column order
     *
     * @return column names as String []
     */
    public String[] getColumnNames() {
        String[] s = new String[fields.size()];
        for (int i = 0; i < s.length; i++) {
            s[i] = fields.get(i).getName();
        }
        return s;
    }

    /**
     * gets the index of a column from a column name
     *
     * case insensitive
     *
     * @param column_name column name as String
     * @return index of column name
     *  -1 for not found
     */
    public int getColumnIdx(String column_name) {
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getName().equalsIgnoreCase(column_name)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * format .dbf header summary
     * @return String
     */
    @Override
    public String toString() {
        if (!isvalid) {
            return "invalid header";
        }

        StringBuffer sb = new StringBuffer();

        sb.append("filetype: ");
        sb.append(String.valueOf(filetype));
        sb.append("\r\nlastupdate: ");
        sb.append(String.valueOf(lastupdate[0]));
        sb.append(" ");
        sb.append(String.valueOf(lastupdate[1]));
        sb.append(" ");
        sb.append(String.valueOf(lastupdate[2]));
        sb.append("\r\nnumberofrecords: ");
        sb.append(String.valueOf(numberofrecords));
        sb.append("\r\nrecordsoffset: ");
        sb.append(String.valueOf(recordsoffset));
        sb.append("\r\nrecordlength: ");
        sb.append(String.valueOf(recordlength));
        sb.append("\r\ntableflags: ");
        sb.append(String.valueOf(tableflags));
        sb.append("\r\nnumber of fields: ");
        sb.append(String.valueOf(fields.size()));

        for (int i = 0; i < fields.size(); i++) {
            sb.append(fields.get(i).toString());
        }

        return sb.toString();
    }
}

/**
 * .dbf header field object
 *
 * @author adam
 *
 */
class DBFField extends Object implements Serializable {

    static final long serialVersionUID = 6130879839715559815L;
    /*
     * .dbf Field records (partial)
     */
    String name;
    char type;
    int displacement;
    int length;
    int decimals;
    int flags;
    byte[] data;		//placeholder for reading byte blocks
	/* don't care autoinc */

    void test() {
    }

    /**
     * constructor for DBFField with first byte separated from
     * rest of the data structure
     *
     * TODO: static and more cleaner
     *
     * @param firstbyte	first byte of .dbf field record as byte
     * @param buffer remaining byes of .dbf field record as ByteBuffer
     */
    public DBFField(byte firstbyte, ByteBuffer buffer) {
        int i;
        byte[] ba = new byte[12];
        ba[0] = firstbyte;
        for (i = 1; i < 11; i++) {
            ba[i] = buffer.get();
        }
        try {
            name = (new String(ba, "US-ASCII")).trim().toUpperCase();
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }

        byte[] ba2 = new byte[1];
        ba2[0] = buffer.get();
        try {
            type = (new String(ba2, "US-ASCII")).charAt(0);
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }

        displacement = (0xFF & buffer.get()) + 256 * ((0xFF & buffer.get())
                + 256 * ((0xFF & buffer.get()) + 256 * (0xFF & buffer.get())));

        length = (0xFF & buffer.get());
        data = new byte[length];

        decimals = (0xFF & buffer.get());
        flags = (0xFF & buffer.get());

        /* skip over end */
        for (i = 0; i < 13; i++) {
            buffer.get();
        }
    }

    /**
     * gets field name
     * @return field name as String
     */
    public String getName() {
        return name;
    }

    /**
     * gets field type
     * @return field type as char
     */
    public char getType() {
        return type;
    }

    /**
     * gets data block
     *
     * for use in .read(getDataBlock()) like functions
     * when reading records
     *
     * @return
     */
    public byte[] getDataBlock() {
        return data;
    }

    /**
     * format Field summary
     * @return String
     */
    @Override
    public String toString() {
        return "name: " + name + " type: " + type + " displacement: " + displacement + " length: " + length + "\r\n";
    }
}

/**
 * collection of .dbf records
 *
 * @author adam
 *
 */
class DBFRecords extends Object implements Serializable {

    static final long serialVersionUID = -2450196133919654852L;
    /**
     * list of DBFRecord
     */
    ArrayList<DBFRecord> records;
    /* TODO: use appropriately for failed constructor */
    boolean isvalid;

    /**
     * constructor for collection of DBFRecord from a DBFHeader and
     * .dbf filename
     * @param filename .dbf file as String
     * @param header dbf header from filename as DBFHeader
     */
    public DBFRecords(String filename, DBFHeader header) {
        /* init */
        records = new ArrayList();
        isvalid = false;

        try {
            /* load all records */
            FileInputStream fis = new FileInputStream(filename);
            FileChannel fc = fis.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate((int) fc.size() - header.getRecordsOffset());
            fc.read(buffer, header.getRecordsOffset());
            buffer.flip();			//prepare for reading



            /* add each record from byte buffer, provide fields list */
            int i = 0;
            ArrayList<DBFField> fields = header.getFields();
            while (i < header.getNumberOfRecords() && buffer.hasRemaining()) {
                records.add(new DBFRecord(buffer, fields));
                i++;
            }

            fis.close();

            isvalid = true;
        } catch (Exception e) {
            System.out.println("loading records error: " + filename + ": " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * gets a value at a row and column
     * @param row row number as int
     * @param column column number as int
     * @return value as String or "" if invalid input or column format
     */
    public String getValue(int row, int column) {
        if (row >= 0 && row < records.size()) {
            return records.get(row).getValue(column);
        }
        return "";
    }

    /**
     * format all Records
     * @return String
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (DBFRecord r : records) {
            sb.append(r.toString());
            sb.append("\r\n");
        }
        return sb.toString();
    }
}

/**
 * .dbf record object
 *
 * @author adam
 *
 */
class DBFRecord extends Object implements Serializable {

    static final long serialVersionUID = 584190536943295242L;
    /**
     * String [] representing the current row record in a .dbf
     */
    String[] record;
    /**
     * deletion flag
     *
     * TODO: make use of this
     */
    int deletionflag;

    /**
     * constructs new DBFRecord from bytebuffer and list of fields
     *
     * TODO: implement more than C and N types
     *
     * @param buffer byte buffer for reading fields as ByteBuffer
     * @param fields fields in this record as ArrayList<DBFField>
     */
    public DBFRecord(ByteBuffer buffer, ArrayList<DBFField> fields) {
        deletionflag = (0xFF & buffer.get());
        record = new String[fields.size()];

        /* iterate through each record to fill */
        for (int i = 0; i < record.length; i++) {
            DBFField f = fields.get(i);
            byte[] data = f.getDataBlock();		//get pre-built byte[]
            buffer.get(data);
            try {
                switch (f.getType()) {
                    case 'C':			//string
                        record[i] = (new String(data, "US-ASCII")).trim();
                        break;
                    case 'N':			//number as string
                        record[i] = (new String(data, "US-ASCII")).trim();
                        break;
                }
            } catch (Exception e) {
                //TODO: is this necessary?
            }
        }
    }

    /**
     * gets the value in this record at a column
     * @param column column index for value to return
     * @return value as String
     */
    public String getValue(int column) {
        if (column < 0 || column >= record.length) {
            return "";
        } else {
            return record[column];
        }
    }

    /**
     * format this record
     * @return String
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (String s : record) {
            sb.append(s);
            sb.append(", ");
        }
        return sb.toString();
    }
}

/**
 * for balancing shape files with large numbers of shapes.
 *
 * creates a grid with each cell listing shapes that overlap with it.
 *
 * TODO: not square dimensions
 *
 * @author adam
 *
 */
class ShapesReference extends Object implements Serializable {

    /**
     * grid with shape index lists as cells
     */
    ArrayList<Integer>[][] mask;
    /**
     * width/height of mask
     */
    int mask_dimension;
    /**
     * intersection multiplier for longitude
     */
    double mask_long_multiplier;
    /**
     * intersection multiplier for latitude
     */
    double mask_lat_multiplier;
    /**
     * mask bounding box, see SimpleRegion boundingbox.
     */
    double[][] boundingbox_all;
    /**
     * reference to all shapes
     */
    ShapeRecords sr;

    /**
     * constructor
     *
     * @param sr_ all shapes for this mask as ShapeRecords
     */
    public ShapesReference(ShapeRecords sr_) {

        sr = sr_;
        boundingbox_all = new double[2][2];

        mask_dimension = (int) Math.sqrt(sr.getNumberOfRecords());

        /* define minimum mask size */
        if (mask_dimension < 3) {
            mask = null;
            return;
        }

        /* create mask */
        mask = new ArrayList[mask_dimension][mask_dimension];

        /* update boundingbox_all */
        boolean first_time = true;
        for (ComplexRegion s : sr_.getRegions()) {
            double[][] bb = s.getBoundingBox();
            if (first_time || boundingbox_all[0][0] > bb[0][0]) {
                boundingbox_all[0][0] = bb[0][0];
            }
            if (first_time || boundingbox_all[1][0] < bb[1][0]) {
                boundingbox_all[1][0] = bb[1][0];
            }
            if (first_time || boundingbox_all[0][1] > bb[0][1]) {
                boundingbox_all[0][1] = bb[0][1];
            }
            if (first_time || boundingbox_all[1][1] < bb[1][1]) {
                boundingbox_all[1][1] = bb[1][1];
            }
            first_time = false;
        }

        System.out.println("boundingbox:" + boundingbox_all[0][0] + " " + boundingbox_all[0][1]
                + " " + boundingbox_all[1][0] + " " + boundingbox_all[1][1]);

        /* get intersecting cells and add */
        ArrayList<ComplexRegion> sra = sr.getRegions();
        for (int j = 0; j < sra.size(); j++) {
            ComplexRegion s = sra.get(j);
            int[][] map = s.getOverlapGridCells_Box(
                    boundingbox_all[0][0], boundingbox_all[0][1], boundingbox_all[1][0], boundingbox_all[1][1], mask_dimension, mask_dimension, s.getBoundingBox(), null, false);

            for (int i = 0; i < map.length; i++) {
                if (mask[map[i][0]][map[i][1]] == null) {
                    mask[map[i][0]][map[i][1]] = new ArrayList<Integer>();
                }
                mask[map[i][0]][map[i][1]].add(j);
            }
        }

        /* calculate multipliers */
        mask_long_multiplier = mask_dimension / (double) (boundingbox_all[1][0] - boundingbox_all[0][0]);
        mask_lat_multiplier = mask_dimension / (double) (boundingbox_all[1][1] - boundingbox_all[0][1]);
    }

    /**
     * performs intersection on one point
     *
     * @param longitude longitude of point to intersect as double
     * @param latitude latitude of point to intersect as double
     * @return shape index if intersection found, or -1 for no intersection
     */
    public int intersection(double longitude, double latitude) {
        ArrayList<ComplexRegion> sra = sr.getRegions();

        int i;

        /* test for mask */
        if (mask != null) {
            /* apply multipliers */
            int long1 = (int) Math.floor((longitude - boundingbox_all[0][0]) * mask_long_multiplier);
            int lat1 = (int) Math.floor((latitude - boundingbox_all[0][1]) * mask_lat_multiplier);
            /* check is within mask bounds */
            if (long1 >= 0 && long1 < mask[0].length
                    && lat1 >= 0 && lat1 < mask.length
                    && mask[long1][lat1] != null) {

                /* get list of shapes to check at this mask cell */
                ArrayList<Integer> ali = mask[long1][lat1];

                /* check each potential cell */
                for (i = 0; i < ali.size(); i++) {
                    if (sra.get(ali.get(i).intValue()).isWithin(longitude, latitude)) {
                        return ali.get(i).intValue();
                    }
                }
            }
        } else {
            /* no mask, check all shapes */
            for (i = 0; i < sra.size(); i++) {
                if (sra.get(i).isWithin(longitude, latitude)) {
                    return i;
                }
            }
        }
        return -1;
    }
}

class IntersectionThread implements Runnable {

    Thread t;
    ShapesReference shapesreference;
    PointPos[] points;
    LinkedBlockingQueue<Integer> lbq;
    int step;
    int[] target;
    CountDownLatch cdl;

    public IntersectionThread(ShapesReference shapesreference_, PointPos[] points_, LinkedBlockingQueue<Integer> lbq_, int step_, int[] target_, CountDownLatch cdl_) {
        t = new Thread(this);
        t.setPriority(Thread.MIN_PRIORITY);

        points = points_;
        shapesreference = shapesreference_;
        lbq = lbq_;
        step = step_;

        target = target_;

        cdl = cdl_;

        t.start();
    }

    @Override
    public void run() {

        int i, idx;

        /* get next batch */
        Integer start;
        try {
            while (true) {
                start = lbq.take();

                //System.out.println("A*: " + start.intValue());

                int end = start.intValue() + step;
                if (end > target.length) {
                    end = target.length;
                }
                int sv = start.intValue();
                for (i = sv; i < end; i++) {
                    if (i > sv && points[i - 1].x == points[i].x && points[i - 1].y == points[i].y) {
                        target[points[i].pos] = target[points[i - 1].pos];
                    } else if ((idx = shapesreference.intersection(points[i].x, points[i].y)) >= 0) {
                        target[points[i].pos] = idx;
                    } else {
                        target[points[i].pos] = -1;
                    }
                }

                cdl.countDown();
            }
        } catch (InterruptedException ie) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isAlive() {
        return t.isAlive();
    }

    void interrupt() {
        t.interrupt();
    }
}

class PointPos {

    public double x, y;
    public int pos;

    PointPos(double x, double y, int pos) {
        this.x = x;
        this.y = y;
        this.pos = pos;
    }
}
