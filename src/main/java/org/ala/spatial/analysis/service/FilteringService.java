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
import java.util.ArrayList;
import java.util.BitSet;

import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.index.OccurrencesIndex;
import org.ala.spatial.analysis.index.FilteringIndex;
import org.ala.spatial.analysis.index.SpeciesRecord;
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
    ArrayList<SpeciesRecord> getTopSpeciesRecord() {
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
            ArrayList<SpeciesRecord> rka = (ArrayList<SpeciesRecord>) ois.readObject();
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
    void saveTopSpeciesRecord(ArrayList<SpeciesRecord> speciesrecord) {
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
            ArrayList<SpeciesRecord> topspeciesrecord = getTopSpeciesRecord();

            /* get new top record key list */
            layerfilters.add(new_filter);
            ArrayList<SpeciesRecord> newspeciesrecord;
            if (new_filter.catagories == null) {
                newspeciesrecord = FilteringIndex.getGridSampleSet(new_filter);
            } else {
                newspeciesrecord = FilteringIndex.getCatagorySampleSet(new_filter);
            }

            //reduce size of newspeciesrecord
            int i, j;
            if (topspeciesrecord != null) {
                ArrayList<SpeciesRecord> output = new ArrayList<SpeciesRecord>(topspeciesrecord.size());

                for (i = 0, j = 0; i < topspeciesrecord.size() && j < newspeciesrecord.size(); /*iterator in loop*/) {
                    if (topspeciesrecord.get(i).record < newspeciesrecord.get(j).record) {
                        //move forward
                        i++;
                    } else if (topspeciesrecord.get(i).record == newspeciesrecord.get(j).record) {
                        //save
                        output.add(topspeciesrecord.get(i));

                        //increment both
                        i++;
                        while (i < topspeciesrecord.size()
                                && topspeciesrecord.get(i).record == topspeciesrecord.get(i - 1).record) {
                            i++;
                        }
                        j++;
                        while (j < newspeciesrecord.size()
                                && newspeciesrecord.get(j).record == newspeciesrecord.get(j - 1).record) {
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
     * @param session_id_ session id as String
     * @param region bounding region for results, or null for none
     * @return number of unqiue species as int
     */
    static public int getSpeciesCount(String session_id_, SimpleRegion region) {
        int i;

        /* load session */
        FilteringService fs = FilteringService.getSession(session_id_);

        /* get top filter speciesrecord */
        ArrayList<SpeciesRecord> rk = fs.getTopSpeciesRecord();
        if (rk == null) {
            return 0;
        }

        /* make list of species, for inside region filter */
        BitSet species = new BitSet(OccurrencesIndex.getSpeciesIndex().length + 1);
        if (region == null) {
            //no region, use all
            for (i = 0; i < rk.size(); i++) {
                species.set(rk.get(i).species);
            }
        } else {
            for (i = 0; i < rk.size(); i++) {
                if (OccurrencesIndex.inRegion(rk.get(i).record, region)) {
                    species.set(rk.get(i).species);
                }
            }
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
     * @param session_id_ session id as String
     * @param region region to restrict results as SimpleRegion or null for none
     * @return list of unqiue species as String, '\r\n' delimited
     */
    static public String getSpeciesList(String session_id_, SimpleRegion region) {
        int i;

        /* load session */
        FilteringService fs = FilteringService.getSession(session_id_);

        /* get top speciesrecords */
        ArrayList<SpeciesRecord> rk = fs.getTopSpeciesRecord();

        /* make species list */
        BitSet species = new BitSet(OccurrencesIndex.getSpeciesIndex().length + 1);
        if (region == null) {
            for (i = 0; i < rk.size(); i++) {
                species.set(rk.get(i).species);
            }
        } else {
            for (i = 0; i < rk.size(); i++) {
                if (OccurrencesIndex.inRegion(rk.get(i).record, region)) {
                    species.set(rk.get(i).species);
                }
            }
        }

        /* count */
        int count = 0;
        for (i = 0; i < species.length(); i++) {
            if (species.get(i)) {
                count++;
            }
        }

        /* make into string of species names */
        StringBuffer sb = new StringBuffer();
        for (i = 0; i < species.size(); i++) {
            if (species.get(i)) {
                sb.append(OccurrencesIndex.getSpeciesIndex()[i].name);
                sb.append("\r\n");
            }
        }

        return sb.toString();
    }

    /**
     * gets samples records from a session and filtered region
     * @param session_id_ session id as String
     * @param region filtered region as SimpleRegion or null for none
     * @return Samples records as String
     */
    static public String getSamplesList(String session_id_, SimpleRegion region) {
        /* load session */
        FilteringService fs = FilteringService.getSession(session_id_);

        /* get top speciesrecord */
        ArrayList<SpeciesRecord> rk = fs.getTopSpeciesRecord();

        /* get record indexes */
        if (rk.size() == 0) {
            return "";	//no records to return;
        }
        int[] records = new int[rk.size()];
        int p = 0;
        int i;
        if (region == null) {
            /* no defined region, use all */
            for (i = 0; i < rk.size(); i++) {
                records[p++] = rk.get(i).record;
            }
        } else {
            /* restrict by region */

            /* TODO: check, could be faster to use
             * OccurrencesIndex.getRecordsInside(region)
             */
            for (i = 0; i < rk.size(); i++) {
                if (OccurrencesIndex.inRegion(rk.get(i).record, region)) {
                    records[p++] = rk.get(i).record;
                }
            }
            if (p > 0) {
                int[] records_tmp = java.util.Arrays.copyOf(records, p);
                records = records_tmp;
            } else {
                return "";	//no records to return
            }
        }

        /* get samples records from records indexes */
        String[] samples = OccurrencesIndex.getSortedRecords(records);

        /* write samples to a file */
        try {
            File temporary_file = java.io.File.createTempFile("filter_sample", ".csv");
            FileWriter fw = new FileWriter(temporary_file);

            /* output header */
            StringBuffer sbHeader = new StringBuffer();
            for (String s : TabulationSettings.occurances_csv_fields) {
                sbHeader.append(s);
                sbHeader.append(",");
            }
            sbHeader.deleteCharAt(sbHeader.length()-1);
            sbHeader.append("\r\n");
            fw.append(sbHeader.toString());
            for (i = 0; i < samples.length; i++) {
                fw.append(samples[i]);
                fw.append("\r\n");
            }

            fw.close();

            return temporary_file.getPath();	//return location of temp file

        } catch (Exception e) {
            (new SpatialLogger()).log("FilteringService: getSamplesList()", e.toString());
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
}
