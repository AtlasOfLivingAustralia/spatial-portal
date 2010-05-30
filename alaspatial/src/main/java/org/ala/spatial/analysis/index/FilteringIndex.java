package org.ala.spatial.analysis.index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.ala.spatial.util.Grid;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.Tile;


/**
 * builder for species list index.
 *
 * requires OccurrencesIndex to be up to date.
 *
 * operates on GridFiles
 * operates on ShapeFiles
 *
 * TODO: incremental and partial update mechanism
 * 
 * @author adam
 *
 */
public class FilteringIndex extends Object implements AnalysisIndexService {

	/**
	 * length of a grid record in bytes
	 */
    static final int GRID_RECORD_LENGTH = 4 * 3 + 4; //3 floats + 1 int
    
    /**
     * length of a catagory record in bytes
     */
    static final int CATAGORY_RECORD_LENGTH = 4 * 2 + 4;//1 short + 2 floats + 1 int
    
    /**
     * mapping for LayerFilters copies, init since static
     */
    static Map<String, LayerFilter> map_layerfilters = new HashMap<String, LayerFilter>();
    
    /**
     * destination of loaded occurances
     */
    ArrayList<String[]> occurances;

    /**
     * default constructor
     */
    public FilteringIndex() {
    	/* load settings file */
        TabulationSettings.load();
    }

    /**
     * performs update of 'indexing' for all points data
     */
    public void occurancesUpdate() {
        makeSPL_GRID();

        makeSPL_CATAGORIES();
        
        /* for onscreen filtering server or client side */
        makeAllScaledShortImages();      
    }
    
    /**
     * performs update of 'indexing' for a new layer (grid or shapefile)
     *
     * @param layername name of the layer to update as String.  To update
     * all layers use null.
     */
    public void layersUpdate(String layername) {
    	/* placeholder for patial update mechanism */
    }

    /**
     * method to determine if the index is up to date
     *
     * @return true if index is up to date
     */
    public boolean isUpdated() {  
    	/* placeholder for patial update mechanism */
        return true;
    }

   
    
    /**
     * make species list index for environmental files
     * 
     * for each grid file intersect and export in points order
     * 
     * uses OccurrencesIndex generated SAM_D_ files
     */
    void makeSPL_GRID() {
        Layer layer;
        int i;
    	/* iterate for each environmental file */
    	for (i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
            layer = TabulationSettings.environmental_data_files[i];
            try {                    
                
            	/* open input file for this layer's values in order of records*/
                RandomAccessFile sam_d_gridfile = new RandomAccessFile(
                        TabulationSettings.index_path
                        + "SAM_D_" + layer.name + ".dat", "r");  
                
                /* open records */
                BufferedReader sam_sorted = new BufferedReader(new FileReader(
                        TabulationSettings.index_path
                        + OccurrencesIndex.SORTED_FILENAME));

                /* temporary variables */
                double value;
                int species_number;
                String species;
                String s;
                String[] line;
                int idx;
                ArrayList<SPLGridRecord> records = new ArrayList<SPLGridRecord>();
                
                int p = 0;			/* maintain original record index number 
                					 * for unqiueness 
                					 */
                
                /* load all valid values and add to records with species number and 
                 * original record index*/
                while (sam_d_gridfile.getFilePointer() < sam_d_gridfile.length()
                        && (s = sam_sorted.readLine()) != null) {

                	/* split up record line for extraction of:
                	 *  species name (2 before last value)
                	 *  longitude (before last value)
                	 *  latitude (last value)
                	 */
                    line = s.split(",");
                    if (line.length > 2) {                        	
                    	species = line[line.length - 3].trim().toLowerCase();
                        value = sam_d_gridfile.readFloat();

                        /* do not want NaN */
                        if (!Double.isNaN(value)) {
                            /* determine species name index value */
                            idx = Arrays.binarySearch(
                            		OccurrencesIndex.getSpeciesIndex(),
                                    new IndexedRecord(species, 0, 0, 0, 0, (byte)0),
                                    new Comparator<IndexedRecord>() {
                                        public int compare(IndexedRecord r1, IndexedRecord r2) {
                                            return r1.name.compareTo(r2.name);
                                        }                                      
                                    });

                            /* TODO: check that it is not negative */
                            species_number = idx;

                            /* put it in a map to sort later */
                            records.add(new SPLGridRecord(value, species_number,p));
                        }                            
                    }
                    p++;	//increment for next original record
                }
               
                /* sort records by environmental value */
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

                /* open output file and write 
                 * TODO: faster write method
                 */
                RandomAccessFile output = new RandomAccessFile(
                        TabulationSettings.index_path
                        + "SPL_" + layer.name + ".dat", "rw");

                for (SPLGridRecord r : records) {
                    output.writeFloat((float) r.value);
                    output.writeInt(r.species_number);
                    output.writeInt(r.record_number);
                }

                output.close();
                sam_d_gridfile.close();
                sam_sorted.close();

                (new SpatialLogger()).log("makeSPL_GRID writing done > " + layer.name);

            } catch (Exception e) {
                (new SpatialLogger()).log("makeSPL_GRID writing", ">" + layer.name + "> " + e.toString());
            }
        }
    
    }

