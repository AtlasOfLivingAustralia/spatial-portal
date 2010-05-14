package org.ala.spatial.analysis.tabulation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.ala.spatial.util.*;

/**
 * builder for sampling index.
 *
 * requires OccurancesIndex to be up to date.
 *
 * operates on GridFiles
 * operates on PostGIS geo tables
 *
 * @author adam
 *
 */
public class SpeciesListIndex extends Object implements AnalysisIndexService, Serializable{
    /* constants - TODO, do this nicely */
	
	private static final long serialVersionUID = 1L;

    static final int MAX_SPECIES_LIMIT = 1000000;
    static final int GRID_RECORD_LENGTH = 4 * 3 + 4; //3 floats + 1 int
    static final int CATAGORY_RECORD_LENGTH = 4 * 2 + 4;//1 short + 2 floats + 1 int
    static final String SPECIES_RANK_FILENAME = "SPL_SPECIES.dat";
    /**
     * postGIS database connection
     */
   //Connection connection;
    /**
     * home for loaded species ranking
     */
    static SPLSpeciesRecord[] species_rank = null;
    static SPLSpeciesRecord[] species_rank_order = null;
    /**
     * destination of loaded occurances
     */
    ArrayList<String[]> occurances;

    /**
     * default constructor
     */
    public SpeciesListIndex() {
        TabulationSettings.load();
    }

    /**
     * performs update of 'indexing' for new points data
     */
    public void occurancesUpdate() {

        makeSPL_SPECIES();

        makeSPL_GRID();

        makeSPL_CATAGORIES();
         
       //System.out.println("about to do makeScaledImagesFromGrids");
        //makeScaledImagesFromGrids();
        makeAllScaledShortImages();
    
       
    }
    
    /**
     * performs update of 'indexing' for a new layer (grid or shapefile)
     *
     * @param layername name of the layer to update as String.  To update
     * all layers use null.
     */
    public void layersUpdate(String layername) {
    }

    /**
     * method to determine if the index is up to date
     *
     * @return true if index is up to date
     */
    public boolean isUpdated() {
        return true;
    }

    void makeSPL_SPECIES() {
        //read SAM_IDX_SPECIES.csv into map, export and sort by rank

        /* load species and setup rank order by frequency */

        int[] rank_record = new int[1000000];//assumption of maximum number of species
        int j;
        for (j = 0; j < rank_record.length; j++) {
            rank_record[j] = 0;
        }

        int value;

        IndexedRecord[] species = OccurancesIndex.getSpeciesIndex();

        for (IndexedRecord r : species) {
            value = r.record_end - r.record_start + 1;
            rank_record[value]++;
        }

        int pos = 1; 				//first position
        int sum = 1; 				//total (placeholder)
        for (j = rank_record.length - 1; j >= 0; j--) {
            if (rank_record[j] > 0) {
                sum += rank_record[j];
                rank_record[j] = pos;
                pos = sum;
            }
        }

        /* write with rank number as object */
        SPLSpeciesRecord[] sr = new SPLSpeciesRecord[species.length];

        int i = 0;
        int rank;
        for (IndexedRecord r : species) {
            value = r.record_end - r.record_start + 1;
            rank = rank_record[value];
            rank_record[value]++;
            sr[i] = new SPLSpeciesRecord(r.name, rank);
            i++;
        }

        try {
            FileOutputStream fos = new FileOutputStream(
                    TabulationSettings.index_path
                    + SPECIES_RANK_FILENAME);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(sr);
            oos.close();
            
            /* and as CSV for debugging */
            FileWriter fw = new FileWriter(
                    TabulationSettings.index_path
                    + SPECIES_RANK_FILENAME + ".csv");
            for (SPLSpeciesRecord r : sr) {
                fw.append(r.name + "," + r.rank + "\r\n");
            }
            fw.close();

            (new SpatialLogger()).log("makeSPL_SPECIES done");
        } catch (Exception e) {
            (new SpatialLogger()).log("makeSPL_SPECIES", e.toString());
        }
    }

