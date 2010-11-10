/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.service;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import org.ala.spatial.util.AnalysisJobSampling;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;

/**
 *
 * @author Adam
 */
public class SamplingLoadedPointsService extends SamplingService {

    public static boolean isLoadedPointsLSID(String lsid) {
        return LoadedPointsService.getLoadedPoints(lsid) != null;
    }

    /**
     * constructor init
     */
    public SamplingLoadedPointsService() {
        TabulationSettings.load();
    }

    /**
     * gets samples; occurrences records + optional intersecting layer values,
     *
     *
     * limit output
     *
     * @param filter species name as String
     * @param layers list of layer names of additional data to include as String []
     * @param region region to restrict results as SimpleRegion
     * @param records sorted pool of records to intersect with as ArrayList<Integer>
     * @param max_rows upper limit of records to return as int
     * @return samples as grid, String [][]
     */
    @Override
    public String[][] sampleSpecies(String filter, String[] layers, SimpleRegion region, ArrayList<Integer> records, int max_rows, AnalysisJobSampling job) {

        String sample = LoadedPointsService.getSampling(filter, Layers.getLayers(layers), region, records, max_rows);

        if (sample != null && sample.length() > 0) {
            String[] ss = sample.split("\r\n");
            int columnCount = ss[0].split(",").length;
            String[][] output = new String[ss.length][columnCount];
            for (int j = 0; j < ss.length; j++) {
                String[] sl = ss[j].split(",");
                int len = Math.min(columnCount, sl.length);
                for (int i = 0; i < len; i++) {
                    output[j][i] = sl[i];
                }
            }
            return output;
        }
        return null;
    }

    /**
     * gets array of points for species (genus, etc) name matches within
     * a specified region
     *
     * @param filter species (genus, etc) name
     * @param region region to filter results by
     * @param records sorted pool of records to intersect with as ArrayList<Integer>
     * @return points as double[], first is longitude, every second is latitude.
     */
    public double[] sampleSpeciesPoints(String filter, SimpleRegion region, ArrayList<Integer> records) {
        return LoadedPointsService.getPointsFlat(filter, region, records);
    }

    /**
     * for Sensitive Coordinates
     *
     * gets array of points for species (genus, etc) name matches within
     * a specified region
     *
     * @param filter species (genus, etc) name
     * @param region region to filter results by
     * @param records sorted pool of records to intersect with as ArrayList<Integer>
     * @return points as double[], first is longitude, every second is latitude.
     */
    public double[] sampleSpeciesPointsSensitive(String filter, SimpleRegion region, ArrayList<Integer> records) {
        return sampleSpeciesPoints(filter, region, records);
    }

    public static String getLSIDAsGeoJSON(String lsid, File outputpath) {
        int i;

        /* get samples records from records indexes */
        String[][] samples = (new SamplingLoadedPointsService()).sampleSpecies(lsid, null, null, null, TabulationSettings.MAX_RECORD_COUNT_DOWNLOAD, null);

        StringBuffer sbGeoJSON = new StringBuffer();
        sbGeoJSON.append("{");
        sbGeoJSON.append("  \"type\": \"FeatureCollection\",");
        sbGeoJSON.append("  \"features\": [");
        for (i = 1; i < samples.length; i++) {
            String s = getRecordAsGeoJSON(samples, i);
            if (s != null) {
                sbGeoJSON.append(s);
                if (i < samples.length - 1) {
                    sbGeoJSON.append(",");
                }
            }
        }
        sbGeoJSON.append("  ],");
        sbGeoJSON.append("  \"crs\": {");
        sbGeoJSON.append("    \"type\": \"EPSG\",");
        sbGeoJSON.append("    \"properties\": {");
        sbGeoJSON.append("      \"code\": \"4326\"");
        sbGeoJSON.append("    }");
        sbGeoJSON.append("  }");
        //sbGeoJSON.append(",  \"bbox\": [");
        //sbGeoJSON.append("    ").append(bbox[0][0]).append(",").append(bbox[0][1]).append(",").append(bbox[1][0]).append(",").append(bbox[1][1]);
        //sbGeoJSON.append("  ]");
        sbGeoJSON.append("}");

        /* write samples to a file */
        try {
            File temporary_file = java.io.File.createTempFile("filter_sample", ".csv", outputpath);
            FileWriter fw = new FileWriter(temporary_file);

            fw.write(sbGeoJSON.toString());

            fw.close();

            return temporary_file.getName();	//return location of temp file

        } catch (Exception e) {
            SpatialLogger.log("SamplingLoadedPointsService: getLSIDAsGeoJSON()", e.toString());
            e.printStackTrace();
        }
        return "";

    }