    /**
     * make species list index for catagorical files
     * 
     * for each catagory file, split by field index value      
     * 
     */
    void makeSPL_CATAGORIES() {
        Layer layer;
        int i;
    	/* iterate for each shape file */
        for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
            layer = TabulationSettings.geo_tables[i];
            try {
            	/* open catagorical value file */
                RandomAccessFile sam_i_layerfile = new RandomAccessFile(
                        TabulationSettings.index_path
                        + SamplingIndex.CATAGORICAL_PREFIX + layer.name
                        + SamplingIndex.VALUE_POSTFIX, "r");
                
                /* open sorted records file */
                BufferedReader sam_sorted = new BufferedReader(new FileReader(
                        TabulationSettings.index_path
                        + OccurrencesIndex.SORTED_FILENAME));

                /* temporary variables */
                int value;
                int species_number;
                String species;
                String s;
                String[] line;
                int idx;
                ArrayList<SPLGridRecord> records = new ArrayList<SPLGridRecord>();
                
                int p = 0;			/* maintain original record index number 
									 * for unqiueness 
									 */
				
				/* load all valid values and add to records with species number and 
				 * original record index*/
                while (sam_i_layerfile.getFilePointer() < sam_i_layerfile.length()
                        && (s = sam_sorted.readLine()) != null) {

                	/* split up record line for extraction of:
                	 *  species name (2 before last value)
                	 *  longitude (before last value)
                	 *  latitude (last value)
                	 */
                    line = s.split(",");
                    if (line.length > 2) {
                        species = line[line.length - 3].trim().toLowerCase();
                        value = sam_i_layerfile.readShort();                            

                        /* get species name index value */
                        idx = Arrays.binarySearch(
                                OccurrencesIndex.getSpeciesIndex(),
                                new IndexedRecord(species, 0,0,0,0,(byte)0),
                                new Comparator<IndexedRecord>() {
                                    public int compare(IndexedRecord r1, IndexedRecord r2) {
                                        return r1.name.compareTo(r2.name);
                                    }
                                });

                        /* TODO: confirm not negative */
                        species_number = idx;

                        /* put it in a map to sort later */
                        records.add(new SPLGridRecord(value, species_number, p));
                    }
                    p++; 		//maintain original record number
                }

                /* sort by catagorical value */
                java.util.Collections.sort(records,
                        new Comparator<SPLGridRecord>() {
                            public int compare(SPLGridRecord r1, SPLGridRecord r2) {
                            	if (r2.value == r1.value) {
                            		return r1.record_number - r2.record_number;
                            	} else if (r1.value < r2.value) {
                                   return -1;
                            	}
                            	return 1;
                            }
                        });

                
                /* open output file and write */
                int idxpos = 0;
                int startpos = 0;
                
                /* iterate across records, exporting each group of records
                 * with the same value into a separate file with serialization
                 * 
                 * exports as species_number and original_sorted_index (key)
                 * 
                 * usage is whole of file
                 */
                int len = records.size();
                for (idxpos = 0; idxpos < len; idxpos++) {
                	if (idxpos+1 == len || records.get(idxpos).value != records.get(idxpos+1).value) {
						//output the previous group of records into a new file 
						  
					    String filename =
					            TabulationSettings.index_path
					            + "SPL_" + layer.name + "_"
					            + records.get(idxpos).value + ".dat";
					
				    
				    	/* open output stream */					    	
				        FileOutputStream fos = new FileOutputStream(filename);
				        BufferedOutputStream bos = new BufferedOutputStream(fos);
				        ObjectOutputStream oos = new ObjectOutputStream(bos);					        
				
				        /* build set */
				        ArrayList<SpeciesRecord> set = new ArrayList<SpeciesRecord>();
				        for (p = startpos; p <= idxpos; p++) {
				             set.add(new SpeciesRecord(records.get(p).species_number,
				            		 				records.get(p).record_number));
				        }
				
				        /* sort by record_number (key) */
				        java.util.Collections.sort(set,
				        		new Comparator<SpeciesRecord>() {
				                    public int compare(SpeciesRecord r1, SpeciesRecord r2) {
				                    	return r1.record - r2.record;
				                    }
				        		});
				        
				        /* write */
				        oos.writeObject(set);
				        oos.close();
					
					    /* preserve starting pos for end of next value */
					    startpos = idxpos+1;
					}
                }                 

                sam_i_layerfile.close();
                sam_sorted.close();

                (new SpatialLogger()).log("makeSPL_CATAGORIES writing done > " + layer.name);

            } catch (Exception e) {
                (new SpatialLogger()).log("makeSPL_CATAGORIES writing", ">" + layer.name + "> " + e.toString());
            }
        }        
    }
    
    /**
     * gets a default LayerFilter for a layer by layer name 
     * @param layer name of layer 
     * @return new default LayerFilter or null if does not exist
     */
    public static LayerFilter getLayerFilter(String layer) {
    	/* if template already loaded copy it */
    	LayerFilter layerfilter = map_layerfilters.get(layer);
    	if (layerfilter == null) {
    		layerfilter = getLayerFilter(Layers.getLayer(layer));
    		map_layerfilters.put(layer,layerfilter); 
    	} 
    	
    	/* if still not loaded, reutrn null */
    	//TODO: log error
    	if (layerfilter == null) {
    		return null;
    	}
    	
    	return layerfilter.copy();
    }
    
    /**
     * gets a default LayerFilter for a layer by layer object
     * 
     * note: use getLayerFilter(String) for public function
     * 
     * @param layer as Layer 
     * @return new default LayerFilter
     */
    private static LayerFilter getLayerFilter(Layer layer) {
        if (layer == null) {
            return null;
        }

        // check that the tabulationsettings is loaded.
        // if not, load it.
        if (!TabulationSettings.loaded) {
            TabulationSettings.load();
        }

        /* is it continous (grid file) */
        File continous_file = new File(TabulationSettings.environmental_data_path
                + layer.name + ".gri");
        if (continous_file.exists()) {
        	/* load grid file and fill layerfilter with min/max */
            try {
                Grid grid = new Grid(TabulationSettings.environmental_data_path
                        + layer.name);

                double minimum = grid.minval;
                double maximum = grid.maxval;

                return new LayerFilter(layer, null, null, minimum, maximum);

            } catch (Exception e) {
            	/* TODO: log error */
                return null;
            }
        }

        /* otherwise, catagorical */

        /*
         * TODO: handle multiple fieldnames
         */
        String fieldname = layer.fields[0].name;
        int i;

        File catagories_file = new File(
                TabulationSettings.index_path
                + SamplingIndex.CATAGORY_LIST_PREFIX
                + layer.name + "_" + fieldname
                + SamplingIndex.CATAGORY_LIST_POSTFIX);
        if (catagories_file.exists()) {
        	/* load catagories names and fill layerfilter with it */
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

                return new LayerFilter(layer, catagories, catagory_names, 0, 0);

            } catch (Exception e) {
                (new SpatialLogger()).log("getLayerExtents(" + layer.name + "), catagorical",
                        e.toString());
            }
        }

        return null;
    }

    /**
     * gets layer extents for a layer
     * 
     * TODO: improve this function, move to LayerService.java
     * 
     * @param layer_name layer name as String
     * @return formatted layer extents as String, 
     * - min and max for grid
     * - list of values for shape
     */
    public static String getLayerExtents(String layer_name) {
        /* is it continous (grid file) */
       File continous_file = new File(
                TabulationSettings.index_path
                + "SPL_" + layer_name + ".dat");
        if (continous_file.exists()) {
        	/* load grid file and get min/max */
            try {
                Grid grid = new Grid(TabulationSettings.environmental_data_path
                        + layer_name);

                double minimum = grid.minval;
                double maximum = grid.maxval;

                return ((float) minimum) + " to " + ((float) maximum);
            } catch (Exception e) {
            	/* log error */
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
        	/* load catagories values file and format */
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
    
    /**
     * gets list of SpeciesRecord (species number and record number) from
     * a layer filter (LayerFilter), grid/environmental/continous data source
     * @param filter layer filter as LayerFilter
     * @return records within the filtered region defined as ArrayList<RecordKey>
     */
    static public ArrayList<SpeciesRecord> getGridSampleSet(LayerFilter filter){
    	/* grid/env filter, binary searching by seeking */

        double dblvalue;        
        
        ArrayList<SpeciesRecord> set = new ArrayList<SpeciesRecord>();

        try {
        	/* open file */
            RandomAccessFile raf = new RandomAccessFile(
                    TabulationSettings.index_path
                    + "SPL_" + filter.layername + ".dat", "r");

            /* seek to first position */
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

            /* seek to last position */
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
                species_number = bb.getInt();
                record = bb.getInt();
                set.add(new SpeciesRecord(species_number,record));
            }

            /* sort by key (record number) */
            java.util.Collections.sort(set,
            		new Comparator<SpeciesRecord>() {
                        public int compare(SpeciesRecord r1, SpeciesRecord r2) {
                        	return r1.record - r2.record;
                        }
            		});

            raf.close();
            
        } catch (Exception e) {
            (new SpatialLogger()).log("apply continous species list file",
                    "layer:" + filter.layername
                    + "> " + e.toString());
        }
        
        return set;
        
    }

    
    /**
     * gets list of SpeciesRecord (species number and record number) from
     * a layer filter (LayerFilter), catagorical/contextual/shapefile data source
     * @param filter layer filter as LayerFilter
     * @return records within the filtered region defined as ArrayList<SpeciesRecord>
     */
	static public ArrayList<SpeciesRecord> getCatagorySampleSet(LayerFilter filter){
    	    	
    	ArrayList<SpeciesRecord> set = new ArrayList<SpeciesRecord>();
    	
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
               
                ArrayList<SpeciesRecord> newset;
                newset = (ArrayList<SpeciesRecord>) oos.readObject();
                oos.close();

                if (set.size() > 0) {  
                    set.addAll(newset);
                } else {
                    set = newset;
                }
            } catch (Exception e) {
                (new SpatialLogger()).log("apply catagory species list file",
                        "layer:" + filter.layername + " cat:" + value
                        + ": species_mask_layer len:"
                        + " : " + k + ">" + e.toString());
            }
        }
        if (filter.catagories.length > 1) {
        	java.util.Collections.sort(set,
            		new Comparator<SpeciesRecord>() {
                        public int compare(SpeciesRecord r1, SpeciesRecord r2) {
                        	return r1.record - r2.record;
                        }
            		});
        }
        return set;
    }  
	

    /**
     * makes indexed tiles for filtering by a region
     * 
     * TODO: dynamic
     */
    public void makeAllScaledShortImages() {
        /* ?? default long lat & dist */
        double longitude_start = 112;
        double longitude_end = 154;
        double latitude_start = -44;//-44;
        double latitude_end = -9;
        int height = 210; //210 42/210
        int width = 252; //252

        int i, j;
        
        /* get all layers */
        Layer[] all_layers = new Layer[TabulationSettings.environmental_data_files.length
                + TabulationSettings.geo_tables.length];
        j = TabulationSettings.environmental_data_files.length;         
        for (i = 0; i < j; i++) {
            all_layers[i] = TabulationSettings.environmental_data_files[i];
        }
        for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
            all_layers[i + j] = TabulationSettings.geo_tables[i];
        }
        
        /* process all layers */
        for (Layer l : all_layers) {
            makeScaledShortImageFromGrid(l, longitude_start, longitude_end, latitude_start, latitude_end, width, height);
        }
    }

  
    /**
     * filter tile creation...
     * 
     * more in docs now
     *
     * @param l	layer as Layer
     * @param longitude_start longitude extent as double
     * @param longitude_end other longitude extent as double
     * @param latitude_start latitude extent as double
     * @param latitude_end other latitude extent as double
     * @param width width resoluion as int
     * @param height height resolution as in 
     */
    public void makeScaledShortImageFromGrid(Layer l, double longitude_start, double longitude_end,
            double latitude_start, double latitude_end, int width, int height) {
    	
    	/* output data */
        Tile [] data;

        /* load raw */
        if (l.type.equals("environmental")) {
            data = getTileFromGrid(l.name, longitude_start, longitude_end, latitude_start, latitude_end, width, height);
        } else {
        	data = getTileFromShape(l, longitude_start, longitude_end, latitude_start, latitude_end, width, height);        	 
        }
        
        /* sort */
        java.util.Arrays.sort(data,
        		new Comparator<Tile>(){
        			public int compare(Tile i1, Tile i2){
        				if(i1.value_ < i2.value_){
        					return -1;
        				}else if(i1.value_ > i2.value_){
        					return 1;
        				}
        				return 0;
        			}
        		});
        
        /* index, only used for non-enviornmental layers */
        boolean has_index = true;           
        if (l.type.equals("environmental")) {
            has_index = false;
        } 
        int [] index = null;
        if (has_index) {
        	int i,j;
        	int max = (int)data[data.length-1].value_;
        	
        	index = new int[max+2];	  
        	int last_idx = 1;
        	for (i=1; i<data.length; i++) {
        		if (data[i].value_ != data[i-1].value_) {        	
        			for (j=last_idx; j<=data[i].value_; j++) {
        				index[j] = i;
        			}
        			last_idx = (int)data[i].value_+1;
        		}
        	}
        	index[max+1] = data.length;
        }
        
        /* write as object*/
        try {            
            FileOutputStream fos = new FileOutputStream(
                    TabulationSettings.index_path
                    + "SPL_IMG_T_" + l.name + ".dat");
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);            
            oos.writeObject(data);
            oos.writeObject(new Boolean(has_index));
            if (has_index) {
            	oos.writeObject(index);
            }
            oos.close();

        } catch (Exception e) {
            /* TODO: log error */
        }
    }

    /**
     * gets Tile data from a grid file onto specified extents
     * 
     * @param layer_name
     * @param longitude_start
     * @param longitude_end
     * @param latitude_start
     * @param latitude_end
     * @param longitude_steps
     * @param latitude_steps
     * @return Tile[]
     */
    public Tile [] getTileFromGrid(String layer_name,
            double longitude_start, double longitude_end,
            double latitude_start, double latitude_end,
            int longitude_steps, int latitude_steps) {
        
        Grid grid = new Grid(
                TabulationSettings.environmental_data_path
                + layer_name);

        /* make points to interrogate */
        double[][] points = new double[longitude_steps * latitude_steps][2];
        
       for (int j = 0; j < latitude_steps; j++) {
          	for (int i = 0; i < longitude_steps; i++) {
                points[j * longitude_steps + i][0] = longitude_start
                        + i / (double) (longitude_steps-1) * (longitude_end - longitude_start);
                points[j * longitude_steps + i][1] = latitude_end 
                        - j / (double) (latitude_steps-1) * (latitude_end - latitude_start);
            }
        }

        /* get layer data */
        double[] values = grid.getValues(points);

        if (values != null && values.length > 0) {
        	int i;

        	/* copy values back to byte data */
        	int countvalues = 0;
        	for(i=0;i<values.length;i++){
        		if(!Double.isNaN(values[i])){
        			countvalues++;
        		}
        	}
        	
        	Tile[] data = new Tile[countvalues];
        	
        	int p = 0;
        	for (i = 0; i < values.length; i++) {
        		if(!Double.isNaN(values[i])){
        			data[p++] = new Tile((float)values[i],i);
        		}
            }        	
        	
            /* return data */
            return data;
        }

        return null;
    }
    
    /**
     * get Tiles from shape file layer
     * 
     * @param l layer as Layer
     * @param longitude_start
     * @param longitude_end
     * @param latitude_start
     * @param latitude_end
     * @param longitude_steps
     * @param latitude_steps
     * @return Tile []
     */
    public Tile [] getTileFromShape(Layer l,
            double longitude_start, double longitude_end,
            double latitude_start, double latitude_end,
            int longitude_steps, int latitude_steps) {

        SimpleShapeFile ssf = new SimpleShapeFile(
				TabulationSettings.environmental_data_path
				+ l.name);
        
        int column_idx = ssf.getColumnIdx(l.fields[0].name);
        
        Tile [] data = ssf.getTileList(column_idx,longitude_start,latitude_start,longitude_end,latitude_end,longitude_steps,latitude_steps);
		
        return data;
     }

};

/**
 * grid data object, value, species number, record number
 * 
 * @author adam
 *
 */
class SPLGridRecord extends Object {
	/**
	 * record value
	 */
	public double value;
	  
	/**
	 * record species number
	 */
    public int species_number;
    
    /**
     * record number
     */
    public int record_number;
    
	/**
	 * constructor for SPLGridRecord
	 * 
	 * @param _value as double 
	 * @param _species_number as int
	 * @param _record_number as int
	 */
    public SPLGridRecord(double _value, int _species_number, int _record_number) {
        value = _value;     
        species_number = _species_number;
        record_number = _record_number;
    }    
};

/**
 * species record, name and ranking number
 * 
 * @author adam
 *
 */
class SPLSpeciesRecord implements Serializable {
	
	static final long serialVersionUID = -6084623663963314054L;
        
    /**
     * species name
     */
    public String name;
    
    /**
     * species ranking number
     */
    public int rank;

    /**
     * constructor 
     * @param _name as String
     * @param _rank as int
     */
    public SPLSpeciesRecord(String _name, int _rank) {
        name = _name;
        rank = _rank;
    }
}