    /**
     * for each grid file intersect and export in points order
     */
    void makeSPL_GRID() {

        /* species rank load */
        loadRank();

        Layer layer;
        int i;
        if (species_rank != null) {
System.out.println("speciesranklen: " + species_rank.length);

            for (i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
                layer = TabulationSettings.environmental_data_files[i];
                try {
                    System.out.println("makespl_grid: " + layer.name);
                    RandomAccessFile sam_d_gridfile = new RandomAccessFile(
                            TabulationSettings.index_path
                            + "SAM_D_" + layer.name + ".dat", "r");
                    BufferedReader sam_sorted = new BufferedReader(new FileReader(
                            TabulationSettings.index_path
                            + OccurancesIndex.SORTED_FILENAME));

                    double value;
                    double longitude;
                    double latitude;
                    int species_number;
                    String species;
                    String s;
                    String[] line;
                    int idx;

                    ArrayList<SPLGridRecord> records = new ArrayList<SPLGridRecord>();
                    int p = 0;
                    
                    while (sam_d_gridfile.getFilePointer() < sam_d_gridfile.length()
                            && (s = sam_sorted.readLine()) != null) {

                        line = s.split(",");
                        if (line.length > 2) {
                            species = line[line.length - 3].trim().toLowerCase();
                            value = sam_d_gridfile.readFloat();

                            /* do not want NaN */
                            if (!Double.isNaN(value)) {
                                try {
                                    longitude = Double.parseDouble(line[line.length - 2].trim());
                                    latitude = Double.parseDouble(line[line.length - 1].trim());
                                } catch (Exception e) {
                                    longitude = 0;
                                    latitude = 0;
                                }

                                idx = Arrays.binarySearch(
                                        species_rank,
                                        new SPLSpeciesRecord(species, 0),
                                        new Comparator<SPLSpeciesRecord>() {
                                            public int compare(SPLSpeciesRecord r1, SPLSpeciesRecord r2) {
                                                return r1.name.compareTo(r2.name);
                                            }                                      
                                        });
//System.out.print("[" + idx + " " + species + "]");
                                species_number = species_rank[idx].rank;

                                /* put it in a map to sort later */
                                records.add(new SPLGridRecord(value, /*longitude, latitude,*/ species_number,p));
                            }
                            p++;

                        }
                    }
                    //		if(sam_d_gridfile.getFilePointer() >0 )
                    //			return;
                    System.out.println("read: " + p + " valid records: " + records.size());

                    java.util.Collections.sort(records,
                            new Comparator<SPLGridRecord>() {

                                public int compare(SPLGridRecord r1, SPLGridRecord r2) {
                                    if (r2.value == r1.value) {
                                        return 0;
                                    } else if (r1.value < r2.value) {
                                        return -1;
                                    }
                                    return 1;
                                }
                            });

                    RandomAccessFile output = new RandomAccessFile(
                            TabulationSettings.index_path
                            + "SPL_" + layer.name + ".dat", "rw");

                    for (SPLGridRecord r : records) {
                        /*
                         * TODO: make nicer export
                         */
                        output.writeFloat((float) r.value);
                     //   output.writeFloat((float) r.longitude);
                     //   output.writeFloat((float) r.latitude);
                        output.writeInt(r.species_number);
                        output.writeInt(r.record_number);
                    }
                    System.out.println("written: " + records.size());

                    output.close();
                    sam_d_gridfile.close();
                    sam_sorted.close();

                    (new SpatialLogger()).log("makeSPL_GRID writing done > " + layer.name);

                } catch (Exception e) {
                    (new SpatialLogger()).log("makeSPL_GRID writing", ">" + layer.name + "> " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        System.out.println("finished makespl_grid");
    }

    /**
     * for each catagory file, split by field index value
     */
    void makeSPL_CATAGORIES() {

        /* species rank load */
        SpeciesListIndex.loadRank();

        Layer layer;
        int i;
        if (species_rank != null) {
            for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
                layer = TabulationSettings.geo_tables[i];
                try {
                    RandomAccessFile sam_i_layerfile = new RandomAccessFile(
                            TabulationSettings.index_path
                            + SamplingIndex.CATAGORICAL_PREFIX + layer.name
                            + SamplingIndex.VALUE_POSTFIX, "r");
                    
                    BufferedReader sam_sorted = new BufferedReader(new FileReader(
                            TabulationSettings.index_path
                            + OccurancesIndex.SORTED_FILENAME));

                    int value;
                    double longitude;
                    double latitude;
                    int species_number;
                    String species;
                    String s;
                    String[] line;
                    int idx;

                    ArrayList<SPLGridRecord> records = new ArrayList<SPLGridRecord>();
                    System.out.println("before read");
                    int p = 0;
                    while (sam_i_layerfile.getFilePointer() < sam_i_layerfile.length()
                            && (s = sam_sorted.readLine()) != null) {

                        line = s.split(",");
                        if (line.length > 2) {
                            species = line[line.length - 3].trim().toLowerCase();
                            value = sam_i_layerfile.readShort();
                            try {
                                longitude = Double.parseDouble(line[line.length - 2].trim());
                                latitude = Double.parseDouble(line[line.length - 1].trim());
                            } catch (Exception e) {
                                longitude = 0;
                                latitude = 0;
                            }

                            idx = Arrays.binarySearch(
                                    species_rank,
                                    new SPLSpeciesRecord(species, 0),
                                    new Comparator<SPLSpeciesRecord>() {
                                        public int compare(SPLSpeciesRecord r1, SPLSpeciesRecord r2) {
                                            return r1.name.compareTo(r2.name);
                                        }
                                    });

                            species_number = species_rank[idx].rank;

                            /* put it in a map to sort later */
                            records.add(new SPLGridRecord(value, /*longitude, latitude,*/ species_number, p));

                        }
                        p++;
                    }

                    System.out.println("after read");
                    java.util.Collections.sort(records,
                            new Comparator<SPLGridRecord>() {

                                public int compare(SPLGridRecord r1, SPLGridRecord r2) {
//                                    if (r2.value == r1.value) {
//                                        /* key generation & sorting here */
//                                        long key1 = r1.species_number
//                                                + ((((long) ((short) (r1.longitude * 100)))) << 32)
//                                                + ((((long) ((short) (r1.latitude * 100)))) << 48);
//
//                                        long key2 = r1.species_number
//                                                + ((((long) ((short) (r1.longitude * 100)))) << 32)
//                                                + ((((long) ((short) (r1.latitude * 100)))) << 48);
//
//                                        long result = key2 - key1;
//                                        if (result < 0L) {
//                                            return -1;
//                                        } else if (result > 0L) {
//                                            return 1;
//                                        } else {
//                                            return 0;
//                                        }
//                                    } else if (r1.value < r2.value) {
//                                        return -1;
//                                    }
//                                    return 1;
                                	if (r2.value == r1.value) {
                                		return r1.record_number - r2.record_number;
                                	 } else if (r1.value < r2.value) {
                                       return -1;
                                   }
                                   return 1;
                                }
                            });
                    System.out.println("after sort");

                    double last_value = 0;
                    RandomAccessFile output = null;

                   
                    int idxpos = 0;
                    int startpos = 0;
                    for (SPLGridRecord r : records) {
                        /*
                         * TODO: make nicer export
                         */
                        idxpos++;
                        if (last_value != r.value) {
                            String filename =
                                    TabulationSettings.index_path
                                    + "SPL_" + layer.name + "_"
                                    + last_value + ".dat";


                            if (idxpos != 0) {
                                //output
                                FileOutputStream fos = new FileOutputStream(filename);
                                BufferedOutputStream bos = new BufferedOutputStream(fos);
                                ObjectOutputStream oos = new ObjectOutputStream(bos);
                                ArrayList<RecordKey> set = new ArrayList<RecordKey>();

                                for (p = startpos; p < idxpos; p++) {
                                 /*   long key = records.get(p).species_number
                                            + ((((long) ((short) (records.get(p).longitude * 100)))) << 32)
                                            + ((((long) ((short) (records.get(p).latitude * 100)))) << 48);
*/
                                    set.add(new RecordKey(records.get(p).species_number,p));
                                }

                                java.util.Collections.sort(set,
                                		new Comparator<RecordKey>() {
		                                    public int compare(RecordKey r1, RecordKey r2) {
		                                    	return r1.key - r2.key;
		                                    }
                                		});
                                oos.writeObject(set);
                                oos.close();
                            }

                            last_value = r.value;
                            startpos = idxpos;
                        }
                    }

                    if (idxpos != 0) {
                        String filename =
                                TabulationSettings.index_path
                                + "SPL_" + layer.name + "_"
                                + last_value + ".dat";

                        //output
                        FileOutputStream fos = new FileOutputStream(filename);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        ArrayList<RecordKey> set = new ArrayList<RecordKey>();

                        for (p = startpos; p < idxpos; p++) {
                         /*   long key = records.get(p).species_number
                                    + ((((long) ((short) (records.get(p).longitude * 100)))) << 32)
                                    + ((((long) ((short) (records.get(p).latitude * 100)))) << 48);
*/
                            set.add(new RecordKey(records.get(p).species_number,p));
                        }

                        java.util.Collections.sort(set,
                        		new Comparator<RecordKey>() {
                                    public int compare(RecordKey r1, RecordKey r2) {
                                    	return r1.key - r2.key;
                                    }
                        		});

                        oos.writeObject(set);
                        oos.close();
                    }

                    sam_i_layerfile.close();
                    sam_sorted.close();

                    (new SpatialLogger()).log("makeSPL_CATAGORIES writing done > " + layer.name);

                } catch (Exception e) {
                    (new SpatialLogger()).log("makeSPL_CATAGORIES writing", ">" + layer.name + "> " + e.toString());
                    e.printStackTrace();
                }
            }
        }
    }
    
    

    static void loadRank() {
        /* only load once */
        if (species_rank == null) {
            try {
                FileInputStream fis = new FileInputStream(
                        TabulationSettings.index_path
                        + SPECIES_RANK_FILENAME);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis);
                species_rank = (SPLSpeciesRecord[]) ois.readObject();
                ois.close();
            } catch (Exception e) {
                (new SpatialLogger()).log("makeSPL_CATAGORIES, read rank", e.toString());
            }

            /* now copy & sort on species_rank */
            species_rank_order = species_rank.clone();

            java.util.Arrays.sort(species_rank_order,
                    new Comparator<SPLSpeciesRecord>() {

                        public int compare(SPLSpeciesRecord r1, SPLSpeciesRecord r2) {
                            return (int) (r1.rank - r2.rank);
                        }
                    });

            System.out.println("rank order");
            for (int k = 0; k < 100; k++) {
                System.out.print(species_rank_order[k].rank + ",");
            }
        }
    }  
    
    static private ArrayList<RecordKey> getGridSampleSet(SPLFilter filter){
    	/* grid/env filter, binary searching by seeking */

        double dblvalue;        
        
        ArrayList<RecordKey> set = new ArrayList<RecordKey>();

        try {
        	
        	long key;
            RandomAccessFile raf = new RandomAccessFile(
                    TabulationSettings.index_path
                    + "SPL_" + filter.layername + ".dat", "r");

            int length = (int) raf.length() / 12; 	//3xint
            int recordpos = length / 2;
            int step = recordpos / 2;
            while (step > 1) {

                raf.seek(recordpos * 12);				//3xint
                dblvalue = raf.readFloat();

                /* determine direction */
                if (dblvalue > filter.minimum_value) {
                    recordpos -= step;
                } else {
                    recordpos += step;
                }
                step /= 2;
            }
            /* roll back to before the list */
            raf.seek(recordpos * 12);					//3xint
            while (recordpos > 0 && raf.readFloat() > filter.minimum_value) {
                recordpos--;
                raf.seek(recordpos * 12);				//3xint
            }
            /* roll forwards to first record */
            raf.seek(recordpos * 12);					//3xint
            while (recordpos < length && raf.readFloat() < filter.minimum_value) {
                recordpos++;
                raf.seek(recordpos * 12);				//3xint
            }
            /* seek to actual position */
            raf.seek(recordpos * 12);					//3xint

            int start_recordpos = recordpos;
            double over_max = filter.maximum_value + 0.000001; //TODO fix this as an actual small value

            length = (int) raf.length() / 12; 	//3xint
            recordpos = length / 2;
            step = recordpos / 2;
            while (step > 1) {

                raf.seek(recordpos * 12);				//3xint
                dblvalue = raf.readFloat();

                /* determine direction */
                if (dblvalue > over_max) {
                    recordpos -= step;
                } else {
                    recordpos += step;
                }
                step /= 2;
            }
            /* roll back to before the list */
            raf.seek(recordpos * 12);					//3xint
            while (recordpos > 0 && raf.readFloat() > over_max) {
                recordpos--;
                raf.seek(recordpos * 12);				//3xint

            }
            /* roll forwards to first record */
            raf.seek(recordpos * 12);					//3xint
            while (recordpos < length && raf.readFloat() < over_max) {
                recordpos++;
                raf.seek(recordpos * 12);				//3xint
            }
            /* seek to actual position */
            raf.seek(recordpos * 12);					//3xint

            int end_recordpos = recordpos - 1;

            /* read between start_recordpos and end_recordpos */
            raf.seek(start_recordpos * 12);				//3xint
            byte[] records = new byte[(end_recordpos - start_recordpos + 1) * 12]; //add a little?

            raf.read(records);

            /* byte buffer */
            ByteBuffer bb = ByteBuffer.wrap(records);

            /* convert records to keys & add to system */
            long len = records.length/12;
            //float longitude, latitude;
            int record, species_number;
            int j;
            for (j = 0; j < len; j ++) {
                //skip first 4
                bb.getFloat();
                //longitude = bb.getFloat();
                //latitude = bb.getFloat();
                //key = bb.getInt()
                //        + ((((long) ((short) (longitude * 100)))) << 32)
                //        + ((((long) ((short) (latitude * 100)))) << 48);
                species_number = bb.getInt();
                record = bb.getInt();
                set.add(new RecordKey(species_number,record));
            }

            java.util.Collections.sort(set,
            		new Comparator<RecordKey>() {
                        public int compare(RecordKey r1, RecordKey r2) {
                        	return r1.key - r2.key;
                        }
            		});

            System.out.println(filter.layername
                    + ":" + filter.minimum_value + ":"
                    + filter.maximum_value + " finds=" + set.size());
            raf.close();
            
        } catch (Exception e) {
            (new SpatialLogger()).log("apply continous species list file",
                    "layer:" + filter.layername
                    + "> " + e.toString());
            e.printStackTrace();
        }
        
        return set;
        
    }

    static private ArrayList<RecordKey> getCatagorySampleSet(SPLFilter filter){
    	    	
    	ArrayList<RecordKey> set = new ArrayList<RecordKey>();
    	
    	/* catagory filter, iterate the whole file for each catagory */
    	int value;
        int k = 0;
        int j;
        for (j = 0; j < filter.catagories.length; j++) {
            value = filter.catagories[j];
            try {
                String filename =
                        TabulationSettings.index_path
                        + "SPL_" + filter.layername + "_"
                        + ((double) value) + ".dat";

                FileInputStream fos = new FileInputStream(filename);
                BufferedInputStream bos = new BufferedInputStream(fos);
                ObjectInputStream oos = new ObjectInputStream(bos);
               
                ArrayList<RecordKey> newset;
                newset = (ArrayList<RecordKey>) oos.readObject();
                System.out.println("got ctx " + filter.layername + " " + value + " " + newset.size());
                oos.close();

                if (set.size() > 0) {  
                	System.out.println("adding to set:" + newset.size());    	
                    set.addAll(newset);
                } else {
                	System.out.println("new set:" + newset.size());
                    set = newset;
                }
            } catch (Exception e) {
                (new SpatialLogger()).log("apply catagory species list file",
                        "layer:" + filter.layername + " cat:" + value
                        + ": species_mask_layer len:"
                        + " : " + k + ">" + e.toString());
            }
        }
        if(filter.catagories.length > 1){
        	java.util.Collections.sort(set,
            		new Comparator<RecordKey>() {
                        public int compare(RecordKey r1, RecordKey r2) {
                        	return r1.key - r2.key;
                        }
            		});
        }
        return set;
    }    
   
    ArrayList<SPLFilter> splfilters;
    String session_id;
    public void popFilter(){
    	if(splfilters != null && splfilters.size() > 0){
    		System.out.println("pop pre: " + splfilters.size());
    		splfilters.remove(splfilters.size()-1);
    		System.out.println("pop post: " + splfilters.size());
    	}
    }  
    public SPLFilter getTopFilter(){
    	if(splfilters != null && splfilters.size() > 0){
    		return splfilters.get(splfilters.size()-1);
    	}
    	return null;
    }
    public ArrayList<RecordKey> getTopRecordKey(){
    	if(splfilters.size() == 0){
    		return null;
    	}
    	 	
    	try{
    		FileInputStream fis = new FileInputStream(
    				TabulationSettings.index_path
	                + session_id + "_filter_" + splfilters.size());
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            ArrayList<RecordKey> rka =  (ArrayList<RecordKey>) ois.readObject();
            ois.close();
            
            return rka;
    	}catch(Exception e){
    		e.printStackTrace();
    	}  
    	
    	return null;
    }
    public void saveTopRecordKey(ArrayList<RecordKey> recordkey){
    	try{
	    	FileOutputStream fos = new FileOutputStream(
	                TabulationSettings.index_path
	                + session_id + "_filter_" + splfilters.size());
	        BufferedOutputStream bos = new BufferedOutputStream(fos);
	        ObjectOutputStream oos = new ObjectOutputStream(bos);
	        oos.writeObject(recordkey);
	        oos.close();
    	}catch(Exception e){    	
    		e.printStackTrace();
    	}
    }
    public void updateFilter(SPLFilter new_filter){
    	if(splfilters.size() == 0 ||
    			!splfilters.get(splfilters.size()-1).layer.name.equals(new_filter.layer.name)){
    		//add
    		
    		ArrayList<RecordKey> toprecordkey = getTopRecordKey();
    		if(splfilters.size() > 0){
    			System.out.println("# for: " + new_filter.layer.name + " from: " + splfilters.get(splfilters.size()-1).layer.name + " updating pre: " + splfilters.size() );
    		}
    		
    		splfilters.add(new_filter);
    		ArrayList<RecordKey> newrecordkey;
    		
    		if (new_filter.catagories == null) {
                newrecordkey = getGridSampleSet(new_filter);
            } else {
                newrecordkey = getCatagorySampleSet(new_filter);
            }    	
    		if(newrecordkey != null){
    			System.out.println("updateFilter(): " + newrecordkey.size() + " " + new_filter.layer.name);
    		}else{
    			System.out.println("updateFilter(): " + 0 + " " + new_filter.layer.name);
    		}
    		
    		//reduce size of newrecordkey
    		int i,j;
    		if(toprecordkey != null){
    			
    			ArrayList<RecordKey> output = new ArrayList<RecordKey>(toprecordkey.size());
    			
	    		for(i=0,j=0;i<toprecordkey.size() && j<newrecordkey.size();){
	    			if(toprecordkey.get(i).key < newrecordkey.get(j).key){
	    				//move forward
	    				i++;
	    			}else if(toprecordkey.get(i).key == newrecordkey.get(j).key){
	    				//save 
	    				output.add(toprecordkey.get(i));
	    				
	    				//increment both
	    				i++;
	    				while(i<toprecordkey.size() && toprecordkey.get(i).key == toprecordkey.get(i-1).key){
	    					i++;
	    				}
	    				j++;
	    				while(j<newrecordkey.size() && newrecordkey.get(j).key == newrecordkey.get(j-1).key){
	    					j++;
	    				}
	    			}else{
	    				//discard in newrecordkey
	    				j++;
	    			}
	    		}   
	    		
	    		newrecordkey = output;
    		}
    		if(newrecordkey != null){
    			System.out.println("$$$ found: " + newrecordkey.size());
    		}
    		saveTopRecordKey(newrecordkey);
    	}else{
    		System.out.println("*************** POPPING ******************");
    		//TODO: nicer handling
    		popFilter();
    		updateFilter(new_filter);
    	}
    }        
    
    /**
     * applyFilter
     * 
     * @param session_id unique session_id for persistant data
     * @param new_filter null to remove last filter
     * 			different layer filter to add filter
     * 			same layer filter to change filter
     */
    static public int applyFilter(String session_id_, SPLFilter new_filter){
    	SpeciesListIndex spi = SpeciesListIndex.getSession(session_id_);
    	
    	if(new_filter == null){
    		//remove top filter
    		spi.popFilter();   		
    	}else{
    		spi.updateFilter(new_filter);    		
    	}
    	
    	spi.save();
    	
    	return spi.splfilters.size();
    }
    
    static public int getLayerCount(String session_id_){
    	SpeciesListIndex spi = SpeciesListIndex.getSession(session_id_);
    	return spi.splfilters.size();
    }
    
    static public int getSpeciesCount(String session_id_){
    	loadRank();
    	
    	SpeciesListIndex spi = SpeciesListIndex.getSession(session_id_);
    	
    	ArrayList<RecordKey> rk = spi.getTopRecordKey();
    	if(rk == null){
    		return 0;
    	}
    	
    	BitSet species = new BitSet(species_rank.length + 1);
    	
    	int i;
    	
    	for(i=0;i<rk.size();i++){
    		species.set(rk.get(i).species);
    	}
    	int count = 0;
    	for(i=0;i<species.length();i++){
    		if(species.get(i)){
    			count++;
    		}
    	}
    	
    	return count;
    }
    
    static public String getSpeciesList(String session_id_){
    	loadRank();
    	
    	SpeciesListIndex spi = SpeciesListIndex.getSession(session_id_);
    	
    	ArrayList<RecordKey> rk = spi.getTopRecordKey();
    	
    	BitSet species = new BitSet(species_rank.length + 1);
    	
    	int i;
    	for(i=0;i<rk.size();i++){
    		species.set(rk.get(i).species);
    	}
    	int count = 0;
    	for(i=0;i<species.length();i++){
    		if(species.get(i)){
    			count++;
    		}
    	}
    	StringBuffer sb = new StringBuffer();
        for (i = 0; i < species.size(); i++) {
            if (species.get(i)) {
                sb.append(species_rank_order[i - 1].name); 	//species_number is 1..n

                sb.append("\r\n");
            }
        }
    	
    	return sb.toString();
    }
    
    static public SpeciesListIndex getSession(String session_id_){
    	//load existing
    	TabulationSettings.load();
    	
    	File f = new File(TabulationSettings.index_path
	                + session_id_ + "_spl");
    	
    	if(f.exists()){
	    	try{
	    		FileInputStream fis = new FileInputStream(
	    				TabulationSettings.index_path
		                + session_id_ + "_spl");
	            BufferedInputStream bis = new BufferedInputStream(fis);
	            ObjectInputStream ois = new ObjectInputStream(bis);
	            SpeciesListIndex spi =  (SpeciesListIndex) ois.readObject();
	            ois.close();
	            
	            return spi;
	    	}catch(Exception e){
	    		e.printStackTrace();
	    	}  
	    	return null;
    	}
	    	
    	//new
    	SpeciesListIndex spi = new SpeciesListIndex();
    	spi.session_id = session_id_;
    	spi.splfilters = new ArrayList<SPLFilter>();
    	
    	return spi;
    }
    
    public void save(){
    	//save this session
    	try{
	    	FileOutputStream fos = new FileOutputStream(
	                TabulationSettings.index_path
	                + session_id + "_spl");
	        BufferedOutputStream bos = new BufferedOutputStream(fos);
	        ObjectOutputStream oos = new ObjectOutputStream(bos);
	        oos.writeObject(this);
	        oos.close();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
    static public int [] getSpeciesGeoMask(SPLFilter[] filters, BitSet species_mask_all) {

        loadRank();

        int i;

        int j;
        int value;
        int species_number;
        long key;


        //make a set of long's
        ArrayList<RecordKey>[] samples_sets = new ArrayList[filters.length];

        for (i = 0; i < filters.length; i++) {
            System.out.println("## (" + i + ") free mem=" + java.lang.Runtime.getRuntime().freeMemory());
            if (filters[i].catagories == null) {
                samples_sets[i] = getGridSampleSet(filters[i]);
            } else {
                samples_sets[i] = getCatagorySampleSet(filters[i]);
            }
        }

        /* apply back to _all mask */
        RecordKey[] current_value = new RecordKey[filters.length];
        int [] current_pos = new int[filters.length];
        int mincount = Integer.MAX_VALUE;
        for (i = 0; i < filters.length; i++) {
            System.out.println(i + ": found latlongspecies:" + samples_sets[i].size());
            if (samples_sets[i].size() > 0) {
                current_value[i] = samples_sets[i].get(0);
            }
            if(mincount > samples_sets[i].size()){
            	mincount = samples_sets[i].size();
            }
            current_pos[i] = 0;
        }
        int counter = 0;
        key = 0;
        int record = 0;
        
        int [] records_found = new int[mincount];
        int records_pos = 0;

        boolean finished = false;
        while (!finished) {
            /* get minimum key */
            key = -1L;
            for (i = 0; i < filters.length; i++) {
                if (current_value[i].key != -1L && (key == -1L || current_value[i].key < key)) {
                    key = current_value[i].key;
             //       record = current_value[i].record;
                }
            }

            if (key == -1L) {
                /* done */
                finished = true;
            } else {
                /* expect all filters to have this key as their current value */
                boolean success = true;
                for (i = 0; i < filters.length; i++) {
                    if (current_value[i].key != key) {
                        success = false;
                    } else {
                        /* increment */
                        if (current_pos[i] < samples_sets[i].size()) {
                            current_value[i] = samples_sets[i].get(current_pos[i]);
                        } else {
                            current_value[i] = new RecordKey(-1,-1);
                        }
                    }
                }
                if (success) {
                    /* success */
                    species_number = new Long(key & 0x00000000ffffffffL).intValue(); //species number is in first 32bits of the long - hope this works

                    species_mask_all.set(species_number);
                    
                    records_found[records_pos++] = record;

                    counter++;
                }
            }
        }

        counter = 0;

        System.out.println("applied to mask, found: " + counter);
        return records_found;
    }
      
    static public String listSpeciesGeo(SPLFilter[] filters) {
        int i, j;
        
        loadRank();

        BitSet species_mask_all = new BitSet(species_rank.length + 1);
        
        int [] records_found = getSpeciesGeoMask(filters,species_mask_all);
        
        int species_number;
        /* convert species number to species name */
        StringBuffer output = new StringBuffer();
        int size = 0;
        for (i = 0; i < species_mask_all.size(); i++) {
            if (species_mask_all.get(i)) {
                species_number = i;

                output.append(species_rank_order[species_number - 1].name);
                //species_number is 1..n

                output.append("\r\n");
                size++;
            }
        }
        System.out.println("converted mask to species list of size " + size);

        return output.toString();
    }
    
    static public int [] listSpeciesGeoSamples(SPLFilter[] filters) {
        int i, j;
        
        loadRank();

        BitSet species_mask_all = new BitSet(species_rank.length + 1);
        
        int [] records_found = getSpeciesGeoMask(filters,species_mask_all);
        
        return records_found;
    }

    static public String[] listArraySpeciesGeo(SPLFilter[] filters) {
        int i, j;
        
        loadRank();

        BitSet species_mask_all = new BitSet(species_rank.length + 1);
        
        int [] records_found = getSpeciesGeoMask(filters,species_mask_all);
        
        //int species_number;

        int count = 0;
        for (i = 0; i < species_mask_all.size(); i++) {
            if (species_mask_all.get(i)) {
                count++;
            }
        }

        /* convert species number to species name */
        String[] output = new String[count];

        count = 0;
        for (i = 0; i < species_mask_all.size(); i++) {
            if (species_mask_all.get(i)) {
                output[count++] = species_rank_order[i - 1].name;
            }
        }

        System.out.println("converted mask to species list of size " + count);

        return output;
    }

    static public int listSpeciesCountGeo(SPLFilter[] filters) {
    	loadRank();

        BitSet species_mask_all = new BitSet(species_rank.length + 1);
        
        int [] records_found = getSpeciesGeoMask(filters,species_mask_all);
        
        int count = 0;
        int i, j;
        for (i = 0; i < species_mask_all.size(); i++) {
            if (species_mask_all.get(i)) {
                count++;
            }
        }

        System.out.println("listSpeciesCountGeo: " + count);
        return count;
    }

    public static SPLFilter getLayerFilter(String layer) {
    	return getLayerFilter(SamplingService.getLayer(layer));
    }
    
    public static SPLFilter getLayerFilter(Layer layer) {
        if (layer == null) {
            return null;
        }

        /* is it continous (grid file) */
        System.out.println("getLayerFilter: " + layer.name);

        File continous_file = new File(TabulationSettings.environmental_data_path
                + layer.name + ".gri");

        SPLFilter splfilter;        

        if (continous_file.exists()) {
            System.out.println("filename=" + continous_file.getName());
            try {
                Grid grid = new Grid(TabulationSettings.environmental_data_path
                        + layer.name);

                double minimum = grid.minval;
                double maximum = grid.maxval;

                return new SPLFilter(layer, null, null, minimum, maximum);

            } catch (Exception e) {
                //return nothing
                return null;
            }
        }

        /* otherwise, catagorical */

        /*
         * TODO: handle multiple fieldnames
         */
        System.out.println("layer.name:" + layer.name + ", fields:" + layer.fields);
        System.out.println("fields len:" + layer.fields.length);
        String fieldname = layer.fields[0].name;
        int i;

        File catagories_file = new File(
                TabulationSettings.index_path
                + SamplingIndex.CATAGORY_LIST_PREFIX
                + layer.name + "_" + fieldname
                + SamplingIndex.CATAGORY_LIST_POSTFIX);
        if (catagories_file.exists()) {
            byte[] data = new byte[(int) catagories_file.length()];
            try {
                FileInputStream fis = new FileInputStream(catagories_file);
                fis.read(data);
                fis.close();

                /* insert (row number) as beginning of each line */
                String str = new String(data);
                data = null;

                String[] catagory_names = str.split("\n");
                int[] catagories = new int[catagory_names.length];

                for (i = 0; i < catagories.length; i++) {
                    catagories[i] = i;
                }

                return new SPLFilter(layer, catagories, catagory_names, 0, 0);

            } catch (Exception e) {
                (new SpatialLogger()).log("getLayerExtents(" + layer.name + "), catagorical",
                        e.toString());
            }
        }


        return null;
    }

    public static String getLayerExtents(String layer_name) {
        /* is it continous (grid file) */
        System.out.println("getLayerExtends: " + layer_name);

        File continous_file = new File(
                TabulationSettings.index_path
                + "SPL_" + layer_name + ".dat");

        if (continous_file.exists()) {
            System.out.println("filename=" + continous_file.getName());
            try {
                Grid grid = new Grid(TabulationSettings.environmental_data_path
                        + layer_name);

                double minimum = grid.minval;
                double maximum = grid.maxval;

                return ((float) minimum) + " to " + ((float) maximum);
            } catch (Exception e) {
                //return nothing
                return "";
            }
        }

        /* otherwise, catagorical */

        /*
         * TODO: handle multiple fieldnames
         */
        String fieldname = "";
        int i;
        for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
            Layer l = TabulationSettings.geo_tables[i];
            if (l.name.equals(layer_name)) {
                fieldname = l.fields[0].name;
                break;
            }
        }
        File catagories_file = new File(
                TabulationSettings.index_path
                + SamplingIndex.CATAGORY_LIST_PREFIX
                + layer_name + "_" + fieldname
                + SamplingIndex.CATAGORY_LIST_POSTFIX);
        if (catagories_file.exists()) {
            byte[] data = new byte[(int) catagories_file.length()];
            try {
                FileInputStream fis = new FileInputStream(catagories_file);
                fis.read(data);
                fis.close();

                /* insert (row number) as beginning of each line */
                String str = new String(data);
                data = null;

                String[] lines = str.split("\n");
                str = null;

                StringBuffer output = new StringBuffer();

                /*
                 * TODO: add -1, missing
                 */
                int row = 0;					// 0..n indexing
                for (String s : lines) {
                    if (s.length() > 0) {
                        output.append(row + "," + s + "\r\n");
                        row = row + 1;
                    }
                }

                return output.toString().replace("\r\n", "<br>\r\n");
            } catch (Exception e) {
                (new SpatialLogger()).log("getLayerExtents(" + layer_name + "), catagorical",
                        e.toString());
            }
        }

        return "";
    }

//    /* make layer images */
//    public void makeScaledByteImagesFromGrids() {
//        /* ?? default long lat & dist */
//        double longitude_start = 112;
//        double longitude_end = 154;
//        double latitude_start = -51;//-44;
//        double latitude_end = -9;
//        int height = 256; //210 42/210
//        int width = 256; //252
//
//        int i, j;
//        Layer[] all_layers = new Layer[TabulationSettings.environmental_data_files.length
//                + TabulationSettings.geo_tables.length];
//        j = TabulationSettings.environmental_data_files.length;
//        for (i = 0; i < j; i++) {
//            all_layers[i] = TabulationSettings.environmental_data_files[i];
//        }
//        for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
//            all_layers[i + j] = TabulationSettings.geo_tables[i];
//        }
//        for (Layer l : all_layers) {
//
//            byte[] data;
//
//            if (l.type.equals("environmental")) {
//                data = getScaledBytesFromGrid(l.name, longitude_start, longitude_end, latitude_start, latitude_end, width, height);
//            } else {
//               // data = getScaledBytesFromDB(l, longitude_start, longitude_end, latitude_start, latitude_end, width, height);
//            	// data = getScaledBytesFromShape(l, longitude_start, longitude_end, latitude_start, latitude_end, width, height);
//            }
//
//            ArrayList<ImageByte> ib_list = new ArrayList<ImageByte>(data.length - 16);
//
//            //sort bytes?? keep ref to xy
//            for (i = 0; i < data.length - 16; i++) {
//                ib_list.add(new ImageByte(data[i], i / height, i % height));
//            }
//
//            java.util.Collections.sort(ib_list,
//                    new Comparator<ImageByte>() {
//
//                        public int compare(ImageByte i1, ImageByte i2) {
//                            int v = i1.value - i2.value;
//                            if (v == 0) {
//                                v = i1.x - i2.x;
//                                if (v == 0) {
//                                    v = i1.y - i2.y;
//                                }
//                            }
//                            return v;
//                        }
//                    });
//
//            /* x2 for 1byte (x,y) for each point
//             * +4 rows to cover 255 levels position starts, 2bytes each
//             */
//            BufferedImage image = new BufferedImage(width, height + 1,
//                    BufferedImage.TYPE_4BYTE_ABGR);
//
//            System.out.println("imgsize:" + width + "," + height + 1 + "," + ib_list.size());
//
//            /* valid when x & y are less than or equal to 255 (1byte)
//             *
//             */
//            int[] image_data = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
//                    null, 0, image.getWidth());
//
//            System.out.println("image_data.length=" + image_data.length);
//
//            int last_byte = ((int) ib_list.get(0).value) + 128;
//            int a, b;
//            int[] index = new int[255];
//            int end_of_image = width * height;
//            int new_last_byte;
//            for (i = 0; i < ib_list.size(); i++) {
//                if (((int) ib_list.get(i).value) + 128 != last_byte) {
//                    new_last_byte = ((int) ib_list.get(i).value) + 128;
//                    while (last_byte < new_last_byte) {
//                        last_byte++;
//                        image_data[last_byte + end_of_image] |= 0xff000000 | ((i * 0x00000001) /*<< (16*last_byte%2)*/);
//                    }
//
//                    a = (i / 256);
//                    b = i % 256;
//
//                    System.out.println("lb:" + (last_byte) + ", " + a + "x256 + " + b + ", i=" + i);
//                }
//
//                /* idx position, beginning on 4th row, remember width is x2 */
//                image_data[i] |= ((0xff000000 | (ib_list.get(i).x << 16) | (255 - ib_list.get(i).y))) /*<< (16*i%2)*/;
//            }
//
//            /*
//             * copy to the end of index
//             */
//            new_last_byte = 255;
//            while (last_byte < new_last_byte) {
//                last_byte++;
//                image_data[last_byte + end_of_image] |= (i * 0x00000001) /*<< (last_byte%2);*/;
//            }
//
//            // ?save the data?
//            try {
//                image.setRGB(0, 0, image.getWidth(), image.getHeight(),
//                        image_data, 0, image.getWidth());
//
//                ImageIO.write(image, "png",
//                        new File(TabulationSettings.index_path
//                        + "SPL_IMG_" + l.name + ".png"));
//
//
//
//            } catch (Exception e) {
//                System.out.println("image gen err: " + l.name + " " + e.toString());
//            }
//            //return; //done testing here
//        }
//    }	

    /* make layer images */
    public void makeAllScaledShortImages() {
        /* ?? default long lat & dist */
        double longitude_start = 112;
        double longitude_end = 154;
        double latitude_start = -51;//-44;
        double latitude_end = -9;
        int height = 210; //210 42/210
        int width = 252; //252

        int i, j;
        Layer[] all_layers = new Layer[TabulationSettings.environmental_data_files.length
                + TabulationSettings.geo_tables.length];
        j = TabulationSettings.environmental_data_files.length;        
        
        for (i = 0; i < j; i++) {
            all_layers[i] = TabulationSettings.environmental_data_files[i];
        }
        for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
            all_layers[i + j] = TabulationSettings.geo_tables[i];
        }
        
        for (Layer l : all_layers) {
            makeScaledShortImageFromGrid(l, longitude_start, longitude_end, latitude_start, latitude_end, width, height);
        }
    }

    /**
     * image output format, probably as:
     *
     * 1. shorts stream in (width*2 x height)
     * 2. index starting at (width*2xheigth) for (additional rows = 256*256/width)
     * 3. capped with one row width extents (TODO)
     *
     *
     * @param l
     * @param longitude_start
     * @param longitude_end
     * @param latitude_start
     * @param latitude_end
     * @param width
     * @param height
     */
    public void makeScaledShortImageFromGrid(Layer l, double longitude_start, double longitude_end,
            double latitude_start, double latitude_end, int width, int height) {

        short[] data;

        if (l.type.equals("environmental")) {
            data = getScaledShortFromGrid(l.name, longitude_start, longitude_end, latitude_start, latitude_end, width, height);
        } else {
           // data = getScaledShortFromDB(l, longitude_start, longitude_end, latitude_start, latitude_end, width, height);
        	 data = getScaledShortFromShape(l, longitude_start, longitude_end, latitude_start, latitude_end, width, height);        	 
        }

        ArrayList<ImageShort> ib_list = new ArrayList<ImageShort>(data.length - 16);

        int i;
        //sort bytes?? keep ref to xy
        int maxx = 0, maxy = 0 ,minx = 0, miny = 0, x, y;
        for (i = 0; i < data.length - 16; i++) {
        	x = i%width;
        	y = i/width;
        	if(i==0 || maxx < x) maxx = x;
        	if(i==0 || minx > x) minx = x;
        	if(i==0 || maxy < y) maxy = y;
        	if(i==0 || miny > y) miny = y;
            ib_list.add(new ImageShort(data[i], x, y));
        }
        System.out.println("*?* " + data.length + ">" + minx + " " + maxx + ", " + miny + " " + maxy);

        java.util.Collections.sort(ib_list,
                new Comparator<ImageShort>() {
                    public int compare(ImageShort i1, ImageShort i2) {
                        int v = i1.value - i2.value;
                        if (v == 0) {
                            v = i1.x - i2.x;
                            if (v == 0) {
                                v = i1.y - i2.y;
                            }
                        }
                        return v;
                    }
                });
        
       
        /* x2 for 1byte (x,y) for each point
         * +4 rows to cover 255 levels position starts, 2bytes each
         */
        BufferedImage image = new BufferedImage(width * 2, height + 2 + 256 * 256 / (width * 2),
                BufferedImage.TYPE_4BYTE_ABGR);

        System.out.println("imgsize:" + width * 2 + "," + height + 2 + 256 * 256 / (width * 2) + "," + ib_list.size());

        /* valid when x & y are less than or equal to 255 (1byte)
         *
         */
        int[] image_data = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());

        System.out.println("image_data.length=" + image_data.length);

        int last_byte = ((int) ib_list.get(0).value) + Short.MAX_VALUE;
        int short_range = Short.MAX_VALUE - Short.MIN_VALUE;
        int a, b;
        int[] index = new int[short_range - 1];
        int end_of_image = width * 2 * height;
        int new_last_byte;
        boolean firsttime = true;
        boolean firstcell = true;
        for (i = 0; i < ib_list.size(); i++) {
            if (((int) ib_list.get(i).value) + Short.MAX_VALUE != last_byte) {
                new_last_byte = ((int) ib_list.get(i).value) + Short.MAX_VALUE;
                while (last_byte < new_last_byte) {
                    last_byte++;
                    /**FIX**/
                    image_data[last_byte + end_of_image] |= 0xFF000000 | ((i * 0x00000001) /*<< (16*last_byte%2)*/);
                    if (firsttime) {
                        System.out.println("first index value at: " + last_byte + " is " + i);
                        firsttime = false;
                    }
                    if (l.type != "environmental") {
                        System.out.println("lb:" + (last_byte) + " i=" + i);
                    }
                }

                a = (i / short_range);
                b = i % short_range;

                //System.out.println("lb:" + (last_byte) + ", " + a + "x256x256 + " + b + ", i=" + i);
            }

            /* idx position, beginning on 4th row, remember width is x2 */
            /**FIX**/
            image_data[i * 2] = 0xFF000000 | ((ib_list.get(i).x * 0x00000001));
            image_data[i * 2 + 1] = 0xFF000000 | ((ib_list.get(i).y * 0x00000001));
        }


        /*
         * copy to the end of index
         */
        new_last_byte = short_range;
        while (last_byte < new_last_byte) {
            last_byte++;
            /**FIX**/
            image_data[last_byte + end_of_image] |= (i * 0x00000001) /*<< (last_byte%2);*/;
        }

        // ?save the data?
        try {

            /** FIX OUTPUT **/
            image.setRGB(0, 0, image.getWidth(), image.getHeight(),
                    image_data, 0, image.getWidth());

            ImageIO.write(image, "png",
                    new File(TabulationSettings.index_path
                    + "SPL_IMG_S_" + l.name + ".png"));

            /* also export sorted ImageShort */
            FileOutputStream fos = new FileOutputStream(
                    TabulationSettings.index_path
                    + "SPL_IMG_S_" + l.name + ".dat");
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);

            oos.writeObject(ib_list);
            oos.close();

        } catch (Exception e) {
            System.out.println("image gen err: " + l.name + " " + e.toString());
        }
        //return; //done testing here
    }

