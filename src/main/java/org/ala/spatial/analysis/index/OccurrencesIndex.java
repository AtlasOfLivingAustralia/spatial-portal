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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
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
 * 			optional hierarchy fields, top down order, e.g. Family, Genus
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
     * length of a species record
     */
    //static final int SPECIES_RECORD_WIDTH = 255 + 4 + 1 + 20 * 4;	//name width, commas, new line, numbers
    
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
    static int [] grid_points_idx = null;
    
    /**
     * reverse index lookup for records points on sorted 0.5 degree grid
     */
    static int [] grid_points_idx_rev = null;
    
    /**
     * all records points on sorted 0.5 degree grid in lat then long order
     */
    static double [][] grid_points = null;
    
    /**
     * grid 360degree/0.5degree square, for entry position into grid_points 
     */
    static int [][] grid_key = null;

    /**
     * default constructor
     */
    public OccurrencesIndex() {
        TabulationSettings.load();
    }

    /**
     * performs update of 'indexing' for new points data
     */
    public void occurancesUpdate() {
        /* order is important */
        loadOccurances();
        exportSortedPoints();
        exportSortedGEOPoints();
        exportSortedGridPoints();
        exportFieldIndexes();   
    }

    /**
     * performs update of 'indexing' for a new layer (grid or shapefile)
     *
     * @param layername name of the layer to update as String.  To update
     * all layers use null.
     */
    public void layersUpdate(String layername) {
        //not applicable
    }

    /**
     * method to determine if the index is up to date
     *
     * @return true if index is up to date
     */
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
     * reads occurances_csv into a sorted map
     *
     * exports as OCC_SORTED.csv
     */
    void loadOccurances() {
        String[] columns = TabulationSettings.occurances_csv_fields;

        /* read occurances_csv */
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(TabulationSettings.occurances_csv));

            String s;
            String[] sa;

            /* lines read */
            int progress = 0;

            /* helps with ',' in text qualifier records */
            int max_columns = 0;

            column_keys = new TreeMap<String, StringBuffer>();

            while ((s = br.readLine()) != null) {

                sa = s.split(",");

                /* handlers for the text qualifiers and ',' in the middle */
                if (sa != null && max_columns == 0) {
                    max_columns = sa.length;
                }
                if (sa != null && sa.length > max_columns) {
                    sa = split(s);
                }

                /* remove quotes for terms */
                for (int i = 0; i < sa.length; i++) {
                    if (sa[i].length() > 0) {
                        sa[i] = sa[i].replace("\"", "");
                    }
                }

                progress++;

                if (progress == 1) {						//first record
                    getColumnPositions(sa);
                } else {
                    /*ignore records with no species or longitude or
                     * latitude (-3, -2, -1 idx) */
                    if (sa[column_positions[columns.length - 3]].length() > 0
                            && sa[column_positions[columns.length - 2]].length() > 0
                            && sa[column_positions[columns.length - 1]].length() > 0) {
                        pushRecord(sa);
                    }
                }

            }

            flushRecord();			//finish writing

            br.close();
            (new SpatialLogger()).log("loadOccurances done");
        } catch (Exception e) {
            (new SpatialLogger()).log("loadoccurances", e.toString());
        }

    }

    /**
     * operates on previously populated sorted map <code>column_keys</code>
     *
     * exports sorted records as csv
     * exports points in same order as sorted records as doubles
     *
     * exports as OCC_SORTED.csv
     * exports as OCC_POINTS.dat
     */
    void exportSortedPoints() {
        String value;
        String key;
        int i;

        String[] line;

        try {
            double longitude;
            double latitude;
            
            /* get all data as preloaded */
            Set<Map.Entry<String, StringBuffer>> entryset = column_keys.entrySet();
        
            Iterator<Map.Entry<String, StringBuffer>> iset = entryset.iterator();

            /* open output file */
            FileWriter sorted = new FileWriter(
                    TabulationSettings.index_path + SORTED_FILENAME);

            Map.Entry<String, StringBuffer> me;

            String s;
            
            ArrayList<Double> al = new ArrayList<Double>();
            
            /* iterate across all data and write only those with valid points*/
            while (iset.hasNext()) {
                me = iset.next();
                value = me.getValue().toString();
                key = me.getKey();

                /* one record per split */
                line = value.split(";"); 				//first string is empty
                for (i = 1; i < line.length; i++) {
                    s = key + "," + line[i];

                    /* force to US-ASCII, fallover if not supported 
                     * TODO: UTF-8 currently impacts on char position in IndexedRecord, fix it
                     */
                    s = new String(s.getBytes("US-ASCII"), "US-ASCII");
                    
                    longitude = 0;
                    latitude = 0;
                    try {
                        String[] longlat = line[i].split(",");
                        
                        longitude = Double.parseDouble(longlat[0]);
                        latitude = Double.parseDouble(longlat[1]);
                        
                        al.add(longitude);
                        al.add(latitude);  
                        
                        sorted.append(s.trim() + "\n");
                    } catch (Exception e) {
                    	/* no action for invaild longlat records */
                    }
                }
            }
            sorted.flush();
            sorted.close();

            /* export points */ 
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME,
                    "rw");
            byte [] b = new byte[al.size()*8];
            ByteBuffer bb = ByteBuffer.wrap(b);            
            for(Double d : al){
            	bb.putDouble(d.doubleValue());
            }
            points.write(b);            
            points.close();
            (new SpatialLogger()).log("exportSortedPoints done");
        } catch (Exception e) {
            (new SpatialLogger()).log("exportSortedPoints", e.toString());
        }
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
     * acts as write buffer into column_keys to take load off
     * sorting
     *
     * @param occurance next species occurances record
     */
    void pushRecord(String[] occurance) {
        String key;

        int i;

        //make key
        key = occurance[column_positions[0]];		//set first value
        for (i = 1; i < column_positions.length - 2; i++) {	//ignore longitude & latitude
            key += "," + occurance[column_positions[i]];
        }

        //check key with prev_key, flush to sorted map only if necessary
        if (prev_key == null) {
            prev_key = key;
            prev_value = column_keys.get(key);
            if (prev_value == null) {
                prev_value = new StringBuffer();
            }
        } else if (!prev_key.equals(key)) {
            flushRecord();			//flush previous, different, key
            prev_key = key;
            prev_value = column_keys.get(key);
            if (prev_value == null) {
                prev_value = new StringBuffer();
            }
        } else {
            /* same prev_key, do nothing */
        }

        /* store as string for later split on ';' and write as csv */
        prev_value.append(";"
                + occurance[column_positions[column_positions.length - 2]] //longitude
                + ","
                + occurance[column_positions[column_positions.length - 1]]);	//latitude

    }

    /**
     * write in memory record to column_keys.
     *
     * takes some load off sorting
     */
    void flushRecord() {
        column_keys.put(prev_key, prev_value);
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
    @SuppressWarnings("unchecked")
	void exportFieldIndexes() {
        String[] columns = TabulationSettings.occurances_csv_fields;

        TreeMap<String, IndexedRecord>[] fw_maps = new TreeMap[columns.length - 2];

        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(
                    TabulationSettings.index_path
                    + SORTED_FILENAME));
            String s;
            String[] sa;

            int progress = 0;

            int i;

            for (i = 0; i < columns.length - 2; i++) {
                fw_maps[i] = new TreeMap<String, IndexedRecord>();
            }

            String[] last_value = new String[columns.length - 2];
            int[] last_position = new int[columns.length - 2];
            int[] last_record = new int[columns.length - 2];

            for (i = 0; i < columns.length - 2; i++) {
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
                if (sa.length >= last_value.length) {
                    for (i = 0; i < columns.length - 2; i++) {
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

            for (i = 0; i < columns.length - 2; i++) {
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

        /* write as array objects for faster reads later*/
        try {
            int i;

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
                        + OTHER_IDX_PREFIX + columns[i] + OTHER_IDX_POSTFIX;

                /* rename the species file */
                if (i == columns.length - 3) {
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
                    fw.append(r.name + "," + r.file_start + "," + r.file_end
                            + "," + r.record_start + "," + r.record_end);
                }
                fw.close();

            }
            (new SpatialLogger()).log("exportFieldIndexes done");
        } catch (Exception e) {
            (new SpatialLogger()).log("exportFieldIndexes, write", e.toString());
        }
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

        if (all_indexes.size() > 0) {
            ArrayList<IndexedRecord> matches = new ArrayList<IndexedRecord>();
            int i = 0;
            for (IndexedRecord[] ir : all_indexes) {
            	/* if a 'type' is specified, use it */
                if (type == null
                        || type.equals(TabulationSettings.occurances_csv_fields[i])) {

                    for (IndexedRecord r : ir) {
                        if (r.name.equals(filter)) {
                            matches.add(r);
                        }
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

        //only load once, corresponding to column names
        int count = 0;
        try {
            int i;
            if (all_indexes.size() == 0) {
                for (i = 0; i < columns.length - 2; i++) {
                    String filename = TabulationSettings.index_path
                            + OTHER_IDX_PREFIX + columns[i] + OTHER_IDX_POSTFIX;

                    /* rename the species file */
                    if (i == columns.length - 3) {
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
                }
            }
            (new SpatialLogger()).log("loadIndexes done");
        } catch (Exception e) {
            (new SpatialLogger()).log("loadIndexes", e.toString());
            e.printStackTrace();
        }

        /* put all_indexes into single_index */
        single_index = new IndexedRecord[count];
        int i = 0;
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
     * @param records array of record indexes to return as int []
     * @return sorted records as String []
     */
    public static String[] getSortedRecords(int [] records) {
    	int i;
    	
    	/* enforce sort */
    	for (i = 1; i < records.length; i++) {
    		if (records[i-1] > records[i]) {
    			java.util.Arrays.sort(records);
    			break;
    		}
    	}
    	
    	String [] lines = new String[records.length];
    	
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
    public static double [] getPoints(int recordstart, int recordend){
    	double [] d = new double[(recordend-recordstart+1)*2];
    	try {    		
    		/* ready requested byte block */ 
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME,
                    "r");
            int number_of_points = (recordend-recordstart+1)*2;
            byte [] b = new byte[(number_of_points)*8];
            points.seek(recordstart*2*8);
            points.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);
            points.close();
            
            /* put into double [] */
            int i;
            for (i = 0; i<number_of_points; i++) {
				d[i] = bb.getDouble();	
			}        
            
    	} catch(Exception e) {
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
    public static double [][] getPointsPairs(){  
    	/* if already loaded, return existing data */
    	if (all_points != null) {
    		return all_points;
    	}
    	
    	double [][] d = null;
    	
    	try {    		
    		/* load all points */ 
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME,
                    "r");
            int number_of_points = ((int)points.length())/8;
            int number_of_records = number_of_points/2;
            byte [] b = new byte[number_of_points*8];            
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
    	}catch(Exception e){
    		(new SpatialLogger()).log("getPointsPairs", e.toString());
    	}
    	
    	/* store for next time */
    	all_points = d;
			
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
    void exportSortedGEOPoints(){
    	/* load OCC_POINTS.dat */
    	double [][] points = getPointsPairs();
    	
    	/* make Points object */
    	Point [] pa = new Point[points.length];
    	int i;
    	for (i=0; i<points.length; i++) {
    		pa[i] = new Point(points[i][0],points[i][1],i);
    	}
    	
    	/* sort by latitude then longitude */
    	java.util.Arrays.sort(pa,
                new Comparator<Point>() {
                    public int compare(Point r1, Point r2) {
                    	double result = r1.latitude - r2.latitude;
                    	if (result == 0) {
                    		result = r1.longitude - r2.longitude;
                    	}                    	
                        return (int)result;
                    }
                });
    	
    	//export points in this new order
    	try {
	        RandomAccessFile raf = new RandomAccessFile(
	                TabulationSettings.index_path + POINTS_FILENAME_GEO,
	                "rw");
	        byte [] b = new byte[pa.length*8*2];
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
	        byte [] b = new byte[pa.length*4];
	        ByteBuffer bb = ByteBuffer.wrap(b);            
	        for (Point p : pa) {
	        	bb.putInt(p.idx);
	        }
	        raf.write(b);            
	        raf.close();
    	} catch(Exception e) {
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
    public static double [][] getPointsPairsGEO(){    	
    	double [][] d = null;
    	try{    		
    		/* load all */ 
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_GEO,
                    "r");
            int number_of_points = ((int)points.length())/8;
            int number_of_records = number_of_points/2;
            byte [] b = new byte[number_of_points*8];            
            points.read(b);            
            ByteBuffer bb = ByteBuffer.wrap(b); 
            points.close();
            
            int i;
            
            // read out doubles 
            d = new double[number_of_records][2];
            for(i = 0;i<number_of_records;i++){
				d[i][0] = bb.getDouble();
				d[i][1] = bb.getDouble();
			}        
            
    	}catch(Exception e){
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
    public static int [] getPointsPairsGEOidx(){    	
    	int [] d = null;
    	try{    		
    		/* points */ 
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_GEO_IDX,
                    "r");
            int number_of_points = ((int)points.length())/4;
            int number_of_records = number_of_points;

            byte [] b = new byte[number_of_points*4];
            
            points.read(b);
            
            ByteBuffer bb = ByteBuffer.wrap(b);      
            int i;
            d = new int[number_of_records];
            for (i = 0; i < number_of_records; i++) {
				d[i] = bb.getInt();
			}        
            points.close();
            
    	}catch(Exception e){
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
    void exportSortedGridPoints(){
    	/* load OCC_POINTS.dat */
    	double [][] points = getPointsPairs();
    	
    	/* load Points object */
    	Point [] pa = new Point[points.length];
    	int i;
    	for (i=0; i<points.length; i++) {
    		pa[i] = new Point(points[i][0],points[i][1],i);
    	}
    	
    	/* sort on 0.5degree grid by latitude then longitude */
    	java.util.Arrays.sort(pa,
                new Comparator<Point>() {
                    public int compare(Point r1, Point r2) {
                    	double result = Math.floor(2*r1.latitude) - Math.floor(2*r2.latitude);
                    	if (result == 0) {
                    		result = r1.longitude - r2.longitude;
                    	}                    	
                        return (int)result;
                    }
                });
    	
    	//export points in this new order
    	try {
	        RandomAccessFile raf = new RandomAccessFile(
	                TabulationSettings.index_path + POINTS_FILENAME_05GRID,
	                "rw");
	        byte [] b = new byte[pa.length*8*2];
	        ByteBuffer bb = ByteBuffer.wrap(b);            
	        for (Point p : pa) {
	        	bb.putDouble(p.longitude);
	        	bb.putDouble(p.latitude);
	        }
	        raf.write(b);            
	        raf.close();
    	} catch(Exception e) {
    		(new SpatialLogger()).log("exportSortedGridPoints", e.toString());
    	}
    	
    	//export lookup for idx/record reference
    	try {
	        RandomAccessFile raf = new RandomAccessFile(
	                TabulationSettings.index_path + POINTS_FILENAME_05GRID_IDX,
	                "rw");
	        byte [] b = new byte[pa.length*4];
	        ByteBuffer bb = ByteBuffer.wrap(b);  
	        
	        /* reverse */
	        int [] idx_reverse = new int[pa.length];
	        for (i = 0; i < pa.length; i++) {
	        	idx_reverse[pa[i].idx] = i;
	        }	        
	        for (i = 0; i < idx_reverse.length; i++) {
	        	bb.putInt(idx_reverse[i]);
	        }	       
	        raf.write(b);  
	        
	        /* forward */
	        b = new byte[pa.length*4];
	        bb = ByteBuffer.wrap(b); 	        
	        for (i = 0; i < pa.length; i++) {
	        	bb.putInt(pa[i].idx);
	        }
	        raf.write(b);  
	        
	        raf.close();
    	} catch(Exception e) {
    		(new SpatialLogger()).log("exportSortedGridPoints", e.toString());
    	}   	
    	
    	//export lookup for idx/record reference
    	
    	/* 720 constant is 360degrees/0.5degree grid */
    	
    	try {
	        RandomAccessFile raf = new RandomAccessFile(
	                TabulationSettings.index_path + POINTS_FILENAME_05GRID_KEY,
	                "rw");
	        byte [] b = new byte[(360*2*360*2)*4];
	        ByteBuffer bb = ByteBuffer.wrap(b); 

	        /* fill grid cells positions/key */
	        int p = 0;
	        int [][] list = new int[720][720];
	        int j;
	        for (i = 0; i < 720; i++) {
	        	for (j = 0; j < 720; j++) {
	        		list[i][j] = -1;
	        	}
	        }
	        for (i = 0; i < pa.length; i++) {
	        	int x = (int)Math.floor(((360+pa[p].longitude)*2)%720);
	        	int y = (int)Math.floor(((pa[p].latitude+180+360)*2)%720);		//latitude is -180 to 180
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
    public static double [][] getPointsPairsGrid(){   
    	/* return existing load if available */
    	if (grid_points != null) {
    		return grid_points;
    	}
    	
    	double [][] d = null;
    	
    	try {    		
    		/* load all points */ 
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_05GRID,
                    "r");
            int number_of_points = ((int)points.length())/8;
            int number_of_records = number_of_points/2;
            byte [] b = new byte[number_of_points*8];            
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
    public static int [] getPointsPairsGrididxRev(){
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
    public static int [] getPointsPairsGrididx(){ 
    	/* return existing load if available */
    	if (grid_points_idx != null) {
    		return grid_points_idx;
    	}
    	
    	int [] d1 = null;
    	int [] d2 = null;
    	int i;
    	try {    		
    		/* load all */ 
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_05GRID_IDX,
                    "r");
            int number_of_points = ((int)points.length())/4;
            int number_of_records = number_of_points;
            byte [] b = new byte[number_of_points*4];   
            
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
    public static int [][] getPointsPairsGridKey(){  
    	/* use previously loaded if available */
    	if(grid_key != null){
    		return grid_key;
    	}
    	
    	int [][] d = null;
    	int i;
    	
    	try {    		
    		/* load all */ 
            RandomAccessFile points = new RandomAccessFile(
                    TabulationSettings.index_path + POINTS_FILENAME_05GRID_KEY,
                    "r");
            int number_of_points = ((int)points.length())/4;
            byte [] b = new byte[number_of_points*4];           
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
    static public boolean inRegion(int record, SimpleRegion r){
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
    static public int[] getRecordsInside(SimpleRegion r){
    	/* init */
    	getPointsPairsGridKey();
    	getPointsPairsGrid();
    	getPointsPairsGrididx();
    	
    	/* make overlay grid from this region */
    	int [][] cells = r.getOverlapGridCells(0, -180, 360, 180, 720, 720, null);
    	
    	int i,j;
    	
    	/* for matching cells, test each record within  */
    	Vector<Integer> records = new Vector<Integer>();
    	for (i=0; i<cells.length; i++) {
    		int start = grid_key[cells[i][1]][cells[i][0]];
    		int end = start;
    		
    		if (cells[i][0] < (720 - 1)) {
    			// not last record on a grid_key row, use limit on next line
    			end = grid_key[cells[i][1]][cells[i][0]+1];
    		} else if(cells[i][1] < (720 - 1)) {
    			// must be last record on a grid key row, 
    			// also not on last row, use next row, first cell grid key
    			end = grid_key[cells[i][1]+1][0];
    		} else {
    			// must be at end of grid_key file, use all
    			end = grid_points.length;    			
    		}
    		
    		//test each potential match
    		for (j = start; j < end; j++) {
    			if (r.isWithin(grid_points[j][0],grid_points[j][1])) {
    				records.add(new Integer(grid_points_idx[j]));
    			}
    		}
    	}
    	
    	/* format matches as int [] */
    	if (records.size() > 0) {
    		int [] data = new int[records.size()];
    		Iterator<Integer> li = records.listIterator();
    		i = 0;
    		while (li.hasNext()) {
    			data[i++] = li.next();
    		}
    		return data;
    	}
    	return null;
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
	public Point(double longitude_, double latitude_, int idx_){
		longitude = longitude_;
		latitude = latitude_;
		idx = idx_;
	}
}
