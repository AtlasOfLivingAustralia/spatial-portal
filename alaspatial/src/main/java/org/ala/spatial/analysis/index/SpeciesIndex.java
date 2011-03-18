/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import org.ala.spatial.util.TabulationSettings;

/**
 * SpeciesIndex is an index of the single_index in all Datasets.
 *
 * Uses are:
 * 1. Search by LSID.
 * 2. Search by taxon name. (returns a list for autocomplete)
 * 3. Search by common name. (returns a list for autocomplete)
 * 4. Get search result type.
 * 5. Get hierarchy parent by type.
 *
 * @author Adam
 */
public class SpeciesIndex {

    /*
     * Records in the index, sorted by lsid.
     */
    static SpeciesIndexRecord[] singleIndex = new SpeciesIndexRecord[0];
    /*
     * Get singleIndex record idx by .idx attribute.
     */
    static int[] singleIndexOrder = new int[0];
    /*
     * sorted list of common names and reference to single_index row in file_pos
     */
    static CommonNameRecord[] common_names_indexed;
    /*
     * list of all common names vs single_index
     */
    static String[] common_names_all_vs_single_index;
    /*
     * sorted list of species names
     */
    static String[] speciesNames;
    /*
     * singleIndex idx for each speciesNames record
     */
    static int[] speciesNamesToIdx;
    /**
     * top level indexes for pre-defined groupings
     */
    static int[] level10;
    static String[] level10lookup; //display names
    static int[] level10colours; //colours
    /**
     * pre-defined groupings from
     *
     */
    static String[] level10names = {"Animalia","Mammalia","Aves","Reptilia","Amphibia","Agnatha","Chrondrichthyes","Osteichthyes","Actinopterygii","Sarcopterygii","Insecta","Plantae","Fungi","Chromista","Protozoa","Bacteria"};
    static String[] level10displayNames = {"01Animals","02Mammals","03Birds","04Reptiles","05Amphibians","06Fish","06Fish","06Fish","06Fish","06Fish","07Insects","08Plants","09Fungi","10Chromista","11Protozoa","12Bacteria"};
    static int[] level10displayColours = {0x00B0F0,0x0033CC,0x66FF33,0x00B050,0x215967,0xFF0000,0xFF0000,0xFF0000,0xFF0000,0xFF0000,0x800080,0xCC00CC,0xFF6600,0x974700,0xFFC000,0xFFFF00};

    static public void init() {
    }

    static public int[] add(IndexedRecord[] addIndex, String[] speciesNames) {
        int[] lookup = new int[addIndex.length];

        SpeciesIndexRecord[] recordsToAdd = new SpeciesIndexRecord[addIndex.length];
        int recordsToAddCount = 0;

        int[] parents = identifyParents(addIndex);

        for (int i = 0; i < addIndex.length; i++) {
            int pos = findPos(addIndex[i].name);
            if (pos < 0) {
                //add to end
                lookup[i] = singleIndex.length + recordsToAddCount;
                recordsToAdd[recordsToAddCount] = new SpeciesIndexRecord(addIndex[i].name, speciesNames[i], null, lookup[i], parents[i], addIndex[i].record_end - addIndex[i].record_start + 1, addIndex[i].type);
                recordsToAddCount++;
            } else {
                //store
                lookup[i] = singleIndex[pos].idx;
                singleIndex[pos].count += addIndex[i].record_end - addIndex[i].record_start + 1;
                singleIndex[pos].type = (byte) Math.min(addIndex[i].type, singleIndex[pos].type);
                if (singleIndex[pos].parent < 0) {
                    singleIndex[pos].parent = parents[i];
                }
            }
        }

        //append new records
        if (recordsToAddCount > 0) {
            append(java.util.Arrays.copyOfRange(recordsToAdd, 0, recordsToAddCount));
        }

        //using lookup, assign parents
        for (int i = 0; i < lookup.length; i++) {
            int currentParent = singleIndex[singleIndexOrder[lookup[i]]].parent;

            //is it a better parent?
            if (currentParent > parents[i]) {
                singleIndex[singleIndexOrder[lookup[i]]].parent = parents[i];
            }
        }

        initLevel10();

        System.out.println("SpeciesIndex size: " + singleIndex.length);

        return lookup;
    }