    /* really really temporary, needs lots of fixing */
    public static ImageShort[] getImageData(Layer l, double longitude_start, double longitude_end,
            double latitude_start, double latitude_end, int width, int height) {
        //make it, or get from cache (??)

        //just return something for now
        try {
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + "SPL_IMG_S_" + l.name + ".dat");
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            ArrayList<ImageShort> isa = (ArrayList<ImageShort>) ois.readObject();
            ImageShort[] is = new ImageShort[isa.size()];
            isa.toArray(is);
            ois.close();
            return is;
        } catch (Exception e) {
            (new SpatialLogger()).log("makeSPL_CATAGORIES, read rank", e.toString());
        }
        return null;
    }


    /* generate layer data on demand: return URL
     *
     * scale between 1 and 255, with 0 as missing data
     * */
    public byte[] getScaledBytesFromGrid(String layer_name,
            double longitude_start, double longitude_end,
            double latitude_start, double latitude_end,
            int longitude_steps, int latitude_steps) {
        byte[] data = new byte[longitude_steps * latitude_steps + 16];

        Grid grid = new Grid(
                TabulationSettings.environmental_data_path
                + layer_name);

        /* make points to interrogate */
        double[][] points = new double[longitude_steps * latitude_steps][2];
        for (int i = 0; i < longitude_steps; i++) {
            for (int j = 0; j < latitude_steps; j++) {
                points[i * latitude_steps + j][0] = longitude_start
                        + i / (double) longitude_steps * (longitude_end - longitude_start);
                points[i * latitude_steps + j][1] = latitude_start
                        + j / (double) latitude_steps * (latitude_end - latitude_start);
            }
        }

        /* get layer data */
        double[] values = grid.getValues(points);
        //for(double d : values){
        //	System.out.print("?" + d);
        //}
        points = null;  //reclaim space?

        if (values != null && values.length > 0) {
            System.out.println("got values size: " + values.length);
            int i;

            /* get max/min */
            double max = Double.NaN;
            double min = Double.NaN;
            for (i = 0; i < values.length; i++) {
                if (!Double.isNaN(values[i]) && (Double.isNaN(max) || max < values[i])) {
                    max = values[i];
                }
                if (!Double.isNaN(values[i]) && (Double.isNaN(min) || min > values[i])) {
                    min = values[i];
                }
            }

            System.out.println("min=" + min + ", max=" + max);

            /* copy values back to byte data */
            int countmissing = 0;
            short mm = 100;
            short mx = 0;
            for (i = 0; i < values.length; i++) {
                if (Double.isNaN(values[i])) {
                    data[i] = -128;
                } else {
                    data[i] = (byte) (((values[i] - min) / (double) (max - min) * 253 + 1) - 128);
                    if (data[i] < mm) {
                        mm = data[i];
                    }
                    if (data[i] > mx) {
                        mx = data[i];
                    }
                }
            }
            values = null; //finished with this
            System.out.println(mm + " to " + mx);
            /* return data */
            return data;
        }

        return null;
    }

    public short[] getScaledShortFromGrid(String layer_name,
            double longitude_start, double longitude_end,
            double latitude_start, double latitude_end,
            int longitude_steps, int latitude_steps) {
        short[] data = new short[longitude_steps * latitude_steps + 16];

        Grid grid = new Grid(
                TabulationSettings.environmental_data_path
                + layer_name);

        /* make points to interrogate */
        double[][] points = new double[longitude_steps * latitude_steps][2];
        for (int i = 0; i < longitude_steps; i++) {
            for (int j = 0; j < latitude_steps; j++) {
                points[i * latitude_steps + j][0] = longitude_start
                        + i / (double) longitude_steps * (longitude_end - longitude_start);
                points[i * latitude_steps + j][1] = latitude_start
                        + j / (double) latitude_steps * (latitude_end - latitude_start);
            }
        }

        /* get layer data */
        double[] values = grid.getValues(points);
        //for(double d : values){
        //	System.out.print("?" + d);
        //}
        points = null;  //reclaim space?

        if (values != null && values.length > 0) {
            System.out.println("got values size: " + values.length);
            int i;

            /* get max/min */
            double max = Double.NaN;
            double min = Double.NaN;
            for (i = 0; i < values.length; i++) {
                if (!Double.isNaN(values[i]) && (Double.isNaN(max) || max < values[i])) {
                    max = values[i];
                }
                if (!Double.isNaN(values[i]) && (Double.isNaN(min) || min > values[i])) {
                    min = values[i];
                }
            }

            System.out.println("min=" + min + ", max=" + max);

            /* copy values back to byte data */
            int countmissing = 0;
            int mm = 70000;
            int mx = 0;
            for (i = 0; i < values.length; i++) {
                if (Double.isNaN(values[i])) {
                    data[i] = Short.MIN_VALUE;
                } else {
                    data[i] = (short) (((values[i] - min) / (double) (max - min) * Short.MAX_VALUE - 1) + Short.MIN_VALUE);
                    if (data[i] < mm) {
                        mm = data[i];
                    }
                    if (data[i] > mx) {
                        mx = data[i];
                    }
                }
            }
            values = null; //finished with this
            System.out.println("layer range: " + mm + " to " + mx);
            /* return data */
            return data;
        }

        return null;
    }
