package org.ala.spatial.analysis.index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.spatial.analysis.cluster.Record;
import org.ala.spatial.util.OccurrencesFieldsUtil;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;
import org.springframework.util.StringUtils;

/**
 * builder for occurances index.
 *
 * expects occurances in a csv containing at least species, longitude
 * and latitude.
 *
 * expects TabulationSettings fields for
 * 	<li>occurances csv file location (has header)
 * 	<li>list of relevant hierarchy related columns in occurances csv
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
public class OccurrencesIndex implements AnalysisIndexService {

    /**
     * sorted records filename
     */
    static final String SORTED_FILENAME = "OCC_SORTED.csv";
    static final String EXCLUDED_FILENAME = "OCC_EXCLUDED.csv";
    /**
     * points filename, in sorted records order
     */
    static final String POINTS_FILENAME = "OCC_POINTS.dat";
    /**
     * sorted store of String[] * 2, first for ConceptId, 2nd for Names
     */
    public static final String SORTED_CONCEPTID_NAMES = "OCC_CONCEPTID_NAMES.dat";
    /**
     * geo sorted points filename, in latitude then longitude sorted order
     */
    static final String POINTS_FILENAME_GEO = "OCC_POINTS_GEO.dat";
    /**
     * list of file positions for SORTED_FILENAME for lines
     */
    static final String SORTED_LINE_STARTS = "OCC_SORTED_LINE_STARTS.dat";
    /**
     * index for geo sorted points file
     */
    static final String POINTS_FILENAME_GEO_IDX = "OCC_POINTS_GEO_IDX.dat";
    /**
     * points filename, in 0.5 degree grid, latitude then longitude, sort order
     */
    static final String POINTS_FILENAME_05GRID = "OCC_POINTS_05GRID.dat";
    /**
     * index for 05grid points file
     */
    static final String POINTS_FILENAME_05GRID_IDX = "OCC_POINTS_05GRID_IDX.dat";
    /**
     * world grid of 05grid point index entry points
     *
     * for retrieving record numbers, and point longitude and latitudes,
     * from 05grid index
     */
    static final String POINTS_FILENAME_05GRID_KEY = "OCC_POINTS_05GRID_KEY.dat";
    /**
     * sensitive coordinates, if available
     */
    static final String SENSITIVE_COORDINATES = "SENSITIVE_COORDINATES";
    /**
     * species index file, contains IndexRecord for each species
     */
    static final String SPECIES_IDX_FILENAME = "OCC_IDX_SPECIES0.dat";
    /**
     * species record array with family record index, to lookup family from
     * record number
     */
    static final String SPECIES_TO_FAMILY = "OCC_SPECIES_TO_FAMILY.dat";
    /**
     * prefix for non-species index files
     */
    static final String OTHER_IDX_PREFIX = "OCC_IDX_";
    /**
     * postfix for non-species index files
     */
    static final String OTHER_IDX_POSTFIX = ".dat";
    /**
     * map of id's to record numbers
     */
    static final String ID_LOOKUP = "ID_LOOKUP.dat";
    /**
     * size of parts to be exported by splitter
     */
    static final int PART_SIZE_MAX = 1000000000;
    /**
     * static instance of all indexed data for filtering
     */
    static ArrayList<IndexedRecord[]> all_indexes = new ArrayList<IndexedRecord[]>();
    /**
     * single static instance of all indexed data for filtering
     */
    static IndexedRecord[] single_index = null;
    /**
     * static instance of all points records
     *
     * for frequent use
     */
    static double[][] all_points = null;
    /**
     * mapping between conceptId's and paired searchable names
     */
    static String[] occurances_csv_field_pairs_ConceptId;
    static String[] occurances_csv_field_pairs_Name;
    static int[] occurances_csv_field_pairs_ToSingleIndex;
    static int[] occurances_csv_field_pairs_FirstFromSingleIndex;  //length == single_index.length
    /**
     * list of all common names vs single_index
     */
    static String[] common_names_all_vs_single_index;

    /**
     * finds isWithin matches against common names
     *
     * output is '\n' separated list of commonName / index(LSID) / scientificName / count
     *
     * @param name partial common name text to search
     * @param index_finds list of strings containing LSIDs to be excluded.  Can be null.
     * @return '\n' separated String of 'commonName / index(LSID) / scientificName / count'
     */
    public static String getCommonNames(String name, String[] index_finds) {
        int j;
        TreeSet<Integer> ss = new TreeSet<Integer>();
        StringBuffer sb = new StringBuffer();
        String nameLowerCase = name.toLowerCase();
        for (int i = 0; i < common_names_indexed.length; i++) {
            if (common_names_indexed[i].nameLowerCase.contains(nameLowerCase)) {
                String lsid = single_index[common_names_indexed[i].index].name;
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
                    int o = occurances_csv_field_pairs_FirstFromSingleIndex[common_names_indexed[i].index];

                    //String sn = single_index[common_names_indexed[i].index].name;
                    String sn = "";
                    if (o >= 0) {
                        sn = occurances_csv_field_pairs_Name[o];
                    }
                    sn = sn.substring(0, 1).toUpperCase() + sn.substring(1).toLowerCase();

                    sb.append(common_names_indexed[i].name).append(" / ").append(lsid) //.append(" / Scientific name: ")
                            .append(" / ").append(getIndexType(single_index[common_names_indexed[i].index].type)).append(": ").append(sn).append(" / found ").append(single_index[common_names_indexed[i].index].record_end
                            - single_index[common_names_indexed[i].index].record_start + 1).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * gets scientificName taxonrank for provided lsid
     * @param lsid
     * @return String [] where String[0] is scientific name and String[1] is taxonrank name
     */
    public static String[] getFirstName(String lsid) {
        loadIndexes();

        String lsidLowerCase = lsid.toLowerCase();

        IndexedRecord searchfor = new IndexedRecord(lsidLowerCase, 0, 0, 0, 0, (byte) 0);
        int pos = java.util.Arrays.binarySearch(single_index, searchfor,
                new Comparator<IndexedRecord>() {

                    public int compare(IndexedRecord r1, IndexedRecord r2) {
                        return r1.name.compareTo(r2.name);
                    }
                });

        if (pos >= 0) {
            String[] out = new String[2];
            out[0] = occurances_csv_field_pairs_Name[occurances_csv_field_pairs_FirstFromSingleIndex[pos]];
            out[1] = getIndexType(single_index[pos].type);
            return out;
        }

        return null;
    }
    /**
     * all occurrences data
     */
    HashMap<Integer, Object> occurrences;
    /**
     * index to actual csv field positions to named
     * fields
     */
    int[] column_positions;
    /**
     * for column_keys write buffer to take some load off sorting
     */
    String prev_key = null;
    /**
     * for column_keys write buffer to take some load off sorting
     */
    StringBuffer prev_value = null;
    /**
     * index lookup for records points on sorted 0.5 degree grid
     */
    static int[] grid_points_idx = null;
    /**
     * reverse index lookup for records points on sorted 0.5 degree grid
     */
    static int[] grid_points_idx_rev = null;
    /**
     * all records points on sorted 0.5 degree grid in lat then long order
     */
    static double[][] grid_points = null;
    /**
     * grid 360degree/0.5degree square, for entry position into grid_points
     */
    static int[][] grid_key = null;
    /**
     * extra_indexes to record number mappings
     *
     * key: String
     * object: ArrayList<Integer> when building, int [] when built.
     */
    static HashMap<String, Object>[] extra_indexes = null;
    /**
     * list of common names against species names
     */
    static String[] common_names;
    /**
     * sorted list of common names and reference to single_index row in file_pos
     */
    static CommonNameRecord[] common_names_indexed;
    /**
     * all sensitive coordinates, or -1
     * [][0] = longitude
     * [][1] = latitude
     */
    static double[][] sensitiveCoordinates;

    /**
     * default constructor
     */
    public OccurrencesIndex() {
        TabulationSettings.load();
    }

    /**
     * performs update of 'indexing' for new points data
     */
    @Override
    public void occurancesUpdate() {
        /* order is important */
        System.out.println("loading occurrences");

        //begin here
        makePartsThreaded();
        mergeParts();
        exportSortedGEOPoints();
        exportSortedGridPoints();

        //run these functions at the same time as other build_all requests
        class makingThread extends Thread {

            public CountDownLatch cdl;

            @Override
            public void run() {
                exportFieldIndexes();
                makeSortedLineStarts();
                loadClusterRecords();
                OccurrencesIndex.makeSensitiveCoordinates(null);
            }
        }
        ;
        makingThread mt = new makingThread();
        mt.start();

        System.out.println("END of OCCURRENCES INDEX");

    }
    TreeMap<String, Integer>[] columnKeys;
    int[] columnKeysToOccurrencesOrder;

    void mergeParts() {
        //count # of parts
        int numberOfParts = 0;
        while ((new File(
                TabulationSettings.index_path
                + SORTED_FILENAME + "_" + numberOfParts)).exists()) {
            numberOfParts++;
        }

        //merge sensitive coordinates parts
        int i;
        try {
            FileWriter fw = new FileWriter(
                    TabulationSettings.index_path
                    + SENSITIVE_COORDINATES + ".csv");

            for (i = 0; i < numberOfParts; i++) {
                BufferedReader r = new BufferedReader(new FileReader(
                        TabulationSettings.index_path
                        + SENSITIVE_COORDINATES
                        + i));
                String s;
                while ((s = r.readLine()) != null) {
                    fw.append(s).append("\n");
                }
                r.close();
            }

            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //TODO: move existing joined output to *_(numberOfParts+1) for inclusion

        String[] lines = new String[numberOfParts];
        BufferedReader[] reader = new BufferedReader[numberOfParts];
        int[] positions = new int[numberOfParts];
        ArrayList<double[][]> points = new ArrayList<double[][]>(numberOfParts);
        int pointsCount = 0;

        try {
            //open output streams (points + sorted data)
            for (i = 0; i < numberOfParts; i++) {
                reader[i] = new BufferedReader(new FileReader(
                        TabulationSettings.index_path + SORTED_FILENAME
                        + "_" + i));
                lines[i] = reader[i].readLine();
                if (lines[i] != null) {
                    lines[i] = lines[i].trim();
                }
                //open points
                double[][] tmp = getPointsPairs(i);
                points.add(tmp);
                positions[i] = 0;
                pointsCount += tmp.length * 2;
            }

            FileWriter fw = new FileWriter(TabulationSettings.index_path
                    + SORTED_FILENAME);
            double[] aPoints = new double[pointsCount];
            int pointsPos = 0;

            //write to file
            int finishedParts = 0;
            String topline;
            while (finishedParts < numberOfParts) {
                //get top record
                topline = null;
                for (i = 0; i < numberOfParts; i++) {
                    if ((topline == null && lines[i] != null)
                            || (topline != null && lines[i] != null
                            && topline.compareTo(lines[i]) > 0)) {
                        topline = lines[i];
                    }
                }

                //write top records & increment
                for (i = 0; i < numberOfParts; i++) {
                    if (lines[i] != null && topline.equalsIgnoreCase(lines[i])) {
                        fw.append(lines[i]);
                        fw.append("\n");

                        //read up next line
                        lines[i] = reader[i].readLine();
                        if (lines[i] == null) {
                            finishedParts++;    //flag that one more is finsihed
                        } else {
                            lines[i] = lines[i].trim();
                        }

                        aPoints[pointsPos] = points.get(i)[positions[i]][0];
                        aPoints[pointsPos + 1] = points.get(i)[positions[i]][1];
                        positions[i]++;
                        pointsPos += 2;
                    }
                }
            }

            fw.close();

            /* export points */
            RandomAccessFile pointsfile = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME,
                    "rw");
            byte[] b = new byte[pointsPos * 8];
            ByteBuffer bb = ByteBuffer.wrap(b);

            for (i = 0; i < pointsPos; i++) {
                bb.putDouble(aPoints[i]);
            }

            pointsfile.write(b);
            pointsfile.close();

            //delete parts
            //for (i = 0; i < numberOfParts; i++) {
            //    (new File(TabulationSettings.index_path
            //            + SORTED_FILENAME + "_" + i)).delete();
            // }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.gc();
    }

    /**
     * performs update of 'indexing' for a new layer (grid or shapefile)
     *
     * @param layername name of the layer to update as String.  To update
     * all layers use null.
     */
    @Override
    public void layersUpdate(String layername) {
        //not applicable
    }

    /**
     * method to determine if the index is up to date
     *
     * @return true if index is up to date
     */
    @Override
    public boolean isUpdated() {
        /* TODO: add each index file */
        File occFile = new File(TabulationSettings.occurances_csv);
        File occSortedFile = new File(TabulationSettings.index_path + SORTED_FILENAME);
        File occPointsFile = new File(TabulationSettings.index_path + POINTS_FILENAME);
        File occSpeciesFile = new File(TabulationSettings.index_path + SPECIES_IDX_FILENAME);

        return occFile.lastModified() < occSortedFile.lastModified()
                && occFile.lastModified() < occSpeciesFile.lastModified()
                && occFile.lastModified() < occPointsFile.lastModified();
    }

    /**
     * splits a csv line with " text qualifier
     *
     * states:
     * 	begin term: if " then set qualified to true
     * 	in term: qualified and ", then end qualified and end term
     * 	in term: not qualified and , then end term
     * 	in term: qualified and "" then "*
     *
     */
    static String[] split(String line) {
        ArrayList<String> al = new ArrayList<String>();
        int i;

        boolean qualified = false;
        boolean begin_term = true;
        boolean end_term = false;

        String word = "";
        for (i = 0; i < line.length(); i++) {
            if (begin_term && line.charAt(i) == '"') {
                qualified = true;
                begin_term = false;
            } else if (!qualified
                    && line.charAt(i) == ',') { //end term
                end_term = true;
            } else if (qualified
                    && line.charAt(i) == '"'
                    && line.length() > i + 1
                    && line.charAt(i + 1) == '"') {//one "
                i++;
                word += line.charAt(i + 1);
            } else if (qualified
                    && line.charAt(i) == '"'
                    && ((line.length() > i + 1
                    && line.charAt(i + 1) == ',')
                    || line.length() == i + 1)) {//end term
                end_term = true;
                qualified = false;
                i++;
            } else {
                word += line.charAt(i);
            }
            if (end_term) {
                begin_term = true;
                al.add(word);
                word = "";
                end_term = false;
            }
        }
        String[] string = new String[al.size()];
        for (i = 0; i < al.size(); i++) {
            string[i] = al.get(i);
        }
        return string;
    }

    /**
     * populates column_positions array with actual column positions
     * of named columns in the header.
     *
     * @param line header of occurances.csv as String []
     */
    void getColumnPositions(String[] line) throws Exception {
        String[] columns = TabulationSettings.occurances_csv_fields;
        column_positions = new int[columns.length];
        int i;
        int j;

        for (i = 0; i < column_positions.length; i++) {
            column_positions[i] = -1;
        }

        for (j = 0; j < line.length; j++) {
            for (i = 0; i < columns.length; i++) {
                if (columns[i].equalsIgnoreCase(line[j])) {
                    column_positions[i] = j;
                }
            }
        }

        StringBuffer msg = new StringBuffer();
        boolean error = false;
        for (i = 0; i < column_positions.length; i++) {
            if (column_positions[i] == -1) {
                msg.append("\r\n").append(columns[i]);
                error = true;
            }
        }
        if (error) {
            System.out.println("FILE HEADER:");
            for (String s : line) {
                System.out.print(s + ",");
            }
            throw new Error("occurrences file has no column: " + msg.toString());
        }
    }

    int[] getSensitiveColumnPositions(String[] line) {
        int[] sensitive_column_positions = new int[3];
        int j;

        for (j = 0; j < line.length; j++) {
            if (TabulationSettings.occurrences_id_field.equalsIgnoreCase(line[j])) {
                sensitive_column_positions[0] = j;
            } else if (TabulationSettings.occurrences_sen_lat_field.equalsIgnoreCase(line[j])) {
                sensitive_column_positions[2] = j;
            } else if (TabulationSettings.occurrences_sen_long_field.equalsIgnoreCase(line[j])) {
                sensitive_column_positions[1] = j;
            }
        }

        if (sensitive_column_positions[0] == -1
                || sensitive_column_positions[1] == -1
                || sensitive_column_positions[2] == -1) {
            System.out.println("cannot find column for one of: "
                    + TabulationSettings.occurrences_id_field
                    + ", " + TabulationSettings.occurrences_sen_long_field
                    + ", " + TabulationSettings.occurrences_sen_lat_field);

            return null;
        }

        return sensitive_column_positions;
    }

    /**
     * operates on OCC_SORTED.csv
     *
     * generates an index for each occurances field (minus longitude
     * and latitude)
     */
    void exportFieldIndexes() {
        System.gc();

        System.out.println("doing exportFieldIndexes (after gc)");

        String[] columns = TabulationSettings.occurances_csv_fields;

        OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();
        ofu.load();

        int countOfIndexed = ofu.onetwoCount;

        /* first n columns are indexed, n=countOfIndexed */
        TreeMap<String, IndexedRecord>[] fw_maps = new TreeMap[countOfIndexed];

        /* build up id lookup for additional columns */
        extra_indexes = new HashMap[ofu.extraIndexes.length];

        int i;

        for (i = 0; i < extra_indexes.length; i++) {
            extra_indexes[i] = new HashMap<String, Object>();
        }

        //names vs concept ids
        TreeMap<String, String> idnames = new TreeMap<String, String>();
        int[] idnamesIdx = new int[TabulationSettings.occurances_csv_field_pairs.length];
        for (int j = 0; j < TabulationSettings.occurances_csv_field_pairs.length; j++) {
            for (i = 0; i < ofu.columnNames.length; i++) {
                if (TabulationSettings.occurances_csv_field_pairs[j].equalsIgnoreCase(ofu.columnNames[i])) {
                    idnamesIdx[j] = i;
                    break;
                }
            }
            if (ofu.columnNames.length == i) {
                System.out.println("ERORR:" + TabulationSettings.occurances_csv_field_pairs[j]);
            }
        }


        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(
                    TabulationSettings.index_path
                    + SORTED_FILENAME));
            String s;
            String[] sa;

            int progress = 0;

            for (i = 0; i < countOfIndexed; i++) {
                fw_maps[i] = new TreeMap<String, IndexedRecord>();
            }
            System.out.println("\r\nsorted columns: " + countOfIndexed);

            String[] last_value = new String[countOfIndexed];
            long[] last_position = new long[countOfIndexed];
            int[] last_record = new int[countOfIndexed];

            for (i = 0; i < countOfIndexed; i++) {
                last_value[i] = "";
                last_position[i] = 0;
                last_record[i] = 0;
            }

            long filepos = 0;
            int recordpos = 0;

            int idColumn = (new OccurrencesFieldsUtil()).onetwoCount;

            ArrayList<Integer>[] obj = new ArrayList[extra_indexes.length];
            String[] prev_key = new String[extra_indexes.length];
            String[] current_key = new String[extra_indexes.length];

            while ((s = br.readLine()) != null) {
                if (progress % 100000 == 0) {
                    System.out.print("\rlines read: " + progress);
                }
                sa = s.split(",");

                progress++;

                //updated = false;
                if (sa.length >= countOfIndexed && sa.length > idColumn) {

                    //conceptid vs names
                    for (i = 0; i < idnamesIdx.length; i += 2) {
                        //valid id's only
                        if (sa[idnamesIdx[i]].length() > 0) {
                            //append 2 spaces for ordering
                            String joined = sa[idnamesIdx[i + 1]].toLowerCase() + "  |  " + sa[idnamesIdx[i]].toLowerCase();
                            idnames.put(joined, sa[idnamesIdx[i]].toLowerCase());
                        }
                    }

                    //add current record to extra_indexes
                    for (i = 0; i < extra_indexes.length; i++) {
                        //only non-empty values
                        if (sa[ofu.extraIndexes[i]].length() > 0) {
                            current_key[i] = sa[ofu.extraIndexes[i]];
                            if (current_key[i].equals(prev_key[i]) && obj[i] != null) {
                                obj[i].add(new Integer(recordpos));
                            } else {
                                prev_key[i] = current_key[i];

                                obj[i] = (ArrayList<Integer>) extra_indexes[i].get(current_key[i]);

                                if (obj[i] == null) {
                                    obj[i] = new ArrayList<Integer>();
                                    extra_indexes[i].put(current_key[i], obj[i]);
                                }
                                obj[i].add(new Integer(recordpos));
                            }
                        }
                    }

                    for (i = 0; i < countOfIndexed; i++) {
                        if (recordpos != 0 && !last_value[i].equalsIgnoreCase(sa[i])) {
                            fw_maps[i].put(last_value[i],
                                    new IndexedRecord(last_value[i].toLowerCase(),
                                    last_position[i],
                                    filepos,
                                    last_record[i],
                                    recordpos - 1, (byte) i));

                            last_position[i] = filepos;
                            last_record[i] = recordpos;

                            last_value[i] = sa[i];
                        } else if (recordpos == 0) {
                            //need to keep first string
                            last_value[i] = sa[i];
                        }
                    }
                }

                recordpos++;
                filepos += s.length() + 1; 	//+1 for '\n'
            }

            for (i = 0; i < countOfIndexed; i++) {
                fw_maps[i].put(last_value[i],
                        new IndexedRecord(last_value[i].toLowerCase(),
                        last_position[i],
                        filepos,
                        last_record[i],
                        recordpos - 1, (byte) i));
            }

            br.close();
        } catch (Exception e) {
            SpatialLogger.log("exportFieldIndexes, read", e.toString());
            e.printStackTrace();
        }

        System.out.println("done read of indexes");

        /* write out extra_indexes */
        //transform ArrayList<Integer> to int[]
        for (i = 0; i < extra_indexes.length; i++) {
            int sz = 0;
            System.out.println("lookup name: " + TabulationSettings.occurances_csv_fields_lookups[i]);

            for (Entry<String, Object> e : extra_indexes[i].entrySet()) {
                ArrayList<Integer> al = (ArrayList<Integer>) e.getValue();
                int[] new_obj = new int[al.size()];
                int z = 0;
                for (Integer q : al) {
                    new_obj[z++] = q.intValue();
                }
                if (sz < 10) {
                    System.out.println(e.getKey() + " > " + al.size());

                }
                sz++;
                e.setValue(new_obj);
            }
            System.out.println("total: " + sz);
        }
        try {
            FileOutputStream fos = new FileOutputStream(
                    TabulationSettings.index_path
                    + ID_LOOKUP);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(extra_indexes);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* write as array objects for faster reads later*/
        try {
            for (i = 0; i < fw_maps.length; i++) {
                Set<Map.Entry<String, IndexedRecord>> set = fw_maps[i].entrySet();
                Iterator<Map.Entry<String, IndexedRecord>> iset = set.iterator();

                IndexedRecord[] ir = new IndexedRecord[set.size()];

                int j = 0;
                while (iset.hasNext()) {
                    /* export the records */
                    ir[j++] = iset.next().getValue();
                }

                String filename = TabulationSettings.index_path
                        + OTHER_IDX_PREFIX + columns[ofu.onestwos[i]] + OTHER_IDX_POSTFIX;

                /* rename the species file, (last indexed column = species column) */
                if (i == countOfIndexed - 1) {
                    filename = TabulationSettings.index_path
                            + SPECIES_IDX_FILENAME;
                }

                FileOutputStream fos = new FileOutputStream(filename);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(ir);
                oos.close();

                /* also write as csv for debugging */
                FileWriter fw = new FileWriter(filename + ".csv");
                for (IndexedRecord r : ir) {
                    fw.append(r.name);
                    fw.append(",");
                    fw.append(String.valueOf(r.file_start));
                    fw.append(",");
                    fw.append(String.valueOf(r.file_end));
                    fw.append(",");
                    fw.append(String.valueOf(r.record_start));
                    fw.append(",");
                    fw.append(String.valueOf(r.record_end));
                    fw.append("\r\n");
                }
                fw.close();
            }
            SpatialLogger.log("exportFieldIndexes done");
        } catch (Exception e) {
            SpatialLogger.log("exportFieldIndexes, write", e.toString());
        }

        //this will fail on the occurances_csv_fields_pairs... loads
        try {
            loadIndexes();
        } catch (Exception e) {
            System.out.println("!!!!!!!!!!!Exception expected here!!!!!!!!!!!!!!");
            e.printStackTrace();
        }

        //write out conceptid vs names
        occurances_csv_field_pairs_ConceptId = new String[idnames.size()];
        occurances_csv_field_pairs_Name = new String[idnames.size()];
        i = 0;
        for (Entry<String, String> et : idnames.entrySet()) {
            occurances_csv_field_pairs_ConceptId[i] = et.getValue();
            occurances_csv_field_pairs_Name[i] = et.getKey().substring(0, et.getKey().indexOf('|')).trim(); //first value is name
            i++;
        }
        //load single_index
        occurances_csv_field_pairs_ToSingleIndex = new int[occurances_csv_field_pairs_ConceptId.length];
        IndexedRecord lookfor = new IndexedRecord("", 0, 0, 0, 0, (byte) -1);
        for (i = 0; i < occurances_csv_field_pairs_ConceptId.length; i++) {
            lookfor.name = occurances_csv_field_pairs_ConceptId[i];
            occurances_csv_field_pairs_ToSingleIndex[i] = java.util.Arrays.binarySearch(single_index,
                    lookfor,
                    new Comparator<IndexedRecord>() {

                        public int compare(IndexedRecord r1, IndexedRecord r2) {
                            return r1.name.compareTo(r2.name);
                        }
                    });
            if (occurances_csv_field_pairs_ToSingleIndex[i] < 0) {
                System.out.println("ERROR2: " + occurances_csv_field_pairs_ConceptId[i] + " : " + occurances_csv_field_pairs_Name[i]);
            } else {
                //duplicates exist in single_index
                while (i > 0 && single_index[occurances_csv_field_pairs_ToSingleIndex[i - 1]].equals(occurances_csv_field_pairs_ConceptId[i])) {
                    i--;
                }
            }
        }
        int j;
        occurances_csv_field_pairs_FirstFromSingleIndex = new int[single_index.length];
        HashMap<String, Integer> hm = new HashMap<String, Integer>();
        for (i = 0; i < occurances_csv_field_pairs_ConceptId.length; i++) {
            hm.put(occurances_csv_field_pairs_ConceptId[i], i);
        }
        for (i = 0; i < single_index.length; i++) {
            Integer in = hm.get(single_index[i].name);
            if (in == null) {
                System.out.println("ERROR3: " + single_index[i].name);
                occurances_csv_field_pairs_FirstFromSingleIndex[i] = -1;
            } else {
                occurances_csv_field_pairs_FirstFromSingleIndex[i] = in.intValue();
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream(
                    TabulationSettings.index_path
                    + SORTED_CONCEPTID_NAMES);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this.occurances_csv_field_pairs_ConceptId);
            oos.writeObject(this.occurances_csv_field_pairs_Name);
            oos.writeObject(this.occurances_csv_field_pairs_FirstFromSingleIndex);
            oos.writeObject(this.occurances_csv_field_pairs_ToSingleIndex);
            oos.close();
            FileWriter fw = new FileWriter(
                    TabulationSettings.index_path
                    + SORTED_CONCEPTID_NAMES + "0.csv");
            for (i = 0; i < occurances_csv_field_pairs_ConceptId.length; i++) {
                fw.append(occurances_csv_field_pairs_ConceptId[i]).append("\r\n");
            }
            fw.close();
            fw = new FileWriter(
                    TabulationSettings.index_path
                    + SORTED_CONCEPTID_NAMES + "1.csv");
            for (i = 0; i < occurances_csv_field_pairs_Name.length; i++) {
                fw.append(occurances_csv_field_pairs_Name[i]).append("\r\n");
            }
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        fw_maps = null;
        System.gc();
    }

    /**
     * returns a list of (species names / type / count) for valid
     * .beginsWith matches
     *
     * @param filter begins with text to search for (NOT LSID, e.g. species name)
     * @param limit limit on output
     * @return formatted species matches as String[]
     */
    static public String[] filterIndex(String filter, int limit) {
        loadIndexes();
        OccurrencesFieldsUtil.load();

        if (filter == null || filter.length() == 0) {
            return new String[0];
        }

        filter = filter.toLowerCase();

        int pos = Arrays.binarySearch(occurances_csv_field_pairs_Name, filter);
        int upperpos = Arrays.binarySearch(occurances_csv_field_pairs_Name, filter.substring(0, filter.length() - 1)
                + ((char) (((int) filter.charAt(filter.length() - 1)) + 1)));
        IndexedRecord lookfor = new IndexedRecord("", 0, 0, 0, 0, (byte) -1);

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
            StringBuffer strbuffer2 = new StringBuffer();
            int i;
            int p;
            for (p = 0, i = pos; i < end; p++, i++) {
                lookfor.name = occurances_csv_field_pairs_ConceptId[i];
                int idx = occurances_csv_field_pairs_ToSingleIndex[i];

                strbuffer2.delete(0, strbuffer2.length());
                //strbuffer2.append(single_index[i].name);
                strbuffer2.append(occurances_csv_field_pairs_Name[i]);
                strbuffer2.append(" / ");
                strbuffer2.append(occurances_csv_field_pairs_ConceptId[i]);
                strbuffer2.append(" / ");
                strbuffer2.append(getIndexType(single_index[idx].type));
                strbuffer2.append(" / found ");
                strbuffer2.append(String.valueOf(single_index[idx].record_end
                        - single_index[idx].record_start + 1));
                matches_array[p] = strbuffer2.toString();
            }
        }

        return matches_array;
    }

    /**
     * returns a list of (species names / type / count) for valid
     * .equalsIgnoreCase matches
     *
     * if input is like "species_name / type" match lookup column name
     * with 'type', e.g. "genus" or "family" or "species".
     *
     * @param filter begins with text to search for (LSID)
     * @return species matches as IndexedRecord[]
     */
    static public IndexedRecord[] filterSpeciesRecords(String filter) {
        filter = filter.toLowerCase();

        /* handle
         * <species name>
         * or
         * <species name> / <type>
         *
        String type = null;
        if (filter.contains("/")) {
        type = filter.split("/")[1].trim();
        filter = filter.split("/")[0].trim();
        }*/

        loadIndexes();

        IndexedRecord searchfor = new IndexedRecord(filter, 0, 0, 0, 0, (byte) 0);
        int pos = java.util.Arrays.binarySearch(single_index, searchfor,
                new Comparator<IndexedRecord>() {

                    public int compare(IndexedRecord r1, IndexedRecord r2) {
                        return r1.name.compareTo(r2.name);
                    }
                });

        if (pos >= 0) {
            IndexedRecord[] indexedRecord = new IndexedRecord[1];
            indexedRecord[0] = single_index[pos];
            return indexedRecord;
        }

        /*if (all_indexes.size() > 0) {
        ArrayList<IndexedRecord> matches = new ArrayList<IndexedRecord>(all_indexes.size());
        int i = 0;
        for (IndexedRecord[] ir : all_indexes) {
        // binary search
        IndexedRecord searchfor = new IndexedRecord(filter, 0, 0, 0, 0, (byte) 0);

        int pos = java.util.Arrays.binarySearch(ir, searchfor,
        new Comparator<IndexedRecord>() {

        public int compare(IndexedRecord r1, IndexedRecord r2) {
        return r1.name.compareTo(r2.name);
        }
        });

        if (pos >= 0 && pos < ir.length) {
        matches.add(ir[pos]);
        break;
        }

        i++;
        }

        // return something if found as []
        if (matches.size() > 0) {
        IndexedRecord[] indexedRecord = new IndexedRecord[matches.size()];
        matches.toArray(indexedRecord);
        return indexedRecord;
        }
        }*/

        return null;
    }
    static int[] speciesNumberInRecordsOrder = null;
    static int[] species_to_family = null;

    /**
     * loads all OccurrencesIndex files for quicker response times
     *
     * excludes points indexes
     */
    static public void loadIndexes() {
        // System.gc();
        TabulationSettings.load();

        if (single_index != null) {
            return;
        }

        loadSortedLineStarts();

        OccurrencesFieldsUtil.load();

        String[] columns = TabulationSettings.occurances_csv_fields;
        String[] columnsSettings = TabulationSettings.occurances_csv_field_settings;

        //only load once, corresponding to column names
        int count = 0;
        int countOfIndexed = 0;
        int i;
        for (i = 0; i < columnsSettings.length; i++) {
            if (columnsSettings[i].equalsIgnoreCase("2")) {
                countOfIndexed++;
            }
        }
        try {
            int indexesLoaded = 0;
            if (all_indexes.size() == 0) {
                for (i = 0; i < columns.length; i++) {
                    if (columnsSettings[i].equalsIgnoreCase("2")) {

                        String filename = TabulationSettings.index_path
                                + OTHER_IDX_PREFIX + columns[i] + OTHER_IDX_POSTFIX;

                        /* rename the species file */
                        if (indexesLoaded == countOfIndexed - 1) {
                            filename = TabulationSettings.index_path
                                    + SPECIES_IDX_FILENAME;
                        }

                        FileInputStream fis = new FileInputStream(filename);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        ObjectInputStream ois = new ObjectInputStream(bis);
                        all_indexes.add((IndexedRecord[]) ois.readObject());

                        ois.close();

                        if (all_indexes.get(all_indexes.size() - 1) != null) {
                            count += all_indexes.get(all_indexes.size() - 1).length;
                        }
                        //print something to double check columns
                        System.out.println("index:" + i + " " + columns[i] + ", " + OccurrencesFieldsUtil.columnNames[i] + " == "
                                + OccurrencesFieldsUtil.columnNames[all_indexes.get(all_indexes.size() - 1)[0].type]);

                        indexesLoaded++;
                    }
                }
            }
            SpatialLogger.log("loadIndexes done");
        } catch (Exception e) {
            SpatialLogger.log("loadIndexes", e.toString());
            e.printStackTrace();
        }

        /* put all_indexes into single_index */
        TreeMap<String, IndexedRecord> si = new TreeMap<String, IndexedRecord>();
        for (IndexedRecord[] rl : all_indexes) {
            for (IndexedRecord r : rl) {
                if (si.get(r.name) == null) {
                    si.put(r.name, r);
                }
            }
        }
        single_index = new IndexedRecord[si.size()];
        i = 0;
        for (Entry<String, IndexedRecord> es : si.entrySet()) {
            single_index[i++] = es.getValue();
        }

        java.util.Arrays.sort(
                single_index,
                new Comparator<IndexedRecord>() {

                    public int compare(IndexedRecord r1, IndexedRecord r2) {
                        return r1.name.compareTo(r2.name);
                    }
                });

        //build speciesNumberInRecordsOrder
        getPointsPairsGrid(); //load up gridded points
        IndexedRecord[] species = all_indexes.get(all_indexes.size() - 1);
        speciesNumberInRecordsOrder = new int[grid_points_idx.length];
        for (i = 0; i < grid_points_idx.length; i++) {
            speciesNumberInRecordsOrder[i] = -1;
        }
        for (i = 0; i < species.length; i++) {
            for (int j = species[i].record_start; j <= species[i].record_end; j++) {
                speciesNumberInRecordsOrder[j] = i;
            }
        }
        //error checking, likely hierarchy problems
        int countmissing = 0;
        for (i = 0; i < grid_points_idx.length; i++) {
            if (speciesNumberInRecordsOrder[i] == -1) {
                countmissing++;
            }
        }
        System.out.println("******* missing: " + countmissing);


        loadCommonNames();  //done last since dependant on indexed records
        loadCommonNamesIndex();  //done last since dependant on indexed records

        /* setup species_to_family idx */
        loadSpeciesToFamilyIdx();

        loadIdLookup();

        loadClusterRecords();

        loadSensitiveCoordinates();

        System.out.println("INDEXES LOADED");

    }

    static void loadSpeciesToFamilyIdx() {
        int i;
        File file = new File(TabulationSettings.index_path
                + SPECIES_TO_FAMILY);
        if (!file.exists()) {
            IndexedRecord[] speciesIdx = all_indexes.get(all_indexes.size() - 1);
            IndexedRecord[] familyIdx = all_indexes.get(TabulationSettings.species_list_first_column_index);
            species_to_family = new int[speciesIdx.length];
            for (i = 0; i < species_to_family.length; i++) {
                species_to_family[i] = -1;
            }
            for (i = 0; i < species_to_family.length; i++) {
                for (int j = 0; j < familyIdx.length; j++) {
                    if (speciesIdx[i].record_start <= familyIdx[j].record_end
                            && speciesIdx[i].record_start >= familyIdx[j].record_start) {
                        species_to_family[i] = j;
                        break;
                    }
                }
            }

            try {
                FileOutputStream fos = new FileOutputStream(
                        TabulationSettings.index_path
                        + SPECIES_TO_FAMILY);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(species_to_family);
                oos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                FileInputStream fis = new FileInputStream(
                        TabulationSettings.index_path
                        + SPECIES_TO_FAMILY);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis);
                species_to_family = (int[]) ois.readObject();
                ois.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * loads id lookup and conceptid vs name
     */
    static void loadIdLookup() {
        try {
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + ID_LOOKUP);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            extra_indexes = (HashMap<String, Object>[]) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + SORTED_CONCEPTID_NAMES);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            occurances_csv_field_pairs_ConceptId = (String[]) ois.readObject();
            occurances_csv_field_pairs_Name = (String[]) ois.readObject();
            occurances_csv_field_pairs_FirstFromSingleIndex = (int[]) ois.readObject();
            occurances_csv_field_pairs_ToSingleIndex = (int[]) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * list available extra_indexes
     */
    static String[] listLookups() {
        OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();
        ofu.load();

        String[] list = new String[TabulationSettings.occurances_csv_fields_lookups.length
                + ofu.twoCount];

        int pos = 0;
        int i;

        for (i = 0; i < TabulationSettings.occurances_csv_fields_lookups.length; i++) {
            list[pos++] = TabulationSettings.occurances_csv_fields_lookups[i];
        }

        for (i = 0; i < ofu.twoCount; i++) {
            list[pos++] = ofu.columnNames[ofu.twos[i]];
        }

        return list;
    }

    /**
     * return int[] of records for listLookup match
     *
     * @param lookup_idx index to lookup as int
     * @param key index value to lookup as String
     * @return records found as int [] or null for none
     */
    public static int[] lookup(int lookup_idx, String key) {
        loadIndexes();
        if (lookup_idx < extra_indexes.length) {
            return (int[]) extra_indexes[lookup_idx].get(key);
        } else {
            return lookupRegular(key);
        }
    }

    /**
     * return int[] of records for listLookup match
     *
     * @param lookupName name of index to lookup as string
     * @param key index value to lookup as String
     * @return records found as int [] or null for none
     */
    public static int[] lookup(String lookupName, String key) {
        loadIndexes();
        String[] lookups = listLookups();
        for (int i = 0; i < lookups.length; i++) {
            if (lookupName.equalsIgnoreCase(lookups[i])) {
                return (int[]) extra_indexes[i].get(key);
            }
        }

        return lookupRegular(key);
    }

    /**
     * return int [] of records for indexed match on key
     * @param key lookup value as String, e.g. species name
     * @return records found as int [] or null for none
     */
    static int[] lookupRegular(String key) {
        //check against regular index
        IndexedRecord[] ir = OccurrencesIndex.filterSpeciesRecords(key);
        if (ir != null && ir.length > 0) {
            int[] output = new int[ir[0].record_end - ir[0].record_start + 1];
            for (int i = 0; i < output.length; i++) {
                output[i] = i + ir[0].record_start;
            }
            return output;
        }

        return null;
    }

    /**
     * loads common names lookup (species records only, one match only)
     */
    static void loadCommonNames() {
        try {
            IndexedRecord[] ir = all_indexes.get(all_indexes.size() - 1);
            common_names = new String[ir.length];

            HashSet<String> unique = new HashSet<String>(all_indexes.size() * 2);
            int duplicates = 0;

            //load the common names file (csv), populate common_names as it goes
            BufferedReader br = new BufferedReader(
                    new FileReader(TabulationSettings.common_names_csv));

            String s;
            String[] sa;
            int i;
            int max_columns = 0;

            int count = 0;
            boolean csv_file = true; //otherwise tab delimited
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
                if (!csv_file || (count == 0 && sa.length == 1)) {
                    csv_file = false;
                    sa = s.split("\t");
                }

                /* handlers for the text qualifiers and ',' in the middle */
                if (sa != null && max_columns == 0) {
                    max_columns = sa.length;
                }
                if (csv_file && sa != null && sa.length > max_columns) {
                    sa = split(s);
                }

                /* test for uniqueness */
                int sz = unique.size();
                unique.add(sa[0] + sa[1].toLowerCase().trim());
                if (sz == unique.size()) {
                    duplicates++;
                    continue;
                }

                /* remove quotes and commas form terms */
                for (i = 0; i < sa.length; i++) {
                    if (sa[i].length() > 0) {
                        sa[i] = sa[i].replace("\"", "");
                        sa[i] = sa[i].replace(",", ";");
                        sa[i] = sa[i].replace("/", ";");
                        sa[i] = sa[i].replace("|", "-");
                    }
                }

                //find scientific name pos
                IndexedRecord lookfor = new IndexedRecord(sa[0].toLowerCase(), 0, 0, 0, 0, (byte) -1);

                int pos = java.util.Arrays.binarySearch(ir,
                        lookfor,
                        new Comparator<IndexedRecord>() {

                            public int compare(IndexedRecord r1, IndexedRecord r2) {
                                return r1.name.toLowerCase().compareTo(r2.name);
                            }
                        });

                if (pos >= 0 && pos < ir.length) {
                    common_names[pos] = sa[1].replace(',', ';');
                }
            }

            // report finds & set to empty string
            for (i = 0; i < common_names.length; i++) {
                if (common_names[i] != null) {
                    count++;
                } else {
                    common_names[i] = "";
                }
            }
            System.out.println("common name finds: " + count);
            System.out.println("common name duplicates: " + duplicates);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * loads common names index (common name with file_pos == single_index position)
     */
    static void loadCommonNamesIndex() {
        try {
            ArrayList<CommonNameRecord> cn = new ArrayList<CommonNameRecord>(single_index.length);

            //load the common names file (csv), populate common_names as it goes
            BufferedReader br = new BufferedReader(
                    new FileReader(TabulationSettings.common_names_csv));

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
                    sa = split(s);
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

                IndexedRecord lookfor = new IndexedRecord(sa[0].toLowerCase(), 0, 0, 0, 0, (byte) -1);

                int pos = java.util.Arrays.binarySearch(single_index,
                        lookfor,
                        new Comparator<IndexedRecord>() {

                            public int compare(IndexedRecord r1, IndexedRecord r2) {
                                return r1.name.toLowerCase().compareTo(r2.name);
                            }
                        });

                if (pos >= 0 && pos < single_index.length) {
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
            common_names_all_vs_single_index = new String[single_index.length];
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
     * gets species IndexedRecords only
     * @return species IndexedRecords as []
     */
    static public IndexedRecord[] getSpeciesIndex() {
        loadIndexes();

        return all_indexes.get(all_indexes.size() - 1);
    }

    /**
     * gets all available IndexedRecords
     * @return all IndexedRecords in one index
     */
    static public IndexedRecord[] getIndex() {
        loadIndexes();

        return single_index;
    }

    /**
     * gets index type column name
     * @return index type column name as String
     */
    static public String getIndexType(int type) {
        //return OccurrencesFieldsUtil.columnNames[type];
        return TabulationSettings.occurances_csv_twos_names[type];
    }
    static long[] sortedLineStarts = null;

    static void makeSortedLineStarts() {
        TabulationSettings.load();

        getPointsPairs();   //ensures all_points is loaded

        sortedLineStarts = new long[all_points.length];
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    TabulationSettings.index_path + SORTED_FILENAME));

            long filepos = 0;
            int idxpos = 0;
            sortedLineStarts[idxpos++] = filepos;
            String line;
            while ((line = br.readLine()) != null && idxpos < sortedLineStarts.length) {
                filepos += line.length() + 1;   //+1 for '\n' //us-ascii enforced elsewhere
                sortedLineStarts[idxpos++] = filepos;
            }
            br.close();
            System.out.println("filepos = " + filepos);
        } catch (Exception e) {
            SpatialLogger.log("getSortedRecords", e.toString());
            e.printStackTrace();
        }

        try {
            FileOutputStream fos = new FileOutputStream(
                    TabulationSettings.index_path
                    + SORTED_LINE_STARTS);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(sortedLineStarts);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void loadSortedLineStarts() {
        if (sortedLineStarts == null) {
            try {
                FileInputStream fis = new FileInputStream(
                        TabulationSettings.index_path
                        + SORTED_LINE_STARTS);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis);
                sortedLineStarts = (long[]) ois.readObject();
                ois.close();
            } catch (Exception e) {
                SpatialLogger.log("getSortedRecords", e.toString());
                e.printStackTrace();
            }
        }
    }

    /**
     * gets sorted records strings for input of sorted record indexes
     *
     * TODO: make use of class variable speciesSortByRecordNumber
     * @param records array of record indexes to return as int []
     * @return sorted records as String []
     */
    public static String[] getSortedRecords(int[] records) {
        int i;

        /* enforce sort */
        for (i = 1; i < records.length; i++) {
            if (records[i - 1] > records[i]) {
                java.util.Arrays.sort(records);
                break;
            }
        }

        String[] lines = new String[records.length];

        long start = System.currentTimeMillis();

        /* iterate through sorted records file and extract only required records */
        try {
            /*LineNumberReader br = new LineNumberReader(
            new FileReader(TabulationSettings.index_path
            + SORTED_FILENAME));

            for (i = 0; i < records.length; i++) {
            int loopcounter = records[i] - br.getLineNumber();
            while (loopcounter > 0) {
            br.readLine();
            loopcounter--;
            }
            lines[i] = br.readLine();
            }
            br.close();*/

            //BufferedReader br = new BufferedReader(new FileReader(
            //           TabulationSettings.index_path + SORTED_FILENAME));

            //long filepos = 0;
            //int idxpos = 0;

            //skip to record, update filepos, read line, increment
            //do{
            // br.skip(sortedLineStarts[records[idxpos]] - filepos);
            // filepos = sortedLineStarts[records[idxpos]];
            // lines[idxpos] = br.readLine();
            // idxpos++;
            //}while(idxpos < lines.length && idxpos < records.length
            //   && lines[idxpos-1] != null);

            //br.close();

            //max line len 100000 characters
            byte[] data = new byte[(int) (100000)];
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + SORTED_FILENAME);

            long filepos = 0;
            int idxpos = 0;

            //skip to record, update filepos, read line, increment
            do {
                fis.skip(sortedLineStarts[records[idxpos]] - filepos);
                filepos = sortedLineStarts[records[idxpos] + 1];
                int len = (int) (sortedLineStarts[records[idxpos] + 1] - sortedLineStarts[records[idxpos]]);
                fis.read(data, 0, len);
                lines[idxpos] = (new String(Arrays.copyOfRange(data, 0, len))).trim();
                idxpos++;
            } while (idxpos < lines.length && idxpos < records.length
                    && lines[idxpos - 1] != null);

            fis.close();

            System.out.println("getSortedRecords: count=" + lines.length + " in " + (System.currentTimeMillis() - start) + "ms");

            return lines;
        } catch (Exception e) {
            SpatialLogger.log("getSortedRecords", e.toString());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * gets sorted records between two file character positions
     *
     * characters returned are in: file_start <= character position < file_end
     *
     * @param file_start first character to return
     * @param file_end  one more than last character to return
     * @return each record between first and end character positions, split by new line as String[]
     */
    public static String[] getSortedRecords(long file_start, long file_end) {
        try {
            byte[] data = new byte[(int) (file_end - file_start)];
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + SORTED_FILENAME);
            fis.skip(file_start); 			//seek to start
            fis.read(data);					//read what is required
            fis.close();

            return (new String(data)).split("\n");		//convert to string
        } catch (Exception e) {
            SpatialLogger.log("getSortedRecords", e.toString());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * gets sorted records between two file character positions
     *
     * characters returned are in: file_start <= character position < file_end
     *
     * @param file_start first character to return
     * @param file_end one more than last character to return
     * @return each record between first and end character positions, as String
     */
    public static String getSortedRecordsString(long file_start, long file_end) {
        try {
            byte[] data = new byte[(int) (file_end - file_start)];
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + SORTED_FILENAME);
            fis.skip(file_start); 			//seek to start
            fis.read(data);					//read what is required
            fis.close();

            return (new String(data));		//convert to string
        } catch (Exception e) {
            SpatialLogger.log("getSortedRecords", e.toString());
            System.out.println(file_start + " to " + file_end);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * gets points corresponding to sorted records between two record positions
     *
     * points returned are in: record start <= record position <= record end
     *
     * @param file_start first character to return
     * @param file_end one more than last character to return
     * @return each record between first and end character positions, split by new line as String[]
     */
    public static double[] getPoints(int recordstart, int recordend) {
        double[] d = new double[(recordend - recordstart + 1) * 2];
        try {
            /* ready requested byte block */
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME,
                    "rw");
            int number_of_points = (recordend - recordstart + 1) * 2;
            byte[] b = new byte[(number_of_points) * 8];
            points.seek(recordstart * 2 * 8);
            points.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);
            points.close();

            /* put into double [] */
            int i;
            for (i = 0; i < number_of_points; i++) {
                d[i] = bb.getDouble();
            }

        } catch (Exception e) {
            SpatialLogger.log("getPoints(" + recordstart + "," + recordend, e.toString());
        }

        return d;
    }

    /**
     * gets all points corresponding to sorted records
     *
     * @return all points as double[n][2]
     * where
     *  n is number of points
     *  [][0] is longitude
     *  [][1] is latitude
     */
    public static double[][] getPointsPairs() {
        long start = System.currentTimeMillis();

        /* if already loaded, return existing data */
        if (all_points != null) {
            return all_points;
        }

        double[][] d = null;

        try {
            /* load all points */
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME,
                    "rw");
            int number_of_points = ((int) points.length()) / 8;
            int number_of_records = number_of_points / 2;
            byte[] b = new byte[number_of_points * 8];
            points.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);
            points.close();

            int i;

            /* read doubles into data structure*/
            d = new double[number_of_records][2];
            for (i = 0; i < number_of_records; i++) {
                d[i][0] = bb.getDouble();
                d[i][1] = bb.getDouble();
            }
        } catch (Exception e) {
            SpatialLogger.log("getPointsPairs", e.toString());
        }

        /* store for next time */
        all_points = d;

        System.out.println("getpointspairs: " + (System.currentTimeMillis() - start) + "ms");
        return d;
    }

    /**
     * for a PART
     *
     * gets all points corresponding to sorted records
     *
     * @param partNumber part to open as int
     * @return all points as double[n][2]
     * where
     *  n is number of points
     *  [][0] is longitude
     *  [][1] is latitude
     */
    public static double[][] getPointsPairs(int partNumber) {
        double[][] d = null;

        try {
            /* load all points */
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME + "_" + partNumber,
                    "rw");
            int number_of_points = ((int) points.length()) / 8;
            int number_of_records = number_of_points / 2;
            byte[] b = new byte[number_of_points * 8];
            points.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);
            points.close();

            int i;

            /* read doubles into data structure*/
            d = new double[number_of_records][2];
            for (i = 0; i < number_of_records; i++) {
                d[i][0] = bb.getDouble();
                d[i][1] = bb.getDouble();
            }
        } catch (Exception e) {
            SpatialLogger.log("getPointsPairs", e.toString());
        }

        return d;
    }

    /**
     * operates on previously OCC_POINTS.dat
     *
     * exports points in sorted on latitude then longitude
     *
     * exports as POINTS_FILENAME_GEO for points data
     * exports as POINTS_FILENAME_GEO_IDX for sorted records indexes
     */
    void exportSortedGEOPoints() {
        /* load OCC_POINTS.dat */
        double[][] points = getPointsPairs();

        /* make Points object */
        Point[] pa = new Point[points.length];
        int i;
        for (i = 0; i < points.length; i++) {
            pa[i] = new Point(points[i][0], points[i][1], i);
        }

        /* sort by latitude then longitude */
        java.util.Arrays.sort(pa,
                new Comparator<Point>() {

                    public int compare(Point r1, Point r2) {
                        double result = r1.latitude - r2.latitude;
                        if (result == 0) {
                            result = r1.longitude - r2.longitude;
                        }
                        return (int) result;
                    }
                });

        //export points in this new order
        try {
            RandomAccessFile raf = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_GEO,
                    "rw");
            byte[] b = new byte[pa.length * 8 * 2];
            ByteBuffer bb = ByteBuffer.wrap(b);
            for (Point p : pa) {
                bb.putDouble(p.longitude);
                bb.putDouble(p.latitude);
            }
            raf.write(b);
            raf.close();
        } catch (Exception e) {
            SpatialLogger.log("exportSortedGEOPoints", e.toString());
        }

        //export lookup for idx/record reference
        try {
            RandomAccessFile raf = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_GEO_IDX,
                    "rw");
            byte[] b = new byte[pa.length * 4];
            ByteBuffer bb = ByteBuffer.wrap(b);
            for (Point p : pa) {
                bb.putInt(p.idx);
            }
            raf.write(b);
            raf.close();
        } catch (Exception e) {
            SpatialLogger.log("exportSortedGEOPoints", e.toString());
        }
    }

    /**
     * gets all points corresponding to sorted records
     * using latitude then longitude sort order
     *
     * @return all points as double[n][2]
     * where
     *  n is number of points
     *  [][0] is longitude
     *  [][1] is latitude
     */
    public static double[][] getPointsPairsGEO() {
        double[][] d = null;
        try {
            /* load all */
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_GEO,
                    "rw");
            int number_of_points = ((int) points.length()) / 8;
            int number_of_records = number_of_points / 2;
            byte[] b = new byte[number_of_points * 8];
            points.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);
            points.close();

            int i;

            // read out doubles
            d = new double[number_of_records][2];
            for (i = 0; i < number_of_records; i++) {
                d[i][0] = bb.getDouble();
                d[i][1] = bb.getDouble();
            }

        } catch (Exception e) {
            SpatialLogger.log("getPointsPairsGEO", e.toString());
        }

        return d;
    }

    /**
     * gets index for all points corresponding to sorted records
     * using latitude then longitude sort order
     *
     * @return records index of latitude then longitude sorted points
     * as records index positions against this method sorted points
     */
    public static int[] getPointsPairsGEOidx() {
        int[] d = null;
        try {
            /* points */
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_GEO_IDX,
                    "r");
            int number_of_points = ((int) points.length()) / 4;
            int number_of_records = number_of_points;

            byte[] b = new byte[number_of_points * 4];

            points.read(b);

            ByteBuffer bb = ByteBuffer.wrap(b);
            int i;
            d = new int[number_of_records];
            for (i = 0; i < number_of_records; i++) {
                d[i] = bb.getInt();
            }
            points.close();

        } catch (Exception e) {
            SpatialLogger.log("getPointsPairsGEOidx", e.toString());
        }

        return d;
    }

    /**
     * operates on previously OCC_POINTS.dat
     *
     * exports points in sorted on latitude then longitude
     * of 0.5 degree grid cells
     *
     * exports as POINTS_FILENAME_GEO for points data
     * exports as POINTS_FILENAME_GEO_IDX for sorted records indexes
     */
    void exportSortedGridPoints() {
        /* load OCC_POINTS.dat */
        double[][] points = getPointsPairs();

        /* load Points object */
        Point[] pa = new Point[points.length];
        int i;
        double longitude, latitude;

        for (i = 0; i < points.length; i++) {
            longitude = points[i][0];
            latitude = points[i][1];

            //assume -360 to +360, adjust
            while (longitude < -180) {
                longitude += 360;
            }
            while (longitude >= 180) {
                longitude -= 360;
            }
            while (latitude < -180) {
                longitude += 360;
            }
            while (latitude >= 180) {
                longitude -= 360;
            }
            pa[i] = new Point(longitude, latitude, i);
        }

        /* sort on 0.5degree grid by latitude then longitude */
        java.util.Arrays.sort(pa,
                new Comparator<Point>() {

                    public int compare(Point r1, Point r2) {
                        double result = (int) (2 * r1.latitude + 360) - (int) (2 * r2.latitude + 360);
                        if (result == 0) {
                            result = (int) (2 * r1.longitude + 360) - (int) (2 * r2.longitude + 360);
                        }
                        return (int) result;
                    }
                });

        //export points in this new order
        try {
            RandomAccessFile raf = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_05GRID,
                    "rw");
            byte[] b = new byte[pa.length * 8 * 2];
            ByteBuffer bb = ByteBuffer.wrap(b);
            for (Point p : pa) {
                bb.putDouble(p.longitude);
                bb.putDouble(p.latitude);
            }
            raf.write(b);
            raf.close();
        } catch (Exception e) {
            SpatialLogger.log("exportSortedGridPoints", e.toString());
        }

        //export lookup for idx/record reference
        try {
            RandomAccessFile raf = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_05GRID_IDX,
                    "rw");
            byte[] b = new byte[pa.length * 4];
            ByteBuffer bb = ByteBuffer.wrap(b);

            /* reverse */
            int[] idx_reverse = new int[pa.length];
            for (i = 0; i < pa.length; i++) {
                idx_reverse[pa[i].idx] = i;
            }
            for (i = 0; i < idx_reverse.length; i++) {
                bb.putInt(idx_reverse[i]);
            }
            raf.write(b);

            /* forward */
            b = new byte[pa.length * 4];
            bb = ByteBuffer.wrap(b);
            for (i = 0; i < pa.length; i++) {
                bb.putInt(pa[i].idx);
            }
            raf.write(b);

            raf.close();
        } catch (Exception e) {
            SpatialLogger.log("exportSortedGridPoints", e.toString());
        }

        //export lookup for idx/record reference

        /* 720 constant is 360degrees/0.5degree grid */

        try {
            RandomAccessFile raf = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_05GRID_KEY,
                    "rw");
            byte[] b = new byte[(360 * 2 * 360 * 2) * 4];
            ByteBuffer bb = ByteBuffer.wrap(b);

            /* fill grid cells positions/key */
            int p = 0;
            int[][] list = new int[720][720];
            int j;
            for (i = 0; i < 720; i++) {
                for (j = 0; j < 720; j++) {
                    list[i][j] = -1;
                }
            }
            int lastx = 0;
            int lasty = 0;
            for (i = 0; i < pa.length; i++) {
                int x = (int) (pa[i].longitude * 2 + 360);          //longitude is -180 to 179.999...
                int y = (int) (pa[i].latitude * 2 + 360);		//latitude is -180 to 179.999...

                if (list[y][x] == -1
                        || list[y][x] > i) {
                    list[y][x] = i;
                }

                if (y < lasty || (y == lasty && x < lastx)) {
                    System.out.print("," + x + " " + y);

                }
                lastx = x;
                lasty = y;
            }

            /* populate blanks and validate order */
            int last_cell = pa.length;
            for (i = (720 - 1); i >= 0; i--) {
                for (j = (720 - 1); j >= 0; j--) {
                    if (list[i][j] == -1) {
                        list[i][j] = last_cell;
                    } else if (last_cell < list[i][j]) {
                        SpatialLogger.log("exportSortedGridPoints, order err");
                    }
                    last_cell = list[i][j];
                }
            }

            /* write */
            for (i = 0; i < 720; i++) {
                for (j = 0; j < 720; j++) {
                    bb.putInt(list[i][j]);
                }
            }

            raf.write(b);
            raf.close();
        } catch (Exception e) {
            SpatialLogger.log("exportSortedGridPoints", e.toString());
        }
    }

    /**
     * gets all points corresponding to sorted records
     * on 0.5 grid using latitude then longitude sort order
     *
     * @return all points as double[n][2]
     * where
     *  n is number of points
     *  [][0] is longitude
     *  [][1] is latitude
     */
    public static double[][] getPointsPairsGrid() {
        /* return existing load if available */
        if (grid_points != null) {
            return grid_points;
        }

        double[][] d = null;

        try {
            /* load all points */
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_05GRID,
                    "r");
            int number_of_points = ((int) points.length()) / 8;
            int number_of_records = number_of_points / 2;
            byte[] b = new byte[number_of_points * 8];
            points.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);
            points.close();

            int i;

            /* read as doubles */
            d = new double[number_of_records][2];
            for (i = 0; i < number_of_records; i++) {
                d[i][0] = bb.getDouble();
                d[i][1] = bb.getDouble();
            }

        } catch (Exception e) {
            SpatialLogger.log("getPointsPairsGrid", e.toString());
        }

        //load idx as well
        getPointsPairsGrididx();
        getPointsPairsGridKey();

        grid_points = d;
        return d;
    }

    /**
     * gets index for all points corresponding to sorted records
     * on 0.5 grid using latitude then longitude sort order
     *
     * @return records index of latitude then longitude sorted points
     * as method sorted points index against original sorted records order
     */
    public static int[] getPointsPairsGrididxRev() {
        /* make loaded */
        getPointsPairsGrididx();

        return grid_points_idx_rev;
    }

    /**
     * gets index for all points corresponding to sorted records
     * on 0.5 grid using latitude then longitude sort order
     *
     * @return records index of latitude then longitude sorted points
     * as records index positions against this method sorted points
     */
    public static int[] getPointsPairsGrididx() {
        /* return existing load if available */
        if (grid_points_idx != null) {
            return grid_points_idx;
        }

        int[] d1 = null;
        int[] d2 = null;
        int i;
        try {
            /* load all */
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_05GRID_IDX,
                    "r");
            int number_of_points = ((int) points.length()) / 4;
            int number_of_records = number_of_points;
            byte[] b = new byte[number_of_points * 4];

            /* read reverse order idx as int */
            points.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);
            d1 = new int[number_of_records / 2];
            for (i = 0; i < number_of_records / 2; i++) {
                d1[i] = bb.getInt();
            }

            /* read forward order idx as int */
            // points.read(b);
            //   bb = ByteBuffer.wrap(b);
            d2 = new int[number_of_records / 2];
            for (; i < number_of_records; i++) {
                d2[i - number_of_records / 2] = bb.getInt();
            }

            System.out.println("num records in key:" + number_of_records);

            points.close();
        } catch (Exception e) {
            SpatialLogger.log("getPointsPairsGrididx", e.toString());
        }

        grid_points_idx_rev = d1;
        grid_points_idx = d2;
        return d2;
    }

    /**
     * gets key of all points corresponding to sorted records
     * on 0.5 grid using latitude then longitude sort order
     *
     * @return records key grid of latitude then longitude sorted order
     * as method sorted points index for each cell
     */
    public static int[][] getPointsPairsGridKey() {
        /* use previously loaded if available */
        if (grid_key != null) {
            return grid_key;
        }

        int[][] d = null;
        int i;

        try {
            /* load all */
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_05GRID_KEY,
                    "r");
            int number_of_points = ((int) points.length()) / 4;
            byte[] b = new byte[number_of_points * 4];
            points.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);

            /* read as int into grid */
            d = new int[720][720];
            int j;
            for (i = 0; i < 720; i++) {
                for (j = 0; j < 720; j++) {
                    d[i][j] = bb.getInt();
                }
            }
            points.close();

        } catch (Exception e) {
            SpatialLogger.log("getPointsPairsGridKey", e.toString());
        }

        grid_key = d;
        return d;
    }

    /**
     * test for one record to determin if inside of one SimpleRegion
     *
     * @param record sorted record index number for testing as int
     * @param r region as SimpleRegion
     * @return true iff record is in region
     */
    static public boolean inRegion(int record, SimpleRegion r) {
        ArrayList<Integer> ali = (ArrayList<Integer>) r.getAttribute("species_records");
        if (ali != null) {
            int pos = java.util.Collections.binarySearch(ali, new Integer(record));
            if (pos >= 0 && pos < ali.size()) {
                return true;
            } else {
                return false;
            }
        } else {
            /* init */
            getPointsPairsGrid();
            getPointsPairsGrididx();

            /* transate off grid */
            int i = grid_points_idx_rev[record];

            /* test */
            return r.isWithin(grid_points[i][0], grid_points[i][1]);
        }
    }

    /**
     * return all records indexes for matches within a SimpleRegion
     *
     * TODO: only test GI_PARTIALLY...
     *
     * @param r region for test as SimpleRegion
     * @return records indexes as int []
     */
    static public int[] getRecordsInside(SimpleRegion r) {
        ArrayList<Integer> ali = (ArrayList<Integer>) r.getAttribute("species_records");
        if (ali != null) {
            int[] output = new int[ali.size()];
            int p = 0;
            for (int k = 0; k < ali.size(); k++, p++) {
                if (ali.get(k) >= 0) {
                    output[p] = ali.get(k);
                }
            }
            output = java.util.Arrays.copyOf(output, p);
            return output;
        }

        /* init */
        getPointsPairsGridKey();
        getPointsPairsGrid();
        getPointsPairsGrididx();

        long starttime = System.currentTimeMillis();

        /* make overlay grid from this region */
        byte[][] mask = new byte[720][720];
        int[][] cells = r.getOverlapGridCells(-180, -180, 180, 180, 720, 720, mask);

        System.out.println("poly:" + r.toString());
        int i, j;

        /* for matching cells, test each record within  */

        Vector<Integer> records = new Vector<Integer>();

        for (i = 0; i < cells.length; i++) {
            int start = grid_key[cells[i][1]][cells[i][0]];
            int end = start;


            if (cells[i][0] < (720 - 1)) {
                // not last record on a grid_key row, use limit on next line
                end = grid_key[cells[i][1]][cells[i][0] + 1];
            } else if (cells[i][1] < (720 - 1)) {
                // must be last record on a grid key row,
                // also not on last row, use next row, first cell grid key
                end = grid_key[cells[i][1] + 1][0];
            } else {
                // must be at end of grid_key file, use all
                end = grid_points.length;
            }

            //test each potential match
            if (mask[cells[i][0]][cells[i][1]] == SimpleRegion.GI_FULLY_PRESENT) {
                for (j = start; j < end; j++) {
                    records.add(new Integer(grid_points_idx[j]));
                }
            } else {
                for (j = start; j < end; j++) {
                    if (r.isWithin(grid_points[j][0], grid_points[j][1])) {
                        records.add(new Integer(grid_points_idx[j]));
                    }
                }
            }
        }

        /* format matches as int [] */
        if (records.size() > 0) {
            int[] data = new int[records.size()];
            Iterator<Integer> li = records.listIterator();
            i = 0;
            while (li.hasNext()) {
                data[i++] = li.next();
            }

            long endtime = System.currentTimeMillis();
            System.out.println("getRecordsInside(): len=" + data.length + " time=" + (endtime - starttime) + "ms");
            return data;
        }
        return null;
    }

    /**
     * return set of Species within a SimpleRegion
     *
     * @param r region for test as SimpleRegion
     * @return list of species delimited by "," as String
     */
    static public String getSpeciesInside(SimpleRegion r) {
        long t1 = System.currentTimeMillis();

        /* init */
        loadIndexes();

        int i, j;

        /* for matching cells, test each record within  */

        IndexedRecord[] species = all_indexes.get(all_indexes.size() - 1);
        IndexedRecord[] familyIdx = all_indexes.get(TabulationSettings.species_list_first_column_index);

        BitSet bitset = new BitSet(OccurrencesIndex.getSpeciesIndex().length + 1);

        long t2 = System.currentTimeMillis();

        ArrayList<Integer> ali = (ArrayList<Integer>) r.getAttribute("species_records");
        if (ali != null) {
            for (int k = 0; k < ali.size(); k++) {
                if (speciesNumberInRecordsOrder[ali.get(k)] >= 0) {
                    bitset.set(speciesNumberInRecordsOrder[ali.get(k)]);
                }
            }
        } else {
            /* make overlay grid from this region */
            byte[][] mask = new byte[720][720];
            int[][] cells = r.getOverlapGridCells(-180, -180, 180, 180, 720, 720, mask);

            for (i = 0; i < cells.length; i++) {
                int start = grid_key[cells[i][1]][cells[i][0]];
                int end = start;

                if (cells[i][0] < (720 - 1)) {
                    // not last record on a grid_key row, use limit on next line
                    end = grid_key[cells[i][1]][cells[i][0] + 1];
                } else if (cells[i][1] < (720 - 1)) {
                    // must be last record on a grid key row,
                    // also not on last row, use next row, first cell grid key
                    end = grid_key[cells[i][1] + 1][0];
                } else {
                    // must be at end of grid_key file, use all
                    end = grid_points.length;
                }

                //test each potential match, otherwise add
                if (mask[cells[i][0]][cells[i][1]] == SimpleRegion.GI_FULLY_PRESENT) {
                    for (j = start; j < end; j++) {
                        if (speciesNumberInRecordsOrder[grid_points_idx[j]] >= 0) {   //TODO: remove this validation, should not be required
                            bitset.set(speciesNumberInRecordsOrder[grid_points_idx[j]]);
                        }
                    }
                } else if (r.getType() == SimpleRegion.BOUNDING_BOX || mask[cells[i][1]][cells[i][0]] == SimpleRegion.GI_PARTIALLY_PRESENT) {
                    for (j = start; j < end; j++) {
                        if (r.isWithin(grid_points[j][0], grid_points[j][1])) {
                            if (speciesNumberInRecordsOrder[grid_points_idx[j]] >= 0) {   //TODO: remove this validation, should not be required
                                bitset.set(speciesNumberInRecordsOrder[grid_points_idx[j]]);
                            }
                        }
                    }
                }
            }
        }

        long t3 = System.currentTimeMillis();

        String output = getSpeciesListRecords(bitset);

        long t4 = System.currentTimeMillis();

        System.out.println("getSpeciesInside(): t2-t1=" + (t2 - t1) + " t3-t2=" + (t3 - t2) + " t4-t3=" + (t4 - t3));

        return output;
    }

    static String getIndexedValue(int index, int record) {
        IndexedRecord[] irs = all_indexes.get(index);

        IndexedRecord ir = new IndexedRecord("", 0, 0, record, 0, (byte) 0);

        int pos = java.util.Arrays.binarySearch(irs, ir,
                new Comparator<IndexedRecord>() {

                    public int compare(IndexedRecord r1, IndexedRecord r2) {
                        return r1.record_start - r2.record_start;
                    }
                });

        if (pos >= 0 && pos < irs.length
                && irs[pos].record_end >= record) {
            return irs[pos].name;
        }

        return "";
    }

    public static void main(String[] args) {
        TabulationSettings.load();
        OccurrencesIndex oi = new OccurrencesIndex();
        oi.exportFieldIndexes();

        //SimpleRegion sr = SimpleShapeFile.parseWKT("POLYGON((116.0 -44.0,116.0 -9.0,117.0 -9.0,117.0 -44.0,116.0 -44.0))");
        //getSpeciesInside(sr);

        //makeSortedLineStarts();

        //loadClusterRecords();

        //OccurrencesIndex oi = new OccurrencesIndex();
        //oi.exportFieldIndexes();
        //       oi.occurancesUpdate();

        //int[] list = OccurrencesIndex.lookup(0, "143");
        //System.out.println("list:" + list);
        //System.out.println("list len:" + list.length);
        //System.out.println("done");
    }

    /**
     * return number of Species within a SimpleRegion
     *
     * @param r region for test as SimpleRegion
     * @return number of species found as int[0], number of occurrences as int[1]
     */
    static public int[] getSpeciesCountInside(SimpleRegion r) {

        /* init */
        loadIndexes();

        int i, j;

        /* for matching cells, test each record within  */
        BitSet bitset = new BitSet(OccurrencesIndex.getSpeciesIndex().length + 1);

        int countOccurrences = 0;
        int errorCount = 0;

        ArrayList<Integer> ali = (ArrayList<Integer>) r.getAttribute("species_records");
        if (ali != null) {
            for (int k = 0; k < ali.size(); k++) {
                if (speciesNumberInRecordsOrder[ali.get(k)] >= 0) {
                    bitset.set(speciesNumberInRecordsOrder[ali.get(k)]);
                }
                countOccurrences++;
            }
        } else {
            /* make overlay grid from this region */
            byte[][] mask = new byte[720][720];
            int[][] cells = r.getOverlapGridCells(-180, -180, 180, 180, 720, 720, mask);

            for (i = 0; i < cells.length; i++) {
                int start = grid_key[cells[i][1]][cells[i][0]];
                int end = start;

                if (cells[i][0] < (720 - 1)) {
                    // not last record on a grid_key row, use limit on next line
                    end = grid_key[cells[i][1]][cells[i][0] + 1];
                } else if (cells[i][1] < (720 - 1)) {
                    // must be last record on a grid key row,
                    // also not on last row, use next row, first cell grid key
                    end = grid_key[cells[i][1] + 1][0];
                } else {
                    // must be at end of grid_key file, use all
                    end = grid_points.length;
                }

                //test each potential match, otherwise add
                if (mask[cells[i][1]][cells[i][0]] == SimpleRegion.GI_FULLY_PRESENT) {
                    for (j = start; j < end; j++) {
                        if (speciesNumberInRecordsOrder[grid_points_idx[j]] >= 0) {   //TODO: remove this validation, should not be required
                            bitset.set(speciesNumberInRecordsOrder[grid_points_idx[j]]);
                            countOccurrences++;
                        }
                        // }else{
                        //     errorCount++;
                        // }
                    }
                } else if (r.getType() == SimpleRegion.BOUNDING_BOX || mask[cells[i][1]][cells[i][0]] == SimpleRegion.GI_PARTIALLY_PRESENT) {
                    //TODO: actually make a mask for bounding boxes
                    for (j = start; j < end; j++) {
                        if (r.isWithin(grid_points[j][0], grid_points[j][1])) {
                            if (speciesNumberInRecordsOrder[grid_points_idx[j]] >= 0) {   //TODO: remove this validation, should not be required
                                bitset.set(speciesNumberInRecordsOrder[grid_points_idx[j]]);
                                countOccurrences++;
                            }
                            //   }else{
                            //       errorCount++;
                            //   }
                        }
                    }
                }
            }
        }

        int speciesCount = 0;
        int len = bitset.length();
        for (i = 0; i < len; i++) {
            if (bitset.get(i)) {
                speciesCount++;
            }
        }

        System.out.println("errorCount=" + errorCount);
        int[] ret = new int[2];
        ret[0] = speciesCount;
        ret[1] = countOccurrences;
        return ret;
    }
    static IndexedRecord[] speciesSortByRecordNumber = null;
    static int[] speciesSortByRecordNumberOrder = null;

    static void makeSpeciesSortByRecordNumber() {
        if (speciesSortByRecordNumber == null) {
            loadIndexes();

            speciesSortByRecordNumber = all_indexes.get(all_indexes.size() - 1).clone();

            //preserve original order in a secondary list, to be returned
            long[] tmp = new long[speciesSortByRecordNumber.length];
            int i;
            for (i = 0; i < speciesSortByRecordNumber.length; i++) {
                tmp[i] = speciesSortByRecordNumber[i].file_end;
                speciesSortByRecordNumber[i].file_end = i;
            }

            java.util.Arrays.sort(speciesSortByRecordNumber,
                    new Comparator<IndexedRecord>() {

                        public int compare(IndexedRecord r1, IndexedRecord r2) {
                            return r1.record_start - r2.record_start;
                        }
                    });

            //return value to borrowed variable
            long t;
            speciesSortByRecordNumberOrder = new int[speciesSortByRecordNumber.length];
            for (i = 0; i < speciesSortByRecordNumber.length; i++) {
                t = speciesSortByRecordNumber[i].file_end;
                speciesSortByRecordNumber[i].file_end = tmp[(int) t];

                speciesSortByRecordNumberOrder[i] = (int) t;
            }
        }
    }

    static public BitSet getSpeciesBitset(ArrayList<Integer> records, SimpleRegion region, Integer occurrencesCount) {
        makeSpeciesSortByRecordNumber();

        int i;
        int spos = 0;
        BitSet species = new BitSet(OccurrencesIndex.getSpeciesIndex().length + 1);

        int ocount = 0;

        //assume records sorted
        for (i = 1; i < records.size(); i++) {
            if (records.get(i - 1).intValue() > records.get(i).intValue()) {
                System.out.println("records not sorted");
                java.util.Collections.sort(records);
                break;
            }
        }

        if (region == null) {
            //no region, use all
            spos = 0;
            i = 0;
            while (i < records.size()) {
                //TODO: work out when best to use binary searching instead of seeking
                int r = records.get(i);

                while (spos < speciesSortByRecordNumber.length
                        && r > speciesSortByRecordNumber[spos].record_end) {   //seek to next species
                    spos++;
                }


                if (spos == speciesSortByRecordNumber.length) {
                    spos--; //TODO: is this correct?
                } else {
                }

                species.set(speciesSortByRecordNumberOrder[spos]);
                ocount++;

                spos++;
                i++;
                if (spos < speciesSortByRecordNumber.length) {
                    while (i < records.size()
                            && records.get(i) < speciesSortByRecordNumber[spos].record_start) {
                        i++;
                    }
                } else {
                    break;
                }
            }
        } else {
            //TODO: better handling when species_records attribute is present
            // in region
            {
                /* not used */
                spos = 0;
                i = 0;
                while (i < records.size()) {
                    //TODO: work out when best to use binary searching instead of seeking
                    int r = records.get(i);
                    while (spos < speciesSortByRecordNumber.length
                            && r > speciesSortByRecordNumber[spos].record_end) {   //seek to next species+1
                        spos++;
                    }
                    i++; //next record
                    if (spos < speciesSortByRecordNumber.length
                            && OccurrencesIndex.inRegion(spos, region)) {

                        species.set(speciesSortByRecordNumberOrder[spos]);
                        ocount++;

                        spos++; //inc

                        //seek to next
                        if (spos < speciesSortByRecordNumber.length) {
                            while (i < records.size()
                                    && records.get(i) < speciesSortByRecordNumber[spos].record_start) {
                                i++;
                            }
                        }
                    }
                }
            }
        }

        if (occurrencesCount != null) {
            occurrencesCount = ocount;
        }

        return species;

    }

    static public String getSpeciesListRecords(BitSet bitset) {
        IndexedRecord[] species = all_indexes.get(all_indexes.size() - 1);
        IndexedRecord[] familyIdx = all_indexes.get(TabulationSettings.species_list_first_column_index);

        StringBuffer sb = new StringBuffer();

        IndexedRecord lookfor = new IndexedRecord("", 0, 0, 0, 0, (byte) -1);

        for (int i = 0; i < bitset.size(); i++) {
            if (bitset.get(i)) {
                //append family
                if (species_to_family[i] >= 0) {
                    //sb.append(StringUtils.capitalize(
                    //      familyIdx[species_to_family[i]].name));
                    lookfor.name = familyIdx[species_to_family[i]].name;


                    int j = java.util.Arrays.binarySearch(single_index,
                            lookfor,
                            new Comparator<IndexedRecord>() {

                                public int compare(IndexedRecord r1, IndexedRecord r2) {
                                    return r1.name.compareTo(r2.name);
                                }
                            });

                    if (j >= 0 && j < occurances_csv_field_pairs_FirstFromSingleIndex.length) {
                        j = occurances_csv_field_pairs_FirstFromSingleIndex[j];

                        if (j >= 0 && j < occurances_csv_field_pairs_Name.length) {
                            sb.append(StringUtils.capitalize(occurances_csv_field_pairs_Name[j]));
                        } else {
                            sb.append("undefined");
                        }
                    } else {
                        sb.append("undefined");
                    }
                } else {
                    sb.append("undefined");
                }

                //append delimeter
                sb.append("*");


                //append scientific name
                lookfor.name = species[i].name;

                int j = java.util.Arrays.binarySearch(single_index,
                        lookfor,
                        new Comparator<IndexedRecord>() {

                            public int compare(IndexedRecord r1, IndexedRecord r2) {
                                return r1.name.compareTo(r2.name);
                            }
                        });

                if (j >= 0 && j < occurances_csv_field_pairs_FirstFromSingleIndex.length) {
                    int k = occurances_csv_field_pairs_FirstFromSingleIndex[j];

                    if (k >= 0 && k < occurances_csv_field_pairs_Name.length) {
                        sb.append(StringUtils.capitalize(occurances_csv_field_pairs_Name[k]));

                        sb.append("*"); //delimeter

                        //append common names if present
                        sb.append(common_names_all_vs_single_index[j]);

                        sb.append("*"); //delimeter

                        //taxon rank
                        sb.append(getIndexType(single_index[j].type));
                    } else {
                        sb.append("undefined");
                    }
                } else {
                    sb.append("undefined");
                }

                sb.append("|"); //next record delimeter
            }
        }

        return sb.toString();
    }
    /*static Record[] cluster_records = null;
    static void loadClusterRecords(){
    if(cluster_records != null){
    return;
    }
    loadIndexes();
    getPointsPairs();

    //check for existing file, load if it exists
    try{
    File f = new File(TabulationSettings.index_path
    + "CLUSTER_RECORDS.dat");
    if(f.exists()){
    FileInputStream fis = new FileInputStream(
    TabulationSettings.index_path
    + "CLUSTER_RECORDS.dat");
    BufferedInputStream bis = new BufferedInputStream(fis);
    ObjectInputStream ois = new ObjectInputStream(bis);
    cluster_records = (Record[]) ois.readObject();
    ois.close();
    }
    }catch(Exception e){
    e.printStackTrace();
    }

    int sciname_pos = -1;
    int prec_pos = -1;
    for(int i=0;i<TabulationSettings.geojson_property_names.length;i++){
    if (TabulationSettings.geojson_property_names[i].equalsIgnoreCase("s")) {
    sciname_pos = i;
    }
    if (TabulationSettings.geojson_property_names[i].equalsIgnoreCase("u")) {
    prec_pos = i;
    }
    }

    cluster_records = new Record[all_points.length];


    System.out.println("Cluster records not built, building now");

    try {
    BufferedReader br = new BufferedReader(
    new FileReader(
    TabulationSettings.index_path
    + SORTED_FILENAME));
    String s;
    String[] sa;

    int recordpos = 0;

    while ((s = br.readLine()) != null) {
    if(recordpos % 100000 == 0){
    System.out.println("progress: " + recordpos);
    }
    sa = s.split(",");

    if (sa[TabulationSettings.geojson_id] != null) {
    if (!sa[TabulationSettings.geojson_id].toLowerCase().equals("null")) {
    cluster_records[recordpos] =
    new Record(sa[TabulationSettings.geojson_id],
    sa[TabulationSettings.geojson_property_fields[sciname_pos]],
    Double.parseDouble(sa[TabulationSettings.geojson_longitude]),
    Double.parseDouble(sa[TabulationSettings.geojson_latitude]),
    sa[TabulationSettings.geojson_property_fields[prec_pos]]);

    }else {
    cluster_records[recordpos] = null;
    }
    }

    recordpos++;
    }

    br.close();
    } catch (Exception e) {
    SpatialLogger.log("cluster records, build", e.toString());
    e.printStackTrace();
    }

    //export cluster records
    try{
    FileOutputStream fos = new FileOutputStream(
    TabulationSettings.index_path
    + "CLUSTER_RECORDS.dat");
    BufferedOutputStream bos = new BufferedOutputStream(fos);
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(cluster_records);
    oos.close();
    }catch(Exception e){
    e.printStackTrace();
    }
    }
    
    public static Vector sampleSpeciesForClustering(String species, SimpleRegion region, int max_records){
    Vector records = new Vector();

    if(species != null){
    IndexedRecord[] ir = OccurrencesIndex.filterSpeciesRecords(species);
    if(ir == null || ir.length == 0){
    return null;
    }
    int count = ir[0].record_end - ir[0].record_start + 1;
    if(max_records < count) count = max_records;
    int end = count + ir[0].record_start;
    if(region == null){
    for(int i=ir[0].record_start;i<=end;i++){
    records.add(cluster_records[i]);
    }
    }else{
    for(int i=ir[0].record_start;i<=end;i++){
    if(region.isWithin(all_points[i][0],all_points[i][1])){
    records.add(cluster_records[i]);
    }
    }
    }
    }else if(region != null) {
    int [] r = getRecordsInside(region);

    int count = r.length;
    if(max_records < count) count = max_records;

    for(int i=0;i<count;i++){
    records.add(cluster_records[r[i]]);
    }
    }

    return records;
    }*/
    static String[][] cluster_records = null;

    static void loadClusterRecords() {
        if (cluster_records != null) {
            return;
        }
        loadIndexes();
        getPointsPairs();

        //check for existing file, load if it exists
        try {
            /*File f = new File(TabulationSettings.index_path
            + "CLUSTER_RECORDS.dat");
            if (f.exists()) {
            FileInputStream fis = new FileInputStream(
            TabulationSettings.index_path
            + "CLUSTER_RECORDS.dat");
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            cluster_records = (String[][]) ois.readObject();
            ois.close();
            return;
            }*/

            File f = new File(TabulationSettings.index_path
                    + "CLUSTER_RECORDS.csv");
            if (f.exists()) {
                long start = System.currentTimeMillis();
                //read in csv to then export dat
                cluster_records = new String[all_points.length][2];
                BufferedReader br = new BufferedReader(
                        new FileReader(
                        TabulationSettings.index_path
                        + "CLUSTER_RECORDS.csv"));
                String s;
                int pos = 0;
                while ((s = br.readLine()) != null) {
                    cluster_records[pos][0] = s;
                    cluster_records[pos][1] = br.readLine();
                    pos++;
                }
                System.out.println("cluster records load: " + (System.currentTimeMillis() - start) + "ms");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        int sciname_pos = -1;
        int prec_pos = -1;
        for (int i = 0; i < TabulationSettings.geojson_property_names.length; i++) {
            if (TabulationSettings.geojson_property_names[i].equalsIgnoreCase("s")) {
                sciname_pos = i;
            }
            if (TabulationSettings.geojson_property_names[i].equalsIgnoreCase("u")) {
                prec_pos = i;
            }
        }

        cluster_records = new String[all_points.length][2];

        System.out.println("Cluster records not built, building now");

        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(
                    TabulationSettings.index_path
                    + SORTED_FILENAME));
            String s;
            String[] sa;

            int recordpos = 0;

            FileWriter fw = new FileWriter(TabulationSettings.index_path
                    + "CLUSTER_RECORDS.csv");

            while ((s = br.readLine()) != null) {
                if (recordpos % 100000 == 0) {
                    System.out.println("progress: " + recordpos);
                    if (recordpos % 2000000 == 0) {
                        System.gc();
                    }
                }
                sa = s.split(",");

                if (sa[TabulationSettings.geojson_id] != null) {
                    if (!sa[TabulationSettings.geojson_id].toLowerCase().equals("null")) {
                        fw.append(sa[TabulationSettings.geojson_id]);
                        fw.append("\n");
                        if (!(sa[TabulationSettings.geojson_property_fields[prec_pos]] == null)) {
                            fw.append(sa[TabulationSettings.geojson_property_fields[prec_pos]]);
                        }
                        fw.append("\n");

                        //cluster_records[recordpos][0] = sa[TabulationSettings.geojson_property_fields[sciname_pos]];
                        //cluster_records[recordpos][1] = sa[TabulationSettings.geojson_property_fields[prec_pos]];
                    } else {
                        //cluster_records[recordpos][0] = null;
                    }
                }

                recordpos++;
            }

            fw.close();
            br.close();
            /*
            //read in csv to then export dat
            cluster_records = new String[all_points.length][2];
            br = new BufferedReader(
            new FileReader(
            TabulationSettings.index_path
            + "CLUSTER_RECORDS.csv"));
            int pos = 0;
            while ((s = br.readLine()) != null) {
            cluster_records[pos][0] = s;
            cluster_records[pos][1] = br.readLine();
            pos++;
            }
            try {
            FileOutputStream fos = new FileOutputStream(
            TabulationSettings.index_path
            + "CLUSTER_RECORDS.dat");
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(cluster_records);
            oos.close();
            } catch (Exception e) {
            e.printStackTrace();
            }*/
        } catch (Exception e) {
            SpatialLogger.log("cluster records, build", e.toString());
            e.printStackTrace();
        }

    }

    /**
     * return Vector of Record
     *
     * @param species lsid as String, or null
     * @param region SimpleRegion, or null
     * @param max_records limit on records to find
     * @return
     */
    public static Vector sampleSpeciesForClustering(String species, SimpleRegion region1, SimpleRegion region2, ArrayList<Integer> rec, int max_records) {
        Vector records = new Vector();

        int rec_pos = 0;

        if (species != null) {
            IndexedRecord[] ir = OccurrencesIndex.filterSpeciesRecords(species);
            if (ir == null || ir.length == 0) {
                return records;
            }
            int count = ir[0].record_end - ir[0].record_start + 1;
            if (max_records < count) {
                count = max_records;
            }
            int end = count + ir[0].record_start;
            for (int i = ir[0].record_start; i <= end; i++) {
                if ((region1 == null || (region1.isWithin(all_points[i][0], all_points[i][1])))
                        && (region2 == null || (region2.isWithin(all_points[i][0], all_points[i][1])))
                        && speciesNumberInRecordsOrder[i] >= 0) {

                    //rec test
                    if (rec != null) {
                        //inc rec
                        while (rec_pos < rec.size() && rec.get(rec_pos) < i) {
                            rec_pos++;
                        }
                        if (rec.get(rec_pos) > i) {
                            continue;   //skip
                        }
                    }

                    records.add(
                            new Record(
                            cluster_records[i][0],
                            occurances_csv_field_pairs_Name[occurances_csv_field_pairs_FirstFromSingleIndex[speciesNumberInRecordsOrder[i]]],
                            all_points[i][0],
                            all_points[i][1],
                            cluster_records[i][1]));
                }
            }
        } else if (rec != null) {
            for (int p = 0; p < rec.size(); p++) {
                int i = rec.get(p);
                if ((region1 == null || (region1.isWithin(all_points[i][0], all_points[i][1])))
                        && (region2 == null || (region2.isWithin(all_points[i][0], all_points[i][1])))
                        && speciesNumberInRecordsOrder[i] >= 0) {
                    records.add(
                            new Record(
                            cluster_records[i][0],
                            occurances_csv_field_pairs_Name[occurances_csv_field_pairs_FirstFromSingleIndex[speciesNumberInRecordsOrder[i]]],
                            all_points[i][0],
                            all_points[i][1],
                            cluster_records[i][1]));
                }
            }
        } else if (region1 != null) {
            int[] r = getRecordsInside(region1);
            if (r != null) {
                int count = r.length;
                if (max_records < count) {
                    count = max_records;
                }

                for (int j = 0; j < count; j++) {
                    int i = r[j];
                    if ((region2 == null || (region2.isWithin(all_points[i][0], all_points[i][1])))
                            && speciesNumberInRecordsOrder[i] >= 0) {
                        records.add(
                                new Record(
                                cluster_records[i][0],
                                occurances_csv_field_pairs_Name[occurances_csv_field_pairs_FirstFromSingleIndex[speciesNumberInRecordsOrder[i]]],
                                all_points[i][0],
                                all_points[i][1],
                                cluster_records[i][1]));
                    }
                }
            }
        }
        return records;
    }

    /**
     * gets parts/positions in occurrences file for procesing
     *
     * String[0] is part number.
     *  implies a seek to Long.parseLong(String[0])*PART_SIZE_MAX
     * String[1] is the starting string
     *  Unless String[0] == "0";
     *  After seek, read lines.  The first line == String[1] is the start.
     * String[2] is the ending string
     *  when reading, terminate when at end of file or 'line read' == String[2]
     * @return
     */
    LinkedBlockingQueue<String[]> getPartsPositions() {
        LinkedBlockingQueue<String[]> lbq = new LinkedBlockingQueue<String[]>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(TabulationSettings.occurances_csv));
            long part = 0;

            //determine end
            long n;
            String[] next;
            String line = null;
            while ((n = br.skip(PART_SIZE_MAX)) > 0 || part == 0) {
                System.out.println("skipped into part: " + part);

                //apply limit for dev
                if (part * PART_SIZE_MAX > TabulationSettings.occurances_csv_max_records && TabulationSettings.occurances_csv_max_records > 0) {
                    break;
                }

                next = new String[3];
                next[0] = String.valueOf(part);
                part++;

                next[1] = line; //previous part's end line

                br.mark(2000000);//max size of 2 lines

                line = br.readLine();  //skip this line, might be partial

                //check if this is a continuation line
                while (line != null && line.length() > 0 && line.charAt(line.length() - 1) == '\\') {
                    String spart = br.readLine();
                    if (spart == null) {  //same as whole line is null
                        break;
                    } else {
                        line.replace('\\', ' ');   //new line is same as 'space'
                        line += spart;
                    }
                }//repeat as necessary

                line = br.readLine(); //this is the end line
                while (line != null && line.length() > 0 && line.charAt(line.length() - 1) == '\\') {
                    String spart = br.readLine();
                    if (spart == null) {  //same as whole line is null
                        break;
                    } else {
                        line.replace('\\', ' ');   //new line is same as 'space'
                        line += spart;
                    }
                }//repeat as necessary

                br.reset();
                next[2] = line; //record end line

                lbq.put(next);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lbq;
    }
    int[] sensitive_column_positions;

    /**
     * read header of occurrences file and setup for column positions
     */
    void initColumnPositions() {
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(TabulationSettings.occurances_csv));

            String s = br.readLine();

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

            String[] sa = s.split(",");

            /* remove quotes and commas form terms */
            for (int i = 0; i < sa.length; i++) {
                if (sa[i].length() > 0) {
                    sa[i] = sa[i].replace("\"", "");
                    sa[i] = sa[i].replace(",", " ");
                }
            }

            getColumnPositions(sa);

            sensitive_column_positions = getSensitiveColumnPositions(sa);

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void makePartsThreaded() {
        SpatialLogger.log("MAKE PARTS THREADED: START");

        LinkedBlockingQueue<String[]> lbq = getPartsPositions();
        CountDownLatch cdl = new CountDownLatch(lbq.size());

        initColumnPositions();

        MakeOccurrenceParts[] threads = new MakeOccurrenceParts[TabulationSettings.analysis_threads];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new MakeOccurrenceParts(lbq, cdl, column_positions, sensitive_column_positions);
            threads[i].start();
        }
        try {
            cdl.await();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].interrupt();
            } catch (Exception e) {
            }
        }

        SpatialLogger.log("MAKE PARTS THREADED: FINISHED");
    }

    static private Vector<String[]> getVSensitiveCoordinates(CountDownLatch cdl) {
        Vector<String[]> vsensitive_coordinates = new Vector<String[]>(1000000);

        SpatialLogger.log("SensitiveCoordinates: start");
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    TabulationSettings.index_path
                    + SENSITIVE_COORDINATES + ".csv"));
            String s;
            String[] sa;

            while ((s = br.readLine()) != null) {
                //id, longitude, latitude
                vsensitive_coordinates.add(s.split(","));
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SpatialLogger.log("SensitiveCoordinates: got points len=" + vsensitive_coordinates.size());

        if (cdl != null) {
            cdl.countDown();
        }

        return vsensitive_coordinates;
    }

    static private void makeSensitiveCoordinates(Vector<String[]> vsensitive_coordinates) {
        //read id, sen long, sen lat
        if (vsensitive_coordinates == null) {
            vsensitive_coordinates = getVSensitiveCoordinates(null);
        }

        java.util.Collections.sort(vsensitive_coordinates, new Comparator<String[]>() {

            public int compare(String[] c1, String[] c2) {
                return c1[0].compareTo(c2[0]);
            }
        });
        SpatialLogger.log("SensitiveCoordinates: sorted");

        int recordCount = getPointsPairs().length;   //for number of records
        sensitiveCoordinates = new double[recordCount][2]; //[][0] long, [][1] lat

        //set to -1
        for (int i = 0; i < sensitiveCoordinates.length; i++) {
            sensitiveCoordinates[i][0] = -1;
            sensitiveCoordinates[i][1] = -1;
        }

        //read in OCC_SORTED.csv.  order sensitive_coordinates by id appearance
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(
                    TabulationSettings.index_path
                    + SORTED_FILENAME));
            String s;
            String[] sa;
            int recordpos = 0;

            int idColumn = (new OccurrencesFieldsUtil()).onetwoCount;

            String[] search_for = new String[3];

            int pos;

            while ((s = br.readLine()) != null) {
                sa = s.split(",");

                search_for[0] = sa[idColumn];
                pos = java.util.Collections.binarySearch(vsensitive_coordinates,
                        search_for,
                        new Comparator<String[]>() {

                            public int compare(String[] c1, String[] c2) {
                                return c1[0].compareTo(c2[0]);
                            }
                        });
                if (pos >= 0 && pos < vsensitive_coordinates.size()) {
                    try {
                        sensitiveCoordinates[recordpos][0] = Double.parseDouble(
                                vsensitive_coordinates.get(pos)[0]);
                        sensitiveCoordinates[recordpos][1] = Double.parseDouble(
                                vsensitive_coordinates.get(pos)[1]);
                    } catch (Exception e) {
                    }
                }

                recordpos++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        SpatialLogger.log("SensitiveCoordinates: matched with id vs recordpos");

        //output
        try {
            FileOutputStream fos = new FileOutputStream(
                    TabulationSettings.index_path
                    + SENSITIVE_COORDINATES);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(sensitiveCoordinates);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SpatialLogger.log("SensitiveCoordinates: exported & done");
    }

    static void loadSensitiveCoordinates() {
        long start = System.currentTimeMillis();
        //input
        try {
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + SENSITIVE_COORDINATES);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            sensitiveCoordinates = (double[][]) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("loadSensitiveCoordinates: " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * for Sensitive Coordinates
     *
     * gets points corresponding to sorted records between two record positions
     *
     * points returned are in: record start <= record position <= record end
     *
     * @param file_start first character to return
     * @param file_end one more than last character to return
     * @return each record between first and end character positions, split by new line as String[]
     */
    public static double[] getPointsSensitive(int recordstart, int recordend) {
        double[] d = new double[(recordend - recordstart + 1) * 2];
        try {
            /* ready requested byte block */
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME,
                    "rw");
            int number_of_points = (recordend - recordstart + 1) * 2;
            byte[] b = new byte[(number_of_points) * 8];
            points.seek(recordstart * 2 * 8);
            points.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);
            points.close();

            /* put into double [] */
            int i;
            for (i = 0; i < number_of_points; i++) {
                d[i] = bb.getDouble();
            }
        } catch (Exception e) {
            SpatialLogger.log("getPoints(" + recordstart + "," + recordend, e.toString());
        }

        double[] dsensitive = new double[(recordend - recordstart + 1) * 2];

        try {
            int number_of_points = (recordend - recordstart + 1) * 2;

            /* put into double [] */
            int i;
            for (i = 0; i < number_of_points; i += 2) {
                dsensitive[i] = sensitiveCoordinates[i / 2][0];
                dsensitive[i + 1] = sensitiveCoordinates[i / 2][1];
            }
        } catch (Exception e) {
            SpatialLogger.log("getPoints(" + recordstart + "," + recordend, e.toString());
        }

        //merge sensitive and regular points, if not missing (-1)
        for (int i = 0; i
                < d.length; i++) {
            if (dsensitive[i] != -1) {
                d[i] = dsensitive[i];
            }
        }

        return d;
    }
    //TODO: bounding box hashmap cleanup
    static HashMap<String, String> lsidBoundingBox = new HashMap<String, String>();

    static public String getLsidBoundingBox(String lsid) {
        String bb = lsidBoundingBox.get(lsid);
        if (bb == null) {
            IndexedRecord[] ir = filterSpeciesRecords(lsid);
            if (ir != null && ir.length > 0) {
                double minx = all_points[ir[0].record_start][0];
                double miny = all_points[ir[0].record_start][1];
                double maxx = all_points[ir[0].record_start][0];
                double maxy = all_points[ir[0].record_start][1];
                for (int i = ir[0].record_start; i <= ir[0].record_end; i++) {
                    if (minx > all_points[i][0]) {
                        minx = all_points[i][0];
                    }
                    if (maxx < all_points[i][0]) {
                        maxx = all_points[i][0];
                    }
                    if (miny > all_points[i][1]) {
                        miny = all_points[i][1];
                    }
                    if (maxy < all_points[i][1]) {
                        maxy = all_points[i][1];
                    }
                }
                StringBuffer sb = new StringBuffer();
                sb.append(minx).append(",").append(miny).append(",").append(maxx).append(",").append(maxy);
                lsidBoundingBox.put(lsid, sb.toString());
            }
        }
        return bb;
    }
}

