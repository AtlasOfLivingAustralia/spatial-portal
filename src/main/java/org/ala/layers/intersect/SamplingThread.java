/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.layers.intersect;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
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
    IntersectionFile [] intersectionFiles;
    ArrayList<String[]> output;
    int threadCount;
    SimpleShapeFileCache simpleShapeFileCache;

    public SamplingThread(LinkedBlockingQueue<Integer> lbq, CountDownLatch cdl, IntersectionFile [] intersectionFiles, double[][] points, ArrayList<String[]> output, int threadCount, SimpleShapeFileCache simpleShapeFileCache) {
        this.lbq = lbq;
        this.cdl = cdl;
        this.points = points;
        this.intersectionFiles = intersectionFiles;
        this.output = output;
        this.threadCount = threadCount;
        this.simpleShapeFileCache = simpleShapeFileCache;

        setPriority(MIN_PRIORITY);
    }

    public void run() {
        try {
            while (true) {
                int pos = lbq.take();
                try {                    
                    sample(points, intersectionFiles[pos], output.get(pos));
                } catch (Exception e) {
                }

                cdl.countDown();
            }
        } catch (Exception e) {
        }
    }

    public void sample(double[][] points, IntersectionFile intersectionFile, String[] output) {
        String shapeFieldName = intersectionFile.getShapeFields();
        String fileName = intersectionFile.getFilePath();
        String name = intersectionFile.getName();
        long start = System.currentTimeMillis();
        logger.info("start sampling " + points.length + " points in " + name + ":" + fileName + " field: " + shapeFieldName);
        if(shapeFieldName != null) {
            intersectShape(fileName, shapeFieldName, points, output);
        } else {
            intersectGrid(fileName, points, output);
        }

        logger.info("finished sampling " + points.length + " points in " + name + ":" + fileName + " in " + (System.currentTimeMillis() - start) + "ms");  
    }

    public void intersectGrid(String filename, double [][] points, String [] output) {
        try {
            Grid grid = new Grid(filename);
            float[] values = null;

            values = grid.getValues2(points);

            if(values != null) {
                for(int i=0;i<output.length;i++) {
                    if(Float.isNaN(values[i])) {
                        output[i] = "";
                    } else {
                        output[i] = String.valueOf(values[i]);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error with grid: " + filename, e);
            e.printStackTrace();
        }
    }

    void intersectShape(String filename, String fieldName, double[][] points, String [] output) {
        try {
            SimpleShapeFile ssf = null;

            if(simpleShapeFileCache == null) {
                logger.info("intersectConfigDao == null");
            } else {
                ssf = simpleShapeFileCache.get(filename);
            }

            if(ssf == null) {
                ssf = new SimpleShapeFile(filename, fieldName);
            }

            String[] catagories;
            int column_idx = ssf.getColumnIdx(fieldName);
            catagories = ssf.getColumnLookup(column_idx);

            int [] values = ssf.intersect(points, catagories, column_idx, threadCount);

            if(values != null) {
                for(int i=0;i<output.length;i++) {
                    if(values[i] >= 0) {
                        output[i] = catagories[values[i]];
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error with shapefile: " + filename, e);
        }
    }

}
