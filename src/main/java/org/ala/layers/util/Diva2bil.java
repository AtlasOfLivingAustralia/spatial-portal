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
package org.ala.layers.util;

import java.io.*;
import java.nio.*;

import org.ala.layers.intersect.IniReader;

public class Diva2bil {

    static public void main(String[] args) {
        if (args.length < 2) {
            System.out.println("diva to hdr bil.\n\n");
            System.out.println("args[0] = diva grid (without .grd or .gri)\n"
                    + "args[1] = bil (.bil and .hdr appended)\n");
            return;
        }

        diva2bil(args[0], args[1]);
    }

    static public boolean diva2bil(String divaFilename, String bilFilename) {
        boolean ret = true;

        try {
            File dataFile = new File(divaFilename + ".gri");

            IniReader ir = new IniReader(divaFilename + ".grd");

            double minx = ir.getDoubleValue("GeoReference", "MinX");
            double maxy = ir.getDoubleValue("GeoReference", "MaxY");
            double xdiv = ir.getDoubleValue("GeoReference", "ResolutionX");
            double ydiv = ir.getDoubleValue("GeoReference", "ResolutionY");
            double nodatavalue = -9999;
            if (ir.valueExists("Data", "NoDataValue")) {
                nodatavalue = ir.getDoubleValue("Data", "NoDataValue");
            }
            int nrows = ir.getIntegerValue("GeoReference", "Rows");
            int ncols = ir.getIntegerValue("GeoReference", "Columns");
            String type = getType(ir.getStringValue("Data", "DataType"));
            int nbytes = getByteCount(type);

            FileWriter fw = new FileWriter(bilFilename + ".hdr");
            fw.append("BYTEORDER      I\n");
            fw.append("LAYOUT         BIL\n");
            fw.append("NROWS      " + nrows + "\n");
            fw.append("NCOLS      " + ncols + "\n");
            fw.append("NBANDS      1\n");
            fw.append("NBITS      " + nbytes * 8 + "\n");
            fw.append("BANDROWBYTES      " + nbytes * ncols + "\n");
            fw.append("TOTALROWBYTES      " + nbytes * ncols + "\n");
            fw.append("PIXELTYPE      " + type + "\n");
            fw.append("ULXMAP      " + (minx + xdiv / 2.0) + "\n");
            fw.append("ULYMAP      " + (maxy - ydiv / 2.0) + "\n");
            fw.append("XDIM      " + xdiv + "\n");
            fw.append("YDIM      " + ydiv + "\n");
            fw.append("NODATA      " + nodatavalue + "\n");

            fw.close();


            //copy gri to bil
            FileInputStream fis = new FileInputStream(dataFile);
            FileOutputStream fos = new FileOutputStream(bilFilename + ".bil");
            byte[] buf = new byte[1024 * 1024];
            int len;
            while ((len = fis.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fis.close();
            fos.close();

            System.out.println("finished\n");
        } catch (Exception e) {
            ret = false;
            e.printStackTrace();
        }
        return ret;
    }

    static String getType(String datatype) {
        datatype = datatype.toUpperCase();

        // Expected from grd file
        if (datatype.equals("INT1BYTE")) {
            datatype = "BYTE";
        } else if (datatype.equals("INT2BYTES")) {
            datatype = "SHORT";
        } else if (datatype.equals("INT4BYTES")) {
            datatype = "INT";
        } else if (datatype.equals("INT8BYTES")) {
            datatype = "LONG";
        } else if (datatype.equals("FLT4BYTES")) {
            datatype = "FLOAT";
        } else if (datatype.equals("FLT8BYTES")) {
            datatype = "DOUBLE";
        } // shorthand for same
        else if (datatype.equals("INT1B") || datatype.equals("BYTE")) {
            datatype = "BYTE";
        } else if (datatype.equals("INT1U") || datatype.equals("UBYTE")) {
            datatype = "UBYTE";
        } else if (datatype.equals("INT2B") || datatype.equals("INT16") || datatype.equals("INT2S")) {
            datatype = "SHORT";
        } else if (datatype.equals("INT4B")) {
            datatype = "INT";
        } else if (datatype.equals("INT8B") || datatype.equals("INT32")) {
            datatype = "LONG";
        } else if (datatype.equals("FLT4B") || datatype.equals("FLOAT32") || datatype.equals("FLT4S")) {
            datatype = "FLOAT";
        } else if (datatype.equals("FLT8B")) {
            datatype = "DOUBLE";
        } // if you rather use Java keyworddatatype...
        else if (datatype.equals("BYTE")) {
            datatype = "BYTE";
        } else if (datatype.equals("SHORT")) {
            datatype = "SHORT";
        } else if (datatype.equals("INT")) {
            datatype = "INT";
        } else if (datatype.equals("LONG")) {
            datatype = "LONG";
        } else if (datatype.equals("FLOAT")) {
            datatype = "FLOAT";
        } else if (datatype.equals("DOUBLE")) {
            datatype = "DOUBLE";
        } // some backwards compatibility
        else if (datatype.equals("INTEGER")) {
            datatype = "INT";
        } else if (datatype.equals("SMALLINT")) {
            datatype = "INT";
        } else if (datatype.equals("SINGLE")) {
            datatype = "FLOAT";
        } else if (datatype.equals("REAL")) {
            datatype = "FLOAT";
        } else {
            System.out.println("GRID unknown type: " + datatype);
            datatype = "UNKNOWN";
        }

        return datatype;
    }

    static int getByteCount(String datatype) {
        int nbytes;
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
        } else if (datatype.equals("FLOAT")) {
            nbytes = 4;
        } else if (datatype.equals("DOUBLE")) {
            nbytes = 8;
        } else {
            nbytes = 0;
        }
        return nbytes;
    }

    static double[] getMinMax(int nbits, String datatype, int nrows, int ncols, String byteOrder, double missingValue, File bilFile) {
        double[] minmax = new double[2];
        minmax[0] = Double.NaN;
        minmax[1] = Double.NaN;
        try {
            RandomAccessFile raf = new RandomAccessFile(bilFile, "r");
            byte[] b = new byte[(int) raf.length()];
            raf.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);
            raf.close();

            if (byteOrder == null || byteOrder.equals("m")) {
                bb.order(ByteOrder.BIG_ENDIAN);
            } else {
                bb.order(ByteOrder.LITTLE_ENDIAN);
            }

            int i;
            int length = nrows * ncols;
            if (datatype.equalsIgnoreCase("UBYTE")
                    || datatype.equalsIgnoreCase("INT1U")) {
                for (i = 0; i < length; i++) {
                    double ret = bb.get();
                    if (ret < 0) {
                        ret += 256;
                    }
                    updateMinMax(minmax, ret, missingValue);
                }
            } else if (datatype.equalsIgnoreCase("BYTE")
                    || datatype.equalsIgnoreCase("INT1BYTE")
                    || datatype.equalsIgnoreCase("INT1B")) {

                for (i = 0; i < length; i++) {
                    updateMinMax(minmax, (double) bb.get(), missingValue);
                }
            } else if (nbits == 16 /*datatype.equalsIgnoreCase("SHORT")
                    || datatype.equalsIgnoreCase("INT2BYTES")
                    || datatype.equalsIgnoreCase("INT2B")
                    || datatype.equalsIgnoreCase("INT16")
                    || datatype.equalsIgnoreCase("INT2S")*/) {
                for (i = 0; i < length; i++) {
                    updateMinMax(minmax, (double) bb.getShort(), missingValue);
                }
            } else if (datatype.equalsIgnoreCase("INT")
                    || datatype.equalsIgnoreCase("INTEGER")
                    || datatype.equalsIgnoreCase("INT4BYTES")
                    || datatype.equalsIgnoreCase("INT4B")
                    || datatype.equalsIgnoreCase("INT32")
                    || datatype.equalsIgnoreCase("SMALLINT")) {
                for (i = 0; i < length; i++) {
                    updateMinMax(minmax, (double) bb.getInt(), missingValue);
                }
            } else if (datatype.equalsIgnoreCase("LONG")
                    || datatype.equalsIgnoreCase("INT8BYTES")
                    || datatype.equalsIgnoreCase("INT8B")
                    || datatype.equalsIgnoreCase("INT64")) {
                for (i = 0; i < length; i++) {
                    updateMinMax(minmax, (double) bb.getLong(), missingValue);
                }
            } else if (datatype.equalsIgnoreCase("FLOAT")
                    || datatype.equalsIgnoreCase("FLT4BYTES")
                    || datatype.equalsIgnoreCase("FLT4B")
                    || datatype.equalsIgnoreCase("FLOAT32")
                    || datatype.equalsIgnoreCase("FLT4S")
                    || datatype.equalsIgnoreCase("REAL")
                    || datatype.equalsIgnoreCase("SINGLE")) {
                for (i = 0; i < length; i++) {
                    updateMinMax(minmax, (double) bb.getFloat(), missingValue);
                }
            } else if (datatype.equalsIgnoreCase("DOUBLE")
                    || datatype.equalsIgnoreCase("FLT8BYTES")
                    || datatype.equalsIgnoreCase("FLT8B")
                    || datatype.equalsIgnoreCase("FLOAT64")
                    || datatype.equalsIgnoreCase("FLT8S")) {
                for (i = 0; i < length; i++) {
                    updateMinMax(minmax, bb.getDouble(), missingValue);
                }
            } else {
                System.out.println("UNKNOWN TYPE: " + datatype);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return minmax;
    }

    static void updateMinMax(double[] minmax, double d, double missingValue) {
        if (d != missingValue) {
            if (Double.isNaN(minmax[0])) {
                minmax[0] = d;
                minmax[1] = d;
            } else {
                if (minmax[0] > d) {
                    minmax[0] = d;
                }
                if (minmax[1] < d) {
                    minmax[1] = d;
                }
            }
        }
    }
}
