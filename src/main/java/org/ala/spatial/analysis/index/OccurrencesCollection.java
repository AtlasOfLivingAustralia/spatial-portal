/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Vector;
import org.ala.spatial.analysis.cluster.Record;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.TabulationSettings;

/**
 *
 * @author Adam
 */
public class OccurrencesCollection {

    static ArrayList<Dataset> datasets = null;

    static public void init() {
        datasets = new ArrayList<Dataset>();
    }

    public static ArrayList<OccurrenceRecordNumbers> getRecordNumbers(OccurrencesFilter filter) {
        ArrayList<OccurrenceRecordNumbers> orn = new ArrayList<OccurrenceRecordNumbers>();

        //get OccurrenceSpecies from all enabled and ready datasets
        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                int[] r = d.getOccurrencesIndex().getRecordNumbers(filter);
                if (r != null) {
                    orn.add(new OccurrenceRecordNumbers(d.getUniqueName(), r));
                }
            }
        }
        return orn;
    }

    public static double[] getPoints(OccurrencesFilter filter) {
        ArrayList<double[]> ap = new ArrayList<double[]>();

        //get OccurrenceSpecies from all enabled and ready datasets
        int count = 0;
        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                double[] p = d.getOccurrencesIndex().getPoints(filter);
                if (p != null) {
                    count += p.length;
                    ap.add(p);
                }
            }
        }

        double[] ps;
        if(ap.size() == 1) {
            ps = ap.get(0);
        } else {
            ps = new double[count];
            int p = 0;
            for (int i = 0; i < ap.size(); i++) {
                System.arraycopy(ap.get(i),0,ps,p,ap.get(i).length);
                p += ap.get(i).length;
            }
        }

        return ps;
    }

    public static double[] getPoints(/*SpeciesColourOption sco*/ String lookupName, String key) {
        ArrayList<double[]> ap = new ArrayList<double[]>();

        //get OccurrenceSpecies from all enabled and ready datasets
        int count = 0;
        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                double[] p = d.getOccurrencesIndex().getPoints(/*sco*/ lookupName, key);
                if (p != null) {
                    count += p.length;
                    ap.add(p);
                }
            }
        }

        double[] ps;
        if(ap.size() == 1) {
            ps = ap.get(0);
        } else {
            ps = new double[count];
            int p = 0;
            for (int i = 0; i < ap.size(); i++) {
                System.arraycopy(ap.get(i),0,ps,p,ap.get(i).length);
                p += ap.get(i).length;
            }
        }

        return ps;
    }

    public static double[] getPoints(OccurrencesFilter filter, ArrayList<SpeciesColourOption> extra) {
        ArrayList<double[]> ap = new ArrayList<double[]>();

        //get OccurrenceSpecies from all enabled and ready datasets
        int count = 0;
        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                ArrayList<SpeciesColourOption> extraPart = null;
                if (extra != null && extra.size() > 0) {
                    extraPart = new ArrayList<SpeciesColourOption>(extra.size());
                    for (int i = 0; i < extra.size(); i++) {
                        extraPart.add(SpeciesColourOption.fromSpeciesColourOption(extra.get(i)));
                    }
                }
                double[] p = d.getOccurrencesIndex().getPoints(filter, extraPart);
                if (p != null) {
                    count += p.length;
                    ap.add(p);

                    if (extra != null && extra.size() > 0) {
                        for (int i = 0; i < extra.size(); i++) {
                            extra.get(i).append(extraPart.get(i));
                        }
                    }
                }
            }
        }

        double[] ps;
        if(ap.size() == 1) {
            ps = ap.get(0);
        } else {
            ps = new double[count];
            int p = 0;
            for (int i = 0; i < ap.size(); i++) {
                System.arraycopy(ap.get(i),0,ps,p,ap.get(i).length);
                p += ap.get(i).length;
            }
        }

        return ps;
    }

    public static ArrayList<String> getFullRecords(OccurrencesFilter filter) {
        ArrayList<String> as = new ArrayList<String>();

        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                ArrayList<String> s = d.getOccurrencesIndex().getFullRecords(filter);
                if (s != null) {
                    as.addAll(s);
                }
            }
        }

        return as;
    }

    /*static String [][] getPartialRecords(<OccurrencesFilter filter) {
    ArrayList<String[][]> as = new ArrayList<String[][]>();

    for(Dataset d : datasets) {
    if(d.isEnabled()) {
    ArrayList<String[][]> s = d.getOccurrencesIndex().getPartialRecords(filter);
    if(s != null) {
    as.addAll(s);
    }
    }
    }

    return as;
    }*/
    /**
     * get species 
     * @param filters
     * @return
     */
    public static ArrayList<OccurrencesSpeciesList> getSpeciesList(OccurrencesFilter filter) {
        ArrayList<OccurrencesSpeciesList> al = new ArrayList<OccurrencesSpeciesList>();

        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                OccurrencesSpeciesList osl = new OccurrencesSpeciesList(d.getUniqueName(), filter);
                if (osl.getSpeciesCount() > 0) {
                    al.add(osl);
                }
            }
        }

        //TODO merge arrays

        return al;
    }

    public static Dataset getDataset(String id) {
        for (Dataset d : datasets) {
            if (d.getUniqueName().equals(id)) {
                return d;
            }
        }
        return null;
    }

    public static String[] getFirstName(String species) {
        return SpeciesIndex.getFirstName(species);
    }

    public static String getCommonNames(String name, String[] aslist, int maxRecords) {
        return SpeciesIndex.getCommonNames(name, aslist, maxRecords);
    }

    public static ArrayList<OccurrenceRecordNumbers> lookup(String key, String value) {
        ArrayList<OccurrenceRecordNumbers> records = new ArrayList<OccurrenceRecordNumbers>();
        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                int[] s = d.getOccurrencesIndex().lookup(key, value);
                if (s != null) {
                    records.add(new OccurrenceRecordNumbers(d.getUniqueName(), s));
                }
            }
        }

        if (records.size() == 0) {
            records = null;
        }

        return records;
    }

    /**
     *
     * @param id dataset uniqueId as String
     * @return OccurrencesIndex for this dataset, if it is ready.  Otherwise null.
     */
    static OccurrencesIndex getOccurrencesIndex(String id) {
        Dataset d = getDataset(id);
        if (d != null) {
            return d.getOccurrencesIndex();
        }
        return null;
    }

    public static Vector getRecords(OccurrencesFilter occurrencesFilter) {
        Vector<Record> ap = new Vector<Record>();

        //get OccurrenceSpecies from all enabled and ready datasets
        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                Vector<Record> p = d.getOccurrencesIndex().getRecords(occurrencesFilter);
                if (p != null) {
                    ap.addAll(p);
                }
            }
        }

        return ap;
    }

    /**
     * returns a list of (species names / type / count) for valid
     * .beginsWith matches
     *
     * @param filter begins with text to search for (NOT LSID, e.g. species name)
     * @param limit limit on output
     * @return formatted species matches as String[]
     */
    public static String[] findSpecies(String filter, int limit) {
        return SpeciesIndex.filterBySpeciesName(filter, limit);
    }

    public static double[] getPointsMinusSensitiveSpecies(OccurrencesFilter occurrencesFilter, StringBuffer removedSpecies) {
        ArrayList<double[]> ap = new ArrayList<double[]>();

        //get OccurrenceSpecies from all enabled and ready datasets
        int count = 0;
        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                double[] p = d.getOccurrencesIndex().getPointsMinusSensitiveSpecies(occurrencesFilter, removedSpecies);
                if (p != null) {
                    count += p.length;
                    ap.add(p);
                }
            }
        }

        if (count == 0) {
            return null;
        }

        double[] ps = new double[count];
        int p = 0;
        for (int i = 0; i < ap.size(); i++) {
            for (double d : ap.get(i)) {
                ps[p] = d;
                p++;
            }
        }

        return ps;
    }

    public static ArrayList<OccurrenceRecordNumbers> getGridSampleSet(LayerFilter new_filter) {
        ArrayList<OccurrenceRecordNumbers> output = new ArrayList<OccurrenceRecordNumbers>();
        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                FilteringIndex fi = d.getFilteringIndex();
                if (fi != null) {
                    int[] records = fi.getGridSampleSet(new_filter);
                    if (records != null) {
                        OccurrenceRecordNumbers orn = new OccurrenceRecordNumbers(d.getUniqueName(), records);
                        output.add(orn);
                    }
                }
            }
        }
        return output;
    }

    public static ArrayList<OccurrenceRecordNumbers> getCatagorySampleSet(LayerFilter new_filter) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static OccurrencesSpeciesList getSpeciesList(ArrayList<OccurrenceRecordNumbers> rk) {
        ArrayList<OccurrencesSpeciesList> al = new ArrayList<OccurrencesSpeciesList>();

        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                for (int i = 0; i < rk.size(); i++) {
                    if (!rk.get(i).getName().equals(d.getUniqueName())) {
                        continue;
                    }
                    OccurrencesSpeciesList osl = new OccurrencesSpeciesList(d.getUniqueName(),
                            new OccurrencesFilter(rk, 100000000));
                    if (osl != null && osl.getSpeciesCount() > 0) {
                        al.add(osl);
                    }
                }
            }
        }

        //TODO: merge results
        if (al.size() > 0) {
            return al.get(0);
        }

        return null;
    }

    /**
     *
     * @param keyEnd
     * @param lsid
     * @param layer1
     * @param x1
     * @param x2
     * @param layer2
     * @param y1
     * @param y2
     * @return number of records highlighted as int
     */
    public static int highlightLsid(String keyEnd, String lsid, Object [] filters) {
        int count = 0;

        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                count += d.getOccurrencesIndex().highlightLsid(keyEnd, lsid, filters);
            }
        }
        return count;
    }

    public static int registerLSIDArea(String id, String lsid, SimpleRegion region) {
        int count = 0;

        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                count += d.getOccurrencesIndex().registerLSIDArea(id, lsid, region);
            }
        }
        return count;
    }

    public static int registerLSIDRecords(String id, String lsid, ArrayList<OccurrenceRecordNumbers> records) {
        int count = 0;

        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                count += d.getOccurrencesIndex().registerLSIDRecords(id, lsid, records);
            }
        }
        return count;
    }

    public static int registerLSID(String id, String[] lsids) {
        int count = 0;

        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                count += d.getOccurrencesIndex().registerLSID(id, lsids);
            }
        }
        return count;
    }

    public static int registerArea(String id, SimpleRegion region) {
        int count = 0;

        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                count += d.getOccurrencesIndex().registerArea(id, region);
            }
        }
        return count;
    }

    public static int registerRecords(String id, ArrayList<OccurrenceRecordNumbers> records) {
        int count = 0;

        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                count += d.getOccurrencesIndex().registerRecords(id, records);
            }
        }
        return count;
    }

    public static int registerHighlight(String lsid, String id, String pid, boolean include) {
        int count = 0;

        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                count += d.getOccurrencesIndex().registerHighlight(new OccurrencesFilter(lsid, 10000000), id, pid, include);
            }
        }
        return count;
    }

    public static String getColourLegend(String lsid, String colourMode) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    static String[] listLookups() {
        TreeSet<String> ts = new TreeSet<String>();
        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                String [] as = d.getOccurrencesIndex().listLookups();
                if(as != null) {
                    for(String s : as) {
                        ts.add(s);
                    }
                }
            }
        }

        if(ts.size() == 0) {
            return null;
        } else {
            String [] as = new String[ts.size()];
            int p = 0;
            for(String s : ts) {
                as[p] = s;
                p++;
            }
            return as;
        }
    }

    static String[] listLookupKeys(String lookupName) {
        TreeSet<String> ts = new TreeSet<String>();
        for (Dataset d : datasets) {
            if (d.isEnabled() && d.isReady()) {
                String [] as = d.getOccurrencesIndex().listLookupKeys(lookupName);
                if(as != null) {
                    for(String s : as) {
                        ts.add(s);
                    }
                }
            }
        }

        if(ts.size() == 0) {
            return null;
        } else {
            String [] as = new String[ts.size()];
            int p = 0;
            for(String s : ts) {
                as[p] = s;
                p++;
            }
            return as;
        }
    }
}
