package org.ala.spatial.analysis.index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.BitSet;
import org.ala.spatial.util.OccurrencesFieldsUtil;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;

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
    /**
     * points filename, in sorted records order
     */
    static final String POINTS_FILENAME = "OCC_POINTS.dat";
    /**
     * geo sorted points filename, in latitude then longitude sorted order
     */
    static final String POINTS_FILENAME_GEO = "OCC_POINTS_GEO.dat";
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
     * species index file, contains IndexRecord for each species 
     */
    static final String SPECIES_IDX_FILENAME = "OCC_IDX_SPECIES.dat";
    /**
     * prefix for non-species index files
     */
    static final String OTHER_IDX_PREFIX = "OCC_IDX_";
    /**
     * postfix for non-species index files
     */
    static final String OTHER_IDX_POSTFIX = ".dat";
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
     * all occurrences data
     */
    HashMap<Integer,Object> occurrences;
    /**
     * object to perform sorting on occurances_csv
     */
    SortedMap<String, StringBuffer> column_keys;
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
        occurrencesParts();
        mergeParts();
        exportSortedGEOPoints();
        exportSortedGridPoints();
        exportFieldIndexes();
    }

    TreeMap<String, Integer> [] columnKeys;
    int [] columnKeysToOccurrencesOrder;

    void mergeParts(){
        //count # of parts
        int numberOfParts = 0;
        while ((new File(
                TabulationSettings.index_path
                + POINTS_FILENAME + "_" + numberOfParts)).exists()){
            numberOfParts++;
        }

        //TODO: move existing joined output to *_(numberOfParts+1) for inclusion

        String [] lines = new String[numberOfParts];
        BufferedReader [] reader = new BufferedReader[numberOfParts];
        int [] positions = new int[numberOfParts];
        ArrayList<double[][]> points = new ArrayList<double[][]>(numberOfParts);
        int pointsCount = 0;

        try{
            //open output streams (points + sorted data)
            int i;
            for(i=0;i<numberOfParts;i++){
                reader[i] = new BufferedReader(new FileReader(
                        TabulationSettings.index_path + SORTED_FILENAME
                        + "_" + i));
                lines[i] = reader[i].readLine();
                if(lines[i] != null){
                    lines[i] = lines[i].trim();
                }
                //open points
                double [][] tmp = getPointsPairs(i);
                points.add(tmp);
                positions[i] = 0;
                pointsCount += tmp.length*2;
            }

            FileWriter fw = new FileWriter(TabulationSettings.index_path
                    + SORTED_FILENAME);
            double [] aPoints = new double[pointsCount];
            int pointsPos = 0;

            //write to file
            int finishedParts = 0;
            String topline;
            while (finishedParts < numberOfParts) {
                //get top record
                topline = null;
                for(i=0;i<numberOfParts;i++){
                    if((topline == null && lines[i] != null)
                            || (topline != null && lines[i] != null
                                && topline.compareTo(lines[i]) > 0)){
                        topline = lines[i];
                    }
                }

                //write top records & increment
                for(i=0;i<numberOfParts;i++){
                    if(lines[i] != null && topline.equals(lines[i])){
                        fw.append(lines[i]);
                        fw.append("\n");
                        lines[i] = reader[i].readLine();
                        if(lines[i] == null){
                            finishedParts++;
                        } else {
                            lines[i] = lines[i].trim();
                        }
                        aPoints[pointsPos] = points.get(i)[positions[i]][0];
                        aPoints[pointsPos+1] = points.get(i)[positions[i]][1];
                        positions[i]++;
                        pointsPos+=2;
                    }
                }
            }

            fw.close();

            /* export points */
            RandomAccessFile pointsfile = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME,
                    "rw");
            byte[] b = new byte[aPoints.length * 8];
            ByteBuffer bb = ByteBuffer.wrap(b);
            for (double d : aPoints) {
                bb.putDouble(d);
            }
            pointsfile.write(b);
            pointsfile.close();

            //delete parts
            for(i=0;i<numberOfParts;i++){
                (new File(TabulationSettings.index_path
                        + SORTED_FILENAME + "_" + i)).delete();
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        System.gc();
    }

    void occurrencesParts(){
        String[] columns = TabulationSettings.occurances_csv_fields;
        String[] columnsSettings =
                TabulationSettings.occurances_csv_field_settings;
        int i;
        columnKeys = new TreeMap[columns.length];
        for(i=0;i<columnKeys.length;i++){
            columnKeys[i] = new TreeMap<String, Integer>();
        }

        int partNumber = 0;
        occurrences = new HashMap<Integer, Object>();

        /* read occurances_csv */
        
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(TabulationSettings.occurances_csv));

            String s;
            String[] sa;
            int [] il;
            Integer iv;

            /* lines read */
            int progress = 0;

            /* helps with ',' in text qualifier records */
            int max_columns = 0;

            OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();

            String [] colnames = ofu.getOutputColumnNames();



            columnKeysToOccurrencesOrder = new int[columnsSettings.length];
            int p = 0;
            for(i=0;i<ofu.onetwoCount;i++){
                columnKeysToOccurrencesOrder[p++] = ofu.onestwos[i];
            System.out.print("[" + ofu.onestwos[i] + " " + colnames[p-1]);
            }
            for(i=0;i<ofu.zeroCount;i++){
                columnKeysToOccurrencesOrder[p++] = ofu.zeros[i];
                System.out.print("[" + ofu.zeros[i] + " " + colnames[p-1]);
            }
            columnKeysToOccurrencesOrder[p++] = ofu.longitudeColumn;
            System.out.print("[" + ofu.longitudeColumn + " " + colnames[p-1]);
            columnKeysToOccurrencesOrder[p] = ofu.latitudeColumn;
            System.out.print("[" + ofu.latitudeColumn + " " + colnames[p-1]);

            column_keys = new TreeMap<String, StringBuffer>();

            int cc = 0;
            while ((s = br.readLine()) != null) {
                if((cc % 500000 == 0) && cc > 0){
                    //export sorted part
                    exportSortedPart(partNumber);

                    //reset
                     occurrences = new HashMap<Integer, Object>();
                    for(i=0;i<columnKeys.length;i++){
                        columnKeys[i] = new TreeMap<String, Integer>();
                    }
                    System.gc();

                    //inc partnumber
                    partNumber++;
                }
                cc++;
                sa = s.split(",");

                /* handlers for the text qualifiers and ',' in the middle */
                if (sa != null && max_columns == 0) {
                    max_columns = sa.length;
                }
                if (sa != null && sa.length > max_columns) {
                    sa = split(s);
                }

                /* remove quotes and commas form terms */
                for (i = 0; i < sa.length; i++) {
                    if (sa[i].length() > 0) {
                        sa[i] = sa[i].replace("\"", "");
                        sa[i] = sa[i].replace(","," ");
                    }
                }

                progress++;

                if (progress == 1) {		//first record
                    getColumnPositions(sa);
                } else {
                    /*ignore records with no species or longitude or
                     * latitude */
                    if (sa.length >= columnsSettings.length
                            && sa[column_positions[ofu.speciesColumn]].length() > 0
                            && sa[column_positions[ofu.longitudeColumn]].length() > 0
                            && sa[column_positions[ofu.latitudeColumn]].length() > 0) {
                        try{
                            //parse long & lat, failure makes record skipped
                            double longitude = Double.parseDouble(sa[column_positions[ofu.longitudeColumn]]);
                            double latitude = Double.parseDouble(sa[column_positions[ofu.latitudeColumn]]);

                            /* get int vs unique key for every column */
                            il = new int[columnsSettings.length];
                            for(i=0;i<columnsSettings.length;i++){
                                iv = columnKeys[i].get(sa[column_positions[i]]);
                                if (iv == null) {
                                    il[i] = columnKeys[i].size();
                                    columnKeys[i].put(sa[column_positions[i]],columnKeys[i].size());
                                } else {
                                    il[i] = iv.intValue();
                                }
                            }

                            /* put into tree */
                            HashMap<Integer, Object> obj = occurrences;
                            HashMap<Integer, Object> objtmp;
                            for(i=0;i<ofu.onetwoCount;i++){
                                objtmp = (HashMap<Integer,Object>)obj.get(Integer.valueOf(il[ofu.onestwos[i]]));
                                if(objtmp == null){
                                    objtmp = new HashMap<Integer,Object>();
                                    obj.put(Integer.valueOf(il[ofu.onestwos[i]]), objtmp);
                                }
                                obj = objtmp;
                            }
                            /* create int[] to add, longitude + latitude + zeros */
                            int [] it = new int[ofu.zeroCount + 2];
                            for(i=0;i<ofu.zeroCount;i++){
                                it[i] = il[ofu.zeros[i]];
                            }
                            it[i++] = il[ofu.longitudeColumn];
                            it[i] = il[ofu.latitudeColumn];

                            ArrayList<int[]> al;

                            //add
                            if(obj.size() == 0){
                                al = new ArrayList<int[]>();
                                al.add(it);
                                obj.put(Integer.valueOf(0), al);
                            } else {
                                al = (ArrayList<int[]>)obj.get(Integer.valueOf(0));
                                al.add(it);
                            }
                        }catch (Exception e){
                           //don't cate
                        }
                    } 
                }
            }

            //export remaining sorted part
            exportSortedPart(partNumber);
            //reset
            occurrences = new HashMap<Integer, Object>();
            for(i=0;i<columnKeys.length;i++){
                columnKeys[i] = new TreeMap<String, Integer>();
            }
            System.gc();


            br.close();
            
            (new SpatialLogger()).log("loadOccurances done");
        } catch (Exception e) {
            (new SpatialLogger()).log("loadoccurances", e.toString());
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
            ArrayList<int []> columnKeysOrder = new ArrayList<int []>(columnKeys.length);
            ArrayList<String []> columnKeysReverseOrderStrings = new ArrayList<String []>(columnKeys.length);
            int j;
            for(i=0;i<columnKeys.length;i++){
                int [] il = new int[columnKeys[columnKeysToOccurrencesOrder[i]].size()];
                String [] ilStringsReverseOrder = new String[columnKeys[columnKeysToOccurrencesOrder[i]].size()];
                Set<Map.Entry<String, Integer>> mes = columnKeys[columnKeysToOccurrencesOrder[i]].entrySet();
                Iterator<Map.Entry<String, Integer>> mei = mes.iterator();
                Map.Entry<String,Integer> me;

                /* make key to order mapping */
                j = 0;
                while (mei.hasNext()){
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
                    TabulationSettings.index_path + SORTED_FILENAME + "_" + partNumber);

            /* store points (long/lat) here until exported */
            ArrayList<Double> aPoints = new ArrayList<Double>(500000);


            //write to file
            StringBuffer s = new StringBuffer();
            writeMap(sorted, aPoints, occurrences,columnKeysOrder, columnKeysReverseOrderStrings, 0, ofu.onetwoCount, s);
                     
            /* export points */
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME + "_" + partNumber,
                    "rw");
            byte[] b = new byte[aPoints.size() * 8];
            ByteBuffer bb = ByteBuffer.wrap(b);
            for (Double d : aPoints) {
                bb.putDouble(d.doubleValue());
            }
            points.write(b);
            points.close();
            
            (new SpatialLogger()).log("exportSortedPoints done");
        } catch (Exception e) {
            (new SpatialLogger()).log("exportSortedPoints", e.toString());
            e.printStackTrace();
        }
    }

    void writeMap(FileWriter sorted, ArrayList<Double> aPoints, HashMap<Integer, Object> map,
            ArrayList<int []> columnKeysOrder,
            ArrayList<String []> columnKeysReverseOrderStrings,
            int depth, int maxDepth, StringBuffer line) {
        /* two cases; last record (one object, is arraylist of String []), 
         * not last record (object is another hash map)
         */
        if(depth == maxDepth){
           ArrayList<int[]> ai =
                    (ArrayList<int[]>)map.get(Integer.valueOf(0));
            int i,j;
            try{
                String linec = new String(line.toString().getBytes("US-ASCII"));
                for(i=0;i<ai.size();i++){                    
                    sorted.append(linec);
                    int [] sl = ai.get(i);
                    for(j=0;j<sl.length;j++){
                        int [] keysOrder = columnKeysOrder.get(depth + j);
                        String [] keysReverseOrderStrings = columnKeysReverseOrderStrings.get(depth + j);
                        String s = new String(keysReverseOrderStrings[keysOrder[sl[j]]].getBytes("US-ASCII"));
                        sorted.append(s);

                        if(j < sl.length-1){
                            sorted.append(",");
                        }
                    }

                    //parse longlat
                    double longitude = 0;
                    double latitude = 0;
                    try{
                        int [] keysOrder = columnKeysOrder.get(depth + j - 2);
                        String [] keysReverseOrderStrings = columnKeysReverseOrderStrings.get(depth + j - 2);
                        longitude = Double.parseDouble(keysReverseOrderStrings[keysOrder[sl[j-2]]]);

                        keysOrder = columnKeysOrder.get(depth + j - 1);
                        keysReverseOrderStrings = columnKeysReverseOrderStrings.get(depth + j - 1);

                        latitude = Double.parseDouble(keysReverseOrderStrings[keysOrder[sl[j-1]]]);
                    }catch (Exception e){

                    }
                    aPoints.add(longitude);
                    aPoints.add(latitude);

                    sorted.append("\n");
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }else{
            //sort this hash map as in a tree map
            TreeMap<Integer, Object> sortedMap = new TreeMap<Integer, Object>();
            int [] keysOrder = columnKeysOrder.get(depth);
            Iterator<Map.Entry<Integer, Object>> it = map.entrySet().iterator();
            Map.Entry<Integer, Object> me;
            while (it.hasNext()) {
                me = it.next();
                //translate Integer value
                sortedMap.put(keysOrder[me.getKey().intValue()], me.getValue());
            }

            //iterate over & write
            String [] keysReverseOrderStrings = columnKeysReverseOrderStrings.get(depth);
            it = sortedMap.entrySet().iterator();
            while (it.hasNext()){
                me = it.next();

                StringBuffer sb = new StringBuffer(line.toString());
                sb.append(keysReverseOrderStrings[me.getKey().intValue()]);
                sb.append(",");

                //drill down
                writeMap(sorted, aPoints, (HashMap<Integer, Object>) me.getValue(), columnKeysOrder,
                        columnKeysReverseOrderStrings, depth+1, maxDepth, sb);
            }

        }
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
    String[] split(String line) {
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
                    && line.length() > i + 1
                    && line.charAt(i + 1) == ',') {//end term
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
    void getColumnPositions(String[] line) {
        String[] columns = TabulationSettings.occurances_csv_fields;
        column_positions = new int[columns.length];
        int i;
        int j;

        for (j = 0; j < line.length; j++) {
            for (i = 0; i < columns.length; i++) {
                if (columns[i].equals(line[j])) {
                    column_positions[i] = j;
                }
            }
        }
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

        int countOfIndexed = ofu.onetwoCount;

        /* first n columns are indexed, n=countOfIndexed */
        TreeMap<String, IndexedRecord>[] fw_maps = new TreeMap[countOfIndexed];

        int i;
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
            int[] last_position = new int[countOfIndexed];
            int[] last_record = new int[countOfIndexed];

            for (i = 0; i < countOfIndexed; i++) {
                last_value[i] = "";
                last_position[i] = 0;
                last_record[i] = 0;
            }

            int filepos = 0;
            int recordpos = 0;

            while ((s = br.readLine()) != null) {
                sa = s.split(",");

                progress++;

                //updated = false;
                if (sa.length >= countOfIndexed) {

                    for (i = 0; i < countOfIndexed; i++) {
                        if (recordpos != 0 && !last_value[i].equals(sa[i])) {
                            fw_maps[i].put(last_value[i],
                                    new IndexedRecord(last_value[i].toLowerCase(),
                                    //lastfilepos,
                                    last_position[i],
                                    filepos,
                                    //lastrecordpos,
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

            for (i = 0; i <countOfIndexed; i++) {
                fw_maps[i].put(last_value[i],
                        new IndexedRecord(last_value[i].toLowerCase(),
                        last_position[i],
                        filepos,
                        last_record[i],
                        recordpos - 1, (byte) i));
            }

            br.close();
        } catch (Exception e) {
            (new SpatialLogger()).log("exportFieldIndexes, read", e.toString());
        }

        System.out.println("done read of indexes");
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
                if (i == countOfIndexed-1) {
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
            (new SpatialLogger()).log("exportFieldIndexes done");
        } catch (Exception e) {
            (new SpatialLogger()).log("exportFieldIndexes, write", e.toString());
        }
        
        fw_maps = null;
        System.gc();
    }

    /**
     * returns a list of (species names / type / count) for valid 
     * .beginsWith matches
     * 
     * @param filter begins with text to search for
     * @param limit limit on output
     * @return formatted species matches as String[]
     */
    static public String[] filterIndex(String filter, int limit) {
        loadIndexes();

        filter = filter.toLowerCase();

        IndexedRecord lookfor = new IndexedRecord(filter, 0, 0, 0, 0, (byte) -1);
        IndexedRecord lookforupper = new IndexedRecord(filter.substring(0, filter.length() - 1), 0, 0, 0, 0, (byte) -1);
        char nextc = (char) (((int) filter.charAt(filter.length() - 1)) + 1);

        lookforupper.name += nextc;

        String[] matches_array = null;

        /* starts with comparator, get first (pos) and last (upperpos) */
        if (!filter.contains("*")) {
            int pos = java.util.Arrays.binarySearch(single_index,
                    lookfor,
                    new Comparator<IndexedRecord>() {

                        public int compare(IndexedRecord r1, IndexedRecord r2) {
                            return r1.name.compareTo(r2.name);
                        }
                    });
            int upperpos = java.util.Arrays.binarySearch(single_index,
                    lookforupper,
                    new Comparator<IndexedRecord>() {

                        public int compare(IndexedRecord r1, IndexedRecord r2) {
                            return r1.name.compareTo(r2.name);
                        }
                    });

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
                strbuffer2.delete(0, strbuffer2.length());
                strbuffer2.append(single_index[i].name);
                strbuffer2.append(" / ");
                strbuffer2.append(getIndexType(single_index[i].type));
                strbuffer2.append(" / found ");
                strbuffer2.append(String.valueOf(single_index[i].record_end
                        - single_index[i].record_start + 1));
                matches_array[p] = strbuffer2.toString();
            }
        }

        return matches_array;
    }

    /**
     * returns a list of (species names / type / count) for valid 
     * .beginsWith matches
     * 
     * if input is like "species_name / type" match lookup column name
     * with 'type', e.g. "genus" or "family" or "species". 
     * 
     * @param filter begins with text to search for
     * @return species matches as IndexedRecord[]
     */
    static public IndexedRecord[] filterSpeciesRecords(String filter) {
        filter = filter.toLowerCase();

        /* handle
         * <species name>
         * or
         * <species name> / <type>
         */
        String type = null;
        if (filter.contains("/")) {
            type = filter.split("/")[1].trim();
            filter = filter.split("/")[0].trim();
        }

        loadIndexes();
System.out.println("indexessize:" + all_indexes.size());
        if (all_indexes.size() > 0) {
            ArrayList<IndexedRecord> matches = new ArrayList<IndexedRecord>();
            int i = 0;
            for (IndexedRecord[] ir : all_indexes) {
                for (IndexedRecord r : ir) {
                    if (r.name.equals(filter)) {
                        matches.add(r);
                        System.out.println(r.name + "," + r.record_start);
                    }
                }
                i++;
            }

            /* return something if found as [] */
            if (matches.size() > 0) {
                IndexedRecord[] indexedRecord = new IndexedRecord[matches.size()];
                matches.toArray(indexedRecord);
                return indexedRecord;
            }
        }

        return null;
    }

    /**
     * loads all OccurrencesIndex files for quicker response times
     * 
     * excludes points indexes
     */
    static void loadIndexes() {
        if (single_index != null) {
            return;
        }
        String[] columns = TabulationSettings.occurances_csv_fields;
        String[] columnsSettings = TabulationSettings.occurances_csv_field_settings;

        //only load once, corresponding to column names
        int count = 0;
        int countOfIndexed = 0;
        int i;
        for(i=0;i<columnsSettings.length;i++){
            if(columnsSettings[i].equals("2")){
                countOfIndexed++;
            }
        }
        try {
            int indexesLoaded = 0;
            if (all_indexes.size() == 0) {
                for (i = 0; i < columns.length; i++) {
                    if (columnsSettings[i].equals("2")){

                        String filename = TabulationSettings.index_path
                                + OTHER_IDX_PREFIX + columns[i] + OTHER_IDX_POSTFIX;

                        /* rename the species file */
                        if (indexesLoaded == countOfIndexed-1) {
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

                        indexesLoaded++;
                    }
                }
            }
            (new SpatialLogger()).log("loadIndexes done");
        } catch (Exception e) {
            (new SpatialLogger()).log("loadIndexes", e.toString());
            e.printStackTrace();
        }

        /* put all_indexes into single_index */
        single_index = new IndexedRecord[count];
        i = 0;
        for (IndexedRecord[] rl : all_indexes) {
            for (IndexedRecord r : rl) {
                single_index[i++] = r;
            }
        }
        java.util.Arrays.sort(
                single_index,
                new Comparator<IndexedRecord>() {

                    public int compare(IndexedRecord r1, IndexedRecord r2) {
                        return r1.name.compareTo(r2.name);
                    }
                });
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
        return TabulationSettings.occurances_csv_fields[type];
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

        /* iterate through sorted records file and extract only required records */
        try {
            LineNumberReader br = new LineNumberReader(
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
            br.close();

            return lines;
        } catch (Exception e) {
            (new SpatialLogger()).log("getSortedRecords", e.toString());
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
    public static String[] getSortedRecords(int file_start, int file_end) {
        try {
            byte[] data = new byte[file_end - file_start];
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + SORTED_FILENAME);
            fis.skip(file_start); 			//seek to start
            fis.read(data);					//read what is required
            fis.close();

            return (new String(data)).split("\n");		//convert to string
        } catch (Exception e) {
            (new SpatialLogger()).log("getSortedRecords", e.toString());
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
    public static String getSortedRecordsString(int file_start, int file_end) {
        try {
            byte[] data = new byte[file_end - file_start];
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + SORTED_FILENAME);
            fis.skip(file_start); 			//seek to start
            fis.read(data);					//read what is required
            fis.close();

            return (new String(data));		//convert to string
        } catch (Exception e) {
            (new SpatialLogger()).log("getSortedRecords", e.toString());
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
            (new SpatialLogger()).log("getPoints(" + recordstart + "," + recordend, e.toString());
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
            (new SpatialLogger()).log("getPointsPairs", e.toString());
        }

        /* store for next time */
        all_points = d;

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
            (new SpatialLogger()).log("getPointsPairs", e.toString());
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
            (new SpatialLogger()).log("exportSortedGEOPoints", e.toString());
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
            (new SpatialLogger()).log("exportSortedGEOPoints", e.toString());
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
            (new SpatialLogger()).log("getPointsPairsGEO", e.toString());
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
            (new SpatialLogger()).log("getPointsPairsGEOidx", e.toString());
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
        for (i = 0; i < points.length; i++) {
            pa[i] = new Point(points[i][0], points[i][1], i);
        }

        /* sort on 0.5degree grid by latitude then longitude */
        java.util.Arrays.sort(pa,
                new Comparator<Point>() {

                    public int compare(Point r1, Point r2) {
                        double result = Math.floor(2 * r1.latitude) - Math.floor(2 * r2.latitude);
                        if (result == 0) {
                            result = r1.longitude - r2.longitude;
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
            (new SpatialLogger()).log("exportSortedGridPoints", e.toString());
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
            (new SpatialLogger()).log("exportSortedGridPoints", e.toString());
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
            for (i = 0; i < pa.length; i++) {
                int x = (int) Math.floor(((360 + pa[p].longitude) * 2) % 720);
                int y = (int) Math.floor(((pa[p].latitude + 180 + 360) * 2) % 720);		//latitude is -180 to 180
                if (list[y][x] == -1
                        || list[y][x] > i) {
                    list[y][x] = i;
                }
            }

            /* populate blanks, test */
            int last_cell = pa.length;
            for (i = (720 - 1); i >= 0; i--) {
                for (j = (720 - 1); j >= 0; j--) {
                    if (list[i][j] == -1) {
                        list[i][j] = last_cell;
                    } else if (last_cell < list[i][j]) {
                        (new SpatialLogger()).log("exportSortedGridPoints, order err");
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
            (new SpatialLogger()).log("exportSortedGridPoints", e.toString());
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
            (new SpatialLogger()).log("getPointsPairsGrid", e.toString());
        }

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
            d1 = new int[number_of_records];
            for (i = 0; i < number_of_records; i++) {
                d1[i] = bb.getInt();
            }

            /* read forward order idx as int */
            points.read(b);
            bb = ByteBuffer.wrap(b);
            d2 = new int[number_of_records];
            for (i = 0; i < number_of_records; i++) {
                d2[i] = bb.getInt();
            }

            points.close();
        } catch (Exception e) {
            (new SpatialLogger()).log("getPointsPairsGrididx", e.toString());
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
            (new SpatialLogger()).log("getPointsPairsGridKey", e.toString());
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
        /* init */
        getPointsPairsGrid();
        getPointsPairsGrididx();

        /* transate off grid */
        int i = grid_points_idx_rev[record];

        /* test */
        return r.isWithin(grid_points[i][0], grid_points[i][1]);
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
        /* init */
        getPointsPairsGridKey();
        getPointsPairsGrid();
        getPointsPairsGrididx();

        /* make overlay grid from this region */
        int[][] cells = r.getOverlapGridCells(0, -180, 360, 180, 720, 720, null);

        System.out.println("poly:" + r.toString());
        int i, j;


        /* for matching cells, test each record within  */

        Vector<Integer> records = new Vector<Integer>();
        System.out.println("gdlen:" + grid_points.length);
        for (j = 0; j < grid_points.length; j++) {
            if (r.isWithin(grid_points[j][0], grid_points[j][1])) {
                records.add(new Integer(grid_points_idx[j]));
            }
        }

    /*    for (i = 0; i < cells.length; i++) {
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
            for (j = start; j < end; j++) {
                if (r.isWithin(grid_points[j][0], grid_points[j][1])) {
                    records.add(new Integer(grid_points_idx[j]));
                }
            }
        }
*/
        /* format matches as int [] */
        if (records.size() > 0) {
            int[] data = new int[records.size()];
            Iterator<Integer> li = records.listIterator();
            i = 0;
            while (li.hasNext()) {
                data[i++] = li.next();
            }
            return data;
        }
        return null;
    }

    static IndexedRecord[] speciesSortByRecordNumber = null;
    static int [] speciesSortByRecordNumberOrder = null;

    static void makeSpeciesSortByRecordNumber(){
        if(speciesSortByRecordNumber == null){
            loadIndexes();

            speciesSortByRecordNumber = all_indexes.get(all_indexes.size()-1).clone();

            //preserve original order in a secondary list, to be returned
            int [] tmp = new int[speciesSortByRecordNumber.length];
            int i;
            for(i=0;i<speciesSortByRecordNumber.length;i++){
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
            int t;
            speciesSortByRecordNumberOrder = new int[speciesSortByRecordNumber.length];
            for(i=0;i<speciesSortByRecordNumber.length;i++){
                t = speciesSortByRecordNumber[i].file_end;
                speciesSortByRecordNumber[i].file_end = tmp[t];

                speciesSortByRecordNumberOrder[i] = t;
            }

        }
    }

    static public BitSet getSpeciesBitset(ArrayList<Integer> records, SimpleRegion region){
        makeSpeciesSortByRecordNumber();

        int i;
        int spos = 0;
        BitSet species = new BitSet(OccurrencesIndex.getSpeciesIndex().length + 1);

        //assume records sorted
        //TODO: validate sort

        if (region == null) {
            //no region, use all
            spos = 0;
            i = 0;
            while(i < records.size()) {
                //TODO: work out when best to use binary searching instead of seeking 
                int r = records.get(i);
                
                while (spos < speciesSortByRecordNumber.length
                        && r > speciesSortByRecordNumber[spos].record_end){   //seek to next species
                    spos++;
                }

                if(spos == speciesSortByRecordNumber.length){
                    spos--; //TODO: is this correct?
                    System.out.println("rsize=" + records.size() + " r:" + r + " i:" + i);
                }
                
                species.set(speciesSortByRecordNumberOrder[spos]);
                spos++;
                i++;
                if(spos < speciesSortByRecordNumber.length){
                    while(i < records.size()
                            && records.get(i) < speciesSortByRecordNumber[spos].record_start){
                        i++;
                    }
                }else{
                    break;
                }
            }
        } else {
            /* not used */
            spos = 0;
            i = 0;
            while(i < records.size()) {
                //TODO: work out when best to use binary searching instead of seeking
                int r = records.get(i);
                while (spos < speciesSortByRecordNumber.length
                        && r > speciesSortByRecordNumber[spos].record_end){   //seek to next species+1
                    spos++;
                }             
                i++; //next record
                if (OccurrencesIndex.inRegion(spos, region)) {
                    species.set(speciesSortByRecordNumberOrder[spos]);

                    spos++; //inc

                    //seek to next
                    if(spos < speciesSortByRecordNumber.length){
                        while(i < records.size()
                                && records.get(i) < speciesSortByRecordNumber[spos].record_start){
                            i++;
                        }
                    }
                }
            }
        }

        return species;

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
class Point extends Object {

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

