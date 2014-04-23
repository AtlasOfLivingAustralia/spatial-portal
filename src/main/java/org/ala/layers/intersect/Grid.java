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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;

import org.apache.log4j.Logger;

/**
 * Grid.java
 * Created on June 24, 2005, 4:12 PM
 *
 * @author Robert Hijmans, rhijmans@berkeley.edu
 *         <p/>
 *         Updated 15/2/2010, Adam
 *         <p/>
 *         Interface for .gri/.grd files for now
 */
public class Grid { //  implements Serializable

    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
    final static int maxGridsLoaded = 1;
    static ArrayList<Grid> all_grids = new ArrayList<Grid>();
    final double noDataValueDefault = -3.4E38;
    public Boolean byteorderLSB = true; // true if file is LSB (Intel)
    public int ncols, nrows;
    public double nodatavalue;
    public Boolean valid;
    public double[] values;
    public double xmin, xmax, ymin, ymax;
    public double xres, yres;
    public String datatype;
    // properties
    public double minval, maxval;
    byte nbytes;
    public String filename;
    float[] grid_data = null;
    public String units;

    /**
     * loads grd for gri file reference
     *
     * @param fname full path and file name without file extension
     *              of .gri and .grd files to open
     */
    public Grid(String fname) { // construct Grid from file
        filename = fname;
        File grifile = new File(filename + ".gri");
        if (!grifile.exists()) {
            grifile = new File(filename + ".GRI");
        }
        File grdfile = new File(filename + ".grd");
        if (!grdfile.exists()) {
            grdfile = new File(filename + ".GRD");
        }
        if (grdfile.exists() && grifile.exists()) {
            readgrd(filename);

            //update xres/yres when xres == 1
            if (xres == 1) {
                xres = (xmax - xmin) / nrows;
                yres = (ymax - ymin) / ncols;
            }
        } else {
            logger.error("cannot find GRID: " + fname);
        }
    }

    Grid(String fname, boolean keepAvailable) { // construct Grid from file
        filename = fname;
        File grifile = new File(filename + ".gri");
        if (!grifile.exists()) {
            grifile = new File(filename + ".GRI");
        }
        File grdfile = new File(filename + ".grd");
        if (!grdfile.exists()) {
            grdfile = new File(filename + ".GRD");
        }
        if (grdfile.exists() && grifile.exists()) {
            readgrd(filename);

            //update xres/yres when xres == 1
            if (xres == 1) {
                xres = (xmax - xmin) / nrows;
                yres = (ymax - ymin) / ncols;
            }
        } else {
            logger.error("Error constructing grid from file: " + fname);
        }

        if (keepAvailable) {
            Grid.addGrid(this);
        }
    }

    static void removeAvailable() {
        synchronized (all_grids) {
            while (all_grids.size() > 0) {
                all_grids.remove(0);
            }
        }
    }

    static void addGrid(Grid g) {
        synchronized (all_grids) {
            if (all_grids.size() == maxGridsLoaded) {
                all_grids.remove(0);
            }
            all_grids.add(g);
        }
    }

    static public Grid getGrid(String filename) {
        synchronized (all_grids) {
            for (int i = 0; i < all_grids.size(); i++) {
                if (filename.equalsIgnoreCase(all_grids.get(i).filename)) {
                    //get and add to the end of grid list
                    Grid g = all_grids.get(i);
                    all_grids.remove(i);
                    all_grids.add(g);
                    return g;
                }
            }
            return new Grid(filename, true);
        }
    }

    static public Grid getLoadedGrid(String filename) {
        synchronized (all_grids) {
            for (int i = 0; i < all_grids.size(); i++) {
                if (filename.equalsIgnoreCase(all_grids.get(i).filename)) {
                    //get and add to the end of grid list
                    Grid g = all_grids.get(i);
                    all_grids.remove(i);
                    all_grids.add(g);
                    return g;
                }
            }
            return null;
        }
    }

    public static Grid getGridStandardized(String filename) {
        synchronized (all_grids) {
            for (int i = 0; i < all_grids.size(); i++) {
                if (filename.equalsIgnoreCase(all_grids.get(i).filename)) {
                    //get and add to the end of grid list
                    Grid g = all_grids.get(i);
                    all_grids.remove(i);
                    all_grids.add(g);
                    return g;
                }
            }

            Grid g = new Grid(filename, true);
            float[] d = g.getGrid();
            double range = g.maxval - g.minval;
            for (int i = 0; i < d.length; i++) {
                d[i] = (float) ((d[i] - g.minval) / range);
            }
            return g;
        }
    }

    //transform to file position
    public int getcellnumber(double x, double y) {
        if (x < xmin || x > xmax || y < ymin || y > ymax) //handle invalid inputs
        {
            return -1;
        }

        int col = (int) ((x - xmin) / xres);
        int row = this.nrows - 1 - (int) ((y - ymin) / yres);

        //limit each to 0 and ncols-1/nrows-1
        if (col < 0) {
            col = 0;
        }
        if (row < 0) {
            row = 0;
        }
        if (col >= ncols) {
            col = ncols - 1;
        }
        if (row >= nrows) {
            row = nrows - 1;
        }
        return (row * ncols + col);
    }

