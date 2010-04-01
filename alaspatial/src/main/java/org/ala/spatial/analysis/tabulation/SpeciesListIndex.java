


package org.ala.spatial.analysis.tabulation;

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
public class SpeciesListIndex implements AnalysisIndexService {
	/* constants - TODO, do this nicely */
	static final int MAX_SPECIES_LIMIT = 1000000;
	static final int GRID_RECORD_LENGTH = 4*3+4; //3 floats + 1 int
	static final int CATAGORY_RECORD_LENGTH = 4*2 + 4;//1 short + 2 floats + 1 int
	static final String SPECIES_RANK_FILENAME = "SPL_SPECIES.dat";
	
	/**
	 * home for loaded species ranking
	 */
	static SPLSpeciesRecord [] species_rank = null;
	static SPLSpeciesRecord [] species_rank_order = null;
	
	/**
	 * destination of loaded occurances
	 */
	ArrayList<String []> occurances;
	
	/**
	 * default constructor	
	 */
	public SpeciesListIndex(){
		TabulationSettings.load();
	}
	
	/**
	 * performs update of 'indexing' for new points data
	 */
	public void occurancesUpdate(){	
		
		makeSPL_SPECIES();
		
		makeSPL_GRID();
		
		makeSPL_CATAGORIES();
		
		System.out.println("about to do makeScaledImagesFromGrids");
		makeScaledImagesFromGrids();
	}

	/**
	 * performs update of 'indexing' for a new layer (grid or shapefile)
	 * 
	 * @param layername name of the layer to update as String.  To update
	 * all layers use null.
	 */
	public void layersUpdate(String layername){
		
	}
	 
	/**
	 * method to determine if the index is up to date
	 * 
	 * @return true if index is up to date
	 */
	public boolean isUpdated(){
		return true;
	}
	
	void makeSPL_SPECIES(){
		//read SAM_IDX_SPECIES.csv into map, export and sort by rank
				
		/* load species and setup rank order by frequency */

		int [] rank_record = new int[1000000];//assumption of maximum number of species
		int j;
		for(j=0;j<rank_record.length;j++){
			rank_record[j] = 0;
		}
		
		int value;
	
		IndexedRecord [] species = OccurancesIndex.getSpeciesIndex();
					
		for(IndexedRecord r : species){
			value = r.record_end - r.record_start + 1;
			rank_record[value]++;
		}			
			
		int pos = 1; 				//first position
		int sum = 1; 				//total (placeholder)
		for(j=rank_record.length-1;j>=0;j--){
			if(rank_record[j] > 0){
				sum += rank_record[j];
				rank_record[j] = pos; 
				pos = sum;
			}
		}

		/* write with rank number as object */
		SPLSpeciesRecord [] sr = new SPLSpeciesRecord[species.length];
					
		int i = 0;
		int rank;
		for(IndexedRecord r : species){
			value = r.record_end - r.record_start + 1;
			rank = rank_record[value];
			rank_record[value]++;
			sr[i] = new SPLSpeciesRecord(r.name, rank);
			i++;
		}
		
		try{	
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
	        for(SPLSpeciesRecord r : sr){
	        	fw.append(r.name + "," + r.rank + "\r\n");
	        }
	        fw.close();
	        
	        (new SpatialLogger()).log("makeSPL_SPECIES done");
		}catch(Exception e){
			(new SpatialLogger()).log("makeSPL_SPECIES", e.toString());
		}
	}

	/**
	 * for each grid file intersect and export in points order
	 */
	void makeSPL_GRID(){
			
		/* species rank load */
		loadRank();
		
		Layer layer;
		int i;
		if(species_rank != null){
			for(i=0;i<TabulationSettings.environmental_data_files.length;i++){
				layer = TabulationSettings.environmental_data_files[i];
				try{
					System.out.println("makespl_grid: " +layer.name);
					RandomAccessFile sam_d_gridfile = new RandomAccessFile(
							TabulationSettings.index_path 
							+ "SAM_D_" + layer.name + ".dat","r");					
					BufferedReader sam_sorted = new BufferedReader(new FileReader(
							TabulationSettings.index_path
							+ OccurancesIndex.SORTED_FILENAME));
										
					double value;
					double longitude;
					double latitude;
					int species_number;
					String species;
					String s;
					String [] line;
					int idx;

					ArrayList<SPLGridRecord> records = new ArrayList<SPLGridRecord>();

					while(sam_d_gridfile.getFilePointer() < sam_d_gridfile.length()
						&& (s = sam_sorted.readLine()) != null) {
						
						line = s.split(",");
						if(line.length > 2){
							species = line[line.length-3].trim();
							value = sam_d_gridfile.readFloat();
							//eucalyptus caleyi 
							/* do not want NaN */
							if(!Double.isNaN(value)){
								try{
									longitude = Double.parseDouble(line[line.length-2].trim());
									latitude = Double.parseDouble(line[line.length-1].trim());
								}catch(Exception e){
									longitude = 0;
									latitude = 0;
								}
								
						//		if(species.equals("Eucalyptus caleyi")){
						//			//Eucalyptus caleyi,151.03,-28.03,18.416666,7
						//			System.out.println(longitude + "," + latitude + ":" + value);
									
						//		}
								
								idx = Arrays.binarySearch(
											species_rank,
											new SPLSpeciesRecord(species,0),
											new Comparator<SPLSpeciesRecord>() {
												public int compare(SPLSpeciesRecord r1, SPLSpeciesRecord r2){
													return r1.name.compareTo(r2.name);
												}
												public boolean equals(SPLSpeciesRecord r1, SPLSpeciesRecord r2){
													return r1.name.equals(r2);
												}
											});
								
								species_number = species_rank[idx].rank;
								
								/* put it in a map to sort later */
								records.add(new SPLGridRecord(value,longitude,latitude,species_number));
							}
														
						}
					}
			//		if(sam_d_gridfile.getFilePointer() >0 )
			//			return;
					
					java.util.Collections.sort(records,
							new Comparator<SPLGridRecord>() {
								public int compare(SPLGridRecord r1, SPLGridRecord r2){
									if(r2.value == r1.value){
										return 0;
									}else if(r1.value < r2.value){
										return -1;
									}
									return 1;
								}								
							}
						);
					
					RandomAccessFile output = new RandomAccessFile(
							TabulationSettings.index_path 
							+ "SPL_" + layer.name + ".dat","rw");					
			
					for(SPLGridRecord r : records){
						/* 
						 * TODO: make nicer export
						 */
						output.writeFloat((float)r.value);
						output.writeFloat((float)r.longitude);
						output.writeFloat((float)r.latitude);
						output.writeInt(r.species_number);
					}
				
					output.close();
					sam_d_gridfile.close();
					sam_sorted.close();
				
					(new SpatialLogger()).log("makeSPL_GRID writing done > " + layer.name);
					
				}catch(Exception e){
					(new SpatialLogger()).log("makeSPL_GRID writing",">" + layer.name + "> " + e.toString());
				}
			}
		}	
		System.out.println("finished makespl_grid");
	}	
	