    static void initLevel10() {
        TreeMap<Integer, String> map = new TreeMap<Integer, String>();
        for(int i=0;i<level10names.length;i++) {
            String lsid = getLSID(level10names[i]);
            if(lsid != null) {
                int pos = findLSID(lsid);
                if(pos >= 0) {
                    map.put(pos, level10displayNames[i]);
                }
            }
        }
        level10 = new int[map.size()];
        level10lookup = new String[map.size()];
        level10colours = new int[map.size()];
        int pos = 0;
        for(Entry<Integer, String> e : map.entrySet()) {
            level10[pos] = e.getKey();
            level10lookup[pos] = e.getValue();
            for(int i=0;i<level10displayNames.length;i++) {
                if(level10lookup[pos].equals(level10displayNames[i])) {
                    level10colours[pos] = level10displayColours[i]; //level10displayNames[i].hashCode()
                    break;
                }
            }
            pos++;
        }
    }

    static public void remove(IndexedRecord[] addIndex) {
        int[] location = new int[addIndex.length];
        int[] size = new int[addIndex.length];
        int errCount = 0;

        for (int i = 0; i < addIndex.length; i++) {
            int pos = findPos(addIndex[i].name);
            if (pos < 0) {
                //add to end
                errCount++;     //this indicates that the index was never added
            } else {
                //store index
                location[i] = pos;
                size[i] = addIndex[i].record_end - addIndex[i].record_start + 1;
            }
        }

        //adjust record counts if the index may have been added
        if (errCount == 0) {
            for (int i = 0; i < location.length; i++) {
                singleIndex[i].count -= size[i];
            }
        }
    }

    private static int findPos(String lsid) {
        int pos = java.util.Arrays.binarySearch(singleIndex,
                new SpeciesIndexRecord(lsid, null, null, 0, 0, 0, (byte) -1),
                new Comparator<SpeciesIndexRecord>() {

                    public int compare(SpeciesIndexRecord r1, SpeciesIndexRecord r2) {
                        return r1.lsid.compareTo(r2.lsid);
                    }
                });

        //pos to index order
        if (pos >= 0) {
            pos = (int) singleIndex[pos].idx;
        }

        return pos;
    }

    private static void append(SpeciesIndexRecord[] recordsToAdd) {
        if (singleIndex.length == 0) {
            singleIndex = recordsToAdd;
        } else {
            SpeciesIndexRecord[] newIndex = new SpeciesIndexRecord[singleIndex.length + recordsToAdd.length];
            System.arraycopy(singleIndex, 0, newIndex, 0, singleIndex.length);
            System.arraycopy(recordsToAdd, 0, newIndex, singleIndex.length, recordsToAdd.length);
            singleIndex = newIndex;
        }

        java.util.Arrays.sort(singleIndex,
                new Comparator<SpeciesIndexRecord>() {

                    public int compare(SpeciesIndexRecord r1, SpeciesIndexRecord r2) {
                        return r1.lsid.compareTo(r2.lsid);
                    }
                });

        singleIndexOrder = new int[singleIndex.length];
        for (int i = 0; i < singleIndex.length; i++) {
            singleIndexOrder[singleIndex[i].idx] = i;
        }

        buildNamesIndex();

        loadCommonNamesIndex();
    }

    static public int findLSID(String lsid) {
        return findPos(lsid);
    }

    /**
     * loads common names index (common name with file_pos == single_index position)
     */
    static void loadCommonNamesIndex() {
        try {
            ArrayList<CommonNameRecord> cn = new ArrayList<CommonNameRecord>(singleIndex.length);

            //load the common names file (csv), populate common_names as it goes
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                    new FileInputStream(TabulationSettings.common_names_csv), "UTF-8"));

            String s;
            String[] sa;
            int i;
            int max_columns = 0;

            int count = 0;
            boolean isCsv = true;
            while ((s = br.readLine()) != null) {
                //check for continuation line
                while (s != null && s.length() > 0 && s.charAt(s.length() - 1) == '\\') {
                    String spart = br.readLine();
                    if (spart == null) {  //same as whole line is null
                        break;
                    } else {
                        s.replace('\\', ' ');   //new line is same as 'space'
                        s += spart;
                    }
                }//repeat as necessary

                sa = s.split(",");
                if (!isCsv || (count == 0 && sa.length == 1)) {
                    sa = s.split("\t");
                    isCsv = false;
                }

                /* handlers for the text qualifiers and ',' in the middle */
                if (sa != null && max_columns == 0) {
                    max_columns = sa.length;
                }
                if (sa.length < max_columns || sa[0] == null || sa[0].length() == 0 || s.contains("\\")) {
                    System.out.println("error with common names line: " + s);
                    continue;
                }
                if (sa != null && sa.length > max_columns) {
                    sa = OccurrencesIndex.split(s);
                }

                /* remove quotes and commas form terms */
                for (i = 0; i < sa.length; i++) {
                    if (sa[i].length() > 0) {
                        sa[i] = sa[i].replace("\"", "");
                        sa[i] = sa[i].replace(",", ";");
                        sa[i] = sa[i].replace("|", "-");
                        sa[i] = sa[i].replace("/", ";");
                    }
                }

                int pos = findLSID(sa[0].toLowerCase().trim());

                if (pos >= 0 && pos < singleIndex.length) {
                    cn.add(new CommonNameRecord(sa[1].replace(',', ';'), pos));
                    count++;
                }
            }

