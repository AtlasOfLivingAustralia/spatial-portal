package org.ala.spatial.sampling;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.spatial.util.CommonData;

/**
 * builder for sampling index.
 *
 * requires OccurrencesIndex to be up to date.
 *
 * operates on GridFiles
 * operates on Shape Files
 *
 * @author adam
 *
 */
public class Sampling {
    static void sample(double[][] points, String facetName, String[] output) {
        String shapeFieldName = CommonData.getFacetShapeNameField(facetName);
        String layerName = CommonData.getFacetLayerName(facetName);
        if(shapeFieldName != null) {
            intersectShape(CommonData.settings.get("sampling_files_path") + layerName, shapeFieldName, points, output);
        } else {
            intersectGrid(CommonData.settings.get("sampling_files_path") + layerName, points, output);
        }
    }

    static void intersectGrid(String filename, double [][] points, String [] output) {
        try {
            Grid grid = new Grid(filename);
            float[] values = grid.getValues2(points);

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
            System.out.println("Error with grid: " + filename);
            e.printStackTrace();
        }
    }

    static void intersectShape(String filename, String fieldName, double[][] points, String [] output) {
        try {
            SimpleShapeFile ssf = new SimpleShapeFile(filename);

            String[] catagories;
            int column_idx = ssf.getColumnIdx(fieldName);
            catagories = ssf.getColumnLookup(column_idx);

            int [] values = ssf.intersect(points, catagories, column_idx, Integer.parseInt(CommonData.settings.get("sampling_thread_count")));

            if(values != null) {
                for(int i=0;i<output.length;i++) {
                    if(values[i] >= 0) {
                        output[i] = catagories[values[i]];
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error with shapefile: " + filename);
            e.printStackTrace();
        }
    }

    public static ArrayList<String []> sampling(ArrayList<String> facetIds, double [][] points) {
        long start = System.currentTimeMillis();
        int threadCount = Integer.parseInt(CommonData.settings.get("sampling_thread_count"));
        SamplingThread[] threads = new SamplingThread[threadCount];
        LinkedBlockingQueue<Integer> lbq = new LinkedBlockingQueue();
        CountDownLatch cdl = new CountDownLatch(facetIds.size());
        ArrayList<String []> output = new ArrayList<String []>();
        for(int i=0;i<facetIds.size();i++) {
            output.add(new String[points.length]);
            lbq.add(i);
        }

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new SamplingThread(lbq, cdl, facetIds, points, output);
            threads[i].start();
        }
        try {
            cdl.await();
        } catch (InterruptedException ex) {
            Logger.getLogger(Sampling.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            for (int i = 0; i < threadCount; i++) {
                try {
                    threads[i].interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("End sampling, threads=" + threadCount
                + " layers=" + facetIds.size()
                + " in " + (System.currentTimeMillis() - start) + "ms");

        return output;
    }
}