	/**
	 * for each catagory file, split by field index value
	 */
	void makeSPL_CATAGORIES(){
			
		/* species rank load */
		SpeciesListIndex.loadRank();
			
		Layer layer;
		int i;
		if(species_rank != null){
			for(i=0;i<TabulationSettings.geo_tables.length;i++){
				layer = TabulationSettings.geo_tables[i];
				try{
					RandomAccessFile sam_i_layerfile = new RandomAccessFile(
							TabulationSettings.index_path
							+ SamplingIndex.CATAGORICAL_PREFIX + layer.name 
							+ SamplingIndex.VALUE_POSTFIX,"r");					
					BufferedReader sam_sorted = new BufferedReader(new FileReader(
							TabulationSettings.index_path
							+ OccurancesIndex.SORTED_FILENAME));

					int value;
					double longitude;
					double latitude;
					int species_number;
					String species;
					String s;
					String [] line;
					int idx;

					ArrayList<SPLGridRecord> records = new ArrayList<SPLGridRecord>();
System.out.println("before read");
					while(sam_i_layerfile.getFilePointer() < sam_i_layerfile.length()
						&& (s = sam_sorted.readLine()) != null) {
						
						line = s.split(",");
						if(line.length > 2){
							species = line[line.length-3].trim();
							value = sam_i_layerfile.readShort();
							try{
								longitude = Double.parseDouble(line[line.length-2].trim());
								latitude = Double.parseDouble(line[line.length-1].trim());
							}catch(Exception e){
								longitude = 0;
								latitude = 0;
							}
							
							idx = Arrays.binarySearch(
										species_rank,
										new SPLSpeciesRecord(species,0),
										new Comparator<SPLSpeciesRecord>() {
											public int compare(SPLSpeciesRecord r1, SPLSpeciesRecord r2){
												return r1.name.compareTo(r2.name);
											}
											public boolean equals(SPLSpeciesRecord r1, SPLSpeciesRecord r2){
												return r1.name.equals(r2);
											}
										});
							
							species_number = species_rank[idx].rank;
							
							/* put it in a map to sort later */
							records.add(new SPLGridRecord(value,longitude,latitude,species_number));
														
						}
					}

					System.out.println("after read");
					java.util.Collections.sort(records,
							new Comparator<SPLGridRecord>() {
							public int compare(SPLGridRecord r1, SPLGridRecord r2){
								if(r2.value == r1.value){
									/* key generation & sorting here */
									long key1 = r1.species_number 
										+((((long)((short)(r1.longitude*100)))) << 32)
										+((((long)((short)(r1.latitude*100)))) << 48);
									
									long key2 = r1.species_number 
										+((((long)((short)(r1.longitude*100)))) << 32)
										+((((long)((short)(r1.latitude*100)))) << 48);
									
									long result = key2 - key1;
									if(result < 0L){
										return -1;
									}else if(result > 0L){
										return 1;
									}else {
										return 0;
									}
								}else if(r1.value < r2.value){
									return -1;
								}
								return 1;
							}								
						});
					System.out.println("after sort");					
					
					double last_value = 0;
					RandomAccessFile output = null;
					
					/*for(SPLGridRecord r : records){
						/* 
						 * TODO: make nicer export
						 *
						if(last_value != r.value){
							if(output != null){
								output.close();
							}
							output = new RandomAccessFile(
									TabulationSettings.index_path
									+ "SPL_" + layer.name + "_" 
									+ r.value + ".dat"
									,"rw");
							last_value = r.value;
						}
						output.writeShort((short)r.value);
						output.writeFloat((float)r.longitude);
						output.writeFloat((float)r.latitude);
						output.writeInt(r.species_number);
					}
					if(output != null){
						output.close();
					}*/
					int idxpos = 0;
					int startpos = 0;
					for(SPLGridRecord r : records){
						/* 
						 * TODO: make nicer export
						 */
						idxpos++;
						if(last_value != r.value){
							String filename =
									TabulationSettings.index_path
									+ "SPL_" + layer.name + "_" 
									+ last_value + ".dat"
									;
							
							
							if(idxpos != 0){
								//output
								FileOutputStream fos = new FileOutputStream(filename);
						        BufferedOutputStream bos = new BufferedOutputStream(fos);
						        ObjectOutputStream oos = new ObjectOutputStream(bos);
						        TreeSet<Long> set = new TreeSet<Long>();
						        
						        for(int p=startpos;p<idxpos;p++){
						        	long key = records.get(p).species_number 
										+((((long)((short)(records.get(p).longitude*100)))) << 32)
										+((((long)((short)(records.get(p).latitude*100)))) << 48);
						        	
						        	set.add(new Long(key));
						        }
						       
						        oos.writeObject(set);
						        oos.close();
							}
							
							last_value = r.value;
							startpos = idxpos;							
						}
						//output.writeShort((short)r.value);
						//output.writeFloat((float)r.longitude);
						//output.writeFloat((float)r.latitude);
						//output.writeInt(r.species_number);
					}
					
					
					if(idxpos != 0){
						String filename =
							TabulationSettings.index_path
							+ "SPL_" + layer.name + "_" 
							+ last_value + ".dat"
							;
						
						//output
						FileOutputStream fos = new FileOutputStream(filename);
				        BufferedOutputStream bos = new BufferedOutputStream(fos);
				        ObjectOutputStream oos = new ObjectOutputStream(bos);
				        TreeSet<Long> set = new TreeSet<Long>();
				        
				        for(int p=startpos;p<idxpos;p++){
				        	long key = records.get(p).species_number 
								+((((long)((short)(records.get(p).longitude*100)))) << 32)
								+((((long)((short)(records.get(p).latitude*100)))) << 48);
				        	
				        	set.add(new Long(key));
				        }
				       
				        oos.writeObject(set);
				        oos.close();
					}
					
			        
			        
				
					
					sam_i_layerfile.close();
					sam_sorted.close();
				
					(new SpatialLogger()).log("makeSPL_CATAGORIES writing done > " + layer.name);
					
				}catch(Exception e){
					(new SpatialLogger()).log("makeSPL_CATAGORIES writing",">" + layer.name + "> " + e.toString());
				}
			}
		}				
	}	
	