    private void setdatatype(String s) {
        s = s.toUpperCase();

        // Expected from grd file
        if (s.equals("INT1BYTE")) {
            datatype = "BYTE";
        } else if (s.equals("INT2BYTES")) {
            datatype = "SHORT";
        } else if (s.equals("INT4BYTES")) {
            datatype = "INT";
        } else if (s.equals("INT8BYTES")) {
            datatype = "LONG";
        } else if (s.equals("FLT4BYTES")) {
            datatype = "FLOAT";
        } else if (s.equals("FLT8BYTES")) {
            datatype = "DOUBLE";
        } // shorthand for same
        else if (s.equals("INT1B") || s.equals("BYTE")) {
            datatype = "BYTE";
        } else if (s.equals("INT1U") || s.equals("UBYTE")) {
            datatype = "UBYTE";
        } else if (s.equals("INT2B") || s.equals("INT16") || s.equals("INT2S")) {
            datatype = "SHORT";
        } else if (s.equals("INT4B")) {
            datatype = "INT";
        } else if (s.equals("INT8B") || s.equals("INT32")) {
            datatype = "LONG";
        } else if (s.equals("FLT4B") || s.equals("FLOAT32") || s.equals("FLT4S")) {
            datatype = "FLOAT";
        } else if (s.equals("FLT8B")) {
            datatype = "DOUBLE";
        } // if you rather use Java keywords...
        else if (s.equals("BYTE")) {
            datatype = "BYTE";
        } else if (s.equals("SHORT")) {
            datatype = "SHORT";
        } else if (s.equals("INT")) {
            datatype = "INT";
        } else if (s.equals("LONG")) {
            datatype = "LONG";
        } else if (s.equals("FLOAT")) {
            datatype = "FLOAT";
        } else if (s.equals("DOUBLE")) {
            datatype = "DOUBLE";
        } // some backwards compatibility
        else if (s.equals("INTEGER")) {
            datatype = "INT";
        } else if (s.equals("SMALLINT")) {
            datatype = "INT";
        } else if (s.equals("SINGLE")) {
            datatype = "FLOAT";
        } else if (s.equals("REAL")) {
            datatype = "FLOAT";
        } else {
            logger.error("GRID unknown type: " + s);
            datatype = "UNKNOWN";
        }

        if (datatype.equals("BYTE") || datatype.equals("UBYTE")) {
            nbytes = 1;
        } else if (datatype.equals("SHORT")) {
            nbytes = 2;
        } else if (datatype.equals("INT")) {
            nbytes = 4;
        } else if (datatype.equals("LONG")) {
            nbytes = 8;
        } else if (datatype.equals("SINGLE")) {
            nbytes = 4;
        } else if (datatype.equals("DOUBLE")) {
            nbytes = 8;
        } else {
            nbytes = 0;
        }
    }

    private void readgrd(String filename) {
        IniReader ir = null;
        if ((new File(filename + ".grd")).exists()) {
            ir = new IniReader(filename + ".grd");
        } else {
            ir = new IniReader(filename + ".GRD");
        }

        setdatatype(ir.getStringValue("Data", "DataType"));
        maxval = (float) ir.getDoubleValue("Data", "MaxValue");
        minval = (float) ir.getDoubleValue("Data", "MinValue");
        ncols = ir.getIntegerValue("GeoReference", "Columns");
        nrows = ir.getIntegerValue("GeoReference", "Rows");
        xmin = ir.getDoubleValue("GeoReference", "MinX");
        ymin = ir.getDoubleValue("GeoReference", "MinY");
        xmax = ir.getDoubleValue("GeoReference", "MaxX");
        ymax = ir.getDoubleValue("GeoReference", "MaxY");
        xres = ir.getDoubleValue("GeoReference", "ResolutionX");
        yres = ir.getDoubleValue("GeoReference", "ResolutionY");
        if (ir.valueExists("Data", "NoDataValue")) {
            nodatavalue = ir.getDoubleValue("Data", "NoDataValue");
        } else {
            nodatavalue = Double.NaN;
        }

        String s = ir.getStringValue("Data", "ByteOrder");

        byteorderLSB = true;
        if (s != null && s.length() > 0) {
            if (s.equals("MSB")) {
                byteorderLSB = false;
            }// default is windows (LSB), not linux or Java (MSB)
        }

        units = ir.getStringValue("Data", "Units");
    }

    public float[] getGrid() {
        int maxArrayLength = Integer.MAX_VALUE - 10;

        if (grid_data != null) {
            return grid_data;
        }
        int length = nrows * ncols;

        float[] ret = new float[length];

        RandomAccessFile afile;
        File f2 = new File(filename + ".GRI");

        try { //read of random access file can throw an exception
            if (!f2.exists()) {
                afile = new RandomAccessFile(filename + ".gri", "r");
            } else {
                afile = new RandomAccessFile(filename + ".GRI", "r");
            }

            byte[] b = new byte[(int) Math.min(afile.length(), maxArrayLength)];

            int i = 0;
            int max = 0;
            int len;
            while ((len = afile.read(b)) > 0) {
                ByteBuffer bb = ByteBuffer.wrap(b);

                if (byteorderLSB) {
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                }

                if (datatype.equalsIgnoreCase("UBYTE")) {
                    max += len;
                    max = Math.min(max, ret.length);
                    for (; i < max; i++) {
                        ret[i] = bb.get();
                        if (ret[i] < 0) {
                            ret[i] += 256;
                        }
                    }
                } else if (datatype.equalsIgnoreCase("BYTE")) {
                    max += len;
                    max = Math.min(max, ret.length);
                    for (; i < max; i++) {
                        ret[i] = bb.get();
                    }
                } else if (datatype.equalsIgnoreCase("SHORT")) {
                    max += len / 2;
                    max = Math.min(max, ret.length);
                    for (; i < max; i++) {
                        ret[i] = bb.getShort();
                    }
                } else if (datatype.equalsIgnoreCase("INT")) {
                    max += len / 4;
                    max = Math.min(max, ret.length);
                    for (; i < max; i++) {
                        ret[i] = bb.getInt();
                    }
                } else if (datatype.equalsIgnoreCase("LONG")) {
                    max += len / 8;
                    max = Math.min(max, ret.length);
                    for (; i < max; i++) {
                        ret[i] = bb.getLong();
                    }
                } else if (datatype.equalsIgnoreCase("FLOAT")) {
                    max += len / 4;
                    max = Math.min(max, ret.length);
                    for (; i < max; i++) {
                        ret[i] = bb.getFloat();
                    }
                } else if (datatype.equalsIgnoreCase("DOUBLE")) {
                    max += len / 8;
                    max = Math.min(max, ret.length);
                    for (; i < max; i++) {
                        ret[i] = (float) bb.getDouble();
                    }
                } else {
                    // / should not happen; catch anyway...
                    max += len / 4;
                    for (; i < max; i++) {
                        ret[i] = Float.NaN;
                    }
                }
            }

            //replace not a number
            for (i = 0; i < length; i++) {
                if ((float) ret[i] == (float) nodatavalue) {
                    ret[i] = Float.NaN;
                }
            }

            afile.close();
        } catch (Exception e) {
            logger.error("An error has occurred - probably a file error", e);
        }
        grid_data = ret;
        return ret;
    }

