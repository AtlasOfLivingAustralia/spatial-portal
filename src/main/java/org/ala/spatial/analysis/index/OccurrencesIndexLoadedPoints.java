package org.ala.spatial.analysis.index;

import java.util.ArrayList;
import java.util.Vector;
import org.ala.spatial.analysis.cluster.Record;
import org.ala.spatial.analysis.service.LoadedPoints;
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

        double[] points = LoadedPointsService.getPointsFlat(filter.searchTerm, null, null);
        int[] r = new int[points.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = i;
        }

        if (extra != null) {
            for (int j = 0; j < extra.size(); j++) {
                if (extra.get(j).isHighlight()) {
                    //treat lsid matches different to fake-lsid-lists
                    if (filter.searchTerm != null && SpeciesIndex.findLSID(filter.searchTerm) >= 0) {
                        //lookup with dataset hash and species offset
                        IndexedRecord ir = filterSpeciesRecords(filter.searchTerm);
                        extra.get(j).assignHighlightData(r, (ir == null) ? 0 : ir.record_start, getHash());
                    } else if (filter.searchTerm != null) {
                        //'highlight' are stored by 'searchTerm' only,
                        //retrieve whole records and translate r to
                        //all 'highlight' records
                        int[] rAll = getRecordNumbers(new OccurrencesFilter(filter.searchTerm, filter.maxRecords));
                        int[] r2 = new int[r.length];
                        int rpos = 0;
                        for (int i = 0; i < rAll.length; i++) {
                            if (r[rpos] == rAll[i]) {
                                r2[rpos] = i;
                                rpos++;
                                if (rpos >= r.length) {
                                    break;
                                }
                            }
                        }
                        extra.get(j).assignHighlightData(r2, 0, getHash());
                    } //cannot get 'highlight' without searchTerm
                } else if (extra.get(j).isTaxon()) {
                    extra.get(j).assignTaxon(r, speciesNumberInRecordsOrder, speciesIndexLookup);
                    //extra.get(j).assignMissingData(points.length / 2);
                } else {
                    extra.get(j).assignData(r, attributesMap.get(extra.get(j).getName()));
                    //extra.get(j).assignMissingData(points.length / 2);
                }
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
            String [] ids = LoadedPointsService.getIds(lsid);
            if (points != null) {
                Vector<Record> vr = new Vector<Record>(points.length);

                for (i = 0; i < points.length && vr.size() < filter.maxRecords; i++) {
                    if (region == null || region.isWithin(points[i][0], points[i][1])) {
                        vr.add(new Record(Long.MIN_VALUE, 'u' + ids[i], points[i][0], points[i][1], Integer.MIN_VALUE));
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
    int highlightLsid(String keyEnd, String lsid, Object[] filters) {
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


        //do and's first
        boolean[] highlight = new boolean[len];
        int numAndFilters = 0;
        for (int andOr = 0; andOr < 2; andOr++) {
            for (int j = 0; j < numFilters; j++) {
                Object[] f = (Object[]) filters[j];

                if (((String) f[0]).equalsIgnoreCase("and") == (andOr == 0)) {
                    numAndFilters++;
                    if (f.length == 3) { //sco or contextual
                        SpeciesColourOption sco = SpeciesColourOption.fromName((String) f[1], false);
                        if (sco != null) {
                            sco.assignData(r, attributesMap.get(sco.getName()));
                            boolean[] h = sco.getFiltered((Object[]) f[2]);
                            for (int i = 0; i < len; i++) {
                                if (h[i]) {
                                    highlightCount[i]++;
                                }
                            }
                        } else { //contextual
                            Layer layer = Layers.getLayer((String) f[1]);
                            String[] filteredCategories = (String[]) f[2];

                            //get data
                            Layer[] ls = new Layer[1];
                            ls[0] = layer;
                            String[] s = LoadedPointsService.getSampling(lsid,
                                    ls, null, null, PART_SIZE_MAX).split("\n");//TODO: max record count

                            for (int i = 0; i < len; i++) {
                                String[] sr = s[i + 1].split(",");  //s has a header, so +1
                                String v = sr[sr.length - 1];
                                for (int k = 0; k < filteredCategories.length; k++) {
                                    if (filteredCategories[k].equalsIgnoreCase(v)) {
                                        highlightCount[i]++;
                                    }
                                }
                            }
                        }
                    } else if (f.length == 4) { //environmental
                        double min = Math.min((Double) f[2], (Double) f[3]);
                        double max = Math.max((Double) f[2], (Double) f[3]);

                        Layer[] ls = new Layer[1];
                        ls[0] = Layers.getLayer((String) f[1]);
                        String[] s = LoadedPointsService.getSampling(lsid,
                                ls, null, null, PART_SIZE_MAX).split("\n");//TODO: max record count

                        for (int i = 0; i < len; i++) {
                            String[] sr = s[i + 1].split(",");  //s has a header, so +1
                            double d = 0;
                            try {
                                d = Double.parseDouble(sr[sr.length - 1]);
                            } catch (Exception e) {
                                d = Double.NaN;
                            }

                            if (d <= max && d >= min) {
                                highlightCount[i]++;
                            } else if (Double.isNaN(d) && (Double.isNaN(min) || Double.isNaN(max))) {
                                highlightCount[i]++;
                            }
                        }
                    }
                }
            }
            if (andOr == 0) {
                //AND update
                if (numAndFilters > 0) {
                    for (int i = 0; i < len; i++) {
                        if (highlightCount[i] == numAndFilters) {
                            highlight[i] = true;
                        } else {
                            highlightCount[i] = 0; //reset for OR test
                        }
                    }
                }
            } else {
                //OR update
                for (int i = 0; i < highlight.length; i++) {
                    if (highlightCount[i] > 0) {
                        highlight[i] = true;
                        count++;
                    }
                }
            }
        }

        RecordSelectionLookup.addSelection(getHash() + keyEnd, highlight);

        return count;
    }

    @Override
    public int registerLSIDArea(String key, String lsid, SimpleRegion region) {
        return registerHighlight(new OccurrencesFilter(lsid, region, null, Integer.MAX_VALUE), key, null, true);
    }

    @Override
    public int registerLSID(String key, String[] lsid) {
        return 0;
    }

    @Override
    public int registerArea(String key, SimpleRegion region) {
        return registerHighlight(new OccurrencesFilter(region, Integer.MAX_VALUE), key, null, true);
    }

    @Override
    public int registerRecords(String key, ArrayList<OccurrenceRecordNumbers> records) {
        return 0;
    }

    @Override
    public int registerLSIDRecords(String key, String lsid, ArrayList<OccurrenceRecordNumbers> records) {
        return 0;
    }

    @Override
    double[] getPoints(/*SpeciesColourOption sco*/String lookupName, String key) {
        return null;
    }

    @Override
    String[] listLookups() {
        return null;
    }

    @Override
    public int[] lookup(String lookupName, String key) {
        return null;
    }

    @Override
    public int registerHighlight(OccurrencesFilter filter, String key, String highlightPid, boolean include) {
        int[] r = getRecordNumbers(filter);
        if (r != null && highlightPid != null) {
            boolean[] highlight = RecordSelectionLookup.getSelection(getHash() + highlightPid);
            int pos = 0;
            for (int i = 0; i < r.length; i++) {
                if (include != highlight[r[i]]) {
                    r[pos] = r[i];
                    pos++;
                }
            }
            r = java.util.Arrays.copyOf(r, pos);
        }

        int count = 0;

        if (r != null && r.length > 0) {
            count = r.length;

            java.util.Arrays.sort(r);

            if(highlightPid != null) {
                RecordsLookup.addRecords(getHash() + key, r);
            }

            //create new loaded points
            String s = LoadedPointsService.getSampling(filter.searchTerm, null, null, null, Integer.MAX_VALUE);
            double [][] points = new double[r.length][2];
            String [] ids = new String[r.length];

            String [] slist = s.split("\r\n");
            for(int i=0;i<r.length;i++) {
                if (r[i] >= slist.length) {
                    //error
                    continue;
                }
                String [] row = slist[r[i] + 1].split(","); //+1 for header
                if(row.length > 0) {
                    ids[i] = row[0];
                }
                if(row.length > 1) {
                    try {
                        points[i][0] = Double.parseDouble(row[1]);
                    } catch (Exception e) {
                        points[i][0] = Double.NaN;
                    }
                }
                if(row.length > 2) {
                    try {
                        points[i][1] = Double.parseDouble(row[2]);
                    } catch (Exception e) {
                        points[i][1] = Double.NaN;
                    }
                }
            }
            LoadedPointsService.addCluster(key, new LoadedPoints(points, "" , ids));
        }

        return count;
    }
}