	static void loadRank(){
		/* only load once */
		if(species_rank == null){
			try{
				FileInputStream fis =  new FileInputStream(
						TabulationSettings.index_path 
						+ SPECIES_RANK_FILENAME);
				BufferedInputStream bis = new BufferedInputStream(fis);
				ObjectInputStream ois = new ObjectInputStream(bis);
				species_rank = (SPLSpeciesRecord[]) ois.readObject();
				ois.close();
			}catch(Exception e){
				(new SpatialLogger()).log("makeSPL_CATAGORIES, read rank", e.toString());
			}
			
			/* now copy & sort on species_rank */
			species_rank_order = species_rank.clone();
			
			java.util.Arrays.sort(species_rank_order,
					new Comparator<SPLSpeciesRecord>() {
						public int compare(SPLSpeciesRecord r1, SPLSpeciesRecord r2){
							return (int)(r1.rank - r2.rank);
						}						
					}
				);
			
			System.out.println("rank order");
			for(int k=0;k<100;k++){
				System.out.print( species_rank_order[k].rank + ",");
			}
		}
	}
	
	static public int listSpeciesCount(SPLFilter [] filters){
		
		int [] species_mask_all = getSpeciesMask(filters);
		int count = 0;
		int i,j;
		for(i=0;i<species_mask_all.length;i++){
			if(species_mask_all[i] > 0){
				for(j=0;j<32;j++){
					if((species_mask_all[i] & (0x1<<j)) > 0){
						count++;
					}
				}
			}
		}
			
		return count;
	}
	static public String listSpecies(SPLFilter [] filters){
			
		int [] species_mask_all = getSpeciesMask(filters);
		
		/* convert species number to species name */
		int species_number;
		StringBuffer output = new StringBuffer();
		int i,j;
		for(i=0;i<species_mask_all.length;i++){
			if(species_mask_all[i] > 0){
				for(j=0;j<32;j++){
					if((species_mask_all[i] & (0x1<<j)) > 0){
						species_number = i*32 + j;						
						
						output.append(species_rank_order[species_number-1].name);
							//species_number is 1..n
						
						output.append("\r\n");
					}
				}
			}
		}
		
		return output.toString();
	}
	
