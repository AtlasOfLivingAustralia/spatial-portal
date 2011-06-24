/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.service;

import java.util.ArrayList;
import org.ala.spatial.analysis.index.BoundingBoxes;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.util.AnalysisJobSampling;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.analysis.index.OccurrenceRecordNumbers;
import org.ala.spatial.analysis.index.OccurrencesCollection;
import org.ala.spatial.analysis.index.OccurrencesFilter;
import org.ala.spatial.analysis.index.SpeciesColourOption;


/**
 *
 * @author Adam
 */
public class SamplingLoadedPointsService extends SamplingService {

    public static boolean isLoadedPointsLSID(String lsid) {
        return LoadedPointsService.getLoadedPoints(lsid) != null;
    }

    public static int registerEnvelope(String key, String lsid, String areaParam) {
        //get envelope info
        LayerFilter[] lf = FilteringService.getFilters(areaParam);

        //sample for these additional columns/filters
        String [] layers = new String[lf.length];
        for(int i=0;i<layers.length;i++) {
            layers[i] = lf[i].layername;
        }
        String s = LoadedPointsService.getSampling(lsid, Layers.getLayers(layers), null, null, Integer.MAX_VALUE);
        
        //parse to new input file
        String [] slist = s.split("\r\n");
        double [][] points = new double[slist.length - 1][2];   //-1 for header
        String [] ids = new String[slist.length - 1];           //-1 for header

        int pos = 0;
        for(int i=1;i<slist.length;i++) {   //+1 for header
            String [] row = slist[i].split(",");
            if(row.length > 0) {
                ids[pos] = row[0];
            }
            if(row.length > 1) {
                try {
                    points[pos][0] = Double.parseDouble(row[1]);
                } catch (Exception e) {
                    points[pos][0] = Double.NaN;
                }
            }
            if(row.length > 2) {
                try {
                    points[pos][1] = Double.parseDouble(row[2]);
                } catch (Exception e) {
                    points[pos][1] = Double.NaN;
                }
            }

            //apply envelope, only environmentals
            //TODO: contextuals
            boolean ok = true;
            for(int j=3;ok = true && j<row.length;j++) {
                try {
                    double d = Double.parseDouble(row[j]);
                    ok = lf[j-3].minimum_value <= d
                            && lf[j-3].maximum_value >= d;
                } catch (Exception e) {
                }
            }

            if(ok) {
                pos++;
            }
        }

        if(pos > 0) {
            double [][] pointsTrim = new double[pos][2];
            String [] idsTrim = new String[pos];
            System.arraycopy(points, 0, pointsTrim, 0, pos);
            System.arraycopy(ids, 0, idsTrim, 0, pos);
            LoadedPointsService.addCluster(key, new LoadedPoints(pointsTrim, "" , idsTrim));
        }

        return pos;
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
//        double[] bb = BoundingBoxes.getLsidBoundingBoxDouble(filter);
//        double[][] regionbb = region.getBoundingBox();
//        if (bb[0] <= regionbb[1][0] && bb[2] >= regionbb[0][0]
//                && bb[1] <= regionbb[1][1] && bb[3] >= regionbb[0][1]) {
//
//            double[] points = sampleSpeciesPoints(filter, null, null);
//
//            // test for region absence
//            if (region == null || points == null) {
//                return points;
//            }
//
//            //TODO: nicer 'get'
//            //TODO: caching on 'extra' data
//            int[] field = null;
//            double[] field_output = null;
//            if (extra != null) {
//                for (int i = 0; i < extra.size(); i++) {
//                    extra.get(i).assignMissingData(points.length / 2);
//                }
//            }
//
//            int i;
//            int count = 0;
//
//            // return all valid points within the region
//            double[] output = new double[points.length];
//            for (i = 0; i < points.length; i += 2) {
//
//                //region test
//                if (region.isWithin(points[i], points[i + 1])) {
//                    if (field != null) {
//                        //uncertainty
//                        field_output[count / 2] = Double.NaN; //30000; //default 30km
//                    }
//
//                    output[count] = points[i];
//                    count++;
//                    output[count] = points[i + 1];
//                    count++;
//                }
//            }
//
//            if (count > 0) {
//                if (field != null) {
//                    //extra.set(1, java.util.Arrays.copyOf(field_output, count / 2));
//                }
//
//                return java.util.Arrays.copyOf(output, count);
//            }
//        }

        //test on bounding box
        double[] bb = BoundingBoxes.getLsidBoundingBoxDouble(filter);

        if (region == null) {
            return OccurrencesCollection.getPoints(new OccurrencesFilter(filter, region, records, TabulationSettings.MAX_RECORD_COUNT_CLUSTER), extra);
        }

        double[][] regionbb = region.getBoundingBox();
        if (bb[0] <= regionbb[1][0] && bb[2] >= regionbb[0][0]
                && bb[1] <= regionbb[1][1] && bb[3] >= regionbb[0][1]) {

            /* get points */
            return OccurrencesCollection.getPoints(new OccurrencesFilter(filter, region, records, TabulationSettings.MAX_RECORD_COUNT_CLUSTER), extra);
        }

        return null;
    }
}