/**
 * points object for housing longitude, latitude and a sorted records index
 *
 * not required for use elsewhere
 *
 * @author adam
 *
 */
class Point
        extends Object {

    /**
     * longitude as double
     */
    public double longitude;
    /**
     * latitude as double
     */
    public double latitude;
    /**
     * sorted records index of this object
     */
    public int idx;

    /**
     * constructor for Point
     * @param longitude_ longitude as double
     * @param latitude_ latitude as double
     * @param idx_ sorted records index of this object
     */
    public Point(double longitude_, double latitude_, int idx_) {
        longitude = longitude_;
        latitude = latitude_;
        idx = idx_;
    }
}

class CommonNameRecord {

    public String name;
    public String nameLowerCase;
    public int index;

    public CommonNameRecord(String name_, int index_) {
        name = name_;
        index = index_;
        nameLowerCase = name_.toLowerCase();
    }
}

class MakeOccurrenceParts extends Thread {

    /**
     * all occurrences data
     */
    HashMap<Integer, Object> occurrences;
    /**
     * index to actual csv field positions to named
     * fields
     */
    int[] column_positions;
    int[] sensitive_column_positions;
    /**
     * for column_keys write buffer to take some load off sorting
     */
    String prev_key = null;
    /**
     * for column_keys write buffer to take some load off sorting
     */
    StringBuffer prev_value = null;
    TreeMap<String, Integer>[] columnKeys;
    int[] columnKeysToOccurrencesOrder;
    LinkedBlockingQueue<String[]> lbq;
    CountDownLatch cdl;