            //copy
            common_names_indexed = new CommonNameRecord[cn.size()];
            cn.toArray(common_names_indexed);

            //sort
            java.util.Arrays.sort(common_names_indexed,
                    new Comparator<CommonNameRecord>() {

                        public int compare(CommonNameRecord r1, CommonNameRecord r2) {
                            return r1.nameLowerCase.compareTo(r2.nameLowerCase);
                        }
                    });


            //common_names_all_matching_single_index
            common_names_all_vs_single_index = new String[singleIndex.length];
            for (i = 0; i < common_names_all_vs_single_index.length; i++) {
                if (common_names_all_vs_single_index[i] == null) {
                    common_names_all_vs_single_index[i] = "";
                }
            }
            for (i = 0; i < common_names_indexed.length; i++) {
                if (common_names_all_vs_single_index[common_names_indexed[i].index].length() > 0) {
                    common_names_all_vs_single_index[common_names_indexed[i].index] += ", ";
                }
                common_names_all_vs_single_index[common_names_indexed[i].index] += common_names_indexed[i].name;
            }


            System.out.println("common name index finds: " + count);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * return common name for single_index_record, otherwise empty string
     *
     * @param single_index_record as int
     * @return common names or empty string
     */
    static public String getCommonNames(int pos) {
        if (pos < 0 || pos >= singleIndexOrder.length) {
            return "";
        } else {
            return common_names_all_vs_single_index[singleIndexOrder[pos]];
        }
    }

    /**
     * return taxon rank name for single_index_record, otherwise empty string
     *
     * @param single_index_record as int
     * @return rank name
     */
    static public String getTaxonRank(int pos) {
        if (pos < 0 || pos >= singleIndexOrder.length) {
            return "";
        } else {
            return getIndexType(singleIndex[singleIndexOrder[pos]].type);
        }
    }

    /**
     * gets index type column name
     * @return index type column name as String
     */
    static public String getIndexType(int type) {
        return TabulationSettings.occurrences_csv_twos_names[type];
    }

    /**
     * returns a list of (species names / type / count) for valid
     * .beginsWith matches
     *
     * @param filter begins with text to search for (NOT LSID, e.g. species name)
     * @param limit limit on output
     * @return formatted species matches as String[]
     */
    static public String[] filterBySpeciesName(String filter, int limit) {
        if (filter == null || filter.length() == 0) {
            return new String[0];
        }

        filter = filter.toLowerCase();

        int pos = Arrays.binarySearch(speciesNames, filter.substring(0, filter.length() - 1)
                + ((char) (((int) filter.charAt(filter.length() - 1)) - 1)) + ((char) 254));       //lower bound
        int upperpos = Arrays.binarySearch(speciesNames, filter.substring(0, filter.length() - 1)
                + ((char) (((int) filter.charAt(filter.length() - 1)) + 1)));       //upper bound
        IndexedRecord lookfor = new IndexedRecord("", 0, 0, (byte) -1);

        String[] matches_array = null;

        /* starts with comparator, get first (pos) and last (upperpos) */
        if (!filter.contains("*")) {
            /* adjust/limit positions found for non-exact matches */
            if (pos < 0) { //don't care if it is the insertion point
                pos = pos * -1;
                pos--;
            }
            if (upperpos < 0) {
                upperpos *= -1;
                upperpos--;
            }

            /* may need both forward and backwards on this pos */
            int end = limit + pos;
            if (end > upperpos) {
                end = upperpos;
            }

            matches_array = new String[end - pos];

            /* format output */
            StringBuilder strbuffer2 = new StringBuilder();
            int i;
            int p;
            for (p = 0, i = pos; i < end; p++, i++) {
                int idx = singleIndexOrder[speciesNamesToIdx[i]];

                strbuffer2.delete(0, strbuffer2.length());
                strbuffer2.append(speciesNames[i]);
                strbuffer2.append(" / ");
                strbuffer2.append(singleIndex[idx].lsid);
                strbuffer2.append(" / ");
                strbuffer2.append(getIndexType(singleIndex[idx].type));
                strbuffer2.append(" / found ");
                strbuffer2.append(String.valueOf(singleIndex[idx].count));
                matches_array[p] = strbuffer2.toString();
            }
        }

        return matches_array;
    }