	static public boolean [] getSpeciesGeoMask(SPLFilter [] filters){
		
		loadRank();
		
		/* setup default species record (32bit) */
//	int [] species_mask_all = new int[species_rank.length/32+1];
		boolean [] species_mask_all2 = new boolean[species_rank.length+1];
		
		int i;
		
		int j;
		int value;
		int species_number;
		long key;
		
		//species_mask_layer = new int[species_rank.length/32+1];	//assume all false
		
		//make a set of long's
		//ArrayList<Long> samples_set = new ArrayList<Long>(200000);
		Set<Long> [] samples_sets = new Set[filters.length];
						
		for(i=0;i<filters.length;i++){
			samples_sets[i] = new TreeSet<Long>();
			
			if(filters[i].catagories == null){
				/* grid/env filter, binary searching by seeking */
				
				double dblvalue;
				int finds = 0;
				
				try{
					RandomAccessFile raf = new RandomAccessFile(
							TabulationSettings.index_path
							+ "SPL_" + filters[i].layername + ".dat","r");
					
					int length = (int)raf.length() / 16; 	//3xfloat + 1xint
					int recordpos = length/2;
					int step = recordpos/2;
					while(step > 1){

						raf.seek(recordpos*16);				//3xfloat + 1xint
						dblvalue = raf.readFloat();
						
						/* determine direction */
						if(dblvalue > filters[i].minimum_value){
							recordpos -= step;
						}else{
							recordpos += step;
						}
						step /= 2;
					}
					/* roll back to before the list */
					raf.seek(recordpos*16);					//3xfloat + 1xint
					while(recordpos > 0 && raf.readFloat() > filters[i].minimum_value){
						recordpos--;
						raf.seek(recordpos*16);				//3xfloat + 1xint					
					}
					/* roll forwards to first record */
					raf.seek(recordpos*16);					//3xfloat + 1xint
					while(recordpos < length && raf.readFloat() < filters[i].minimum_value){
						recordpos++;
						raf.seek(recordpos*16);				//3xfloat + 1xint
					}
					/* seek to actual position */
					raf.seek(recordpos*16);					//3xfloat + 1xint	
					
					int start_recordpos = recordpos;
					double over_max = filters[i].maximum_value + 0.000001; //TODO fix this as an actual small value
					
					length = (int)raf.length() / 16; 	//3xfloat + 1xint
					recordpos = length/2;
					step = recordpos/2;
					while(step > 1){

						raf.seek(recordpos*16);				//3xfloat + 1xint
						dblvalue = raf.readFloat();
						
						/* determine direction */
						if(dblvalue > over_max){
							recordpos -= step;
						}else{
							recordpos += step;
						}
						step /= 2;
					}
					/* roll back to before the list */
					raf.seek(recordpos*16);					//3xfloat + 1xint
					while(recordpos > 0 && raf.readFloat() > over_max){
						recordpos--;
						raf.seek(recordpos*16);				//3xfloat + 1xint
					
					}
					/* roll forwards to first record */
					raf.seek(recordpos*16);					//3xfloat + 1xint
					while(recordpos < length && raf.readFloat() < over_max){
						recordpos++;
						raf.seek(recordpos*16);				//3xfloat + 1xint
					}
					/* seek to actual position */
					raf.seek(recordpos*16);					//3xfloat + 1xint	
					
					int end_recordpos = recordpos-1;
					
					/* read between start_recordpos and end_recordpos */
					raf.seek(start_recordpos*16);
					byte [] records = new byte[(end_recordpos - start_recordpos + 1)*16+4]; //add a little?
					
					raf.read(records);
					
					/* byte buffer */
					ByteBuffer bb = ByteBuffer.wrap(records);
					
					/* convert records to keys & add to system */
					long len = records.length*16;
					float longitude, latitude;
					
					for(j=0;j<len;j+=16){
						//skip first 4
						bb.getFloat();
						longitude = bb.getFloat();
						latitude = bb.getFloat();
						species_number = bb.getInt();
						
						key = species_number 
							+((((long)((short)(longitude*100)))) << 32)
							+((((long)((short)(latitude*100)))) << 48);
						samples_sets[i].add(new Long(key));
					}
					
					
				/*	
					while(raf.getFilePointer() < raf.length()){
						dblvalue = raf.readFloat();
						
						float longitude = raf.readFloat();	//longitude
						float latitude = raf.readFloat();	//latitude
						
						species_number = raf.readInt(); 		//species number						
						
						
						key = species_number 
							+((((long)((short)(longitude*100)))) << 32)
							+((((long)((short)(latitude*100)))) << 48);
						
						if(dblvalue <= filters[i].maximum_value &&
								dblvalue >= filters[i].minimum_value){
							samples_sets[i].add(new Long(key));
							finds++;
						}
						else if(dblvalue > filters[i].maximum_value){
							//done
							System.out.println("breaking: " + dblvalue + ">" 
									+ filters[i].maximum_value);
							break;
						}
					}*/
					System.out.println(filters[i].layername + 
							":" + filters[i].minimum_value + ":" + 
							filters[i].maximum_value + " finds=" + finds);
					raf.close();
				}catch (Exception e){
					(new SpatialLogger()).log("apply continous species list file",
							"layer:" + filters[i].layername 
							+ "> " + e.toString());
				}
				
			}else {
				/* catagory filter, iterate the whole file for each catagory */
				species_number = -1;
				int k = 0;
				for(j=0;j<filters[i].catagories.length;j++){
					value = filters[i].catagories[j];
					try{
						String filename=
								TabulationSettings.index_path
								+ "SPL_" + filters[i].layername + "_" 
								+ ((double)value) + ".dat"
								;
						
						FileInputStream fos = new FileInputStream(filename);
				        BufferedInputStream bos = new BufferedInputStream(fos);
				        ObjectInputStream oos = new ObjectInputStream(bos);
				        TreeSet<Long> set;
				        
				       				       
				        set = (TreeSet<Long>) oos.readObject();
				        System.out.println("got ctx " + filters[i].layername + " " + value + " " +  set.size());
				        oos.close();
				        
				        if(samples_sets[i].size() > 0){
				        	samples_sets[i].addAll(set);
				        }else{
				        	samples_sets[i] = set;
				        }
				        
				        /*
						k = 0;
						while(raf.getFilePointer() < raf.length()){
							k++;
							/* TODO: region filtering 
							raf.readShort();	//catagory number
							float longitude = raf.readFloat();	//longitude
							float latitude = raf.readFloat();	//latitude
							
							species_number = raf.readInt(); 		//species number
							
							
							key = species_number 
							+((((long)((short)(longitude*100)))) << 32)
							+((((long)((short)(latitude*100)))) << 48);
							
						//	if(species_rank_order[species_number-1].name.equals("Eucalyptus caleyi")){
								//Eucalyptus caleyi,151.03,-28.03,18.416666,7
							//	System.out.println(longitude + "," + latitude + ":" + cat + ">" + Long.toString(key));
								
							//}
							
							samples_sets[i].add(new Long(key));
						}
						raf.close();*/
					}catch (Exception e){
						(new SpatialLogger()).log("apply catagory species list file",
								"layer:" + filters[i].layername + " cat:" + value 
								+ "> " + species_number + ": species_mask_layer len:" 
								
								+" : " + k + ">" + e.toString());
					}
				}				
			}
		}	
			
		/* apply back to _all mask */
		long [] current_value = new long[filters.length];
		Iterator<Long> [] its = new Iterator[filters.length];
		for(i=0;i<filters.length;i++){
			System.out.println(i + ": found latlongspecies:" + samples_sets[i].size());
			its[i] = samples_sets[i].iterator();
			if(its[i].hasNext()){
				current_value[i] = its[i].next().longValue();
			}
		}
		int counter = 0;		
		key = 0;		
		
		boolean finished = false;
		while(!finished){
			/* get minimum key */
			key = -1L;
			for(i=0;i<filters.length;i++){
				if(current_value[i] != -1L && (key == -1L || current_value[i] < key)){
					key = current_value[i];
				}
			}
//			if(filters.length > 1){
//				System.out.println(Long.toString(key) + "^" + Long.toString(current_value[0]) + " & " + Long.toString(current_value[1]));
//			}else{
//				System.out.println(Long.toString(key) + "^" + Long.toString(current_value[0]));
//			}
			if(key == -1L){
				/* done */
				finished = true;
			}else{
				/* expect all filters to have this key as their current value */
				boolean success = true;
				for(i=0;i<filters.length;i++){
					if(current_value[i] != key){
//						System.out.println("X " + Long.toString(key) + "^" + Long.toString(current_value[0]));
						success = false;
					}else{
						/* increment */
						if(its[i].hasNext()){
							current_value[i] = its[i].next().longValue();
						}else{
							current_value[i] = -1L;
						}
					}
				}
				if(success){
					/* success */
					species_number = new Long(key & 0x00000000ffffffffL).intValue(); //species number is in first 32bits of the long - hope this works
	//				System.out.println(Long.toString(key) + "," + Long.toString(key & 0x00000000ffffffffL) + "," + species_number);
	//				species_mask_all[species_number/32] |= 0x1 << (species_number%32);
					species_mask_all2[species_number] = true;
					counter++;
				//	if(species_rank_order[species_number-1].name.equals("Eucalyptus caleyi")){
						//Eucalyptus caleyi,151.03,-28.03,18.416666,7
				//		System.out.println("key="+Long.toString(key));						
				//	}
				}				
			}		
		}
		
		counter = 0;
	
	
		
		System.out.println("applied to mask, found: " + counter);
		return species_mask_all2;
	}
	