    public MakeOccurrenceParts(LinkedBlockingQueue<String[]> lbq_, CountDownLatch cdl_, int[] column_positions_, int[] sensitive_column_positions_) {
        setPriority(Thread.MIN_PRIORITY);
        lbq = lbq_;
        cdl = cdl_;
        column_positions = column_positions_;
        sensitive_column_positions = sensitive_column_positions_;
    }

    public void run() {
        try {
            while (true) {
                String[] next = lbq.take();
                makePart(Long.parseLong(next[0]), next[1], next[2]);
                cdl.countDown();
            }
        } catch (Exception e) {
        }
    }

    void makePart(long part_number, String firstLine, String terminatingLine) {
        String[] columns = TabulationSettings.occurances_csv_fields;
        String[] columnsSettings =
                TabulationSettings.occurances_csv_field_settings;
        int i;
        columnKeys = new TreeMap[columns.length];
        for (i = 0; i < columnKeys.length; i++) {
            columnKeys[i] = new TreeMap<String, Integer>();
        }

        occurrences = new HashMap<Integer, Object>();

        int id_column_idx = sensitive_column_positions[0];
        int slong_column_idx = sensitive_column_positions[1];
        int slat_column_idx = sensitive_column_positions[2];

        /* read occurances_csv */
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(TabulationSettings.occurances_csv));

