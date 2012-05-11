package org.ala.spatial.sampling;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Properties;
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
        if (shapeFieldName != null) {
            intersectShape(CommonData.settings.get("sampling_files_path") + "shape/" + layerName, shapeFieldName, points, output);
        } else if (facetName.startsWith("cl")) {
            //shape_diva?
            intersectShapeGrid(CommonData.settings.get("sampling_files_path") + "shape_diva/" + layerName, points, output);
        } else if (facetName.startsWith("el")) {
            intersectGrid(CommonData.settings.get("sampling_files_path") + "diva/" + layerName, points, output);
        } else {
            //look for an analysis layer
//            File maxent = new File(CommonData.settings.get("analysis_output_dir") + "maxent/" + facetName + "/" + facetName + ".grd");
//            File aloc = new File(CommonData.settings.get("analysis_output_dir") + "aloc/" + facetName + "/" + "aloc" + ".grd");
//            String[] split = facetName.split("_");
//            File sxsSRichness = null;
//            File sxsODensity = null;
//            File gdmTransLayer = null;
//            File envelope = null;
//            if(split != null && split.length > 1) {
//                if(split[1].equals("srichness")) {
//                    sxsSRichness = new File(CommonData.settings.get("analysis_output_dir") + "sitesbyspecies/" + split[0] + "/species_richness.grd");
//                } else if(split[1].equals("odensity")) {
//                    sxsODensity = new File(CommonData.settings.get("analysis_output_dir") + "sitesbyspecies/" + split[0] + "/occurrence_density.grd");
//                } else {
//                    int pos = facetName.indexOf("_");
//                    String[] gdmparts = new String [] {facetName.substring(0,pos), facetName.substring(pos+1) };
//                    gdmTransLayer = new File(CommonData.settings.get("analysis_output_dir") + "gdm/" + split[0] + "/" + gdmparts[1] + "Tran.grd");
//                }
//                envelope = new File(CommonData.settings.get("analysis_output_dir") + "envelope/" + split[0] + "/envelope.grd");
//            }
//
//            System.out.println("Analysis layer: " + facetName);
//            System.out.println(maxent.getPath() + ", " + maxent.exists());
//            System.out.println(aloc.getPath() + ", " + aloc.exists());
//            if (sxsSRichness != null) {
//                System.out.println(sxsSRichness.getPath() + ", " + sxsSRichness.exists());
//            }
//            if (sxsODensity != null) {
//                System.out.println(sxsODensity.getPath() + ", " + sxsODensity.exists());
//            }
//            if (gdmTransLayer != null) {
//                System.out.println(gdmTransLayer.getPath() + ", " + gdmTransLayer.exists());
//            }
//            if (envelope != null) {
//                System.out.println(envelope.getPath() + ", " + envelope.exists());
//            }
//
//            File file = null;
//            if (maxent.exists()) {
//                file = maxent;
//            } else if (aloc.exists()) {
//                file = aloc;
//            } else if (sxsSRichness != null && sxsSRichness.exists()) {
//                file = sxsSRichness;
//            } else if (sxsODensity != null && sxsODensity.exists()) {
//                file = sxsODensity;
//            } else if(gdmTransLayer != null && gdmTransLayer.exists()) {
//                file = gdmTransLayer;
//            } else if (envelope != null && envelope.exists()) {
//                file = envelope;
//            }

            String [] info = getAnalysisLayerInfo(facetName);
            if (info != null) {
                System.out.println("found file for sampling: " + info[1]);
                intersectGrid(info[1], points, output);
            }
        }
        System.out.println("finished sampling " + points.length + " points in " + facetName + ":" + layerName + " in " + (System.currentTimeMillis() - start) + "ms");
    }

    static void intersectGrid(String filename, double[][] points, String[] output) {
        try {
            Grid grid = new Grid(filename);
            float[] values = null;


//            if(points.length > 10000) { 
            //load whole grid
            values = grid.getValues3(points, 40960);
//            } else {
//                values = grid.getValues(points);
//            }

            if (values != null) {
                for (int i = 0; i < output.length; i++) {
                    if (Float.isNaN(values[i])) {
                        output[i] = "n/a";
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

    static void intersectShapeGrid(String filename, double[][] points, String[] output) {
        try {
            System.out.println("intersectShapeGrid: " + filename);
            Grid grid = new Grid(filename);
            Properties p = new Properties();
            p.load(new FileReader(filename + ".txt"));
            
            float[] values = null;

            values = grid.getValues3(points, 40960);

            if (values != null) {
                for (int i = 0; i < output.length; i++) {
                    System.out.print(", " + values[i]);
                    if (Float.isNaN(values[i])) {
                        output[i] = "n/a";
                    } else {
                        String v = p.getProperty(String.valueOf((int)values[i]));
                        System.out.print("=" + v);
                        output[i] = v;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error with shape grid: " + filename);
            e.printStackTrace();
        }
    }

    static void intersectShape(String filename, String fieldName, double[][] points, String[] output) {
        try {
            SimpleShapeFile ssf = CommonData.ssfCache.get(filename.replace(CommonData.settings.get("sampling_files_path") + "shape/", ""));
            if (ssf == null) {
                ssf = new SimpleShapeFile(filename, fieldName);
            }

            String[] catagories;
            int column_idx = ssf.getColumnIdx(fieldName);
            catagories = ssf.getColumnLookup(column_idx);

            int[] values = ssf.intersect(points, catagories, column_idx, Integer.parseInt(CommonData.settings.get("sampling_thread_count")));

            if (values != null) {
                for (int i = 0; i < output.length; i++) {
                    if (values[i] >= 0) {
                        output[i] = catagories[values[i]];
                    } else {
                        output[i] = "n/a";
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error with shapefile: " + filename);
            e.printStackTrace();
        }
    }

    public static ArrayList<String[]> sampling(ArrayList<String> facetIds, double[][] points) {
        long start = System.currentTimeMillis();
        int threadCount = Integer.parseInt(CommonData.settings.get("sampling_thread_count"));
        SamplingThread[] threads = new SamplingThread[threadCount];
        LinkedBlockingQueue<Integer> lbq = new LinkedBlockingQueue();
        CountDownLatch cdl = new CountDownLatch(facetIds.size());
        ArrayList<String[]> output = new ArrayList<String[]>();
        for (int i = 0; i < facetIds.size(); i++) {
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

    /**
     * get info on an analysis layer
     * @param id layer id as String
     * @return String [] with [0] = analysis id, [1] = path to grid file, [2] = analysis type
     */
    static public String[] getAnalysisLayerInfo(String id) {
        String analysisOutputPath = CommonData.settings.get("analysis_output_dir");
        String gid, filename, name;
        gid = filename = name = null;
        if (id.startsWith("species_")) {
            //maxent layer
            gid = id.substring("species_".length());
            filename = analysisOutputPath + File.separator + "maxent" + File.separator + gid + File.separator + gid;
            name = "Prediction";
        } else if (id.startsWith("aloc_")) {
            //aloc layer
            gid = id.substring("aloc_".length());
            filename = analysisOutputPath + File.separator + "aloc" + File.separator + gid + File.separator + "aloc";
            name = "Classification";
        } else if (id.startsWith("odensity_")) {
            //occurrence density layer
            gid = id.substring("odensity_".length());
            filename = analysisOutputPath + File.separator + "sitesbyspecies" + File.separator + gid + File.separator + "occurrence_density";
            name = "Occurrence Density";
        } else if (id.startsWith("srichness_")) {
            //species richness layer
            gid = id.substring("srichness_".length());
            filename = analysisOutputPath + File.separator + "sitesbyspecies" + File.separator + gid + File.separator + "species_richness";
            name = "Species Richness";
        } else if (id.endsWith("_odensity")) {
            //occurrence density layer
            gid = id.substring(0, id.length() - "odensity_".length());
            filename = analysisOutputPath + File.separator + "sitesbyspecies" + File.separator + gid + File.separator + "occurrence_density";
            name = "Occurrence Density";
        } else if (id.endsWith("_srichness")) {
            //species richness layer
            gid = id.substring(0, id.length() - "srichness_".length());
            filename = analysisOutputPath + File.separator + "sitesbyspecies" + File.separator + gid + File.separator + "species_richness";
            name = "Species Richness";
        } else if (id.startsWith("envelope_")) {
            //envelope layer
            gid = id.substring("envelope_".length());
            filename = analysisOutputPath + File.separator + "envelope" + File.separator + gid + File.separator + "envelope";
            name = "Environmental Envelope";
        } else if (id.startsWith("gdm_")) {
            //gdm layer
            int pos1 = id.indexOf("_");
            int pos2 = id.lastIndexOf("_");
            String[] gdmparts = new String [] {id.substring(0,pos1), id.substring(pos1+1, pos2), id.substring(pos2+1) };
            gid = gdmparts[2];
            filename = analysisOutputPath + File.separator + "gdm" + File.separator + gid + File.separator + gdmparts[1];
            //Layer tmpLayer = layerDao.getLayerByName(gdmparts[1].replaceAll("Tran", ""));
            //name = "Transformed " + tmpLayer.getDisplayname();
            name = "Transformed " + CommonData.getFacetLayerDisplayName(gdmparts[1].replaceAll("Tran", ""));
        } else if (id.contains("_")) {
            //2nd form of gdm layer name, why?
            int pos = id.indexOf("_");
            String[] gdmparts = new String [] {id.substring(0,pos), id.substring(pos+1) };
            gid = gdmparts[0];
            filename = analysisOutputPath + File.separator + "gdm" + File.separator + gid + File.separator + gdmparts[1] + "Tran";
            System.out.println("id: " + id);
            System.out.println("parts: " + gdmparts[0] + ", " + gdmparts[1]);
            System.out.println("filename: " + filename);
            //Layer tmpLayer = layerDao.getLayerByName(gdmparts[1].replaceAll("Tran", ""));
            //name = "Transformed " + tmpLayer.getDisplayname();
            name = "Transformed " + CommonData.getFacetLayerDisplayName(gdmparts[1]);
        }

        if (gid != null) {
            return new String[] {gid, filename, name};
        } else {
            return null;
        }
    }
}