//
//    public byte[] getScaledBytesFromDB(Layer l,
//            double longitude_start, double longitude_end,
//            double latitude_start, double latitude_end,
//            int longitude_steps, int latitude_steps) {
//
//        byte[] data = new byte[longitude_steps * latitude_steps + 16];
//
//        connectDatabase();
//        String query = "";
//
//        Statement s;
//        ResultSet r;
//
//        /* interrogate db for these points*/
//        double[] values = new double[longitude_steps * latitude_steps];
//        double lng, lat;
//        String tablename = l.name;
//        String fieldname = l.fields[0].name;
//        int pos;
//
//        SPLFilter filter = getLayerFilter(l);
//        for (String s1 : filter.catagory_names) {
//            System.out.println(s1);
//        }
//
//        for (int i = 0; i < longitude_steps; i++) {
//            for (int j = 0; j < latitude_steps; j++) {
//                pos = i * latitude_steps + j;
//
//                lng = longitude_start
//                        + i / (double) longitude_steps * (longitude_end - longitude_start);
//                lat = latitude_start
//                        + j / (double) latitude_steps * (latitude_end - latitude_start);
//
//                query = "select " + fieldname + " from "
//                        + tablename
//                        + " where ST_WITHIN(ST_MAKEPOINT("
//                        + lng + "," + lat + "),the_geom)";
//                try {
//                    s = connection.createStatement();
//
//                    r = s.executeQuery(query);
//
//                    /**
//                     * TODO: handle duplicate intersects (also border cases)
//                     */
//                    if (r.next()) {
//                        try {
//                            String ss = r.getString(1);
//
//                            int idx = 0;
//                            for (idx = 0; idx < filter.catagory_names.length; idx++) {
//                                if (filter.catagory_names[idx].equals(ss)) {
//                                    break;
//                                }
//                            }
//                            if (idx == filter.catagory_names.length) {
//                                idx = -1;
//                            }
//
//                            if (idx < 0) {
//                                System.out.print(ss + ">" + idx + ";");
//                            }
//                            if (idx >= 0) {
//                                values[pos] = idx;
//                            } else {
//                                values[pos] = Double.NaN;
//                            }
//                        } catch (Exception e) {
//                            values[pos] = Double.NaN;
//                        }
//                    } else {
//                        values[pos] = Double.NaN;
//                    }
//                } catch (Exception e) {
//                    values[pos] = Double.NaN;
//                }
//            }
//        }
//
//        if (values != null && values.length > 0) {
//            System.out.println("got values size: " + values.length);
//            int i;
//
//            /* copy values back to byte data */
//            for (i = 0; i < values.length; i++) {
//                if (Double.isNaN(values[i])) {
//                    data[i] = -128;
//                } else {
//                    data[i] = (byte) (values[i] - 127);
//                }
//            }
//            values = null; //finished with this
//
//            /* return data */
//            return data;
//        }
//
//        return null;
//    }
//
//    public short[] getScaledShortFromDB(Layer l,
//            double longitude_start, double longitude_end,
//            double latitude_start, double latitude_end,
//            int longitude_steps, int latitude_steps) {
//
//        short[] data = new short[longitude_steps * latitude_steps + 16];
//
//        connectDatabase();
//        String query = "";
//
//        Statement s;
//        ResultSet r;
//
//        /* interrogate db for these points*/
//        double[] values = new double[longitude_steps * latitude_steps];
//        double lng, lat;
//        String tablename = l.name;
//        String fieldname = l.fields[0].name;
//        int pos;
//
//        SPLFilter filter = getLayerFilter(l);
//        for (String s1 : filter.catagory_names) {
//            System.out.println(s1);
//        }
//
//        for (int i = 0; i < longitude_steps; i++) {
//            for (int j = 0; j < latitude_steps; j++) {
//                pos = i * latitude_steps + j;
//
//                lng = longitude_start
//                        + i / (double) longitude_steps * (longitude_end - longitude_start);
//                lat = latitude_start
//                        + j / (double) latitude_steps * (latitude_end - latitude_start);
//
//                query = "select " + fieldname + " from "
//                        + tablename
//                        + " where ST_WITHIN(ST_MAKEPOINT("
//                        + lng + "," + lat + "),the_geom)";
//                try {
//                    s = connection.createStatement();
//
//                    r = s.executeQuery(query);
//
//                    /**
//                     * TODO: handle duplicate intersects (also border cases)
//                     */
//                    if (r.next()) {
//                        try {
//                            String ss = r.getString(1);
//
//                            int idx = 0;
//                            for (idx = 0; idx < filter.catagory_names.length; idx++) {
//                                if (filter.catagory_names[idx].equals(ss)) {
//                                    break;
//                                }
//                            }
//                            if (idx == filter.catagory_names.length) {
//                                idx = -1;
//                            }
//
//                            if (idx < 0) {
//                                System.out.print(ss + ">" + idx + ";");
//                            }
//                            if (idx >= 0) {
//                                values[pos] = idx;
//                            } else {
//                                values[pos] = Double.NaN;
//                            }
//                        } catch (Exception e) {
//                            values[pos] = Double.NaN;
//                        }
//                    } else {
//                        values[pos] = Double.NaN;
//                    }
//                } catch (Exception e) {
//                    values[pos] = Double.NaN;
//                }
//            }
//        }
//
//        if (values != null && values.length > 0) {
//            System.out.println("got values size: " + values.length);
//            int i;
//
//            int mm = 0;
//            /* copy values back to byte data */
//            for (i = 0; i < values.length; i++) {
//                if (Double.isNaN(values[i])) {
//                    data[i] = Short.MIN_VALUE;
//                } else {
//                    data[i] = (short) (values[i] - Short.MIN_VALUE + 1);
//                    if (data[i] < mm) {
//                        mm = data[i];
//                    }
//                }
//            }
//            values = null; //finished with this
//
//            System.out.println("minimum idx value for catagorical layer: " + mm);
//            /* return data */
//            return data;
//        }
//
//        return null;
//    }
    
    public short[] getScaledShortFromShape(Layer l,
            double longitude_start, double longitude_end,
            double latitude_start, double latitude_end,
            int longitude_steps, int latitude_steps) {

        short[] data = new short[longitude_steps * latitude_steps + 16];
        
        SimpleShapeFile ssf = new SimpleShapeFile(
				TabulationSettings.environmental_data_path
				+ l.name);
        
        int column_idx = ssf.getColumnIdx(l.fields[0].name);
        
        short [][] mask = ssf.getShortMask(column_idx,longitude_start,latitude_start,longitude_end,latitude_end,longitude_steps,latitude_steps);
		
        int i,j;

        int mm = 0;
        /* copy values back to byte data */
        int p = 0;
        for(i=0;i<latitude_steps;i++){
			for(j=0;j<longitude_steps;j++){
				data[p++] = (short) (mask[i][j]);
	            if (data[p-1] < mm) {
	                mm = data[p-1];
	            }
			}
		}
        int [] count = new int[1000];
        for(i=0;i<p;i++){
        	count[data[i]+1]++;
        }
        System.out.println("counting numbers");
        for(i=0;i<1000;i++){
        	if(count[i] > 0){
        		System.out.print(" " + (i-1) + "#" + count[i]);
        	}
        }
           
        System.out.println("minimum idx value for catagorical layer: " + mm);
        
        /* return data */
        return data;
    }

