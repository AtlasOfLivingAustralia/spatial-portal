package org.ala.spatial.analysis.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import org.ala.spatial.analysis.cluster.Record;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.index.OccurrenceRecordNumbers;
import org.ala.spatial.analysis.index.OccurrencesCollection;
import org.ala.spatial.analysis.index.OccurrencesFilter;
import org.ala.spatial.analysis.index.OccurrencesSpeciesList;
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
    ArrayList<OccurrenceRecordNumbers> getTopSpeciesRecord() {
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
            ArrayList<OccurrenceRecordNumbers> rka = (ArrayList<OccurrenceRecordNumbers>) ois.readObject();
            ois.close();

            return rka;
        } catch (Exception e) {
            e.printStackTrace();
            SpatialLogger.log("FilteringService: getTopSpeciesRecord()", e.toString());
        }

        return null;
    }

    /**
     * saves SpeciesRecord list (species number and record number)
     * for top filter applied in a session
     * 
     */
    void saveTopSpeciesRecord(ArrayList<OccurrenceRecordNumbers> speciesrecord) {
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
            e.printStackTrace();
            SpatialLogger.log("FilteringService: saveTopeSpeciesRecord()", e.toString());
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
                || !layerfilters.get(layerfilters.size() - 1).layer.name.equalsIgnoreCase(new_filter.layer.name)) {
            //add


            /* get current top speciesrecord */
            ArrayList<OccurrenceRecordNumbers> topspeciesrecord = getTopSpeciesRecord();

            /* get new top record key list */
            layerfilters.add(new_filter);
            ArrayList<OccurrenceRecordNumbers> newspeciesrecord;
            if (new_filter.catagories == null) {
                newspeciesrecord = OccurrencesCollection.getGridSampleSet(new_filter);
            } else {
                newspeciesrecord = OccurrencesCollection.getCatagorySampleSet(new_filter);
            }

            //reduce size of newspeciesrecord
            int i, j;

            //pair up newspeciesrecord with topspeciesrecord
            if(topspeciesrecord == null) {
                saveTopSpeciesRecord(newspeciesrecord);
            }else {
                ArrayList<OccurrenceRecordNumbers> outputArray = new ArrayList<OccurrenceRecordNumbers>();
                for(int k=0;k<newspeciesrecord.size();k++) {
                    for(int m=0;m<topspeciesrecord.size();m++) {
                        if(!topspeciesrecord.get(m).getName().equals(newspeciesrecord.get(k).getName())) {
                            continue;
                        }

                        int [] tsa = topspeciesrecord.get(m).getRecords();
                        int [] nsr = newspeciesrecord.get(m).getRecords();

                        int [] output = new int [tsa.length];
                        int p = 0;
                        for (i = 0, j = 0; i < tsa.length && j < nsr.length; ) { //iterator in loop
                            if (tsa[i] < nsr[j]) {
                                //move forward
                                i++;
                            } else if (tsa[i] == nsr[j]) {
                                //save
                                output[p] = tsa[i];
                                p++;

                                //increment both
                                i++;
                                while (i < tsa.length
                                        && tsa[i] == tsa[i - 1]) {
                                    i++;
                                }
                                j++;
                                while (j < nsr.length
                                        && nsr[j] == nsr[j-1]) {
                                    j++;
                                }
                            } else {
                                //discard in nsr
                                j++;
                            }
                        }

                        if(p > 0) {
                            nsr = java.util.Arrays.copyOf(output, p);
                            outputArray.add(new OccurrenceRecordNumbers(newspeciesrecord.get(k).getName(),nsr));
                        }
                    }
                }
                saveTopSpeciesRecord(outputArray);
            }
            
        } else {
            //TODO: incremental updates

            // test for any change
            LayerFilter top_filter = getTopFilter();

            //comparison
            if (top_filter.layername.equals(new_filter.layername)) {
                if (top_filter.catagories == null && new_filter.catagories == null) {
                    //enviornmental layers check
                    if (top_filter.minimum_value == new_filter.minimum_value
                            && top_filter.maximum_value == new_filter.maximum_value) {
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
     * @return number of unqiue species as int[0], number of occurrences as int[1]
     */
    static public int[] getSpeciesCount(String session_id_, SimpleRegion region) {
        OccurrencesSpeciesList osl = null;

        /* check for "none" session */
        if (session_id_.equals("none")) {
            /* TODO: fix this up when no longer using ArrayList
            in getSpeciesBitset */
            ArrayList<OccurrencesSpeciesList> list = OccurrencesCollection.getSpeciesList(new OccurrencesFilter(region, TabulationSettings.MAX_RECORD_COUNT_CLUSTER));
            if(list != null && list.size() > 0){
                osl = list.get(0);
            }
        } else {

            /* load session */
            FilteringService fs = FilteringService.getSession(session_id_);

            /* get top filter speciesrecord */
            ArrayList<OccurrenceRecordNumbers> rk = fs.getTopSpeciesRecord();
            if (rk == null) {
                int[] ret = new int[2];
                ret[0] = 0;
                ret[1] = 0;
                return ret;
            }

            /* make list of species, for inside region filter */
            osl = OccurrencesCollection.getSpeciesList(rk);
        }        

        int[] ret = new int[2];
        if(osl != null) {
            ret[0] = osl.getSpeciesCount();
            ret[1] = osl.getOccurrencesCount();
        }
        return ret;
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
        long start = System.currentTimeMillis();

        OccurrencesFilter occurrencesFilter = null;

        /* check for "none" session */
        if (session_id_.equals("none")) {
            occurrencesFilter = new OccurrencesFilter(region, TabulationSettings.MAX_RECORD_COUNT_CLUSTER);
        } else {
            /* load session */
            FilteringService fs = FilteringService.getSession(session_id_);

            //put records into filter
            occurrencesFilter = new OccurrencesFilter(fs.getTopSpeciesRecord(), TabulationSettings.MAX_RECORD_COUNT_CLUSTER);
        }

        String output = null;

        ArrayList<OccurrencesSpeciesList> osl = OccurrencesCollection.getSpeciesList(occurrencesFilter);
        if(osl != null && osl.size() > 0){
            StringBuffer sb = new StringBuffer();
            for(String s : osl.get(0).getSpeciesList()){
                sb.append(s).append("|");
            }

            //any species distributions?
            if(region != null) {
                int [] r = ShapeIntersectionService.getIntersections(region);
                if(r != null) {
                    String [] lsids = ShapeIntersectionService.convertToLsids(r);
                    String str = sb.toString();
                    for(int i=0;i<lsids.length;i++) {
                        if(!str.contains(lsids[i])) {
                            //append the missing entry
                            sb.append(OccurrencesSpeciesList.getSpeciesListEntryFromADistribution(lsids[i])).append("|");
                        }
                    }
                }
            }
            output = sb.toString();
        }

        long end = System.currentTimeMillis();

        System.out.println("getspecieslist: " + (end - start) + "ms");

        return output;
    }

    /**
     * gets samples records from a session and filtered region
     * @param session_id_ session id as String
     * @param region filtered region as SimpleRegion or null for none
     * @param max_records maximum record number as int
     * @return Samples records in a csv in the filename returned as String
     */
    static public String getSamplesList(String session_id_, SimpleRegion region, int maximum_records) {
        int i;

        OccurrencesFilter occurrencesFilter = null;

        /* check for "none" session */
        if (session_id_.equals("none")) {
            occurrencesFilter = new OccurrencesFilter(region, maximum_records);
        } else {
            /* load session */
            FilteringService fs = FilteringService.getSession(session_id_);

            //put records into filter
             occurrencesFilter = new OccurrencesFilter(fs.getTopSpeciesRecord(), maximum_records);
        }

        /* get samples records from records indexes */
        ArrayList<String> samples = OccurrencesCollection.getFullRecords(occurrencesFilter);

        //test for no records
        if (samples == null || samples.size() == 0) {
            return "";
        }

        /* write samples to a file */
        try {
            SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
            String sdate = date.format(new Date());

            File temporary_file = java.io.File.createTempFile("Sample_" + sdate + "_", ".csv");
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
            for (i = 0; i < samples.size(); i++) {
                fw.append(samples.get(i));
                fw.append("\r\n");

                // exit if MAX_RECORD_COUNT_DOWNLOAD reached
                if (i == TabulationSettings.MAX_RECORD_COUNT_DOWNLOAD - 1) {
                    break;
                }
            }

            fw.close();

            return temporary_file.getPath();	//return location of temp file

        } catch (Exception e) {
            SpatialLogger.log("FilteringService: getSamplesList()", e.toString());
            e.printStackTrace();
        }

        return ""; //failed
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

//                if (fs.layerfilters == null) {
//                    System.out.println("filteringservice:" + session_id_ + " no layers");
//                } else {
//                    System.out.println("filteringservice:" + session_id_ + " layercount:" + fs.layerfilters.size());
//                }
                return fs;
            } catch (Exception e) {
                SpatialLogger.log("FilteringService: getSession(" + session_id_ + ")", e.toString());
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
            SpatialLogger.log("FilteringService: save()", e.toString());
        }
    }

    /**
     * gets list of record numbers filtered in a session
     *
     * @param session_id_ valid session id as String, may be wrapped as
     * "ENVELOPE(session_id)"
     * @return list of record numbers as int []
     */
    public static ArrayList<OccurrenceRecordNumbers> getRecords(String session_id_) {
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

    private static String getRecordAsGeoJSON(Record record) {
        StringBuffer sbRec = new StringBuffer();
        sbRec.append("{");
        sbRec.append("  \"type\":\"Feature\",");
        sbRec.append("  \"id\":\"occurrences.data.").append(record.getId()).append("\",");
        sbRec.append("  \"geometry\":{");
        sbRec.append("      \"type\":\"Point\",");
        sbRec.append("      \"coordinates\":[\"").append(record.getLongitude()).append("\",\"").append(record.getLatitude()).append("\"]");
        sbRec.append("   },");
        sbRec.append("  \"geometry_name\":\"the_geom\",");
        sbRec.append("  \"properties\":{");

        sbRec.append("      \"").append("u").append("\":\"").append(record.getUncertainity()).append("\"");
        sbRec.append(",");

        sbRec.append("      \"").append("oi").append("\":\"").append(record.getId()).append("\"");

        sbRec.append("  }");
        sbRec.append("}");

        return sbRec.toString();

    }
}
