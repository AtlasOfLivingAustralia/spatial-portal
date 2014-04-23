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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import org.ala.layers.intersect.Grid;

/**
 * @author Adam
 */
public class GridCacheBuilder {

    public static void main(String[] args) throws IOException {
//        args = new String[]{"e:\\layers\\ready\\diva", "e:\\layers\\ready\\diva_cache"};

        System.out.println("args[0]=diva grid input dir\nargs[1]=cached diva grid output dir\n\n");

        //load up all diva grids in a directory
        ArrayList<Grid> grids = loadGridHeaders(args[0]);

        //identify groups
        ArrayList<ArrayList<Grid>> groups = identifyGroups(grids);

        //write large enough groups
        for (int i = 0; i < groups.size(); i++) {
            writeGroup(args[1], groups.get(i));
        }
    }

    private static ArrayList<Grid> loadGridHeaders(String directory) {
        ArrayList<Grid> grids = new ArrayList<Grid>();

        File dir = new File(directory);
        for (File f : dir.listFiles()) {
            try {
                if (f.getName().endsWith(".grd")) {
                    Grid g = new Grid(f.getPath().substring(0, f.getPath().length() - 4));
                    grids.add(g);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return grids;
    }

    private static ArrayList<ArrayList<Grid>> identifyGroups(ArrayList<Grid> grids) {
        ArrayList<ArrayList<Grid>> groups = new ArrayList<ArrayList<Grid>>();

        for (int i = 0; i < grids.size(); i++) {
            boolean newGroup = true;
            for (int j = 0; j < groups.size(); j++) {
                if (compareGrids(groups.get(j).get(0), grids.get(i))) {
                    groups.get(j).add(grids.get(i));
                    newGroup = false;
                    break;
                }
            }
            if (newGroup) {
                ArrayList<Grid> gs = new ArrayList<Grid>();
                gs.add(grids.get(i));
                groups.add(gs);
            }
        }

        return groups;
    }

    private static boolean compareGrids(Grid g1, Grid g2) {
        //compare extents and nrows and ncols
        return g1.ncols == g2.ncols
                && g1.nrows == g2.nrows
                && g1.xmin == g2.xmin
                && g1.ymin == g2.ymin
                && g1.xmax == g2.xmax
                && g1.ymax == g2.ymax;
    }

    private static void writeGroup(String dir, ArrayList<Grid> group) throws IOException {
        File f = null;
        int count = 0;
        while ((f = new File(dir + File.separator + "group_" + count + ".txt")).exists()) {
            count++;
        }

        System.out.println("writing: " + f.getName());
        for (int i = 0; i < group.size(); i++) {
            System.out.println("   has: " + group.get(i).filename);
        }

        writeGroupHeader(f, group);
        writeGroupGRD(new File(dir + File.separator + "group_" + count), group);
        writeGroupGRI(new File(dir + File.separator + "group_" + count + ".gri"), group);
    }

    private static void writeGroupHeader(File f, ArrayList<Grid> group) throws IOException {
        FileWriter fw = new FileWriter(f);
        for (int i = 0; i < group.size(); i++) {
            fw.write(group.get(i).filename);
            fw.write("\n");
        }
        fw.close();
    }

    private static void writeGroupGRD(File file, ArrayList<Grid> group) {
        Grid g = group.get(0);
        g.writeHeader(file.getPath(), g.xmin, g.ymin, g.xmin + g.xres * g.nrows, g.ymin + g.yres * g.ncols, g.xres, g.yres, g.nrows, g.ncols, g.minval, g.maxval);
    }

    private static void writeGroupGRI(File file, ArrayList<Grid> group) throws FileNotFoundException, IOException {
        Grid g = group.get(0);
        RandomAccessFile[] raf = new RandomAccessFile[group.size()];
        RandomAccessFile output = new RandomAccessFile(file, "rw");

        for (int i = 0; i < group.size(); i++) {
            raf[i] = new RandomAccessFile(group.get(i).filename + ".gri", "r");
        }

        int length = g.ncols * g.nrows;
        int size = 4;
        byte[] b = new byte[size * group.size() * g.ncols];
        float noDataValue = Float.MAX_VALUE * -1;

        byte[] bi = new byte[g.ncols * 8];
        float[][] rows = new float[group.size()][g.ncols];

        for (int i = 0; i < g.nrows; i++) {
            //read
            for (int j = 0; j < raf.length; j++) {
                nextRowOfFloats(rows[j], group.get(j).datatype, group.get(j).byteorderLSB, g.ncols, raf[j], bi, (float) g.nodatavalue);
            }

            //write
            ByteBuffer bb = ByteBuffer.wrap(b);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            for (int k = 0; k < g.ncols; k++) {
                for (int j = 0; j < raf.length; j++) {
                    //float f = getNextValue(raf[j], group.get(j));
                    float f = rows[j][k];
                    if (Float.isNaN(f)) {
                        bb.putFloat(noDataValue);
                    } else {
                        bb.putFloat(f);
                    }
                }
            }
            output.write(b);
        }

        output.close();

        for (int i = 0; i < raf.length; i++) {
            raf[i].close();
        }
    }

    static public float getNextValue(RandomAccessFile raf, Grid g) {
        float f = Float.NaN;
        try {
            int size;
            byte[] b;
            if (g.datatype.charAt(0) == 'B') {//"BYTE")) {
                f = raf.readByte();
            } else if (g.datatype.charAt(0) == 'U') {//equalsIgnoreCase("UBYTE")) {
                f = raf.readByte();
                if (f < 0) {
                    f += 256;
                }
            } else if (g.datatype.charAt(0) == 'S') {//equalsIgnoreCase("SHORT")) {
                size = 2;
                b = new byte[size];
                raf.read(b);
                if (g.byteorderLSB) {
                    f = (short) (((0xFF & b[1]) << 8) | (b[0] & 0xFF));
                } else {
                    f = (short) (((0xFF & b[0]) << 8) | (b[1] & 0xFF));
                }
            } else if (g.datatype.charAt(0) == 'I') {//equalsIgnoreCase("INT")) {
                size = 4;
                b = new byte[size];
                raf.read(b);
                if (g.byteorderLSB) {
                    f = ((0xFF & b[3]) << 24) | ((0xFF & b[2]) << 16) + ((0xFF & b[1]) << 8) + (b[0] & 0xFF);
                } else {
                    f = ((0xFF & b[0]) << 24) | ((0xFF & b[1]) << 16) + ((0xFF & b[2]) << 8) + ((0xFF & b[3]) & 0xFF);
                }
            } else if (g.datatype.charAt(0) == 'L') {//equalsIgnoreCase("LONG")) {
                size = 8;
                b = new byte[size];
                raf.read(b);
                if (g.byteorderLSB) {
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
            } else if (g.datatype.charAt(0) == 'F') {//.equalsIgnoreCase("FLOAT")) {
                size = 4;
                b = new byte[size];
                raf.read(b);
                ByteBuffer bb = ByteBuffer.wrap(b);
                if (g.byteorderLSB) {
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                }
                f = bb.getFloat();

            } else if (g.datatype.charAt(0) == 'D') {//.equalsIgnoreCase("DOUBLE")) {
                size = 8;
                b = new byte[8];
                raf.read(b);
                ByteBuffer bb = ByteBuffer.wrap(b);
                if (g.byteorderLSB) {
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                }
                f = (float) bb.getDouble();
            }
            //replace not a number            
            if ((float) f == (float) g.nodatavalue) {
                f = Float.NaN;
            }
        } catch (Exception e) {
        }
        return f;
    }

    static void nextRowOfFloats(float[] row, String datatype, boolean byteOrderLSB, int ncols, RandomAccessFile raf, byte[] b, float noDataValue) throws IOException {
        int size = 4;
        if (datatype.charAt(0) == 'U') {
            size = 1;
        } else if (datatype.charAt(0) == 'B') {
            size = 1;
        } else if (datatype.charAt(0) == 'S') {
            size = 2;
        } else if (datatype.charAt(0) == 'I') {
            size = 4;
        } else if (datatype.charAt(0) == 'L') {
            size = 8;
        } else if (datatype.charAt(0) == 'F') {
            size = 4;
        } else if (datatype.charAt(0) == 'D') {
            size = 8;
        }

        raf.read(b, 0, size * ncols);
        ByteBuffer bb = ByteBuffer.wrap(b);
        if (byteOrderLSB) {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        } else {
            bb.order(ByteOrder.BIG_ENDIAN);
        }

        int i;
        int length = ncols;
        if (datatype.charAt(0) == 'U') {
            for (i = 0; i < length; i++) {
                float ret = bb.get();
                if (ret < 0) {
                    ret += 256;
                }
                row[i] = ret;
            }
        } else if (datatype.charAt(0) == 'B') {

            for (i = 0; i < length; i++) {
                row[i] = (float) bb.get();
            }
        } else if (datatype.charAt(0) == 'S') {
            for (i = 0; i < length; i++) {
                row[i] = (float) bb.getShort();
            }
        } else if (datatype.charAt(0) == 'I') {
            for (i = 0; i < length; i++) {
                row[i] = (float) bb.getInt();
            }
        } else if (datatype.charAt(0) == 'L') {
            for (i = 0; i < length; i++) {
                row[i] = (float) bb.getLong();
            }
        } else if (datatype.charAt(0) == 'F') {
            for (i = 0; i < length; i++) {
                row[i] = (float) bb.getFloat();
            }
        } else if (datatype.charAt(0) == 'D') {
            for (i = 0; i < length; i++) {
                row[i] = (float) bb.getDouble();
            }
        } else {
            System.out.println("UNKNOWN TYPE: " + datatype);
        }

        for (i = 0; i < length; i++) {
            if (row[i] == noDataValue) {
                row[i] = Float.NaN;
            }
        }
    }
}