	static public String listSpeciesGeo(SPLFilter [] filters){
		int i;
		boolean [] species_mask_all = getSpeciesGeoMask(filters);
		int species_number;
		/* convert species number to species name */
		StringBuffer output = new StringBuffer();
		int size = 0;
		for(i=0;i<species_mask_all.length;i++){
			if(species_mask_all[i]){
						species_number = i;
						
						output.append(species_rank_order[species_number-1].name);
							//species_number is 1..n
						
						output.append("\r\n");
						size++;
				
			}
		}
		System.out.println("converted mask to species list of size " + size);
		
		return output.toString();
	}
	
	static public String[] listArraySpeciesGeo(SPLFilter [] filters){
		int i;
		boolean [] species_mask_all = getSpeciesGeoMask(filters);
		int species_number;
		
		int count = 0;
		for(i=0;i<species_mask_all.length;i++){
			if(species_mask_all[i]){
				count++;				
			}
		}
		
		/* convert species number to species name */
		String [] output = new String[count];
		
		count = 0;
		for(i=0;i<species_mask_all.length;i++){
			if(species_mask_all[i]){
				output[count++] = species_rank_order[i-1].name;				
			}
		}
			
		System.out.println("converted mask to species list of size " + count);
		
		return output;
	}
	
	
	static public int listSpeciesCountGeo(SPLFilter [] filters){
		boolean [] species_mask_all = getSpeciesGeoMask(filters);
		int count = 0;
		int i,j;
		for(i=0;i<species_mask_all.length;i++){
			if(species_mask_all[i]){				
				count++;			
			}
		}
			
		System.out.println("listSpeciesCountGeo: " + count);
		return count;
	}
	