    /**
     * for DomainGenerator
     * <p/>
     * writes out a list of double (same as getGrid() returns) to a file
     * <p/>
     * byteorderlsb
     * data type, FLOAT
     *
     * @param newfilename
     * @param dfiltered
     */
    public void writeGrid(String newfilename, int[] dfiltered, double xmin, double ymin, double xmax, double ymax, double xres, double yres, int nrows, int ncols) {
        int size, i, length = dfiltered.length;
        double maxvalue = Integer.MAX_VALUE * -1;
        double minvalue = Integer.MAX_VALUE;

        //write data as whole file
        RandomAccessFile afile;
        try { //read of random access file can throw an exception
            afile = new RandomAccessFile(newfilename + ".gri", "rw");

            size = 4;
            byte[] b = new byte[size * length];
            ByteBuffer bb = ByteBuffer.wrap(b);

            if (byteorderLSB) {
                bb.order(ByteOrder.LITTLE_ENDIAN);
            } else {
                bb.order(ByteOrder.BIG_ENDIAN);
            }
            for (i = 0; i < length; i++) {
                bb.putInt(dfiltered[i]);
            }

            afile.write(b);

            afile.close();
        } catch (Exception e) {
            logger.error("error writing grid file", e);
        }


        writeHeader(newfilename, xmin, ymin, xmin + xres * ncols, ymin + yres * nrows, xres, yres, nrows, ncols, minvalue, maxvalue, "INT4BYTES", "-9999");

    }

    /**
     * for grid cutter
     * <p/>
     * writes out a list of double (same as getGrid() returns) to a file
     * <p/>
     * byteorderlsb
     * data type, FLOAT
     *
     * @param newfilename
     * @param dfiltered
     */
    public void writeGrid(String newfilename, double[] dfiltered, double xmin, double ymin, double xmax, double ymax, double xres, double yres, int nrows, int ncols) {
        int size, i, length = dfiltered.length;
        double maxvalue = Double.MAX_VALUE * -1;
        double minvalue = Double.MAX_VALUE;

        //write data as whole file
        RandomAccessFile afile;
        try { //read of random access file can throw an exception
            afile = new RandomAccessFile(newfilename + ".gri", "rw");

            size = 4;
            byte[] b = new byte[size * length];
            ByteBuffer bb = ByteBuffer.wrap(b);

            if (byteorderLSB) {
                bb.order(ByteOrder.LITTLE_ENDIAN);
            } else {
                bb.order(ByteOrder.BIG_ENDIAN);
            }
            for (i = 0; i < length; i++) {
                if (Double.isNaN(dfiltered[i])) {
                    bb.putFloat((float) noDataValueDefault);
                } else {
                    if (minvalue > dfiltered[i]) {
                        minvalue = dfiltered[i];
                    }
                    if (maxvalue < dfiltered[i]) {
                        maxvalue = dfiltered[i];
                    }
                    bb.putFloat((float) dfiltered[i]);
                }
            }

            afile.write(b);

            afile.close();
        } catch (Exception e) {
            logger.error("error writing grid file", e);
        }

        writeHeader(newfilename, xmin, ymin, xmin + xres * ncols, ymin + yres * nrows, xres, yres, nrows, ncols, minvalue, maxvalue, "FLT4BYTES", String.valueOf(noDataValueDefault));
    }

    public void writeGrid(String newfilename, float[] dfiltered, double xmin, double ymin, double xmax, double ymax, double xres, double yres, int nrows, int ncols) {
        int size, i, length = dfiltered.length;
        double maxvalue = Double.MAX_VALUE * -1;
        double minvalue = Double.MAX_VALUE;

        //write data as whole file
        RandomAccessFile afile;
        try { //read of random access file can throw an exception
            afile = new RandomAccessFile(newfilename + ".gri", "rw");

            size = 4;
            byte[] b = new byte[size * length];
            ByteBuffer bb = ByteBuffer.wrap(b);

            if (byteorderLSB) {
                bb.order(ByteOrder.LITTLE_ENDIAN);
            } else {
                bb.order(ByteOrder.BIG_ENDIAN);
            }
            for (i = 0; i < length; i++) {
                if (Double.isNaN(dfiltered[i])) {
                    bb.putFloat((float) noDataValueDefault);
                } else {
                    if (minvalue > dfiltered[i]) {
                        minvalue = dfiltered[i];
                    }
                    if (maxvalue < dfiltered[i]) {
                        maxvalue = dfiltered[i];
                    }
                    bb.putFloat((float) dfiltered[i]);
                }
            }

            afile.write(b);

            afile.close();
        } catch (Exception e) {
            logger.error("error writing grid file", e);
        }

        writeHeader(newfilename, xmin, ymin, xmin + xres * ncols, ymin + yres * nrows, xres, yres, nrows, ncols, minvalue, maxvalue, "FLT4BYTES", String.valueOf(noDataValueDefault));

    }

    public void writeHeader(String newfilename, double xmin, double ymin, double xmax, double ymax, double xres, double yres, int nrows, int ncols, double minvalue, double maxvalue) {
        writeHeader(newfilename, xmin, ymin, xmax, ymax, xres, yres, nrows, ncols, minvalue, maxvalue, "FLT4BYTES", String.valueOf(noDataValueDefault));
    }

