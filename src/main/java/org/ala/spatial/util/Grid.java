package org.ala.spatial.util;

import java.io.File;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;

/**
 * Grid.java
 * Created on June 24, 2005, 4:12 PM
 *
 * @author Robert Hijmans, rhijmans@berkeley.edu
 *
 * Updated 15/2/2010, Adam
 *
 * Interface for .gri/.grd files for now
 */
public class Grid { //  implements Serializable

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
    String filename;
    float[] grid_data = null;

    /**
     * loads grd for gri file reference
     * @param fname full path and file name without file extension
     * of .gri and .grd files to open
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
            //log error
            System.out.println("cannot find GRID: " + fname);

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
            //log error
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
            if (all_grids.size() == TabulationSettings.max_grids_load) {
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
            System.out.println("GRID unknown type: " + s);
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

    }

    public float[] getGrid() {
        if (grid_data != null) {
            return grid_data;
        }
        int length = nrows * ncols;

        float[] ret = new float[length];

        int i;
        RandomAccessFile afile;
        File f2 = new File(filename + ".GRI");

        try { //read of random access file can throw an exception
            if (!f2.exists()) {
                afile = new RandomAccessFile(filename + ".gri", "r");
            } else {
                afile = new RandomAccessFile(filename + ".GRI", "r");
            }

            byte[] b = new byte[(int) afile.length()];
            afile.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);
            afile.close();

            if (byteorderLSB) {
                bb.order(ByteOrder.LITTLE_ENDIAN);
            }

            if (datatype.equalsIgnoreCase("UBYTE")) {
                for (i = 0; i < length; i++) {
                    ret[i] = bb.get();
                    if (ret[i] < 0) {
                        ret[i] += 256;
                    }
                }
            } else if (datatype.equalsIgnoreCase("BYTE")) {
                for (i = 0; i < length; i++) {
                    ret[i] = bb.get();
                }
            } else if (datatype.equalsIgnoreCase("SHORT")) {
                for (i = 0; i < length; i++) {
                    ret[i] = bb.getShort();
                }
            } else if (datatype.equalsIgnoreCase("INT")) {
                for (i = 0; i < length; i++) {
                    ret[i] = bb.getInt();
                }
            } else if (datatype.equalsIgnoreCase("LONG")) {
                for (i = 0; i < length; i++) {
                    ret[i] = bb.getLong();
                }
            } else if (datatype.equalsIgnoreCase("FLOAT")) {
                for (i = 0; i < length; i++) {
                    ret[i] = bb.getFloat();
                }
            } else if (datatype.equalsIgnoreCase("DOUBLE")) {
                for (i = 0; i < length; i++) {
                    ret[i] = (float) bb.getDouble();
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
            //log error - probably a file error
            e.printStackTrace();
        }
        grid_data = ret;
        return ret;
    }

    /**
     * for DomainGenerator
     *
     * writes out a list of double (same as getGrid() returns) to a file
     *
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
            e.printStackTrace();
        }


        writeHeader(newfilename, xmin, ymin, xmax, ymax, xres, yres, nrows, ncols, minvalue, maxvalue, "INT4BYTES", "-9999");

    }

    /**
     * for grid cutter
     *
     * writes out a list of double (same as getGrid() returns) to a file
     *
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
            e.printStackTrace();
        }


        writeHeader(newfilename, xmin, ymin, xmax, ymax, xres, yres, nrows, ncols, minvalue, maxvalue, "FLT4BYTES", String.valueOf(noDataValueDefault));

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
            e.printStackTrace();
        }

        writeHeader(newfilename, xmin, ymin, xmax, ymax, xres, yres, nrows, ncols, minvalue, maxvalue, "FLT4BYTES", String.valueOf(noDataValueDefault));

    }

    void writeHeader(String newfilename, double xmin, double ymin, double xmax, double ymax, double xres, double yres, int nrows, int ncols, double minvalue, double maxvalue) {
        writeHeader(newfilename, xmin, ymin, xmax, ymax, xres, yres, nrows, ncols, minvalue, maxvalue, "FLT4BYTES", String.valueOf(noDataValueDefault));
    }

    void writeHeader(String newfilename, double xmin, double ymin, double xmax, double ymax, double xres, double yres, int nrows, int ncols, double minvalue, double maxvalue, String datatype, String nodata) {
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
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * do get values of grid for provided points.
     *
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
            //log error - probably a file error
            System.out.println("GRID: " + e.toString());
            e.printStackTrace();
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
            if(Float.isNaN(d)) {
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
            System.out.println(this.filename + " ERR header(" + this.minval + " " + this.maxval + ") actual(" + min + " " + max + ") number missing(" + numMissing + " of " + data.length + ")");
        } else {
            System.out.println(this.filename + " OK header(" + this.minval + " " + this.maxval + ") number missing(" + numMissing + " of " + data.length + ")");
        }
    }

    /**
     *
     * @param points input array for longitude and latitude
     *                  double[number_of_points][2]
     * @return array of .gri file values corresponding to the
     *          points provided
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
                        if(ret[i] < 0) {
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
                        //System.out.println("missing env: lng=" + points[i][0] + " lat=" + points[i][0] + " pos=" + pos);
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
                System.out.println("datatype not supported in Grid.getValues: " + datatype);
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
            e.printStackTrace();
        }
        return ret;
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
                // / should not happen
                System.out.println("unsupported grid data type: " + datatype);
            }

            afile.write(bb.array());
            afile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Cut a one grid against the missing values of another.
     *
     * They must be aligned.
     */
    public void writeCommonGrid(String dirname) {

        double[][] points = new double[TabulationSettings.grd_ncols * TabulationSettings.grd_nrows][2];
        for (int i = 0; i < TabulationSettings.grd_nrows; i++) {
            for (int j = 0; j < TabulationSettings.grd_ncols; j++) {
                points[(TabulationSettings.grd_nrows - 1 - i) * TabulationSettings.grd_ncols + j][0] = TabulationSettings.grd_xmin + j * TabulationSettings.grd_xdiv + TabulationSettings.grd_xdiv/2.0;
                points[(TabulationSettings.grd_nrows - 1 - i) * TabulationSettings.grd_ncols + j][1] = TabulationSettings.grd_ymin + i * TabulationSettings.grd_ydiv + TabulationSettings.grd_ydiv/2.0;
            }
        }

        float[] actual = getValues2(points);
        int length = actual.length;

        float minvalue = Float.NaN, maxvalue = Float.NaN;
        for (int i = 0; i < length; i++) {
            if (Float.isNaN(minvalue) || actual[i] < minvalue) {
                minvalue = actual[i];
            }
            if (Float.isNaN(maxvalue) || actual[i] > maxvalue) {
                maxvalue = actual[i];
            }
        }

        int i;
        RandomAccessFile afile;
        File f2 = new File(filename + ".GRI");

        String newfilename = filename.substring(0, filename.lastIndexOf(File.separator) + 1) + dirname + filename.substring(filename.lastIndexOf(File.separator));

        try { //read of random access file can throw an exception

            String nodata = String.valueOf(noDataValueDefault);
            int size = 4;
            if (datatype.equals("BYTE")) {
                size = 1;
                nodata = String.valueOf((byte)nodatavalue);
            } else if(datatype.equals("UBYTE")) {
                size = 1;
                nodata = String.valueOf((int)nodatavalue);
            } else if (datatype.equals("SHORT")) {
                size = 2;
                nodata = String.valueOf((short)nodatavalue);
            } else if (datatype.equals("INT")) {
                size = 4;
                nodata = String.valueOf((int)nodatavalue);
            } else if (datatype.equals("LONG")) {
                size = 8;
                nodata = String.valueOf((long)nodatavalue);
            } else if (datatype.equals("FLOAT")) {
                size = 4;
                nodata = String.valueOf((float)nodatavalue);
            } else if (datatype.equals("DOUBLE")) {
                size = 8;
                nodata = String.valueOf((double)nodatavalue);
            }

            writeHeader(newfilename, TabulationSettings.grd_xmin, TabulationSettings.grd_ymin, TabulationSettings.grd_xmax, TabulationSettings.grd_ymax, TabulationSettings.grd_xdiv, TabulationSettings.grd_ydiv, TabulationSettings.grd_nrows, TabulationSettings.grd_ncols, minvalue, maxvalue, datatype, nodata);

            byte[] b = new byte[length * size];
            ByteBuffer bb = ByteBuffer.wrap(b);

            if (byteorderLSB) {
                bb.order(ByteOrder.LITTLE_ENDIAN);
            }

            if (datatype.equalsIgnoreCase("UBYTE")) {
                for (i = 0; i < length; i++) {
                    if (Float.isNaN(actual[i])) {
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
                    if (Float.isNaN(actual[i])) {
                        bb.putShort((short) nodatavalue);
                    } else {
                        bb.putShort((short) actual[i]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("INT")) {
                for (i = 0; i < length; i++) {
                    if (Float.isNaN(actual[i])) {
                        bb.putInt((int) nodatavalue);
                    } else {
                        bb.putInt((int) actual[i]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("LONG")) {
                for (i = 0; i < length; i++) {
                    if (Float.isNaN(actual[i])) {
                        bb.putLong((long) nodatavalue);
                    } else {
                        bb.putLong((long) actual[i]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("FLOAT")) {
                for (i = 0; i < length; i++) {
                    if (Float.isNaN(actual[i])) {
                        bb.putFloat((float) nodatavalue);
                    } else {
                        bb.putFloat(actual[i]);
                    }
                }
            } else if (datatype.equalsIgnoreCase("DOUBLE")) {
                for (i = 0; i < length; i++) {
                    if (Float.isNaN(actual[i])) {
                        bb.putDouble((double) nodatavalue);
                    } else {
                        bb.putDouble((double) actual[i]);
                    }
                }
            } else {
                // / should not happen
                System.out.println("unsupported grid data type: " + datatype);
            }

            if (!f2.exists()) {
                (new File(filename + ".gri")).delete();
                afile = new RandomAccessFile(filename + ".gri", "rw");
            } else {
                (new File(filename + ".GRI")).delete();
                afile = new RandomAccessFile(filename + ".GRI", "rw");
            }
            afile.write(bb.array());
            afile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length > 2 && args[0].equals("overlay_mv")) {
            TabulationSettings.load();

            //to common grid
            Grid grid = new Grid(TabulationSettings.environmental_data_path + args[2]);//args[2]);
            if(grid.xmin != TabulationSettings.grd_xmin
                    || grid.xmax != TabulationSettings.grd_xmax
                    || grid.ymin != TabulationSettings.grd_ymin
                    || grid.ymax != TabulationSettings.grd_ymax
                    || grid.xres != TabulationSettings.grd_xdiv
                    || grid.yres != TabulationSettings.grd_ydiv ) {
                grid.writeCommonGrid("");
            }

            //mask
            Grid sourceOfMissingValues = new Grid(TabulationSettings.environmental_data_path + args[1]);
            Grid fileToOverwrite = new Grid(TabulationSettings.environmental_data_path + args[2]);
            fileToOverwrite.mergeMissingValues(sourceOfMissingValues, true);

            //fix up min/max
            grid = new Grid(TabulationSettings.environmental_data_path + args[2]);//args[2]);
            grid.writeCommonGrid("");
        } else if (args.length > 2 && args[0].equals("overlay_nmv")) {
            TabulationSettings.load();

            //to common grid
            Grid grid = new Grid(TabulationSettings.environmental_data_path + args[2]);//args[2]);
            if(grid.xmin != TabulationSettings.grd_xmin
                    || grid.xmax != TabulationSettings.grd_xmax
                    || grid.ymin != TabulationSettings.grd_ymin
                    || grid.ymax != TabulationSettings.grd_ymax
                    || grid.xres != TabulationSettings.grd_xdiv
                    || grid.yres != TabulationSettings.grd_ydiv ) {
                grid.writeCommonGrid("");
            }

            //mask
            Grid sourceOfMissingValues = new Grid(TabulationSettings.environmental_data_path + args[1]);
            Grid fileToOverwrite = new Grid(TabulationSettings.environmental_data_path + args[2]);
            fileToOverwrite.mergeMissingValues(sourceOfMissingValues, false);

            //fix up min/max
            grid = new Grid(TabulationSettings.environmental_data_path + args[2]);//args[2]);
            grid.writeCommonGrid("");
        } else if (args.length > 0 && args[0].equals("common_grid")) {
            TabulationSettings.load();

            for(int i=0;i<TabulationSettings.environmental_data_files.length;i++) {
                Grid grid = new Grid(TabulationSettings.environmental_data_path + TabulationSettings.environmental_data_files[i].name );//args[2]);
                if(grid.xmin != TabulationSettings.grd_xmin
                        || grid.xmax != TabulationSettings.grd_xmax
                        || grid.ymin != TabulationSettings.grd_ymin
                        || grid.ymax != TabulationSettings.grd_ymax
                        || grid.xres != TabulationSettings.grd_xdiv
                        || grid.yres != TabulationSettings.grd_ydiv ) {
                    grid.writeCommonGrid("");
                }
            }
        } else if (args.length > 1 && args[0].equals("redo_min_max")) {
            TabulationSettings.load();
            Grid grid = new Grid(TabulationSettings.environmental_data_path + args[1] );//args[2]);
            grid.writeCommonGrid("");

        } else {
            System.out.println("apply missing values from one aligned grid file to another");
            System.out.println("params: overlay_mv layerName_source_of_mv layerName_to_merge");
            System.out.println("");
            System.out.println("inverse of overlay_mv");
            System.out.println("params: overlay_nmv layerName_source_of_mv layerName_to_merge");
            System.out.println("");
            System.out.println("recalculate layer min & max");
            System.out.println("params: redo_min_max layerName");
            System.out.println("");
            System.out.println("create common grids, if required and put new files to subdir common_grid");
            System.out.println("params: common_grid");
        }
//
//        TabulationSettings.load();
//        Grid sourceOfMissingValues = new Grid(TabulationSettings.environmental_data_path + "adefm");
//        //String [] layer = {"srain2","soilm_mean","soilm_cv","ndvi_mean","elevation"};
//        //String [] layer = {"soilm_mean","soilm_cv"};
//        String [] layer = {"bath_topo_ausbath_09_v4"};
//        for(int i=0;i<layer.length;i++) {
//            //to common grid
//            Grid grid = new Grid(TabulationSettings.environmental_data_path + layer[i]);//args[2]);
//            if(grid.xmin != TabulationSettings.grd_xmin
//                    || grid.xmax != TabulationSettings.grd_xmax
//                    || grid.ymin != TabulationSettings.grd_ymin
//                    || grid.ymax != TabulationSettings.grd_ymax
//                    || grid.xres != TabulationSettings.grd_xdiv
//                    || grid.yres != TabulationSettings.grd_ydiv ) {
//                grid.writeCommonGrid("");
//            }
//
//            //mask
//            Grid fileToOverwrite = new Grid(TabulationSettings.environmental_data_path + layer[i]);
//            fileToOverwrite.mergeMissingValues(sourceOfMissingValues, false);
//
//            //fix up min/max
//            grid = new Grid(TabulationSettings.environmental_data_path + layer[i] );
//            grid.writeCommonGrid("");
//        }
    }
}
