package org.ala.spatial.analysis.service;

import bsh.This;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;

import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.index.OccurrencesIndex;
import org.ala.spatial.analysis.index.FilteringIndex;
import org.ala.spatial.analysis.index.IndexedRecord;
import org.ala.spatial.util.OccurrencesFieldsUtil;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;

/**
 * entry point to filtering functions
 * 
 * operates by externally defined session_id (String)
 * 
 * - applyFilter(String session_id_, LayerFilter new_filter)
 * 		for addition/update/removal of top filter on a session
 * 
 * - getSpeciesCount(String session_id_, SimpleRegion region): int
 * 		returns number of species within the filtered session and 
 * 		optional region
 * 
 * - getSpeciesList(String session_id_, SimpleRegion region): String
 * 		returns list of species within the filtered session and 
 * 		optional region
 * 
 * - getSamplesList(String session_id_, SimpleRegion region): String
 * 		returns list of samples within the filtered sessoin and 
 * 		optional region 
 * 
 *
 * @author adam
 *
 */
public class FilteringService implements Serializable {

    static final long serialVersionUID = 6598125472355988943L;

    /**
     * maintained list of filters applied
     * 
     */
    ArrayList<LayerFilter> layerfilters;
    /**
     * session id for current object
     * 
     */
    String session_id;

    /**
     * constructor
     */
    FilteringService() {
        TabulationSettings.load();
    }

    /**
     * removes an a applied filter from a session 
     * 
     */
    void popFilter() {
        if (layerfilters != null && layerfilters.size() > 0) {
            layerfilters.remove(layerfilters.size() - 1);
        }
    }

    /**
     * gets top filter applied in a session
     * 
     */
    LayerFilter getTopFilter() {
        if (layerfilters != null && layerfilters.size() > 0) {
            return layerfilters.get(layerfilters.size() - 1);
        }
        return null;
    }

    /**
     * gets top SpeciesRecord list (species number and record number)
     * for top filter applied in a session
     * 
     */
    ArrayList<Integer> getTopSpeciesRecord() {
        /* no filters, return null */
        if (layerfilters.size() == 0) {
            return null;
        }

        try {
            /* open session filter file */
            /* TODO: sessions get their own filepath */
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + session_id + "_filter_" + layerfilters.size());
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            ArrayList<Integer> rka = (ArrayList<Integer>) ois.readObject();
            ois.close();

            return rka;
        } catch (Exception e) {
            (new SpatialLogger()).log("FilteringService: getTopSpeciesRecord()", e.toString());
        }