	static public int[] getSpeciesMask(SPLFilter [] filters){
		loadRank();
			
		/* setup default species record (32bit) */
		int [] species_mask_all = new int[species_rank.length/32+1];
		int [] species_mask_layer;	
		
		int i;
		byte [] griddata = new byte[GRID_RECORD_LENGTH];
		byte [] catagorydata = new byte[CATAGORY_RECORD_LENGTH];
		
		int j;
		int value;
		int species_number;
		
		species_mask_layer = new int[species_rank.length/32+1];	//assume all false
		
		for(i=0;i<filters.length;i++){
			
			for(j=0;j<species_mask_layer.length;j++){
				species_mask_layer[j] = 0;
			}
			
			if(filters[i].catagories == null){
				/* grid/env filter, binary searching by seeking */
				
				/*
				 * TODO: binary seeking
				 */
				double dblvalue;
				int finds = 0;
				
				try{
					RandomAccessFile raf = new RandomAccessFile(
							TabulationSettings.index_path
							+ "SPL_" + filters[i].layername + ".dat","r");
					
					//export as is to see it?
		/*	while(raf.getFilePointer() < raf.length()){
				System.out.println(raf.readFloat() + "," + raf.readFloat() 
						+ "," + raf.readFloat() + "," + raf.readInt());
			}*/
					/* seeking to start */
					int length = (int)raf.length() / 16; 	//3xfloat + 1xint
	//		System.out.println("length:" + raf.length() + " records:" + length);
					int recordpos = length/2;
					int step = recordpos/2;
					while(step > 1){

						raf.seek(recordpos*16);				//3xfloat + 1xint
						dblvalue = raf.readFloat();
						
		System.out.println("dbl:" + dblvalue + " record:" + recordpos + " step:" + step);
		
						/* determine direction */
						if(dblvalue > filters[i].minimum_value){
							recordpos -= step;
						}else{
							recordpos += step;
						}
						step /= 2;
					}
					/* roll back to before the list */
					raf.seek(recordpos*16);					//3xfloat + 1xint
					while(recordpos > 0 && raf.readFloat() > filters[i].minimum_value){
						recordpos--;
						raf.seek(recordpos*16);				//3xfloat + 1xint
					
					}
					/* roll forwards to first record */
					raf.seek(recordpos*16);					//3xfloat + 1xint
					while(recordpos < length && raf.readFloat() < filters[i].minimum_value){
						recordpos++;
						raf.seek(recordpos*16);				//3xfloat + 1xint
					}
					/* seek to actual position */
					raf.seek(recordpos*16);					//3xfloat + 1xint					
					
					while(raf.getFilePointer() < raf.length()){
						dblvalue = raf.readFloat();
						
		//				System.out.println(dblvalue);
						
						/* TODO: region filtering */
						float longitude = raf.readFloat();	//longitude
						float latitude = raf.readFloat();	//latitude
						
						species_number = raf.readInt(); 		//species number
						
						if(dblvalue <= filters[i].maximum_value &&
								dblvalue >= filters[i].minimum_value){
							species_mask_layer[species_number/32] |= 0x1 << (species_number%32);
							
				/*			System.out.println("$" + dblvalue + "," 
									+ longitude + "," + latitude + "," + species_number 
									+ species_rank_order[species_number].name  + ":"
									+ species_rank_order[species_number].rank + ","
									+ species_rank_order[species_number-1].name
									+ species_rank_order[species_number-1].rank );*/
							finds++;
						}
						else if(dblvalue > filters[i].maximum_value){
							/* done */
							System.out.println("breaking: " + dblvalue + ">" 
									+ filters[i].maximum_value);
							break;
						}
					}
					System.out.println(filters[i].layername + 
							":" + filters[i].minimum_value + ":" + 
							filters[i].maximum_value + " finds=" + finds);
					raf.close();
				}catch (Exception e){
					(new SpatialLogger()).log("apply continous species list file",
							"layer:" + filters[i].layername 
							+ "> " + e.toString());
				}
				
			}else {
				/* catagory filter, iterate the whole file for each catagory */
				species_number = -1;
				int k = 0;
				for(j=0;j<filters[i].catagories.length;j++){
					value = filters[i].catagories[j];
					try{
						RandomAccessFile raf = new RandomAccessFile(
								TabulationSettings.index_path
								+ "SPL_" + filters[i].layername + "_" 
								+ ((double)value) + ".dat"
								,"r");	
						k = 0;
						while(raf.getFilePointer() < raf.length()){
							k++;
							/* TODO: region filtering */
							raf.readShort();	//catagory number
							raf.readFloat();	//longitude
							raf.readFloat();	//latitude
							
							species_number = raf.readInt(); 		//species number
							
							species_mask_layer[species_number/32] |= 0x1 << (species_number%32);
						}
						raf.close();
					}catch (Exception e){
						(new SpatialLogger()).log("apply catagory species list file",
								"layer:" + filters[i].layername + " cat:" + value 
								+ "> " + species_number + ": species_mask_layer len:" 
								+ species_mask_layer.length
								+" : " + k + ">" + e.toString());
					}
				}				
			}
			
			/* apply back to _all mask 
			 * first time is OR, subsequent is AND */
			if(i == 0){ 
				species_mask_all = species_mask_layer.clone();
			}else{
				for(j=0;j<species_mask_layer.length;j++){
					species_mask_all[j] = 
						species_mask_all[j] & species_mask_layer[j];
				}
			}			
		}	
		
		return species_mask_all;
	}
	