            FileWriter fwExcluded = new FileWriter(TabulationSettings.index_path
                    + OccurrencesIndex.EXCLUDED_FILENAME + part_number);

            FileWriter fws = new FileWriter(
                    TabulationSettings.index_path
                    + OccurrencesIndex.SENSITIVE_COORDINATES + part_number);

            String s;
            String[] sa;
            int[] il;
            Integer iv;

            /* lines read */
            int progress = 0;

            /* helps with ',' in text qualifier records */
            int max_columns = column_positions.length;
            ;

            OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();

            columnKeysToOccurrencesOrder = new int[columnsSettings.length];
            int p = 0;
            for (i = 0; i < ofu.onetwoCount; i++) {
                columnKeysToOccurrencesOrder[p++] = ofu.onestwos[i];
            }
            for (i = 0; i < ofu.zeroCount; i++) {
                columnKeysToOccurrencesOrder[p++] = ofu.zeros[i];
            }
            columnKeysToOccurrencesOrder[p++] = ofu.longitudeColumn;
            columnKeysToOccurrencesOrder[p] = ofu.latitudeColumn;

            int cc = 0;
            br.skip(part_number * OccurrencesIndex.PART_SIZE_MAX);

            //if part number == 0, skip header as well, otherwise skip to first
            //valid line (2nd to read after br.skip()
            s = br.readLine();
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

