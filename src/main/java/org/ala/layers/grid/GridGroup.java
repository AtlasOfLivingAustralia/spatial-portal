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
package org.ala.layers.grid;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import org.ala.layers.intersect.IniReader;

/**
 * @author Adam
 */
public class GridGroup {

    ArrayList<String> names;
    ArrayList<String> files;
    float[] cell;
    float[] emptyCell;
    RandomAccessFile raf;
    int cellSize;
    byte[] buffer;
    Long bufferOffset;
    byte[] b;
    int size;
    public Boolean byteorderLSB;
    public int ncols, nrows;
    public double nodatavalue;
    public Boolean valid;
    public double[] values;
    public double xmin, xmax, ymin, ymax;
    public double xres, yres;
    public String datatype;
    byte nbytes;
    public String filename;

    public GridGroup(String fname) throws IOException {
        filename = fname;
        readgrd(filename.substring(0, filename.length() - 4) + ".grd");

        raf = new RandomAccessFile(filename.substring(0, filename.length() - 4) + ".gri", "r");

        readHeader(fname);

        buffer = new byte[64 * 4 * cellSize];    //must be multiple of 64
        bufferOffset = raf.length();
        size = 4;
        b = new byte[4 * cellSize];
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

    private void readgrd(String filename) {
        IniReader ir = new IniReader(filename);

        datatype = "FLOAT";
        ncols = ir.getIntegerValue("GeoReference", "Columns");
        nrows = ir.getIntegerValue("GeoReference", "Rows");
        xmin = ir.getDoubleValue("GeoReference", "MinX");
        ymin = ir.getDoubleValue("GeoReference", "MinY");
        xmax = ir.getDoubleValue("GeoReference", "MaxX");
        ymax = ir.getDoubleValue("GeoReference", "MaxY");
        xres = ir.getDoubleValue("GeoReference", "ResolutionX");
        yres = ir.getDoubleValue("GeoReference", "ResolutionY");
        if (ir.valueExists("Data", "NoDataValue")) {
            nodatavalue = (float) ir.getDoubleValue("Data", "NoDataValue");
        } else {
            nodatavalue = Double.NaN;
        }

        String s = ir.getStringValue("Data", "ByteOrder");

        byteorderLSB = true;
        if (s != null && s.length() > 0) {
            if (s.equals("MSB")) {
                byteorderLSB = false;
            }
        }
    }

    public HashMap<String, Float> sample(double longitude, double latitude) throws IOException {
        HashMap<String, Float> map = new HashMap<String, Float>();

        float[] c = readCell(longitude, latitude);

        for (int i = 0; i < c.length; i++) {
            map.put(names.get(i), c[i]);
        }

        return map;
    }

    float[] readCell(double longitude, double latitude) throws IOException {
        //seek
        long pos = getcellnumber(longitude, latitude);
        if (pos >= 0) {
            //getBytes(raf, buffer, bufferOffset, pos * size * cellSize, b);
            raf.seek(pos * size * cellSize);
            raf.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);
            if (byteorderLSB) {
                bb.order(ByteOrder.LITTLE_ENDIAN);
            }
            for (int i = 0; i < cell.length; i++) {
                cell[i] = bb.getFloat();
                if (cell[i] == Float.MAX_VALUE * -1) {
                    cell[i] = Float.NaN;
                }
            }
            return cell;
        } else {
            return emptyCell;
        }
    }

    //    /**
//     * buffering on top of RandomAccessFile
//     *
//     * @param afile
//     * @param buffer
//     * @param fileOffset
//     * @param bufferPos
//     * @param seekTo
//     * @return
//     */
//    private void getBytes(RandomAccessFile raf, byte[] buffer, Long bufferOffset, long seekTo, byte[] dest) throws IOException {
//        long relativePos = seekTo - bufferOffset;
//        if (relativePos < 0) {
//            raf.seek(seekTo);
//            bufferOffset = seekTo;
//            raf.read(buffer);
//            for (int i = 0; i < dest.length; i++) {
//                dest[i] = buffer[i];
//            }
//        } else if (relativePos >= 0 && relativePos < buffer.length) {
//            for (int i = 0; i < dest.length; i++) {
//                dest[i] = buffer[i + (int) relativePos];
//            }
//        } else if (relativePos - buffer.length < buffer.length) {
//            bufferOffset += buffer.length;
//            raf.read(buffer);
//            int offset = (int) (relativePos - buffer.length);
//            for (int i = 0; i < dest.length; i++) {
//                dest[i] = buffer[i + offset];
//            }
//        } else {
//            raf.seek(seekTo);
//            bufferOffset = seekTo;
//            raf.read(buffer);
//            for (int i = 0; i < dest.length; i++) {
//                dest[i] = buffer[i];
//            }
//        }
//    }
    private void readHeader(String fname) throws IOException {
        names = new ArrayList<String>();
        files = new ArrayList<String>();

        BufferedReader br = new BufferedReader(new FileReader(fname));
        String line;
        while ((line = br.readLine()) != null) {
            names.add(line);
            files.add(line);
        }

        br.close();

        cellSize = names.size();
        cell = new float[cellSize];
        emptyCell = new float[cellSize];
        for (int i = 0; i < emptyCell.length; i++) {
            emptyCell[i] = Float.NaN;
        }
    }
}
