/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.service;

import java.util.ArrayList;
import org.ala.spatial.analysis.index.BoundingBoxes;
import org.ala.spatial.analysis.index.OccurrenceRecordNumbers;
import org.ala.spatial.analysis.index.SpeciesColourOption;
import org.ala.spatial.util.AnalysisJobSampling;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.SimpleRegion;
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
    public String[][] sampleSpecies(String filter, String[] layers, SimpleRegion region, ArrayList<OccurrenceRecordNumbers> records, int max_rows, AnalysisJobSampling job) {

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
    @Override
    public double[] sampleSpeciesPoints(String filter, SimpleRegion region, ArrayList<OccurrenceRecordNumbers> records) {
        return LoadedPointsService.getPointsFlat(filter, region, records);
    }

    /**
     * for Sensitive Coordinates
     *
     * gets array of points for species (genus, etc) name matches within
     * a specified region
     *
     * removes points for all species that are sensitive
     *
     * @param filter species (genus, etc) name
     * @param region region to filter results by
     * @param records sorted pool of records to intersect with as ArrayList<Integer>
     * @return points as double[], first is longitude, every second is latitude.
     */
    @Override
    public double[] sampleSpeciesPointsMinusSensitiveSpecies(String filter, SimpleRegion region, ArrayList<OccurrenceRecordNumbers> records, StringBuffer removedSpecies) {
        return sampleSpeciesPoints(filter, region, records);
    }

    /**
     * gets array of points for species (genus, etc) name matches within
     * a specified region
     *
     * can return other field or sampling for points returned
     *
     * @param filter species (genus, etc) name
     * @param region region to filter results by
     * @param records sorted pool of records to intersect with as ArrayList<Integer>
     * @return points as double[], first is longitude, every second is latitude.
     */
    @Override
    public double[] sampleSpeciesPoints(String filter, SimpleRegion region, ArrayList<OccurrenceRecordNumbers> records, ArrayList<SpeciesColourOption> extra) {
        //test on bounding box
        double[] bb = BoundingBoxes.getLsidBoundingBoxDouble(filter);
        double[][] regionbb = region.getBoundingBox();
        if (bb[0] <= regionbb[1][0] && bb[2] >= regionbb[0][0]
                && bb[1] <= regionbb[1][1] && bb[3] >= regionbb[0][1]) {

            double[] points = sampleSpeciesPoints(filter, region, records);

            // test for region absence
            if (region == null || points == null) {
                return points;
            }

            //TODO: nicer 'get'
            //TODO: caching on 'extra' data
            int[] field = null;
            double[] field_output = null;
            if (extra != null) {
                for (int i = 0; i < extra.size(); i++) {
                    extra.get(i).assignMissingData(points.length / 2);
                }
            }

            int i;
            int count = 0;

            // return all valid points within the region
            double[] output = new double[points.length];
            for (i = 0; i < points.length; i += 2) {

                //region test
                if (region.isWithin(points[i], points[i + 1])) {
                    if (field != null) {
                        //uncertainty
                        field_output[count / 2] = Double.NaN; //30000; //default 30km
                    }

                    output[count] = points[i];
                    count++;
                    output[count] = points[i + 1];
                    count++;
                }
            }

            if (count > 0) {
                if (field != null) {
                    //extra.set(1, java.util.Arrays.copyOf(field_output, count / 2));
                }

                return java.util.Arrays.copyOf(output, count);
            }
        }

        return null;
    }
}