            //identify lowest level NAME column
            //TODO: look at occurances_csv_field_pairs and find it
            int species_name_column = column_positions[ofu.speciesColumn] + 1;

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

                if (s.equals(terminatingLine)) {
                    break;
                }
                if (s.length() != s.getBytes().length) {
                    i = 2;
                }

                //apply limit for dev
                if (progress > TabulationSettings.occurances_csv_max_records && TabulationSettings.occurances_csv_max_records > 0) {
                    break;
                }

                cc++;
                sa = s.split(",");

                /* handlers for the text qualifiers and ',' in the middle */
                if (sa != null && sa.length > max_columns) {
                    sa = OccurrencesIndex.split(s);
                }

                /* remove quotes and commas form terms */
                for (i = 0; i < sa.length; i++) {
                    if (sa[i].length() > 0) {
                        sa[i] = sa[i].replace("\"", "");
                        sa[i] = sa[i].replace(",", " ");
                    }
                }

                progress++;

                //export for sensitive coordinates
                if (slat_column_idx >= sa.length
                        || slong_column_idx >= sa.length
                        || id_column_idx >= sa.length) {
                    System.out.println(s);
                } else if (sa[slong_column_idx] != null && sa[slong_column_idx].length() > 0
                        && sa[slat_column_idx] != null && sa[slat_column_idx].length() > 0) {
                    fws.append(sa[id_column_idx]).append(",").append(sa[slong_column_idx]).append(",").append(sa[slat_column_idx]).append("\n");
                }