        return null;
    }

    /**
     * saves SpeciesRecord list (species number and record number)
     * for top filter applied in a session
     * 
     */
    void saveTopSpeciesRecord(ArrayList<Integer> speciesrecord) {
        try {
            /* write out as object, tag with sessionid, 'filter' and filtersize */
            FileOutputStream fos = new FileOutputStream(
                    TabulationSettings.index_path
                    + session_id + "_filter_" + layerfilters.size());
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(speciesrecord);
            oos.close();
        } catch (Exception e) {
            (new SpatialLogger()).log("FilteringService: saveTopeSpeciesRecord()", e.toString());
        }
    }

    /**
     * updates top filter and top SpeciesRecord
     * 
     * case 1: filter is same as current top filter
     * - remove previous filter
     * - reapply
     * 
     * case 2: filter is different from current top filter
     * - load current top speciesrecords
     * - get new filter speciesrecords
     * - save intersection between the two speciesrecord lists
     *       * 
     */
    void updateFilter(LayerFilter new_filter) {
        if (layerfilters.size() == 0
                || !layerfilters.get(layerfilters.size() - 1).layer.name.equals(new_filter.layer.name)) {
            //add


            /* get current top speciesrecord */
            ArrayList<Integer> topspeciesrecord = getTopSpeciesRecord();

            /* get new top record key list */
            layerfilters.add(new_filter);
            ArrayList<Integer> newspeciesrecord;
            if (new_filter.catagories == null) {
                newspeciesrecord = FilteringIndex.getGridSampleSet(new_filter);
            } else {
                newspeciesrecord = FilteringIndex.getCatagorySampleSet(new_filter);
            }

            //reduce size of newspeciesrecord
            int i, j;
            if (topspeciesrecord != null) {
                ArrayList<Integer> output = new ArrayList<Integer>(topspeciesrecord.size());

                for (i = 0, j = 0; i < topspeciesrecord.size() && j < newspeciesrecord.size(); /*iterator in loop*/) {
                    if (topspeciesrecord.get(i).intValue() < newspeciesrecord.get(j).intValue()) {
                        //move forward
                        i++;
                    } else if (topspeciesrecord.get(i).intValue() == newspeciesrecord.get(j).intValue()) {
                        //save
                        output.add(topspeciesrecord.get(i).intValue());

                        //increment both
                        i++;
                        while (i < topspeciesrecord.size()
                                && topspeciesrecord.get(i).intValue() == topspeciesrecord.get(i - 1).intValue()) {
                            i++;
                        }
                        j++;
                        while (j < newspeciesrecord.size()
                                && newspeciesrecord.get(j).intValue() == newspeciesrecord.get(j - 1).intValue()) {
                            j++;
                        }
                    } else {
                        //discard in newspeciesrecord
                        j++;
                    }
                }

                newspeciesrecord = output;
            }
            saveTopSpeciesRecord(newspeciesrecord);
        } else {
            //TODO: incremental updates

            /* test for any change */
            LayerFilter top_filter = getTopFilter();

            //comparison
            if (top_filter.layername == new_filter.layername) {
                if (top_filter.catagories == null && new_filter.catagories == null) {
                    //enviornmental layers check
                    if (top_filter.minimum_value == new_filter.minimum_value
                            && top_filter.maximum_value == top_filter.maximum_value) {
                        return; //no changes required
                    }
                }
                if (top_filter.catagories != null && new_filter.catagories != null) {
                    int i;
                    for (i = 0; i < top_filter.catagories.length && i < new_filter.catagories.length; i++) {
                        if (top_filter.catagories[i] != new_filter.catagories[i]) {
                            break;
                        }
                    }
                    if (i == top_filter.catagories.length
                            && i == new_filter.catagories.length) {
                        return; //no changes required
                    }
                }
            }
            popFilter();
            updateFilter(new_filter);
        }
    }

    /**
     * applies a layer filter to the session 
     * 
     * @param session_id unique session_id for persistant data
     * @param new_filter null to remove last filter
     * 			different layer filter to add filter
     * 			same layer filter to change filter
     */
    static public int applyFilter(String session_id_, LayerFilter new_filter) {

        FilteringService fs = FilteringService.getSession(session_id_);

        if (new_filter == null) {
            //remove top filter
            fs.popFilter();
        } else {
            fs.updateFilter(new_filter);
        }

        fs.save();

        return fs.layerfilters.size();
    }

    /**
     * gets number of species filtered in a session
     *
     * also supports "none" session id for returning
     * species count against a region only.
     *
     * @param session_id_ session id as String, or "none"
     * @param region bounding region for results, or null for none
     * @return number of unqiue species as int
     */
    static public int getSpeciesCount(String session_id_, SimpleRegion region) {
        int i;

        BitSet species;

        /* check for "none" session */
        if (session_id_.equals("none")) {
            /* TODO: fix this up when no longer using ArrayList
            in getSpeciesBitset */
            return OccurrencesIndex.getSpeciesCountInside(region);
        } else {

            /* load session */
            FilteringService fs = FilteringService.getSession(session_id_);

            /* get top filter speciesrecord */
            ArrayList<Integer> rk = fs.getTopSpeciesRecord();
            if (rk == null) {
                return 0;
            }

            /* make list of species, for inside region filter */
            species = OccurrencesIndex.getSpeciesBitset(rk, region); //new BitSet(OccurrencesIndex.getSpeciesIndex().length + 1);
        }

        /* count species */
        int count = 0;
        for (i = 0; i < species.length(); i++) {
            if (species.get(i)) {
                count++;
            }
        }

        return count;
    }

    /**
     * gets names of species filtered in a session
     *
     * supports "none" session id
     * 
     * @param session_id_ session id as String, none for no env filter
     * @param region region to restrict results as SimpleRegion or null for none
     * @return list of unqiue species as String, ',' delimited
     */
    static public String getSpeciesList(String session_id_, SimpleRegion region) {
        int i;

        long start = System.currentTimeMillis();

        BitSet species;

        /* check for "none" session */
        if (session_id_.equals("none")) {
            /* TODO: fix this up when no longer using ArrayList
            in getSpeciesBitset */
            return OccurrencesIndex.getSpeciesInside(region);
        } else {
            /* load session */
            FilteringService fs = FilteringService.getSession(session_id_);

            /* get top speciesrecords */
            ArrayList<Integer> rk = fs.getTopSpeciesRecord();

            /* make species list */
            species = OccurrencesIndex.getSpeciesBitset(rk, region);
        }

        /* make into string of species names */
        String output = OccurrencesIndex.getSpeciesListRecords(species);

        long end = System.currentTimeMillis();

        System.out.println("getspecieslist: " + (end - start) + "ms");


        return output;
    }

    /**
     * gets samples records from a session and filtered region
     * @param session_id_ session id as String
     * @param region filtered region as SimpleRegion or null for none
     * @return Samples records as String
     */
    static public String getSamplesList(String session_id_, SimpleRegion region) {
        int[] records;
        int i;

        /* check for "none" session */
        if (session_id_.equals("none")) {
            /* TODO: fix this up when no longer using ArrayList
            in getSpeciesBitset */
            records = OccurrencesIndex.getRecordsInside(region);
            System.out.println("region:" + records);

        } else {
            /* load session */
            FilteringService fs = FilteringService.getSession(session_id_);

            /* get top speciesrecord */
            ArrayList<Integer> rk = fs.getTopSpeciesRecord();

            /* get record indexes */
            if (rk.size() == 0) {
                return "";	//no records to return;
            }
            records = new int[rk.size()];
            int p = 0;

            if (region == null) {
                /* no defined region, use all */
                for (i = 0; i < rk.size(); i++) {
                    records[p++] = rk.get(i);
                }
            } else {
                /* restrict by region */

                /* TODO: check, could be faster to use
                 * OccurrencesIndex.getRecordsInside(region)
                 */
                for (i = 0; i < rk.size(); i++) {
                    if (OccurrencesIndex.inRegion(rk.get(i), region)) {
                        records[p++] = rk.get(i);
                    }
                }
                if (p > 0) {
                    int[] records_tmp = java.util.Arrays.copyOf(records, p);
                    records = records_tmp;
                } else {
                    return "";	//no records to return
                }
            }
        }

        //test for no records
        if (records == null || records.length == 0) {
            return "";
        }

        /* get samples records from records indexes */
        String[] samples = OccurrencesIndex.getSortedRecords(records);

        /* write samples to a file */
        try {
            File temporary_file = java.io.File.createTempFile("filter_sample", ".csv");
            FileWriter fw = new FileWriter(temporary_file);

            /* output header */
            OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();
            StringBuffer sbHeader = new StringBuffer();
            for (String s : ofu.getOutputColumnNames()) {
                sbHeader.append(s);
                sbHeader.append(",");
            }
            sbHeader.deleteCharAt(sbHeader.length() - 1);
            sbHeader.append("\r\n");
            fw.append(sbHeader.toString());
            for (i = 0; i < samples.length; i++) {
                fw.append(samples[i]);
                fw.append("\r\n");
                
                // exit if MAX_RECORD_COUNT reached
                if (i==TabulationSettings.MAX_RECORD_COUNT-1) break;
            }

            fw.close();

            return temporary_file.getPath();	//return location of temp file

        } catch (Exception e) {
            (new SpatialLogger()).log("FilteringService: getSamplesList()", e.toString());
            e.printStackTrace();
        }

        return ""; //failed
    }

    public static String getSamplesListAsGeoJSON(String session_id_, SimpleRegion region, File outputpath) {
        int[] records;
        int i;

        /* check for "none" session */
        if (session_id_.equals("none")) {
            /* TODO: fix this up when no longer using ArrayList
            in getSpeciesBitset */
            records = OccurrencesIndex.getRecordsInside(region);
            System.out.println("region:" + records);

        } else {
            /* load session */
            FilteringService fs = FilteringService.getSession(session_id_);

            /* get top speciesrecord */
            ArrayList<Integer> rk = fs.getTopSpeciesRecord();

            /* get record indexes */
            if (rk.size() == 0) {
                return "";	//no records to return;
            }
            records = new int[rk.size()];
            int p = 0;

            if (region == null) {
                /* no defined region, use all */
                for (i = 0; i < rk.size(); i++) {
                    records[p++] = rk.get(i);
                }
            } else {
                /* restrict by region */

                /* TODO: check, could be faster to use
                 * OccurrencesIndex.getRecordsInside(region)
                 */
                for (i = 0; i < rk.size(); i++) {
                    if (OccurrencesIndex.inRegion(rk.get(i), region)) {
                        records[p++] = rk.get(i);
                    }
                }
                if (p > 0) {
                    int[] records_tmp = java.util.Arrays.copyOf(records, p);
                    records = records_tmp;
                } else {
                    return "";	//no records to return
                }
            }
        }

        //test for no records
        if (records == null || records.length == 0) {
            return "";
        }

        /* get samples records from records indexes */
        String[] samples = OccurrencesIndex.getSortedRecords(records);
        double[][] bbox = region.getBoundingBox();

        StringBuffer sbGeoJSON = new StringBuffer();
        sbGeoJSON.append("{");
        sbGeoJSON.append("  \"type\": \"FeatureCollection\",");
        sbGeoJSON.append("  \"features\": [");
        for (i = 0; i < samples.length; i++) {
            sbGeoJSON.append(getRecordAsGeoJSON(samples[i]));
            if (i<samples.length-1) sbGeoJSON.append(","); 
            //sbGeoJSON.append("\r\n");
        }
        sbGeoJSON.append("  ],");
        sbGeoJSON.append("  \"crs\": {");
        sbGeoJSON.append("    \"type\": \"EPSG\",");
        sbGeoJSON.append("    \"properties\": {");
        sbGeoJSON.append("      \"code\": \"4326\"");
        sbGeoJSON.append("    }");
        sbGeoJSON.append("  },");
        sbGeoJSON.append("  \"bbox\": [");
        sbGeoJSON.append("    ").append(bbox[0][0]).append(",").append(bbox[0][1]).append(",").append(bbox[1][0]).append(",").append(bbox[1][1]);
        sbGeoJSON.append("  ]");
        sbGeoJSON.append("}");

        /* write samples to a file */
        try {
            File temporary_file = java.io.File.createTempFile("filter_sample", ".csv", outputpath);
            FileWriter fw = new FileWriter(temporary_file);

            fw.write(sbGeoJSON.toString());

            fw.close();

            return temporary_file.getName();	//return location of temp file

        } catch (Exception e) {
            (new SpatialLogger()).log("FilteringService: getSamplesListAsGeoJSON()", e.toString());
            e.printStackTrace();
        }
        return "";

    }

    private static String getRecordAsGeoJSON(String rec) {
        String[] recdata = rec.split(",");

        StringBuffer sbRec = new StringBuffer();
        sbRec.append("{");
        sbRec.append("  \"type\":\"Feature\",");
        sbRec.append("  \"id\":\"occurrences.data.").append(recdata[11]).append("\",");
        sbRec.append("  \"geometry\":{");
        sbRec.append("      \"type\":\"Point\",");
        sbRec.append("      \"coordinates\":[").append(recdata[32]).append(",").append(recdata[33]).append("]");
        sbRec.append("   },");
        sbRec.append("  \"geometry_name\":\"the_geom\",");
        sbRec.append("  \"properties\":{");
        sbRec.append("      \"occurrenceid\":\"").append(recdata[11]).append("\",");
        sbRec.append("      \"dataprovideruid\":\"").append(recdata[12]).append("\",");
        sbRec.append("      \"dataprovidername\":\"").append(recdata[13]).append("\",");
        sbRec.append("      \"dataresourceuid\":\"").append(recdata[14]).append("\",");
        sbRec.append("      \"institutioncode\":\"").append(recdata[17]).append("\",");
        sbRec.append("      \"collectioncode\":\"").append(recdata[20]).append("\",");
        sbRec.append("      \"cataloguenumber\":\"").append(recdata[21]).append("\",");
        sbRec.append("      \"family\":\"").append(recdata[5]).append("\",");
        sbRec.append("      \"scientificname\":\"").append(recdata[10]).append("\",");
        sbRec.append("      \"taxonconceptid\":\"").append(recdata[22]).append("\",");
        sbRec.append("      \"longitude\":\"").append(recdata[24]).append("\",");
        sbRec.append("      \"latitude\":\"").append(recdata[25]).append("\",");
        sbRec.append("      \"occurrencedate\":\"").append(recdata[30]).append("\"");
        sbRec.append("  }");
        sbRec.append("}");

        return sbRec.toString(); 

    }

    /**
     * gets a FilteringIndex object for a session
     * 
     * creates new one if none exists
     * 
     * @param session_id_ session id as String
     * @return FilteringIndex object
     */
    static FilteringService getSession(String session_id_) {
        //load existing
        TabulationSettings.load();

        /* test if session exists */
        File f = new File(TabulationSettings.index_path
                + session_id_ + "_spl");

        if (f.exists()) {
            //load and return existing session
            try {
                FileInputStream fis = new FileInputStream(
                        TabulationSettings.index_path
                        + session_id_ + "_spl");
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis);
                FilteringService fs = (FilteringService) ois.readObject();
                ois.close();

                if (fs.layerfilters == null) {
                    System.out.println("filteringservice:" + session_id_ + " no layers");
                } else {
                    System.out.println("filteringservice:" + session_id_ + " layercount:" + fs.layerfilters.size());
                }
                return fs;
            } catch (Exception e) {
                (new SpatialLogger()).log("FilteringService: getSession(" + session_id_ + ")", e.toString());
            }
            return null;
        }

        //otherwise new FilteringIndex
        FilteringService fs = new FilteringService();
        fs.session_id = session_id_;
        fs.layerfilters = new ArrayList<LayerFilter>();

        return fs;
    }

    /**
     * saves a session
     */
    void save() {
        //save this session
        try {
            FileOutputStream fos = new FileOutputStream(
                    TabulationSettings.index_path
                    + session_id + "_spl");
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            oos.close();
        } catch (Exception e) {
            (new SpatialLogger()).log("FilteringService: save()", e.toString());
        }
    }

    /**
     * gets list of record numbers filtered in a session
     *
     * @param session_id_ valid session id as String, may be wrapped as
     * "ENVELOPE(session_id)"
     * @return list of record numbers as ArrayList<Integer>
     */
    public static ArrayList<Integer> getRecords(String session_id_) {
        if (session_id_.startsWith("ENVELOPE(")) {
            session_id_ = session_id_.replace("ENVELOPE(", "");
            session_id_ = session_id_.replace(")", "");
        }
        FilteringService fs = FilteringService.getSession(session_id_);
        return fs.getTopSpeciesRecord();
    }

    /**
     * gets list of filters applied to a session (MUST BE ENVIRONMENTAL ONLY)
     *
     * @param session_id_ valid session id as String, may be wrapped as
     * "ENVELOPE(session_id)"
     * @return list of record numbers as LayerFilter[]
     */
    public static LayerFilter[] getFilters(String session_id_) {
        if (session_id_.startsWith("ENVELOPE(")) {
            session_id_ = session_id_.replace("ENVELOPE(", "");
            session_id_ = session_id_.replace(")", "");
        }
        FilteringService fs = FilteringService.getSession(session_id_);
        LayerFilter[] lf = new LayerFilter[fs.layerfilters.size()];
        fs.layerfilters.toArray(lf);
        System.out.println("getFilters:" + fs.layerfilters.size() + " " + lf.length);
        return lf;
    }
}