    public void writeHeader(String newfilename, double xmin, double ymin, double xmax, double ymax, double xres, double yres, int nrows, int ncols, double minvalue, double maxvalue, String datatype, String nodata) {
        try {
            FileWriter fw = new FileWriter(newfilename + ".grd");

            fw.append("[General]");
            fw.append("\r\n").append("Title=").append(newfilename);
            fw.append("\r\n").append("[GeoReference]");
            fw.append("\r\n").append("Projection=GEOGRAPHIC");
            fw.append("\r\n").append("Datum=WGS84");
            fw.append("\r\n").append("Mapunits=DEGREES");
            fw.append("\r\n").append("Columns=").append(String.valueOf(ncols));
            fw.append("\r\n").append("Rows=").append(String.valueOf(nrows));
            fw.append("\r\n").append("MinX=").append(String.format("%.2f", xmin));
            fw.append("\r\n").append("MaxX=").append(String.format("%.2f", xmax));
            fw.append("\r\n").append("MinY=").append(String.format("%.2f", ymin));
            fw.append("\r\n").append("MaxY=").append(String.format("%.2f", ymax));
            fw.append("\r\n").append("ResolutionX=").append(String.valueOf(xres));
            fw.append("\r\n").append("ResolutionY=").append(String.valueOf(yres));
            fw.append("\r\n").append("[Data]");
            fw.append("\r\n").append("DataType=" + datatype);
            fw.append("\r\n").append("MinValue=").append(String.valueOf(minvalue));
            fw.append("\r\n").append("MaxValue=").append(String.valueOf(maxvalue));
            fw.append("\r\n").append("NoDataValue=").append(nodata);
            fw.append("\r\n").append("Transparent=0");
            fw.flush();
            fw.close();
        } catch (Exception e) {
            logger.error("error writing grid file header", e);

        }
    }

    /**
     * do get values of grid for provided points.
     * <p/>
     * loads whole grid file as double[] in process
     *
     * @param points
     * @return
     */
    public float[] getValues2(double[][] points) {
        if (points == null || points.length == 0) {
            return null;
        }

        //init output structure
        float[] ret = new float[points.length];

        //load whole grid
        float[] grid = getGrid();
        int glen = grid.length;
        int length = points.length;
        int i, pos;

        //points loop
        for (i = 0; i < length; i++) {
            pos = getcellnumber(points[i][0], points[i][1]);
            if (pos >= 0 && pos < glen) {
                ret[i] = grid[pos];
            } else {
                ret[i] = Float.NaN;
            }
        }

        return ret;
    }