                {
                    /*ignore records with no species or longitude or
                     * latitude */

                    if (sa.length >= columnsSettings.length
                            //&& sa[column_positions[ofu.speciesColumn]].length() > 0
                            && sa[column_positions[ofu.longitudeColumn]].length() > 0
                            && sa[column_positions[ofu.latitudeColumn]].length() > 0) {
                        try {
                            //if no lsid at the lowest level, take the name as lsid
                            /*if (sa.length >= columnsSettings.length
                            && sa[column_positions[ofu.speciesColumn]].length() == 0) {
                            sa[column_positions[ofu.speciesColumn]] = " " + sa[species_name_column];
                            }*/
                            //parse long & lat, failure makes record skipped
                            Double.parseDouble(sa[column_positions[ofu.longitudeColumn]]);
                            Double.parseDouble(sa[column_positions[ofu.latitudeColumn]]);

                            /* get int vs unique key for every column */
                            il = new int[columnsSettings.length];
                            for (i = 0; i < columnsSettings.length; i++) {
                                iv = columnKeys[i].get(sa[column_positions[i]]);
                                if (iv == null) {
                                    il[i] = columnKeys[i].size();
                                    columnKeys[i].put(sa[column_positions[i]], columnKeys[i].size());
                                } else {
                                    il[i] = iv.intValue();
                                }
                            }

                            /* put into tree */
                            HashMap<Integer, Object> obj = occurrences;
                            HashMap<Integer, Object> objtmp;
                            for (i = 0; i < ofu.onetwoCount; i++) {
                                objtmp = (HashMap<Integer, Object>) obj.get(Integer.valueOf(il[ofu.onestwos[i]]));
                                if (objtmp == null) {
                                    objtmp = new HashMap<Integer, Object>();
                                    obj.put(Integer.valueOf(il[ofu.onestwos[i]]), objtmp);
                                }
                                obj = objtmp;
                            }

                            /* create int[] to add, longitude + latitude + zeros */
                            int[] it = new int[ofu.zeroCount + 2];
                            for (i = 0; i < ofu.zeroCount; i++) {
                                it[i] = il[ofu.zeros[i]];
                            }
                            it[i++] = il[ofu.longitudeColumn];
                            it[i] = il[ofu.latitudeColumn];

                            ArrayList<int[]> al;

                            //add
                            if (obj.size() == 0) {
                                al = new ArrayList<int[]>();
                                al.add(it);
                                obj.put(Integer.valueOf(0), al);
                            } else {
                                al = (ArrayList<int[]>) obj.get(Integer.valueOf(0));
                                al.add(it);
                            }
                        } catch (Exception e) {
                            //error
                            fwExcluded.append(s).append("\n");
                        }
                    } else {
                        fwExcluded.append(s).append("\n");
                    }
                }
            }

            //export remaining sorted part
            exportSortedPart((int) part_number);
            //reset
            occurrences = new HashMap<Integer, Object>();
            for (i = 0; i < columnKeys.length; i++) {
                columnKeys[i] = new TreeMap<String, Integer>();
            }
            System.gc();

            fwExcluded.close();
            br.close();

            fws.close();

            SpatialLogger.log("loadOccurances done: " + part_number);
        } catch (Exception e) {
            SpatialLogger.log("loadoccurances", e.toString());
            e.printStackTrace();
        }
    }

    void exportSortedPart(int partNumber) {
        System.out.println("exporting part: " + partNumber);

        String[] columns = TabulationSettings.occurances_csv_fields;
        int i;

        OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();

        try {
            /* sorting & exporting of keys */
            ArrayList<int[]> columnKeysOrder = new ArrayList<int[]>(columnKeys.length);
            ArrayList<String[]> columnKeysReverseOrderStrings = new ArrayList<String[]>(columnKeys.length);
            int j;
            for (i = 0; i < columnKeys.length; i++) {
                int[] il = new int[columnKeys[columnKeysToOccurrencesOrder[i]].size()];
                String[] ilStringsReverseOrder = new String[columnKeys[columnKeysToOccurrencesOrder[i]].size()];
                Set<Map.Entry<String, Integer>> mes = columnKeys[columnKeysToOccurrencesOrder[i]].entrySet();
                Iterator<Map.Entry<String, Integer>> mei = mes.iterator();
                Map.Entry<String, Integer> me;

                /* make key to order mapping */
                j = 0;
                while (mei.hasNext()) {
                    me = mei.next();
                    il[me.getValue().intValue()] = j;
                    ilStringsReverseOrder[j] = me.getKey();
                    j++;
                }

                columnKeysOrder.add(il);
                columnKeysReverseOrderStrings.add(ilStringsReverseOrder);
            }

            /* open output file */
            FileWriter sorted = new FileWriter(
                    TabulationSettings.index_path + OccurrencesIndex.SORTED_FILENAME + "_" + partNumber);

            /* store points (long/lat) here until exported */
            ArrayList<Double> aPoints = new ArrayList<Double>(500000);


            //write to file
            StringBuffer s = new StringBuffer();
            writeMap(sorted, aPoints, occurrences, columnKeysOrder, columnKeysReverseOrderStrings, 0, ofu.onetwoCount, s);
            sorted.close();

            /* export points */
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + OccurrencesIndex.POINTS_FILENAME + "_" + partNumber,
                    "rw");
            byte[] b = new byte[aPoints.size() * 8];
            ByteBuffer bb = ByteBuffer.wrap(b);
            for (Double d : aPoints) {
                bb.putDouble(d.doubleValue());
            }
            points.write(b);
            points.close();

            SpatialLogger.log("exportSortedPoints done");
        } catch (Exception e) {
            SpatialLogger.log("exportSortedPoints", e.toString());
            e.printStackTrace();
        }
    }

    void writeMap(FileWriter sorted, ArrayList<Double> aPoints, HashMap<Integer, Object> map,
            ArrayList<int[]> columnKeysOrder,
            ArrayList<String[]> columnKeysReverseOrderStrings,
            int depth, int maxDepth, StringBuffer line) {
        /* two cases; last record (one object, is arraylist of String []),
         * not last record (object is another hash map)
         */
        if (depth == maxDepth) {
            ArrayList<int[]> ai =
                    (ArrayList<int[]>) map.get(Integer.valueOf(0));
            int i, j;
            try {
                String linec = new String(line.toString().getBytes("US-ASCII"));
                for (i = 0; i < ai.size(); i++) {
                    sorted.append(linec);
                    int[] sl = ai.get(i);
                    for (j = 0; j < sl.length; j++) {
                        int[] keysOrder = columnKeysOrder.get(depth + j);
                        String[] keysReverseOrderStrings = columnKeysReverseOrderStrings.get(depth + j);
                        String s = new String(keysReverseOrderStrings[keysOrder[sl[j]]].getBytes("US-ASCII"));
                        sorted.append(s);

                        if (j < sl.length - 1) {
                            sorted.append(",");
                        }
                    }

                    //parse longlat
                    double longitude = 0;
                    double latitude = 0;
                    try {
                        int[] keysOrder = columnKeysOrder.get(depth + j - 2);
                        String[] keysReverseOrderStrings = columnKeysReverseOrderStrings.get(depth + j - 2);
                        longitude = Double.parseDouble(keysReverseOrderStrings[keysOrder[sl[j - 2]]]);

                        keysOrder = columnKeysOrder.get(depth + j - 1);
                        keysReverseOrderStrings = columnKeysReverseOrderStrings.get(depth + j - 1);

                        latitude = Double.parseDouble(keysReverseOrderStrings[keysOrder[sl[j - 1]]]);
                    } catch (Exception e) {
                    }
                    aPoints.add(longitude);
                    aPoints.add(latitude);

                    sorted.append("\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            //sort this hash map as in a tree map
            TreeMap<Integer, Object> sortedMap = new TreeMap<Integer, Object>();
            int[] keysOrder = columnKeysOrder.get(depth);
            Iterator<Map.Entry<Integer, Object>> it = map.entrySet().iterator();
            Map.Entry<Integer, Object> me;
            while (it.hasNext()) {
                me = it.next();
                //translate Integer value
                sortedMap.put(keysOrder[me.getKey().intValue()], me.getValue());
            }

            //iterate over & write
            String[] keysReverseOrderStrings = columnKeysReverseOrderStrings.get(depth);
            it = sortedMap.entrySet().iterator();
            while (it.hasNext()) {
                me = it.next();

                StringBuffer sb = new StringBuffer(line.toString());
                sb.append(keysReverseOrderStrings[me.getKey().intValue()]);
                sb.append(",");

                //drill down
                writeMap(sorted, aPoints, (HashMap<Integer, Object>) me.getValue(), columnKeysOrder,
                        columnKeysReverseOrderStrings, depth + 1, maxDepth, sb);
            }
        }
    }
}
