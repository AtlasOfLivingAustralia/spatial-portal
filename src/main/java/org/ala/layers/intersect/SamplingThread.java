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
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.layers.dto.GridClass;
import org.ala.layers.dto.IntersectionFile;
import org.apache.log4j.Logger;

/**
 *
 * @author Adam
 */
public class SamplingThread extends Thread {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(SamplingThread.class);
    LinkedBlockingQueue<Integer> lbq;
    CountDownLatch cdl;
    double[][] points;
    IntersectionFile[] intersectionFiles;
    ArrayList<String> output;
    int threadCount;
    SimpleShapeFileCache simpleShapeFileCache;
    int gridBufferSize;

    public SamplingThread(LinkedBlockingQueue<Integer> lbq, CountDownLatch cdl, IntersectionFile[] intersectionFiles, double[][] points, ArrayList<String> output, int threadCount, SimpleShapeFileCache simpleShapeFileCache, int gridBufferSize) {
        this.lbq = lbq;
        this.cdl = cdl;
        this.points = points;
        this.intersectionFiles = intersectionFiles;
        this.output = output;
        this.threadCount = threadCount;
        this.simpleShapeFileCache = simpleShapeFileCache;
        this.gridBufferSize = gridBufferSize;

        setPriority(MIN_PRIORITY);
    }

    public void run() {
        try {
            while (true) {
                int pos = lbq.take();
                try {
                    StringBuilder sb = new StringBuilder();
                    sample(points, intersectionFiles[pos], sb);
                    output.set(pos, sb.toString());
                } catch (Exception e) {
                }

                cdl.countDown();
            }
        } catch (Exception e) {
        }
    }

    public void sample(double[][] points, IntersectionFile intersectionFile, StringBuilder sb) {
        if (intersectionFile == null) {
            return;
        }

        HashMap<Integer, GridClass> classes = intersectionFile.getClasses();
        String shapeFieldName = intersectionFile.getShapeFields();
        String fileName = intersectionFile.getFilePath();
        String name = intersectionFile.getFieldId();
        long start = System.currentTimeMillis();
        logger.info("start sampling " + points.length + " points in " + name + ":" + fileName + (shapeFieldName == null ? "" : " field: " + shapeFieldName));
        if (shapeFieldName != null) {
            intersectShape(fileName, shapeFieldName, points, sb);
        } else if (classes != null) {
            intersectGridAsContextual(fileName, classes, points, sb);
        } else {
            intersectGrid(fileName, points, sb);
        }

        logger.info("finished sampling " + points.length + " points in " + name + ":" + fileName + " in " + (System.currentTimeMillis() - start) + "ms");
    }

    public void intersectGrid(String filename, double[][] points, StringBuilder sb) {
        try {
            Grid grid = new Grid(filename);
            float[] values = null;

            //values = grid.getValues2(points);
            values = grid.getValues3(points, gridBufferSize);

            if (values != null) {
                for (int i = 0; i < points.length; i++) {
                    if (i > 0) {
                        sb.append("\n");
                    }
                    if (!Float.isNaN(values[i])) {
                        sb.append(values[i]);
                    }
                }
            } else {
                for (int i = 1; i < points.length; i++) {
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            logger.error("Error with grid: " + filename, e);
            e.printStackTrace();
        }
    }

    public void intersectGridAsContextual(String filename, HashMap<Integer, GridClass> classes, double[][] points, StringBuilder sb) {
        try {
            Grid grid = new Grid(filename);
            float[] values = null;
            GridClass gc;

            //values = grid.getValues2(points);
            values = grid.getValues3(points, gridBufferSize);

            if (values != null) {
                for (int i = 0; i < points.length; i++) {
                    if (i > 0) {
                        sb.append("\n");
                    }
                    gc = classes.get(values[i]);
                    if (gc != null) {
                        sb.append(gc.getName());
                    }
                }
            } else {
                for (int i = 1; i < points.length; i++) {
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            logger.error("Error with grid: " + filename, e);
            e.printStackTrace();
        }
    }

    void intersectShape(String filename, String fieldName, double[][] points, StringBuilder sb) {
        try {
            SimpleShapeFile ssf = null;

            if (simpleShapeFileCache == null) {
            } else {
                ssf = simpleShapeFileCache.get(filename);
            }

            if (ssf == null) {
                logger.debug("shape file not in cache: " + filename);
                ssf = new SimpleShapeFile(filename, fieldName);
            }

            String[] catagories;
            int column_idx = ssf.getColumnIdx(fieldName);
            catagories = ssf.getColumnLookup(column_idx);

            int[] values = ssf.intersect(points, catagories, column_idx, threadCount);

            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        sb.append("\n");
                    }
                    if (values[i] >= 0) {
                        sb.append(catagories[values[i]]);
                    }
                }
            } else {
                for (int i = 1; i < points.length; i++) {
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error with shapefile: " + filename, e);
        }
    }
}