    float[] getGrid(double xmin, double ymin, double xmax, double ymax) {
        //expects largest y at the top
        //expects input ranges inside of grid ranges

        int width = (int) ((xmax - xmin) / xres);
        int height = (int) ((ymax - ymin) / yres);
        int startx = (int) ((xmin - this.xmin) / xres);
        int endx = startx + width;
        int starty = (int) ((ymin - this.ymin) / yres);
        //int endy = starty + height;

        int length = width * height;

        float[] ret = new float[length];
        int pos = 0;

        int i;
        RandomAccessFile afile;
        File f2 = new File(filename + ".GRI");

        int size = 4;
        if (datatype.equals("BYTE") || datatype.equals("UBYTE")) {
            size = 1;
        } else if (datatype.equals("SHORT")) {
            size = 2;
        } else if (datatype.equals("INT")) {
            size = 4;
        } else if (datatype.equals("LONG")) {
            size = 8;
        } else if (datatype.equals("FLOAT")) {
            size = 4;
        } else if (datatype.equals("DOUBLE")) {
            size = 8;
        }

        try { //read of random access file can throw an exception
            if (!f2.exists()) {
                afile = new RandomAccessFile(filename + ".gri", "r");
            } else {
                afile = new RandomAccessFile(filename + ".GRI", "r");
            }

            //seek to first raster
            afile.seek(this.ncols * starty * size);

            //read relevant rasters
            int readSize = this.ncols * height * size;
            int readLen = this.ncols * height;
            byte[] b = new byte[readSize];
            afile.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);
            afile.close();

            if (byteorderLSB) {
                bb.order(ByteOrder.LITTLE_ENDIAN);
            }

            if (datatype.equalsIgnoreCase("BYTE")) {
                for (i = 0; i < readLen; i++) {
                    int x = i % this.ncols;
                    if (x < startx || x >= endx) {
                        bb.get();
                    } else {
                        ret[pos++] = bb.get();
                    }
                }
            } else if (datatype.equalsIgnoreCase("UBYTE")) {
                for (i = 0; i < readLen; i++) {
                    int x = i % this.ncols;
                    if (x < startx || x >= endx) {
                        bb.get();
                    } else {
                        ret[pos] = bb.get();
                        if (ret[pos] < 0) {
                            ret[pos] += 256;
                        }
                        pos++;
                    }
                }
            } else if (datatype.equalsIgnoreCase("SHORT")) {
                for (i = 0; i < readLen; i++) {
                    int x = i % this.ncols;
                    if (x < startx || x >= endx) {
                        bb.getShort();
                    } else {
                        ret[pos++] = bb.getShort();
                    }
                }
            } else if (datatype.equalsIgnoreCase("INT")) {
                for (i = 0; i < readLen; i++) {
                    int x = i % this.ncols;
                    if (x < startx || x >= endx) {
                        bb.getInt();
                    } else {
                        ret[pos++] = bb.getInt();
                    }
                }
            } else if (datatype.equalsIgnoreCase("LONG")) {
                for (i = 0; i < readLen; i++) {
                    int x = i % this.ncols;
                    if (x < startx || x >= endx) {
                        bb.getLong();
                    } else {
                        ret[pos++] = bb.getLong();
                    }
                }
            } else if (datatype.equalsIgnoreCase("FLOAT")) {
                for (i = 0; i < readLen; i++) {
                    int x = i % this.ncols;
                    if (x < startx || x >= endx) {
                        bb.getFloat();
                    } else {
                        ret[pos++] = bb.getFloat();
                    }
                }
            } else if (datatype.equalsIgnoreCase("DOUBLE")) {
                for (i = 0; i < readLen; i++) {
                    int x = i % this.ncols;
                    if (x < startx || x >= endx) {
                        bb.getDouble();
                    } else {
                        ret[pos++] = (float) bb.getDouble();
                    }
                }
            } else {
                // / should not happen; catch anyway...
                for (i = 0; i < length; i++) {
                    ret[i] = Float.NaN;
                }
            }
            //replace not a number
            for (i = 0; i < length; i++) {
                if ((float) ret[i] == (float) nodatavalue) {
                    ret[i] = Float.NaN;
                }
            }
        } catch (Exception e) {
            logger.error("GRID: " + e.toString(), e);
        }
        grid_data = ret;
        return ret;
    }

    public void printMinMax() {
        float min = Float.MAX_VALUE;
        float max = -1 * Float.MAX_VALUE;
        float[] data = this.getGrid();
        int numMissing = 0;
        for (float d : data) {
            if (Float.isNaN(d)) {
                numMissing++;
            }
            if (d < min) {
                min = d;
            }
            if (d > max) {
                max = d;
            }
        }
        if (min != this.minval || max != this.maxval) {
            logger.error(this.filename + " ERR header(" + this.minval + " " + this.maxval + ") actual(" + min + " " + max + ") number missing(" + numMissing + " of " + data.length + ")");
        } else {
            logger.error(this.filename + " OK header(" + this.minval + " " + this.maxval + ") number missing(" + numMissing + " of " + data.length + ")");
        }
    }

    /**
     * @param points input array for longitude and latitude
     *               double[number_of_points][2]
     * @return array of .gri file values corresponding to the
     * points provided
     */
    public float[] getValues(double[][] points) {

        //confirm inputs since they come from somewhere else
        if (points == null || points.length == 0) {
            return null;
        }

        //use preloaded grid data if available
        Grid g = Grid.getLoadedGrid(filename);
        if (g != null) {
            return g.getValues2(points);
        }

        float[] ret = new float[points.length];

        int length = points.length;
        int size, i, pos;
        byte[] b;
        RandomAccessFile afile;
        File f2 = new File(filename + ".GRI");

        try { //read of random access file can throw an exception
            if (!f2.exists()) {
                afile = new RandomAccessFile(filename + ".gri", "r");
            } else {
                afile = new RandomAccessFile(filename + ".GRI", "r");
            }

            if (datatype.equalsIgnoreCase("BYTE")) {
                size = 1;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    pos = getcellnumber(points[i][0], points[i][1]);
                    if (pos >= 0) {
                        afile.seek(pos * size);
                        ret[i] = afile.readByte();
                    } else {
                        ret[i] = Float.NaN;
                    }
                }
            } else if (datatype.equalsIgnoreCase("UBYTE")) {
                size = 1;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    pos = getcellnumber(points[i][0], points[i][1]);
                    if (pos >= 0) {
                        afile.seek(pos * size);
                        ret[i] = afile.readByte();
                        if (ret[i] < 0) {
                            ret[i] += 256;
                        }
                    } else {
                        ret[i] = Float.NaN;
                    }
                }
            } else if (datatype.equalsIgnoreCase("SHORT")) {
                size = 2;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    pos = getcellnumber(points[i][0], points[i][1]);
                    if (pos >= 0) {
                        afile.seek(pos * size);
                        afile.read(b);
                        if (byteorderLSB) {
                            ret[i] = (short) (((0xFF & b[1]) << 8) | (b[0] & 0xFF));
                        } else {
                            ret[i] = (short) (((0xFF & b[0]) << 8) | (b[1] & 0xFF));
                        }
                        //ret[i] = afile.readShort();
                    } else {
                        ret[i] = Float.NaN;
                    }
                }
            } else if (datatype.equalsIgnoreCase("INT")) {
                size = 4;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    pos = getcellnumber(points[i][0], points[i][1]);
                    if (pos >= 0) {
                        afile.seek(pos * size);
                        afile.read(b);
                        if (byteorderLSB) {
                            ret[i] = ((0xFF & b[3]) << 24) | ((0xFF & b[2]) << 16) + ((0xFF & b[1]) << 8) + (b[0] & 0xFF);
                        } else {
                            ret[i] = ((0xFF & b[0]) << 24) | ((0xFF & b[1]) << 16) + ((0xFF & b[2]) << 8) + ((0xFF & b[3]) & 0xFF);
                        }
                        //ret[i] = afile.readInt();
                    } else {
                        ret[i] = Float.NaN;
                    }
                }
            } else if (datatype.equalsIgnoreCase("LONG")) {
                size = 8;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    pos = getcellnumber(points[i][0], points[i][1]);
                    if (pos >= 0) {
                        afile.seek(pos * size);
                        afile.read(b);
                        if (byteorderLSB) {
                            ret[i] = ((long) (0xFF & b[7]) << 56) + ((long) (0xFF & b[6]) << 48)
                                    + ((long) (0xFF & b[5]) << 40) + ((long) (0xFF & b[4]) << 32)
                                    + ((long) (0xFF & b[3]) << 24) + ((long) (0xFF & b[2]) << 16)
                                    + ((long) (0xFF & b[1]) << 8) + (0xFF & b[0]);
                        } else {
                            ret[i] = ((long) (0xFF & b[0]) << 56) + ((long) (0xFF & b[1]) << 48)
                                    + ((long) (0xFF & b[2]) << 40) + ((long) (0xFF & b[3]) << 32)
                                    + ((long) (0xFF & b[4]) << 24) + ((long) (0xFF & b[5]) << 16)
                                    + ((long) (0xFF & b[6]) << 8) + (0xFF & b[7]);
                        }
                        //ret[i] = afile.readLong();
                    } else {
                        ret[i] = Float.NaN;
                    }
                }
            } else if (datatype.equalsIgnoreCase("FLOAT")) {
                size = 4;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    pos = getcellnumber(points[i][0], points[i][1]);
                    if (pos >= 0) {
                        afile.seek(pos * size);
                        afile.read(b);
                        ByteBuffer bb = ByteBuffer.wrap(b);
                        if (byteorderLSB) {
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                        }
                        ret[i] = bb.getFloat();
                    } else {
                        ret[i] = Float.NaN;
                    }

                }
            } else if (datatype.equalsIgnoreCase("DOUBLE")) {
                size = 8;
                b = new byte[8];
                for (i = 0; i < length; i++) {
                    pos = getcellnumber(points[i][0], points[i][1]);
                    if (pos >= 0) {
                        afile.seek(pos * size);
                        afile.read(b);
                        ByteBuffer bb = ByteBuffer.wrap(b);
                        if (byteorderLSB) {
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                        }
                        ret[i] = (float) bb.getDouble();

                        //ret[i] = afile.readFloat();
                    } else {
                        ret[i] = Float.NaN;
                    }
                }
            } else {
                logger.error("datatype not supported in Grid.getValues: " + datatype);
                // / should not happen; catch anyway...
                for (i = 0; i < length; i++) {
                    ret[i] = Float.NaN;
                }
            }
            //replace not a number
            for (i = 0; i < length; i++) {
                if ((float) ret[i] == (float) nodatavalue) {
                    ret[i] = Float.NaN;
                }
            }

            afile.close();
        } catch (Exception e) {
            logger.error("error getting grid file values", e);
        }
        return ret;
    }

    /**
     * @param points input array for longitude and latitude
     *               double[number_of_points][2] and sorted latitude then longitude
     * @return array of .gri file values corresponding to the
     * points provided
     */
    public float[] getValues3(double[][] points, int bufferSize) {
        //confirm inputs since they come from somewhere else
        if (points == null || points.length == 0) {
            return null;
        }

        //use preloaded grid data if available
        Grid g = Grid.getLoadedGrid(filename);
        if (g != null && g.grid_data != null) {
            return g.getValues2(points);
        }

        int length = points.length;
        int size, i;
        byte[] b;

        RandomAccessFile afile;

        File f2 = new File(filename + ".GRI");

        try { //read of random access file can throw an exception
            if (!f2.exists()) {
                afile = new RandomAccessFile(filename + ".gri", "r");
            } else {
                afile = new RandomAccessFile(filename + ".GRI", "r");
            }

            if (afile.length() < 80 * 1024 * 1024) {
                afile.close();
                return getValues2(points);
            }

            byte[] buffer = new byte[bufferSize];    //must be multiple of 64
            Long bufferOffset = afile.length();

            float[] ret = new float[points.length];

            //get cell numbers
            int[][] cells = new int[points.length][2];
            for (int j = 0; j < points.length; j++) {
                if (Double.isNaN(points[j][0]) || Double.isNaN(points[j][1])) {
                    cells[j][0] = -1;
                    cells[j][1] = j;
                } else {
                    cells[j][0] = getcellnumber(points[j][0], points[j][1]);
                    cells[j][1] = j;
                }
            }
            java.util.Arrays.sort(cells, new Comparator<int[]>() {

                @Override
                public int compare(int[] o1, int[] o2) {
                    if (o1[0] == o2[0]) {
                        return o1[1] - o2[1];
                    } else {
                        return o1[0] - o2[0];
                    }
                }
            });

            if (datatype.equalsIgnoreCase("BYTE")) {
                size = 1;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    if (i > 0 && cells[i - 1][0] == cells[i][0]) {
                        ret[cells[i][1]] = ret[cells[i - 1][1]];
                        continue;
                    }
                    if (cells[i][0] >= 0) {
                        ret[cells[i][1]] = getByte(afile, buffer, bufferOffset, cells[i][0] * size);
                    } else {
                        ret[cells[i][1]] = Float.NaN;
                    }
                }
            } else if (datatype.equalsIgnoreCase("UBYTE")) {
                size = 1;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    if (i > 0 && cells[i - 1][0] == cells[i][0]) {
                        ret[cells[i][1]] = ret[cells[i - 1][1]];
                        continue;
                    }
                    if (cells[i][0] >= 0) {
                        ret[cells[i][1]] = getByte(afile, buffer, bufferOffset, cells[i][0] * size);
                        if (ret[cells[i][1]] < 0) {
                            ret[cells[i][1]] += 256;
                        }
                    } else {
                        ret[cells[i][1]] = Float.NaN;
                    }
                }
            } else if (datatype.equalsIgnoreCase("SHORT")) {
                size = 2;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    if (i > 0 && cells[i - 1][0] == cells[i][0]) {
                        ret[cells[i][1]] = ret[cells[i - 1][1]];
                        continue;
                    }
                    if (cells[i][0] >= 0) {
                        bufferOffset = getBytes(afile, buffer, bufferOffset, cells[i][0] * (long) size, b);
                        if (byteorderLSB) {
                            ret[cells[i][1]] = (short) (((0xFF & b[1]) << 8) | (b[0] & 0xFF));
                        } else {
                            ret[cells[i][1]] = (short) (((0xFF & b[0]) << 8) | (b[1] & 0xFF));
                        }
                        //ret[cells[i][1]] = afile.readShort();
                    } else {
                        ret[cells[i][1]] = Float.NaN;
                    }
                }
            } else if (datatype.equalsIgnoreCase("INT")) {
                size = 4;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    if (i > 0 && cells[i - 1][0] == cells[i][0]) {
                        ret[cells[i][1]] = ret[cells[i - 1][1]];
                        continue;
                    }
                    if (cells[i][0] >= 0) {
                        bufferOffset = getBytes(afile, buffer, bufferOffset, cells[i][0] * (long) size, b);
                        if (byteorderLSB) {
                            ret[cells[i][1]] = ((0xFF & b[3]) << 24) | ((0xFF & b[2]) << 16) + ((0xFF & b[1]) << 8) + (b[0] & 0xFF);
                        } else {
                            ret[cells[i][1]] = ((0xFF & b[0]) << 24) | ((0xFF & b[1]) << 16) + ((0xFF & b[2]) << 8) + ((0xFF & b[3]) & 0xFF);
                        }
                        //ret[cells[i][1]] = afile.readInt();
                    } else {
                        ret[cells[i][1]] = Float.NaN;
                    }
                }
            } else if (datatype.equalsIgnoreCase("LONG")) {
                size = 8;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    if (i > 0 && cells[i - 1][0] == cells[i][0]) {
                        ret[cells[i][1]] = ret[cells[i - 1][1]];
                        continue;
                    }
                    if (cells[i][0] >= 0) {
                        bufferOffset = getBytes(afile, buffer, bufferOffset, cells[i][0] * (long) size, b);
                        if (byteorderLSB) {
                            ret[cells[i][1]] = ((long) (0xFF & b[7]) << 56) + ((long) (0xFF & b[6]) << 48)
                                    + ((long) (0xFF & b[5]) << 40) + ((long) (0xFF & b[4]) << 32)
                                    + ((long) (0xFF & b[3]) << 24) + ((long) (0xFF & b[2]) << 16)
                                    + ((long) (0xFF & b[1]) << 8) + (0xFF & b[0]);
                        } else {
                            ret[cells[i][1]] = ((long) (0xFF & b[0]) << 56) + ((long) (0xFF & b[1]) << 48)
                                    + ((long) (0xFF & b[2]) << 40) + ((long) (0xFF & b[3]) << 32)
                                    + ((long) (0xFF & b[4]) << 24) + ((long) (0xFF & b[5]) << 16)
                                    + ((long) (0xFF & b[6]) << 8) + (0xFF & b[7]);
                        }
                        //ret[cells[i][1]] = afile.readLong();
                    } else {
                        ret[cells[i][1]] = Float.NaN;
                    }
                }
            } else if (datatype.equalsIgnoreCase("FLOAT")) {
                size = 4;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    if (i > 0 && cells[i - 1][0] == cells[i][0]) {
                        ret[cells[i][1]] = ret[cells[i - 1][1]];
                        continue;
                    }
                    if (cells[i][0] >= 0) {
                        bufferOffset = getBytes(afile, buffer, bufferOffset, cells[i][0] * (long) size, b);
                        ByteBuffer bb = ByteBuffer.wrap(b);
                        if (byteorderLSB) {
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                        }
                        ret[cells[i][1]] = bb.getFloat();
                    } else {
                        ret[cells[i][1]] = Float.NaN;
                    }

                }
            } else if (datatype.equalsIgnoreCase("DOUBLE")) {
                size = 8;
                b = new byte[8];
                for (i = 0; i < length; i++) {
                    if (i > 0 && cells[i - 1][0] == cells[i][0]) {
                        ret[cells[i][1]] = ret[cells[i - 1][1]];
                        continue;
                    }
                    if (cells[i][0] >= 0) {
                        getBytes(afile, buffer, bufferOffset, cells[i][0] * (long) size, b);
                        ByteBuffer bb = ByteBuffer.wrap(b);
                        if (byteorderLSB) {
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                        }
                        ret[cells[i][1]] = (float) bb.getDouble();

                        //ret[cells[i][1]] = afile.readFloat();
                    } else {
                        ret[cells[i][1]] = Float.NaN;
                    }
                }
            } else {
                logger.error("datatype not supported in Grid.getValues: " + datatype);
                // / should not happen; catch anyway...
                for (i = 0; i < length; i++) {
                    ret[i] = Float.NaN;
                }
            }
            //replace not a number
            for (i = 0; i < length; i++) {
                if ((float) ret[i] == (float) nodatavalue) {
                    ret[i] = Float.NaN;
                }
            }

            afile.close();

            return ret;
        } catch (Exception e) {
            logger.error("error getting grid file values", e);
        }
        return null;
    }


    /*
     * Cut a one grid against the missing values of another.
     *
     * They must be aligned.
     */
    public void mergeMissingValues(Grid sourceOfMissingValues, boolean hideMissing) {
        float[] cells = sourceOfMissingValues.getGrid();

        float[] actual = getGrid();

        int length = actual.length;

        int i;
        RandomAccessFile afile;
        File f2 = new File(filename + ".GRI");

        try { //read of random access file can throw an exception
            if (!f2.exists()) {
                afile = new RandomAccessFile(filename + ".gri", "rw");
            } else {
                afile = new RandomAccessFile(filename + ".GRI", "rw");
            }

            byte[] b = new byte[(int) afile.length()];

            ByteBuffer bb = ByteBuffer.wrap(b);

            if (byteorderLSB) {
                bb.order(ByteOrder.LITTLE_ENDIAN);
            }

            afile.seek(0);

            if (datatype.equalsIgnoreCase("UBYTE")) {
                for (i = 0; i < length; i++) {
                    if (hideMissing == Float.isNaN(cells[i])) {
                        if (nodatavalue >= 128) {
                            bb.put((byte) (nodatavalue - 256));
                        } else {
                            bb.put((byte) nodatavalue);
                        }
                    } else {
                        if (actual[i] >= 128) {
                            bb.put((byte) (actual[i] - 256));
                        } else {
                            bb.put((byte) actual[i]);
                        }
                    }
                }
            } else if (datatype.equalsIgnoreCase("BYTE")) {
                for (i = 0; i < length; i++) {
                    bb.put((byte) actual[i]);
                }
            } else if (datatype.equalsIgnoreCase("SHORT")) {
                for (i = 0; i < length; i++) {
                    if (hideMissing == Float.isNaN(cells[i])) {
                        bb.putShort((short) nodatavalue);
                    } else {
                        bb.putShort((short) actual[i]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("INT")) {
                for (i = 0; i < length; i++) {
                    if (hideMissing == Float.isNaN(cells[i])) {
                        bb.putInt((int) nodatavalue);
                    } else {
                        bb.putInt((int) actual[i]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("LONG")) {
                for (i = 0; i < length; i++) {
                    if (hideMissing == Float.isNaN(cells[i])) {
                        bb.putLong((long) nodatavalue);
                    } else {
                        bb.putLong((long) actual[i]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("FLOAT")) {
                for (i = 0; i < length; i++) {
                    if (hideMissing == Float.isNaN(cells[i])) {
                        bb.putFloat((float) nodatavalue);
                    } else {
                        bb.putFloat(actual[i]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("DOUBLE")) {
                for (i = 0; i < length; i++) {
                    if (hideMissing == Float.isNaN(cells[i])) {
                        bb.putDouble((double) nodatavalue);
                    } else {
                        bb.putDouble((double) actual[i]);
                    }
                }
            } else {
                // should not happen
                logger.error("unsupported grid data type: " + datatype);
            }

            afile.write(bb.array());
            afile.close();
        } catch (Exception e) {
            logger.error("error getting grid file values", e);
        }
    }

    /**
     * buffering on top of RandomAccessFile
     */
    private byte getByte(RandomAccessFile raf, byte[] buffer, Long bufferOffset, long seekTo) throws IOException {
        long relativePos = seekTo - bufferOffset;
        if (relativePos < 0) {
            raf.seek(seekTo);
            bufferOffset = seekTo;
            raf.read(buffer);
            return buffer[0];
        } else if (relativePos >= 0 && relativePos < buffer.length) {
            return buffer[(int) relativePos];
        } else if (relativePos - buffer.length < buffer.length) {
            bufferOffset += buffer.length;
            raf.read(buffer);
            return buffer[(int) (relativePos - buffer.length)];
        } else {
            raf.seek(seekTo);
            bufferOffset = seekTo;
            raf.read(buffer);
            return buffer[0];
        }
    }

    /**
     * buffering on top of RandomAccessFile
     */
    private Long getBytes(RandomAccessFile raf, byte[] buffer, Long bufferOffset, long seekTo, byte[] dest) throws IOException {
        long relativePos = seekTo - bufferOffset;
        if (relativePos < 0) {
            if (seekTo < 0) {
                seekTo = 0;
            }
            raf.seek(seekTo);
            bufferOffset = seekTo;
            raf.read(buffer);
            System.arraycopy(buffer, 0, dest, 0, dest.length);
        } else if (relativePos >= 0 && relativePos < buffer.length) {
            System.arraycopy(buffer, (int) relativePos, dest, 0, dest.length);
        } else if (relativePos - buffer.length < buffer.length) {
            bufferOffset += buffer.length;
            raf.read(buffer);
            int offset = (int) (relativePos - buffer.length);
            System.arraycopy(buffer, offset, dest, 0, dest.length);
        } else {
            raf.seek(seekTo);
            bufferOffset = seekTo;
            raf.read(buffer);
            System.arraycopy(buffer, 0, dest, 0, dest.length);
        }

        return bufferOffset;
    }

    /**
     * @return calculated min and max values of a grid file as float [] where [0] is min and [1] is max.
     */
    public float[] calculatetMinMax() {

        float[] ret = new float[2];
        ret[0] = Float.MAX_VALUE;
        ret[1] = Float.MAX_VALUE * -1;

        long i;
        int size;
        byte[] b;
        RandomAccessFile afile;

        try { //read of random access file can throw an exception
            File f2 = new File(filename + ".GRI");
            if (!f2.exists()) {
                afile = new RandomAccessFile(filename + ".gri", "r");
            } else {
                afile = new RandomAccessFile(filename + ".GRI", "r");
            }

            long length = ((long) nrows) * ((long) ncols);
            float f;

            if (datatype.equalsIgnoreCase("BYTE")) {
                size = 1;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    f = afile.readByte();
                    if (f != (float) nodatavalue) {
                        ret[0] = Math.min(f, ret[0]);
                        ret[1] = Math.max(f, ret[1]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("UBYTE")) {
                size = 1;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    f = afile.readByte();
                    if (f < 0) {
                        f += 256;
                    }
                    if (f != (float) nodatavalue) {
                        ret[0] = Math.min(f, ret[0]);
                        ret[1] = Math.max(f, ret[1]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("SHORT")) {
                size = 2;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    afile.read(b);
                    if (byteorderLSB) {
                        f = (short) (((0xFF & b[1]) << 8) | (b[0] & 0xFF));
                    } else {
                        f = (short) (((0xFF & b[0]) << 8) | (b[1] & 0xFF));
                    }
                    if (f != (float) nodatavalue) {
                        ret[0] = Math.min(f, ret[0]);
                        ret[1] = Math.max(f, ret[1]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("INT")) {
                size = 4;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    afile.read(b);
                    if (byteorderLSB) {
                        f = ((0xFF & b[3]) << 24) | ((0xFF & b[2]) << 16) + ((0xFF & b[1]) << 8) + (b[0] & 0xFF);
                    } else {
                        f = ((0xFF & b[0]) << 24) | ((0xFF & b[1]) << 16) + ((0xFF & b[2]) << 8) + ((0xFF & b[3]) & 0xFF);
                    }
                    if (f != (float) nodatavalue) {
                        ret[0] = Math.min(f, ret[0]);
                        ret[1] = Math.max(f, ret[1]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("LONG")) {
                size = 8;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    afile.read(b);
                    if (byteorderLSB) {
                        f = ((long) (0xFF & b[7]) << 56) + ((long) (0xFF & b[6]) << 48)
                                + ((long) (0xFF & b[5]) << 40) + ((long) (0xFF & b[4]) << 32)
                                + ((long) (0xFF & b[3]) << 24) + ((long) (0xFF & b[2]) << 16)
                                + ((long) (0xFF & b[1]) << 8) + (0xFF & b[0]);
                    } else {
                        f = ((long) (0xFF & b[0]) << 56) + ((long) (0xFF & b[1]) << 48)
                                + ((long) (0xFF & b[2]) << 40) + ((long) (0xFF & b[3]) << 32)
                                + ((long) (0xFF & b[4]) << 24) + ((long) (0xFF & b[5]) << 16)
                                + ((long) (0xFF & b[6]) << 8) + (0xFF & b[7]);
                    }
                    if (f != (float) nodatavalue) {
                        ret[0] = Math.min(f, ret[0]);
                        ret[1] = Math.max(f, ret[1]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("FLOAT")) {
                size = 4;
                b = new byte[size];
                for (i = 0; i < length; i++) {
                    afile.read(b);
                    ByteBuffer bb = ByteBuffer.wrap(b);
                    if (byteorderLSB) {
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                    }
                    f = bb.getFloat();
                    if (f != (float) nodatavalue) {
                        ret[0] = Math.min(f, ret[0]);
                        ret[1] = Math.max(f, ret[1]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("DOUBLE")) {
                size = 8;
                b = new byte[8];
                for (i = 0; i < length; i++) {
                    afile.read(b);
                    ByteBuffer bb = ByteBuffer.wrap(b);
                    if (byteorderLSB) {
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                    }
                    f = (float) bb.getDouble();
                    if (f != (float) nodatavalue) {
                        ret[0] = Math.min(f, ret[0]);
                        ret[1] = Math.max(f, ret[1]);
                    }
                }
            } else {
                logger.error("datatype not supported in Grid.getValues: " + datatype);
            }

            afile.close();
        } catch (Exception e) {
            logger.error("error calculating min/max of a grid file", e);
        }
        return ret;
    }
}
