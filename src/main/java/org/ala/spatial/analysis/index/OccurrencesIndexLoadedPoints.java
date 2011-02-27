package org.ala.spatial.analysis.index;

import java.util.ArrayList;
import java.util.Vector;
import org.ala.spatial.analysis.cluster.Record;
import org.ala.spatial.analysis.service.LoadedPointsService;
import org.ala.spatial.analysis.service.SamplingLoadedPointsService;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.util.SimpleRegion;

/**
 * builder for occurrences index.
 *
 * expects occurrences in a csv containing at least species, longitude
 * and latitude.
 *
 * expects TabulationSettings fields for
 * 	<li>occurrences csv file location (has header)
 * 	<li>list of relevant hierarchy related columns in occurrences csv
 * 		the last three must relate to species, longitude and latitude
 *	<li>index directory for writing index files
 *	<li>
 *
 * creates and interfaces into:
 * <li><code>OCC_SORTED.csv</code>
 * 		csv with header row.
 *
 * 		records contain:
 * 			optional hierarchy fields, top down order, e.g. Family,
 * 			species name
 * 			longitude as decimal
 * 			latitude as decimal
 *
 * 	<li><code>OCC_SPECIES.csv</code>
 * 		csv with no header row.
 *
 * 		records contain:
 * 			species name
 * 			starting file position occurance in OCC_SORTED.csv
 * 			ending file position occurance in OCC_SORTED.csv
 * 			starting record number in OCC_SORTED.csv
 * 			ending record number in OCC_SORTED.csv
 *
 * @author adam
 *
 */
public class OccurrencesIndexLoadedPoints extends OccurrencesIndex {

    /**
     * constructor
     */
    OccurrencesIndexLoadedPoints(Dataset d, String occurrencesFilename, String directoryName) {
        super(d, occurrencesFilename, directoryName);
    }

    /**
     * performs update of 'indexing' for new points data
     */
    @Override
    public void occurrencesUpdate() {
    }

    /**
     * method to determine if the index is up to date
     *
     * Compares dates on last written index file
     *      <code>index_path + SORTED_CONCEPTID_NAMES + "1.csv"</code>
     * to
     *      <code>occurrences_csv</code>
     *
     * @return true if index is up to date
     */
    @Override
    public boolean isUpToDate() {
        return true;
    }

    /**
     * Loaded Points can require access by an LSID.
     *
     * No 'species in area' functions permitted.
     *
     * @param filter OccurrencesFilter for restricting results.
     *
     * @return int [] or record numbers.  Returns null when no records.
     */
    @Override
    public int[] getRecordNumbers(OccurrencesFilter filter) {
        if (filter.searchTerm == null
                || !SamplingLoadedPointsService.isLoadedPointsLSID(filter.searchTerm)) {
            return null;
        }

        int i, j;

//        int[] records = filter.records;
        SimpleRegion region = filter.region;
        String lsid = filter.searchTerm;
        if (lsid != null) {
            double[][] points = LoadedPointsService.getPoints(lsid, null, null);
            if (points != null) {
                int[] foundRecords = new int[points.length];
                j = 0;
                for (i = 0; i < foundRecords.length && j < filter.maxRecords; i++) {
                    if (region == null || region.isWithin(points[i][0], points[i][1])) {
                        foundRecords[j] = i;
                        j++;
                    }
                }
                if (j > 0) {
                    return java.util.Arrays.copyOf(foundRecords, j);
                }
            }
        }

        return null;
    }

    @Override
    double[] getPoints(OccurrencesFilter filter) {
        if (filter.searchTerm == null
                || !SamplingLoadedPointsService.isLoadedPointsLSID(filter.searchTerm)) {
            return null;
        }

        return LoadedPointsService.getPointsFlat(filter.searchTerm, filter.region, null);
    }

    @Override
    double[] getPoints(OccurrencesFilter filter, ArrayList<SpeciesColourOption> extra) {
        if (filter.searchTerm == null
                || !SamplingLoadedPointsService.isLoadedPointsLSID(filter.searchTerm)) {
            return null;
        }

        double[] points = LoadedPointsService.getPointsFlat(filter.searchTerm, filter.region, null);

        if (extra != null) {
            for(int i=0;i<extra.size();i++) {
                extra.get(i).assignMissingData(points.length / 2);
            }
        }

        return points;
    }