	public static SPLFilter getLayerFilter(Layer layer){
		if(layer == null){
			return null;
		}
		
		/* is it continous (grid file) */
		System.out.println("getLayerFilter: " + layer.name);
		
		File continous_file = new File(
				TabulationSettings.index_path 
				+ "SPL_" + layer.name + ".dat");
		
		SPLFilter splfilter;
		
		if(continous_file.exists()){
			System.out.println("filename=" + continous_file.getName());
			try{
				RandomAccessFile raf = new RandomAccessFile(
						continous_file,"r");
				
				int pos = 0;
				
				/* read first number, ignore NaN's */
				double minimum = raf.readFloat();
				while(raf.getFilePointer() < raf.length() && 
						Double.isNaN(minimum)){
					pos += 16;			//size of record
					raf.seek(pos);
					minimum = raf.readFloat();
				}
			
				/* seek to the end */
				pos = (int)raf.length()-16;			//floats at size 4				
				raf.seek(pos);		
				double maximum = Double.NaN;
				raf.seek(pos);
				while(pos > 16 && 
						Double.isNaN(maximum)){
				//System.out.println(maximum + " @ " + pos);
					maximum = raf.readFloat();
					pos-=16;
					raf.seek(pos);
				}
				
				raf.close();
				
				return new SPLFilter(layer,null,null,minimum,maximum);
	
			}catch (Exception e){
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
		if(catagories_file.exists()){
			byte [] data = new byte[(int)catagories_file.length()];
			try{
				FileInputStream fis = new FileInputStream(catagories_file);
				fis.read(data);
				fis.close();
				
				/* insert (row number) as beginning of each line */
				String str = new String(data);
				data=null;
				
				String [] catagory_names = str.split("\n");
				int [] catagories = new int[catagory_names.length];

				for(i=0;i<catagories.length;i++){
					catagories[i] = i;
				}
				
				return new SPLFilter(layer,catagories,catagory_names,0,0);
				
			}catch(Exception e){
				(new SpatialLogger()).log("getLayerExtents(" + layer.name + "), catagorical",
						e.toString());
			}
		}
		
		
		return null;
	}
	
	public static String getLayerExtents(String layer_name){
		/* is it continous (grid file) */
		System.out.println("getLayerExtends: " + layer_name);
		
		File continous_file = new File(
				TabulationSettings.index_path 
				+ "SPL_" + layer_name + ".dat");
		
		if(continous_file.exists()){
			System.out.println("filename=" + continous_file.getName());
			try{
				RandomAccessFile raf = new RandomAccessFile(
						continous_file,"r");
				
				int pos = 0;
				
				/* read first number, ignore NaN's */
				double minimum = raf.readFloat();
				while(raf.getFilePointer() < raf.length() && 
						Double.isNaN(minimum)){
					pos += 16;			//size of record
					raf.seek(pos);
					minimum = raf.readFloat();
				}
			
				/* seek to the end */
				pos = (int)raf.length()-16;			//floats at size 4				
				raf.seek(pos);		
				double maximum = Double.NaN;
				raf.seek(pos);
				while(pos > 16 && 
						Double.isNaN(maximum)){
				//System.out.println(maximum + " @ " + pos);
					maximum = raf.readFloat();
					pos-=16;
					raf.seek(pos);
				}
				
				raf.close();
			
				return ((float)minimum) + " to " + ((float)maximum);
			}catch (Exception e){
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
		for(i=0;i<TabulationSettings.geo_tables.length;i++){
			Layer l = TabulationSettings.geo_tables[i];
			if(l.name.equals(layer_name)){
				fieldname = l.fields[0].name;
				break;
			}
		}
		File catagories_file = new File(
				TabulationSettings.index_path
				+ SamplingIndex.CATAGORY_LIST_PREFIX 
				+ layer_name + "_" + fieldname 
				+ SamplingIndex.CATAGORY_LIST_POSTFIX);
		if(catagories_file.exists()){
			byte [] data = new byte[(int)catagories_file.length()];
			try{
				FileInputStream fis = new FileInputStream(catagories_file);
				fis.read(data);
				fis.close();
				
				/* insert (row number) as beginning of each line */
				String str = new String(data);
				data=null;
				
				String [] lines = str.split("\n");
				str=null;
				
				StringBuffer output = new StringBuffer();
				
				/* 
				 * TODO: add -1, missing
				 */
				int row = 0;					// 0..n indexing
				for(String s : lines){
					if(s.length() > 0){
						output.append(row + "," + s + "\r\n");
						row = row + 1;
					}
				}
				
				return output.toString().replace("\r\n","<br>\r\n");				
			}catch(Exception e){
				(new SpatialLogger()).log("getLayerExtents(" + layer_name + "), catagorical",
						e.toString());
			}
		}		
		
		return "";
	}
	
	/* make layer images */
	public void makeScaledImagesFromGrids(){
		/* ?? default long lat & dist */
		double longitude_start = 112;
		double longitude_end = 154;
		double latitude_start = -44;
		double latitude_end = -9;
		int height = 210;
		int width = 252;
		
		int i,j;
		
		for(Layer l : TabulationSettings.environmental_data_files){
			byte [] data = getScaledBytesFromGrid(l.name,longitude_start,longitude_end,latitude_start, latitude_end, width,height);
			
			ArrayList<ImageByte> ib_list = new ArrayList<ImageByte>(data.length-16);
			
			//sort bytes?? keep ref to xy
			for(i=0;i<data.length-16;i++){
				ib_list.add(new ImageByte(data[i],i/height,i%height));
				//System.out.println(ib_list.get(i).x + "," + ib_list.get(i).y + ":");
			}
			
			java.util.Collections.sort(ib_list,
					new Comparator<ImageByte>(){
						public int compare(ImageByte i1, ImageByte i2){
							int v = i1.value - i2.value;
							if(v == 0){
								v = i1.x - i2.x;
								if(v == 0){
									 v = i1.y - i2.y;
								}
							}
							return v;
						}
			});
			/*for(i=0;i<data.length-16;i++){
				//ib_list.add(new ImageByte(data[i],i/height,i%height));
				System.out.print(ib_list.get(i).x + "," + ib_list.get(i).y + ":");
			}*/
			
		//	for(int k=0;k<ib_list.size();k+=500){
		//		System.out.print(ib_list.get(k).value + ",");
		//	}
			/* x2 for 1byte (x,y) for each point
			 * +4 rows to cover 255 levels position starts, 2bytes each
			 */
			BufferedImage image = new BufferedImage(width,height+8,
					BufferedImage.TYPE_4BYTE_ABGR);
			
			System.out.println("imgsize:" + width + "," + height+8 + "," + ib_list.size());
			
			/* valid when x & y are less than or equal to 255 (1byte)
			 * 
			 */
			int [] image_data = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
					null, 0, image.getWidth());
			
			System.out.println(image_data.length);
			
			
			int last_byte = 0;
			int a,b;
			//a = 255; b = 126;
			//System.out.println("a=" + a + " b=" + b);
			int [] index = new int[255];
			for(i=0;i<ib_list.size();i++){
				if(((int)ib_list.get(i).value)+128 != last_byte){
					a = (i/256);
					b = i%256;
					
					//image_data[(last_byte)/2] = (((int)a)*256+b) << (16*((last_byte+1)%2));
					image_data[last_byte] = i;
					index[last_byte] = i;
					System.out.println("lb:" + (last_byte) + ", " + a + "x256 + " + b);
					last_byte = ((int)ib_list.get(i).value)+128;
				}
				/*if(((int)ib_list.get(i+1).value)+128 != last_byte){
					a = (i+1)/(256);
					b = (i+1)%256;
					
					//image_data[(last_byte)/2] = (((int)a)*256+b) << (16*((last_byte+1)%2));
					image_data[last_byte] = i;
					System.out.println("lb:" + (last_byte) + ", " + a + "x256 + " + b);
					last_byte = ((int)ib_list.get(i).value)+128;
				}*/
		//		System.out.println(i + "," + i/2+width/2*4);
				/* idx position, beginning on 4th row, remember width is x2 */
				int value = ib_list.get(i).x *256 //x
					+ ib_list.get(i).y  //y
					;//+ ib_list.get(i+1).x *256 //x, 2nd set
					//+ ib_list.get(i+1).y; //y, 2nd set
				
				
				image_data[i+width*8] = value;
				
				/*if(value == 0){
					System.out.print(ib_list.get(i).x + "," + ib_list.get(i).y + "," + 
							(ib_list.get(i).x *256 + ib_list.get(i).y)*256*256 +":");
					System.out.print(ib_list.get(i+1).x + "," + ib_list.get(i+1).y + "," + 
							(ib_list.get(i+1).x *256 + ib_list.get(i+1).y) + ":" );
					
					/*
					System.out.print("," + i + " >" + image_data[i/2+width/2*4] 
  				        + "(" + i/2+width/2*4 + "):" +ib_list.get(i).x  + "," + ib_list.get(i).y 
  						+ "," + ib_list.get(i+1).x + ib_list.get(i+1).y);			}*/
			}
			
			// ?save the data?
			try{
				image.setRGB(0, 0, image.getWidth(), image.getHeight(),
						image_data, 0, image.getWidth());

				ImageIO.write(image, "png", 
					new File(TabulationSettings.index_path
					+ "SPL_IMG_" + l.name + ".png"));
				
				ImageIO.write(image, "bmp", 
						new File(TabulationSettings.index_path
						+ "SPL_IMG_" + l.name + ".bmp"));
				
				String filename = TabulationSettings.index_path 
				+ "SPL_IMG_" + l.name + ".dat";
				
				FileOutputStream fos = new FileOutputStream(filename);
		        BufferedOutputStream bos = new BufferedOutputStream(fos);
		        ObjectOutputStream oos = new ObjectOutputStream(bos);
		        ImageByte [] ib = new ImageByte[ib_list.size()];
		        ib_list.toArray(ib);
		        oos.writeObject(ib);
		        
		        oos.close();
				
			}catch(Exception e){
				System.out.println("image gen err: " + l.name + " " + e.toString());
			}	
			//return; //done testing here
		}
	}
		
	/* generate layer data on demand: return URL 
	 * 
	 * scale between 1 and 255, with 0 as missing data
	 * */
	
	public byte[] getScaledBytesFromGrid(String layer_name, 
			double longitude_start, double longitude_end,
			double latitude_start, double latitude_end, 
			int longitude_steps, int latitude_steps){
		byte [] data = new byte[longitude_steps*latitude_steps+16];
		
		Grid grid = new Grid(
				TabulationSettings.environmental_data_path 
				+ layer_name);
		
		/* make points to interrogate */
		double [][] points = new double[longitude_steps*latitude_steps][2];
		for(int i=0;i<longitude_steps;i++){
			for(int j=0;j<latitude_steps;j++){
				points[i*latitude_steps + j][0] = longitude_start 
					+ i/(double)longitude_steps*(longitude_end - longitude_start);
				points[i*latitude_steps + j][1] = latitude_start 
					+ j/(double)latitude_steps*(latitude_end - latitude_start);
			//	System.out.print("points[" + i*latitude_steps + j + "):" +
			//			points[i*latitude_steps + j][0]);
			}			
		}
		
		/* get layer data */
		double [] values = grid.getValues(points);
		//for(double d : values){
		//	System.out.print("?" + d);
		//}
		points = null;  //reclaim space?
		
		if(values != null && values.length > 0){
			System.out.println("got values size: " + values.length);
			int i;
			
			/* get max/min */
			double max = Double.NaN;
			double min = Double.NaN;
			for(i=0;i<values.length;i++){
				if(!Double.isNaN(values[i]) && (Double.isNaN(max) || max < values[i])){
					max = values[i];
				}
				if(!Double.isNaN(values[i]) && (Double.isNaN(min) || min > values[i])){
					min = values[i];
				}
			}
			
			System.out.println("min=" + min + ", max=" + max);
			
			/* copy values back to byte data */
			for(i=0;i<values.length;i++){
				if(Double.isNaN(values[i])){
					data[i] =  0;
				}else{
					data[i] = (byte)(((values[i] - min)/(double)(max-min)*254 + 1)-128);
				//	System.out.print("*" + data[i] + ">" + values[i]);
				}
			}
			values = null; //finished with this
			
			/* set last 16 bytes as two floats, min and max */
			float fmin = (float) min;
			float fmax = (float) max;
			
			byte [] bbdata = new byte[16];
			
			ByteBuffer bb = ByteBuffer.wrap(bbdata);
			bb.putFloat(fmin);
			bb.putFloat(fmax);			
			
			/* return data */
			return data;
		}	
				
		return null;
	}
	
	public ImageByte [] getImageData(Layer layer){
		try{
			FileInputStream fis =  new FileInputStream(
					TabulationSettings.index_path 
					+ "SPL_IMG_" + layer.name + ".dat");
			
			BufferedInputStream bis = new BufferedInputStream(fis);
			ObjectInputStream ois = new ObjectInputStream(bis);
			ImageByte [] ib = (ImageByte[]) ois.readObject();
			ois.close();
			
			return ib;
			
		}catch(Exception e){
			System.out.println("getimagedata(" + layer.name + "):" + e.toString());
		}
		return null;
	}
	
	public int applyImageData(ImageByte[] ib, Integer min_idx, Integer max_idx, int [] img_bytes){
		
	/*	if(last_idx > 0 && max_idx. < ib.length && ib != null && ib.length > 0 
				&& new_threshold >= 0 && new_threshold <= 256){
			byte threshold = (byte)(new_threshold-128);
			byte ct = ib[last_idx].value;
			
			if(ct > threshold){
				while(last_idx > 0){
					img_bytes[]
				}
			}
		}*/
		return 0;		
	}
};

class SPLGridRecord {
	public SPLGridRecord(double _value, double _longitude, double _latitude, int _species_number){
		value = _value;
		longitude = _longitude;
		latitude = _latitude;
		species_number = _species_number;
	}	
	public double value;
	public double longitude;
	public double latitude;
	public int species_number;
	
};

class SPLSpeciesRecord implements Serializable{
	
	/**
	 * default
	 */
	private static final long serialVersionUID = 1L;
	
	public SPLSpeciesRecord(String _name, int _rank){
		name = _name;
		rank = _rank;
	}
	public String name;
	public int rank;
}

class ImageByte{
	
	public byte value;
	public int x;
	public int y;
	
	public ImageByte(byte _value, int _x, int _y){
		value = _value;
		x = _x;
		y = _y;
	}
}