    /**
     * return family name for record, otherwise empty string
     *
     * @param species_idx as int
     * @return family name or empty string
     */
    static public String getFamilyName(int pos) {
        while (pos >= 0 && singleIndex[singleIndexOrder[pos]].type > TabulationSettings.species_list_first_column_index) {
            pos = singleIndex[singleIndexOrder[pos]].parent;
        }

        if (pos >= 0 && singleIndex[singleIndexOrder[pos]].type == TabulationSettings.species_list_first_column_index) {
            return singleIndex[singleIndexOrder[pos]].name;
        } else {
            return "undefined";
        }
    }

    /**
     * finds isWithin matches against common names
     *
     * output is '\n' separated list of commonName / index(LSID) / scientificName / count
     *
     * @param name partial common name text to search
     * @param index_finds list of strings containing LSIDs to be excluded.  Can be null.
     * @return '\n' separated String of 'commonName / index(LSID) / scientificName / count'
     */
    static public String getCommonNames(String name, String[] index_finds, int maxRecords) {
        if (name == null) {
            return "";
        }

        int j;
        TreeSet<Integer> ss = new TreeSet<Integer>();
        StringBuffer sb = new StringBuffer();

        name = name.trim().toLowerCase();
        String nameLowerCase = name.replaceAll("[^a-z]", "");

        //Test with 'contains' for a-z only
        //and a 'contains' against lowercase for international characters
        int findCount = 0;

        for (int i = 0; i < common_names_indexed.length && findCount < maxRecords; i++) {
            if ((nameLowerCase.length() > 0 && common_names_indexed[i].nameAZ.contains(nameLowerCase))
                    || common_names_indexed[i].nameLowerCase.contains(name)) {
                String lsid = singleIndex[common_names_indexed[i].index].lsid;
                //check within index_finds for the lsid
                if (index_finds != null) {
                    for (j = 0; j < index_finds.length; j++) {
                        if (index_finds[j].contains(lsid)) {
                            break;
                        }
                    }
                    if (j < index_finds.length) {
                        continue;
                    }
                }
                //determine if index already present, add if missing
                int s1 = ss.size();
                ss.add(common_names_indexed[i].index);
                if (ss.size() > s1) {
                    int o = singleIndexOrder[common_names_indexed[i].index];

                    String sn = "unknown";
                    String type = "unknown";
                    String commonNames = "";
                    int count = 0;
                    if (o >= 0) {
                        sn = singleIndex[o].name;
                        type = getIndexType(singleIndex[o].type);
                        commonNames = getCommonNames(o);
                        count = singleIndex[o].count;
                    }
                    sn = sn.substring(0, 1).toUpperCase() + sn.substring(1).toLowerCase();

                    sb.append(common_names_indexed[i].name).append(" / ").append(lsid).append(" / ").append(type).append(": ").append(sn).append(" / found ").append(count).append("\n");

                    findCount++;
                }
            }
        }
        return sb.toString();
    }

    /**
     * return LSID for single_index_record
     *
     * @param single_index_record as int
     * @return rank name
     */
    static public String getLSID(int pos) {
        if (pos < 0 || pos >= singleIndexOrder.length) {
            return "";
        } else {
            return singleIndex[singleIndexOrder[pos]].lsid;
        }
    }

    /**
     * return first found LSID by speciesName
     *
     * @param speicesName String
     * @return LSID as String
     */
    static public String getLSID(String speciesName) {
        String[] s = filterBySpeciesName(speciesName, 1);
        if (s != null && s.length > 0) {
            String[] words = s[0].split("/");
            if (words.length > 1) {
                int pos = findLSID(words[1].trim());
                return getLSID(pos);
            }
        }
        return "";
    }

    /**
     * gets scientificName taxonrank for provided lsid
     * @param lsid
     * @return String [] where String[0] is scientific name and String[1] is taxonrank name
     */
    static public String[] getFirstName(String lsid) {
        int pos = findLSID(lsid);

        if (pos >= 0) {
            String[] out = new String[2];
            out[0] = singleIndex[pos].name;
            //} else {
            //    out[0] = "unknown";
            //}
            out[1] = getIndexType(singleIndex[pos].type);
            return out;
        }

        return null;
    }

    /**
     * return scientifc name for record, otherwise empty string
     *
     * @param species_idx as int
     * @return scientific name or "undefined"
     */
    static public String getScientificName(int pos) {
        return singleIndex[pos].name;
        //return "undefined";
    }