    /**
     * creates a file with geojson for lsid at outputpath
     *
     * returns filename (first line), number of parts (2nd line)
     *
     * @param lsid
     * @param outputpath
     * @return
     */
    public static String getLSIDAsGeoJSONIntoParts(String lsid, File outputpath) {
        int i;

        /* get samples records from records indexes */
        String[][] samples = (new SamplingLoadedPointsService()).sampleSpecies(lsid, null, null, null, TabulationSettings.MAX_RECORD_COUNT_DOWNLOAD, null);

        int max_parts_size = 2000;

        int count = 0;

        //-1 on samples.length for header
        int partCount = (int) Math.ceil((samples.length - 1) / (double) max_parts_size);

        //test for filename, return if it exists
        File file;
        String filename = outputpath + File.separator + lsid.replace(":", "_").replace(".", "_");
        try {
            file = new File(filename + "_" + (partCount - 1));
            if (file.exists()) {
                return lsid.replace(":", "_").replace(".", "_") + "\n" + partCount;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int j = 1; j < samples.length; j += max_parts_size) {

            StringBuffer sbGeoJSON = new StringBuffer();
            sbGeoJSON.append("{");
            sbGeoJSON.append("\"type\": \"FeatureCollection\",");
            sbGeoJSON.append("\"features\": [");
            int len = j + max_parts_size;
            if (len > samples.length) {
                len = samples.length;
            }
            for (i = j; i < len; i++) {
                String s = getRecordAsGeoJSON(samples, i);
                if (s != null) {
                    sbGeoJSON.append(s);
                    if (i < len - 1) {
                        sbGeoJSON.append(",");
                    }
                }
            }
            sbGeoJSON.append("],");
            sbGeoJSON.append("\"crs\": {");
            sbGeoJSON.append("\"type\": \"EPSG\",");
            sbGeoJSON.append("\"properties\": {");
            sbGeoJSON.append("\"code\": \"4326\"");
            sbGeoJSON.append("}");
            sbGeoJSON.append("}");
            sbGeoJSON.append("}");

            /* write samples to a file */
            try {
                //File temporary_file = java.io.File.createTempFile("filter_sample", ".csv", outputpath);
                FileWriter fw = new FileWriter(
                        filename + "_" + count);
                count++;

                fw.write(sbGeoJSON.toString());

                fw.close();

                //return temporary_file.getName();	//return location of temp file

            } catch (Exception e) {
                SpatialLogger.log("SamplingLoadedPointsService: getLSIDAsGeoJSON()", e.toString());
                e.printStackTrace();
            }
        }
        return lsid.replace(":", "_").replace(".", "_") + "\n" + partCount;

    }

    private static String getRecordAsGeoJSON(String[][] rec, int rw) {
        if (rec == null || rec.length <= rw || rec[rw].length <= TabulationSettings.geojson_latitude) {
            return null;
        }

        for (int i = 0; i < TabulationSettings.geojson_latitude; i++) {
            if (rec[rw][i] == null) {
                return null;
            }
        }

        StringBuffer sbRec = new StringBuffer();
        sbRec.append("{");
        sbRec.append("  \"type\":\"Feature\",");
        sbRec.append("  \"id\":\"occurrences.data.").append(rec[rw][0]).append("\",");  //record id at 0
        sbRec.append("  \"geometry\":{");
        sbRec.append("      \"type\":\"Point\",");
        //longitude at [2], latitude at [3]
        sbRec.append("      \"coordinates\":[\"").append(rec[rw][2]).append("\",\"").append(rec[rw][3].trim()).append("\"]");
        sbRec.append("   },");
        sbRec.append("  \"geometry_name\":\"the_geom\",");
        sbRec.append("  \"properties\":{");
        //no properties
        sbRec.append("  }");
        sbRec.append("}");

        return sbRec.toString();
    }
}
