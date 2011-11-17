package org.ala.spatial.sampling;

import java.io.File;
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
        long start = System.currentTimeMillis();
        System.out.println("start sampling " + points.length + " points in " + facetName + ":" + layerName);
        if(shapeFieldName != null) {
            intersectShape(CommonData.settings.get("sampling_files_path") + "shape/" + layerName, shapeFieldName, points, output);
        } else if (facetName.startsWith("el")) {
            intersectGrid(CommonData.settings.get("sampling_files_path") + "diva/" + layerName, points, output);
        } else {
            //look for an analysis layer
            File maxent = new File(CommonData.settings.get("analysis_output_dir") + "maxent/" + facetName + "/" + facetName + ".grd");
            File aloc = new File(CommonData.settings.get("analysis_output_dir") + "aloc/" + facetName + "/" + facetName + ".grd");
            String [] split = facetName.split("_");
            File sxsSRichness = null;
            File sxsODensity = null;
            if(split.length > 1) {
                if(split[1].equals("srichness")) {
                    sxsSRichness = new File(CommonData.settings.get("analysis_output_dir") + "sitesbyspecies/" + split[0] + "/species_richness.grd");
                } else {
                    sxsODensity = new File(CommonData.settings.get("analysis_output_dir") + "sitesbyspecies/" + split[0] + "/occurrence_density.grd");
                }
            }

            System.out.println("Analysis layer: " + facetName);
            System.out.println(maxent.getPath() + ", " + maxent.exists());
            System.out.println(aloc.getPath() + ", " + aloc.exists());
            if(sxsSRichness != null) {
                System.out.println(sxsSRichness.getPath() + ", " + sxsSRichness.exists());                
            }
            if(sxsODensity != null) {
                System.out.println(sxsODensity.getPath() + ", " + sxsODensity.exists());
            }

            File file = null;
            if(maxent.exists()) {
                file = maxent;
            } else if(aloc.exists()) {
                file = aloc;
            } else if(sxsSRichness != null && sxsSRichness.exists()) {
                file = sxsSRichness;
            } else if(sxsODensity != null && sxsODensity.exists()) {
                file = sxsODensity;
            }
            if(file != null) {
                System.out.println("found file for sampling: "+ file.getPath().substring(0, file.getPath().length() -4));
                intersectGrid(file.getPath().substring(0, file.getPath().length() -4), points, output);
            }
        }
        System.out.println("finished sampling " + points.length + " points in " + facetName + ":" + layerName + " in " + (System.currentTimeMillis() - start) + "ms");
    }

    static void intersectGrid(String filename, double [][] points, String [] output) {
        try {
            Grid grid = new Grid(filename);
            float[] values = null;

            
//            if(points.length > 10000) { 
                //load whole grid
                values = grid.getValues3(points, 40960);
//            } else {
//                values = grid.getValues(points);
//            }

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
            SimpleShapeFile ssf = CommonData.ssfCache.get(filename.replace(CommonData.settings.get("sampling_files_path") + "shape/", ""));
            if(ssf == null) {
                ssf = new SimpleShapeFile(filename, fieldName);
            }

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