    /**
     * return scientific name for record with the level10 index, otherwise empty string
     *
     * @param species_idx as int
     * @return scientific name or "Other"
     */
    static public String getScientificNameLevel10(int pos) {
        int p = java.util.Arrays.binarySearch(level10, pos);
        if(p >= 0) {
            return level10lookup[p];
        } else {
            return "99Other";
        }

        //return "undefined";
    }

    private static void buildNamesIndex() {
        SpeciesIndexRecord[] sir = singleIndex.clone();

        //sort
        java.util.Arrays.sort(sir,
                new Comparator<SpeciesIndexRecord>() {

                    public int compare(SpeciesIndexRecord r1, SpeciesIndexRecord r2) {
                        return r1.name.toLowerCase().compareTo(r2.name.toLowerCase());
                    }
                });

        speciesNames = new String[sir.length];
        speciesNamesToIdx = new int[sir.length];

        for (int i = 0; i < sir.length; i++) {
            speciesNames[i] = sir[i].name.toLowerCase();
            speciesNamesToIdx[i] = sir[i].idx;
        }
    }

    public static int getCount(int pos) {
        return singleIndex[pos].count;
    }

    static int size() {
        return singleIndex.length;
    }

    private static int[] identifyParents(IndexedRecord[] addIndex) {
        //init parents to -1
        int[] parents = new int[addIndex.length];
        for (int i = 0; i < parents.length; i++) {
            parents[i] = -1;
        }

        //After sorting a 'parent' is only 'before' a child.
        //Move back until the 'type' is < current type, then continue back
        //until passed one record without a parent assigned

        IndexedRecord2[] copyIndex = new IndexedRecord2[addIndex.length];
        for (int i = 0; i < copyIndex.length; i++) {
            copyIndex[i] = new IndexedRecord2(addIndex[i].name, addIndex[i].record_start, addIndex[i].record_end, addIndex[i].type, i);
        }
        java.util.Arrays.sort(copyIndex,
                new Comparator<IndexedRecord2>() {

                    public int compare(IndexedRecord2 r1, IndexedRecord2 r2) {
                        if (r1.record_start == r2.record_start) {
                            return r1.type - r2.type;
                        }
                        return r1.record_start - r2.record_start;
                    }
                });

        int start, end, j;
        for (int i = 1; i < copyIndex.length; i++) {
            if (copyIndex[i].name.equals("urn:lsid:biodiversity.org.au:afd.taxon:558a729a-789b-4b00-a685-8843dc447319")) {
                i = i + 1;
                i = i - 1;
            }
            if (i > 0 && copyIndex[i].record_start == copyIndex[i - 1].record_end + 1
                    && copyIndex[i].type == copyIndex[i - 1].type) {
                parents[(int) copyIndex[i].pos] = parents[(int) copyIndex[i - 1].pos];
                continue;
            }

            start = copyIndex[i].record_start;
            end = copyIndex[i].record_end;

            for (j = i - 1; j == i - 1 || parents[(int) copyIndex[j + 1].pos] != -1; j--) {
                //is [j] a parent of [i]?
                if (copyIndex[j].record_start <= start
                        && copyIndex[j].record_end >= end) {
                    parents[(int) copyIndex[i].pos] = (int) copyIndex[j].pos;
                    break;
                }
            }
        }

        return parents;
    }

    static int getHash(int parentLevel, int pos) {
        if(parentLevel >= 10) {
            //match to level10 members
            while(pos >= 0 && java.util.Arrays.binarySearch(level10, pos) < 0) {
                pos = singleIndex[singleIndexOrder[pos]].parent;
            }
            if(pos >= 0) {
                //need hash of displayname because of duplicates
                int p = java.util.Arrays.binarySearch(level10, pos);
                if(p >= 0) {
                    return level10colours[p];
                }
            }
        } else {
            while (pos >= 0 && singleIndex[singleIndexOrder[pos]].type > parentLevel) {
                pos = singleIndex[singleIndexOrder[pos]].parent;
            }
            if (pos >= 0) {
                return singleIndex[singleIndexOrder[pos]].hash;
            }
        }

        return 0xFFFFFFFF;    //white
    }

    static int getParentPos(int parentLevel, int pos) {
        if(parentLevel >= 10) {
            //match to level10 members
            while(pos >= 0 && java.util.Arrays.binarySearch(level10, pos) < 0) {
                pos = singleIndex[singleIndexOrder[pos]].parent;
            }
        } else {
            while (pos >= 0 && singleIndex[singleIndexOrder[pos]].type > parentLevel) {
                pos = singleIndex[singleIndexOrder[pos]].parent;
            }
        }

        return pos;
    }
}