    @Override
    ArrayList<String> getFullRecords(OccurrencesFilter filter) {
        if (filter.searchTerm == null
                || !SamplingLoadedPointsService.isLoadedPointsLSID(filter.searchTerm)) {
            return null;
        }

        SamplingService ss = new SamplingLoadedPointsService();

        String[] columns = null;
        if (filter.columns != null && filter.columns.size() > 0) {
            columns = new String[filter.columns.size()];
            filter.columns.toArray(columns);
        }
        String[][] data = ss.sampleSpecies(filter.searchTerm, columns, filter.region, null, filter.maxRecords);

        ArrayList<String> records = null;
        if (data != null) {
            records = new ArrayList<String>();
            for (int i = 0; i < data.length; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append(data[i][0]);
                for (int j = 1; j < data[i].length; j++) {
                    sb.append(",").append(data[i][j]);
                }
                records.add(sb.toString());
            }
        }

        return records;
    }

    @Override
    Vector<Record> getRecords(OccurrencesFilter filter) {
        if (filter.searchTerm == null
                || !SamplingLoadedPointsService.isLoadedPointsLSID(filter.searchTerm)) {
            return null;
        }

        int i;

 //       int[] records = filter.records;
        SimpleRegion region = filter.region;
        String lsid = filter.searchTerm;
        if (lsid != null) {
            double[][] points = LoadedPointsService.getPoints(lsid, null, null);
            if (points != null) {
                Vector<Record> vr = new Vector<Record>(points.length);

                for (i = 0; i < points.length && vr.size() < filter.maxRecords; i++) {
                    if (region == null || region.isWithin(points[i][0], points[i][1])) {
                        vr.add(new Record(i, null, points[i][0], points[i][1], Integer.MIN_VALUE));
                    }
                }

                if (vr.size() > 0) {
                    return vr;
                }
            }
        }

        return null;
    }

    /**
     * returns a list of (species names / type / count) for valid
     * .beginsWith matches
     *
     * @param filter begins with text to search for (NOT LSID, e.g. species name)
     * @param limit limit on output
     * @return formatted species matches as String[]
     */
    public String[] filterIndex(String filter, int limit) {
        return null;
    }

    @Override
    double[] getPointsMinusSensitiveSpecies(OccurrencesFilter occurrencesFilter, StringBuffer removedSpecies) {
        return getPoints(occurrencesFilter);
    }

    /**
     *
     * @param filter OccurrencesFilter for restricting results.  Supports only
     * one of: <code>filter.record</code> OR <code>filter.region</code>.
     *
     * @return int [] aligning to lowest level species index in
     * <code>all_indexes</code>.  Each value lists the number of occurrences
     * found.
     */
    @Override
    int[] getSpeciesCounts(OccurrencesFilter filter) {
        return null;
    }

    @Override
    int highlightLsid(String keyEnd, String lsid, Object [] filters) {
        return 0;
        /* TODO: finish this function
        SamplingIndex ss = new SamplingIndex(index_path, getPointsPairs());
        int[] r = getRecordNumbers(new OccurrencesFilter(lsid, PART_SIZE_MAX)); //TODO: config limited
        float [] d1 = ss.getRecordsFloat(layer1, r);
        float [] d2 = ss.getRecordsFloat(layer2, r);

        boolean [] highlight = new boolean[r.length];
        int count = 0;
        for(int i=0;i<r.length;i++) {
            if(d1[i] <= x2 && d1[i] >= x1
                    && d2[i] <= y2 && d2[i] >= y1) {
                highlight[i] = true;
                count ++;
            }
        }

        RecordSelectionLookup.addSelection(getHash() + keyEnd, highlight);

        return count;*/
    }

    public int registerLSID(String key, String[] lsid) {
        return 0;
    }

    public int registerArea(String key, SimpleRegion region) {
        return 0;
    }

    public int registerRecords(String key, ArrayList<OccurrenceRecordNumbers> records) {
        return 0;
    }
}