//    public ImageByte[] getImageData(Layer layer) {
//        try {
//            FileInputStream fis = new FileInputStream(
//                    TabulationSettings.index_path
//                    + "SPL_IMG_" + layer.name + ".dat");
//
//            BufferedInputStream bis = new BufferedInputStream(fis);
//            ObjectInputStream ois = new ObjectInputStream(bis);
//            ImageByte[] ib = (ImageByte[]) ois.readObject();
//            ois.close();
//
//            return ib;
//
//        } catch (Exception e) {
//            System.out.println("getimagedata(" + layer.name + "):" + e.toString());
//        }
//        return null;
//    }

//    public int applyImageData(ImageByte[] ib, Integer min_idx, Integer max_idx, int[] img_bytes) {
//
//        /*	if(last_idx > 0 && max_idx. < ib.length && ib != null && ib.length > 0
//        && new_threshold >= 0 && new_threshold <= 256){
//        byte threshold = (byte)(new_threshold-128);
//        byte ct = ib[last_idx].value;
//
//        if(ct > threshold){
//        while(last_idx > 0){
//        img_bytes[]
//        }
//        }
//        }*/
//        return 0;
//    }

//    /**
//     * postgres db connection load
//     * @return
//     */
//    private boolean connectDatabase() {
//        try {
//            System.out.println(
//                    TabulationSettings.db_connection_string + " > "
//                    + TabulationSettings.db_username + " > "
//                    + TabulationSettings.db_password);
//
//
//            Class.forName("org.postgresql.Driver");
//
//            connection = DriverManager.getConnection(
//                    TabulationSettings.db_connection_string,
//                    TabulationSettings.db_username,
//                    TabulationSettings.db_password);
//
//        } catch (Exception e) {
//            (new SpatialLogger()).log("connectDatabase", e.toString());
//            return false;
//        }
//        return connection != null;
//    }
};
/*
class SPLGridRecord {
    public SPLGridRecord(double _value, double _longitude, double _latitude, int _species_number, int _record_number) {
        value = _value;
        longitude = _longitude;
        latitude = _latitude;
        species_number = _species_number;
        record_number = _record_number;
    }
    public double value;
    public double longitude;
    public double latitude;
    public int species_number;
    public int record_number;
};
*/
class SPLGridRecord extends Object {
    public SPLGridRecord(double _value, int _species_number, int _record_number) {
        value = _value;
      //  longitude = _longitude;
      // latitude = _latitude;
        species_number = _species_number;
        record_number = _record_number;
    }
    public double value;
   // public double longitude;
   // public double latitude;
    public int species_number;
    public int record_number;
};
class SPLSpeciesRecord implements Serializable {

    /**
     * default
     */
    private static final long serialVersionUID = 1L;

    public SPLSpeciesRecord(String _name, int _rank) {
        name = _name;
        rank = _rank;
    }
    public String name;
    public int rank;
}

//class ImageByte extends Object implements Serializable {
//
//    /**
//     * default
//     */
//    private static final long serialVersionUID = 1L;
//    public byte value;
//    public int x;
//    public int y;
//
//    public ImageByte(byte _value, int _x, int _y) {
//        value = _value;
//        x = _x;
//        y = _y;
//    }
//}

class SampleKey extends Object{
	public SampleKey(long key_, int record_, float longitude_, float latitude_){
		key = key_;
		record = record_;
		longitude = longitude_;
		latitude = latitude_;
	}
	public long key;
	public int record;
	public float longitude;
	public float latitude;
}

class RecordKey extends Object implements Serializable{
	/**
     * default
     */
    private static final long serialVersionUID = 1L;
    
	public RecordKey(int species_, int key_){
		species = species_;
		key = key_;
	}
	public int species;
	public int key;
}

