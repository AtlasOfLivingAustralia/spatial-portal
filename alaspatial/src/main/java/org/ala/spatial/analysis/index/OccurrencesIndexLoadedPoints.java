package org.ala.spatial.analysis.index;

import java.util.ArrayList;
import java.util.Vector;
import org.ala.spatial.analysis.cluster.Record;
import org.ala.spatial.analysis.service.LoadedPointsService;
import org.ala.spatial.analysis.service.SamplingLoadedPointsService;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.Layers;
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
        //String layer1, double x1, double x2, String layer2, double y1, double y2
        int[] r = getRecordNumbers(new OccurrencesFilter(lsid, PART_SIZE_MAX)); //TODO: config limited
        if (r == null || r.length == 0) {
            RecordSelectionLookup.addSelection(getHash() + keyEnd, null);
            return 0;
        }

        int len = r.length;
        int numFilters = filters.length;
        int count = 0;

        byte[] highlightCount = new byte[len];

        for(int j=0;j<numFilters;j++) {
            Object [] f = (Object[]) filters[j];
            if(f.length == 2) { //sco or contextual
                SpeciesColourOption sco = SpeciesColourOption.fromName((String) f[0], false);
                if(sco != null) {
                    sco.assignData(r, attributesMap.get(sco.getName()));
                    boolean [] h = sco.getFiltered((Object[]) f[1]);
                    for (int i = 0; i < len; i++) {
                        if(h[i]){
                            highlightCount[i]++;
                        }
                    }
                } else { //contextual
                    Layer layer = Layers.getLayer((String) f[0]);
                    String [] filteredCategories = (String []) f[1];
                    
                    //get data
                    //int [] cat = ss.getRecordsInt(layer.name, r);
                    Layer [] ls = new Layer[1];
                    ls[0] = layer;
                    String [] s = LoadedPointsService.getSampling(lsid,
                            ls, null, null, PART_SIZE_MAX).split("\n");//TODO: max record count

                    for (int i = 0; i < len; i++) {
                        String [] sr = s[i+1].split(",");  //s has a header, so +1
                        String v = sr[sr.length-1];
                        for(int k=0;k<filteredCategories.length;k++) {                            
                            if(filteredCategories[k].equalsIgnoreCase(v)) {
                                highlightCount[i]++;
                            }
                        }
                    }
                }
            } else if(f.length == 3) { //environmental
                double min = Math.min((Double)f[1], (Double)f[2]);
                double max = Math.max((Double)f[1], (Double)f[2]);

                Layer [] ls = new Layer[1];
                ls[0] = Layers.getLayer((String) f[0]);
                String [] s = LoadedPointsService.getSampling(lsid,
                            ls, null, null, PART_SIZE_MAX).split("\n");//TODO: max record count

                for (int i = 0; i < len; i++) {
                    String [] sr = s[i+1].split(",");  //s has a header, so +1
                    double d = 0;
                    try {
                        d = Double.parseDouble(sr[sr.length-1]);
                    } catch (Exception e) {}

                    if(d <= max && d >= min) {
                        highlightCount[i]++;
                    } else if(Double.isNaN(d) && max >= 0 && min <=0) {
                        highlightCount[i]++;
                    }
                }
            }
        }

        boolean[] highlight = new boolean[len];

        //AND
        for(int i=0;i<len;i++) {
            if(highlightCount[i] == numFilters) {
                highlight[i] = true;
                count++;
            }
        }

//        //OR
//        for(int i=0;i<highlight.length;i++) {
//            if(count[i] > 0) {
//                highlight[i] = true;
//                count++;
//            }
//        }

        RecordSelectionLookup.addSelection(getHash() + keyEnd, highlight);

        return count;
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

    double[] getPoints(/*SpeciesColourOption sco*/ String lookupName, String key) {
        return null;
    }

    String[] listLookups() {
        return null;
    }

    public int[] lookup(String lookupName, String key) {
        return null;
    }
}
