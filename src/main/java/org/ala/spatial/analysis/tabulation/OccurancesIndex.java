


package org.ala.spatial.analysis.tabulation;


import java.io.*;
import java.util.*;
import org.ala.spatial.util.*;

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
public class OccurancesIndex implements AnalysisIndexService {
	/* constants */
	static final String SORTED_FILENAME = "OCC_SORTED.csv";
	static final String POINTS_FILENAME = "OCC_POINTS.dat";
	static final String SPECIES_IDX_FILENAME = "OCC_IDX_SPECIES.dat";
	static final String OTHER_IDX_PREFIX = "OCC_IDX_";
	static final String OTHER_IDX_POSTFIX = ".dat";
	static final int SPECIES_RECORD_WIDTH = 255 + 4 + 1 + 20*4;	//name width, commas, new line, numbers
		
	/**
	 * static instance of all indexed data for filtering
	 */
	static ArrayList<IndexedRecord[]> all_indexes = new ArrayList<IndexedRecord[]>();
	static IndexedRecord[] single_index = null;
	
	/**
	 * object to perform sorting on occurances_csv
	 */
	SortedMap<String,StringBuffer> column_keys;
	
	/**
	 * index to actual csv field positions to named
	 * fields
	 */
	int [] column_positions;

	/**
	 * for column_keys write buffer to take some load off sorting
	 */
	String prev_key = null;
	
	/**
	 * for column_keys write buffer to take some load off sorting
	 */
	StringBuffer prev_value = null;

	/**
	 * default constructor
	 */
	public OccurancesIndex(){
		TabulationSettings.load();
	}
	
	/**
	 * performs update of 'indexing' for new points data
	 */
	public void occurancesUpdate(){
		/* these two must be done as a pair */
		loadOccurances();		
		exportSortedPoints();
		
		/* this can be done in isolation */
		exportFieldIndexes();
	}

	/**
	 * performs update of 'indexing' for a new layer (grid or shapefile)
	 * 
	 * @param layername name of the layer to update as String.  To update
	 * all layers use null.
	 */
	public void layersUpdate(String layername){
		//not applicable
	}
	 
	/**
	 * method to determine if the index is up to date
	 * 
	 * @return true if index is up to date
	 */
	public boolean isUpdated(){
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
	 * 
	 * @return list of names for indexed fields.  Used in other functions
	 * as optional inputs.
	 */
	public String [] listTypes(){
		//occurances fields list, minus last two (longitude, latitude)
		/*
		String [] sa = TabulationSettings.occurances_fields;
		return java.util.Arrays.subarray(sa,0,sa.length-2);
		 */
		return null;
	}

	/**
	 * reads occurances_csv into a sorted map
	 * 
	 * exports as OCC_SORTED.csv
	 */
	void loadOccurances(){
		String [] columns = TabulationSettings.occurances_csv_fields;
		
		/* read occurances_csv */
		try{
			System.out.println("loading file: " + TabulationSettings.occurances_csv);
			BufferedReader br = new BufferedReader(
					new FileReader(TabulationSettings.occurances_csv));
			
			String s;
			String [] sa;

			/* lines read */
			int progress = 0; 
			
			/* helps with ',' in text qualifier records */
			int max_columns = 0;
			
			column_keys = new TreeMap<String,StringBuffer>();

			while((s = br.readLine()) != null) {
				
				sa = s.split(",");	
				
				/* handlers for the text qualifiers and ',' in the middle */
				if(sa != null && max_columns == 0){
					max_columns = sa.length;
				}
				if(sa != null && sa.length > max_columns){
					sa = split(s);
				}

				/* remove quotes for terms */
				for(int i=0;i<sa.length;i++){
					if(sa[i].length() > 0){
						sa[i] = sa[i].replace("\"","");
					}
				}
				
				progress++;
				
				if(progress == 1){						//first record					
					getColumnPositions(sa);
				}else{
					/*ignore records with no species or longitude or 
					 * latitude (-3, -2, -1 idx) */
					if(sa[column_positions[columns.length-3]].length() > 0 
							&& sa[column_positions[columns.length-2]].length() > 0
							&& sa[column_positions[columns.length-1]].length() > 0){						
						pushRecord(sa);
					}
				}

			} 

			flushRecord();			//finish writing

			br.close();
			(new SpatialLogger()).log("loadOccurances done");
		}catch(Exception e){
			(new SpatialLogger()).log("loadoccurances",e.toString());
		}
		
	}

	/**
	 * operates on previously populated sorted map <code>column_keys</code>
	 * 
	 * exports as OCC_SORTED.csv
	 * exports as OCC_POINTS.dat
	 */
	void exportSortedPoints(){
		String value;
		String key;
		int i;

		String [] line;

		try{			
			double longitude;
			double latitude;
			Set<Map.Entry<String,StringBuffer>> entryset = column_keys.entrySet();
			System.out.println("keys size: " + entryset.size());
			Iterator<Map.Entry<String,StringBuffer>> iset = entryset.iterator();

			FileWriter sorted = new FileWriter(
				TabulationSettings.index_path + SORTED_FILENAME);

			RandomAccessFile points = new RandomAccessFile(
					TabulationSettings.index_path + POINTS_FILENAME,
					"rw");

			Map.Entry<String,StringBuffer> me;
				
			String s;
			
			while(iset.hasNext()){
				me = iset.next();
				value = me.getValue().toString();
				key = me.getKey();

				/* one record per split */
				line = value.split(";"); 				//first string is empty
				for(i=1;i<line.length;i++){
					s = key + "," + line[i];
					
					/* force to US-ASCII, fallover if not supported */
					s = new String(s.getBytes("US-ASCII"),"US-ASCII");
					
					sorted.append(s.trim() + "\n");
					longitude = 0;
					latitude = 0;
					try{
						String [] longlat = line[i].split(",");
						longitude = Double.parseDouble(longlat[0]);
						latitude = Double.parseDouble(longlat[1]);

					}catch(Exception e){
					}

					points.writeDouble(longitude);
					points.writeDouble(latitude);
				}
			}
			sorted.flush();
			sorted.close();

			points.close();
			(new SpatialLogger()).log("exportSortedPoints done");
		}catch(Exception e){
			(new SpatialLogger()).log("exportSortedPoints",e.toString());
		}
	}
	
	/**
	 * splits a csv line correctly
	 *   
	 * states:
	 * 	begin term: if " then set qualified to true
	 * 	in term: qualified and ", then end qualified and end term
	 * 	in term: not qualified and , then end term
	 * 	in term: qualified and "" then "*   
	 *
	 */
	String [] split(String line){
		ArrayList<String> al = new ArrayList<String>();
		int i;

		boolean qualified = false;
		boolean begin_term = true;
		boolean end_term = false;

		String word = "";
		for(i=0;i<line.length();i++){
			if(begin_term && line.charAt(i) == '"'){
				qualified = true;	
				begin_term = false;
			}else if(!qualified && 
				line.charAt(i) == ','){ //end term
				end_term = true;
			}else if(qualified && 
				line.charAt(i) == '"' 
				&& line.length() > i+1 
				&& line.charAt(i+1) == '"'){//one "
				i++;
				word += line.charAt(i+1);
			}else if(qualified && 
				line.charAt(i) == '"' 
				&& line.length() > i+1 
				&& line.charAt(i+1) == ','){//end term
				end_term = true;
				qualified = false;
				i++;
			}else{
				word += line.charAt(i);
			}
			if(end_term){
				begin_term = true;
				al.add(word);
				word = "";
				end_term = false;
			}				
		}
		String [] string = new String[al.size()];
		for(i=0;i<al.size();i++){
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
	void pushRecord(String [] occurance){		
		String key;

		int i;

		//make key
		key = occurance[column_positions[0]];		//set first value
		for(i=1;i<column_positions.length-2;i++){	//ignore longitude & latitude
			key += "," + occurance[column_positions[i]];
		}

		//check key with prev_key, flush to sorted map only if necessary
		if(prev_key == null){
			prev_key = key;
			prev_value = column_keys.get(key);
			if(prev_value == null){
				prev_value = new StringBuffer();			
			}
		}else if(!prev_key.equals(key)){
			flushRecord();			//flush previous, different, key
			prev_key = key;
			prev_value = column_keys.get(key);
			if(prev_value == null){
				prev_value = new StringBuffer();			
			}
		}else{
			/* same prev_key, do nothing */
		}
		
		/* store as string for later split on ';' and write as csv */
		prev_value.append(";" 
			+ occurance[column_positions[column_positions.length-2]] 	//longitude
			+ "," 
			+ occurance[column_positions[column_positions.length-1]]);	//latitude
		
	}

	/**
	 * write in memory record to column_keys.
	 * 
	 * takes some load off sorting
	 */
	void flushRecord(){
		column_keys.put(prev_key,prev_value);
	}
	
	/**
	 * populates column_positions array with actual column positions
	 * of named columns in the header.
	 * 
	 * @param line header of occurances.csv as String []
	 */
	void getColumnPositions(String [] line){
		System.out.println("in getColumnPositions()");
		
		String [] columns = TabulationSettings.occurances_csv_fields;
		column_positions = new int[columns.length];		
		int i;
		int j;
		
		for(j=0;j<line.length;j++){
			for(i=0;i<columns.length;i++){
				if(columns[i].equals(line[j])){
					column_positions[i] = j;					
				}
			}
		}	
		System.out.println("out getColumnPositions()");
	}
	
	/**
	 * operates on OCC_SORTED.csv
	 * 
	 * generates an index for each occurances field (minus longitude
	 * and latitude)
	 */
	void exportFieldIndexes(){
		String [] columns = TabulationSettings.occurances_csv_fields;
		
		TreeMap<String,IndexedRecord> [] fw_maps = new TreeMap[columns.length-2];
		System.out.println("exportFieldIndexes start");
		try{
			BufferedReader br = new BufferedReader(
					new FileReader(
							TabulationSettings.index_path + 
							SORTED_FILENAME));
			String s;
			String [] sa;

			int progress = 0;

			int i;
	
			for(i=0;i<columns.length-2;i++){
				fw_maps[i] = new TreeMap<String,IndexedRecord>();
			}			

			String [] last_value = new String[columns.length-2];
			int [] last_position = new int[columns.length-2];
			int [] last_record = new int[columns.length-2];

			for(i=0;i<columns.length-2;i++){
				last_value[i] = "";
				last_position[i] = 0;
				last_record[i] = 0;
			}

			int filepos = 0;
			int recordpos = 0;
//			int lastfilepos = 0;
	//		int lastrecordpos = 0;
			//boolean updated = false;
			System.out.println("exportFieldIndexes about to read lines");
			while((s = br.readLine()) != null) {
				sa = s.split(",");			
				
				progress++;
			
				//updated = false;
				if(sa.length >= last_value.length){
					for(i=0;i<columns.length-2;i++){
						if(recordpos != 0 && !last_value[i].equals(sa[i])){
			
							fw_maps[i].put(last_value[i],
									new IndexedRecord(last_value[i].toLowerCase(),
											//lastfilepos,
											last_position[i],
											filepos,
											//lastrecordpos,
											last_record[i],
											recordpos-1,(byte)i));
							
							last_position[i] = filepos;
							last_record[i] = recordpos;

							last_value[i] = sa[i];
						}else if(recordpos == 0){
							//need to keep first string
							
							last_value[i] = sa[i];
						}
					}
					
				}

				recordpos++;
				filepos += s.length() + 1; 	//+1 for '\n'
			} 
			
			for(i=0;i<columns.length-2;i++){
				fw_maps[i].put(last_value[i],
						new IndexedRecord(last_value[i].toLowerCase(),
								last_position[i],
								filepos,
								last_record[i],
								recordpos-1,(byte)i));
				
				System.out.println("exportFieldIndexes fw_maps(" + i + ") size=" + fw_maps[i].size());
			}
			
			br.close();
		}catch(Exception e){
			(new SpatialLogger()).log("exportFieldIndexes, read",e.toString());
		}
		
		/* write as array objects for faster reads later*/
		System.out.println("exportFieldIndexes write");
		try{		
			int i;
						                             
			for(i=0;i<fw_maps.length;i++){
				System.out.println("exportFieldIndexes writing " + i);
				
				Set<Map.Entry<String,IndexedRecord>> set = fw_maps[i].entrySet();
				Iterator<Map.Entry<String,IndexedRecord>> iset = set.iterator();

				IndexedRecord [] ir = new IndexedRecord[set.size()];
				
				int j = 0;
				while(iset.hasNext()){
					/* export the records */
					ir[j++] = iset.next().getValue();
				}
				
				String filename = TabulationSettings.index_path
					+ OTHER_IDX_PREFIX + columns[i] + OTHER_IDX_POSTFIX;
				
				/* rename the species file */
				if(i == columns.length-3){
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
		        for(IndexedRecord r : ir){
		        	fw.append(r.name + "," + r.file_start + "," + r.file_end 
		        			+ "," + r.record_start + "," + r.record_end);
		        }		        
		        fw.close();
		        
			}
			(new SpatialLogger()).log("exportFieldIndexes done");
		}catch(Exception e){
			(new SpatialLogger()).log("exportFieldIndexes, write",e.toString());
		}			
	}
	
	static public String [] filterSpecies(String filter,int limit){
		loadIndexes();
		
		/* TODO: operate on all indexes, not only the species index */
		System.out.println("all_indexes.size():" + all_indexes.size());
		ArrayList<String> matches = new ArrayList<String>();
		
		if(all_indexes.size() > 0){
			int i = 0;
			for(IndexedRecord [] ir : all_indexes){
			//IndexedRecord [] ir = all_indexes.get(all_indexes.size()-1);
				System.out.println("list length:" + ir.length);
							
				int counted = 0;
				
				for(IndexedRecord r : ir){
					/*
					 * TODO: update to support wildcard matches
					 * 
					 * TODO: make nice output format
					 */
					if(r.name.startsWith(filter)){
						System.out.println("r.name:" + r.name);
						matches.add(r.name + " / " 
								+ TabulationSettings.occurances_csv_fields[i]
								+ " / found " + String.valueOf(r.record_end-r.record_start+1));
						
						/* keep a limit on top, but only per search type */
						counted++;
						if(counted >= limit){
							break;
						}						
					}
				}
				i++;
			}	
			System.out.println("found:" + matches.size());
			
			if(matches.size() > 0){
				String str [] = new String [matches.size ()];
				matches.toArray (str);
				return str;
			}
		}
		
		return null;
	}
	
	static public String [] filterIndex(String filter,int limit){
		loadIndexes();
		
		filter = filter.toLowerCase();
		
		/* TODO: operate on single index */
				
		//ArrayList<String> matches = new ArrayList<String>();
		
		IndexedRecord lookfor = new IndexedRecord(filter,0,0,0,0,(byte)-1);
		IndexedRecord lookforupper = new IndexedRecord(filter.substring(0,filter.length()-1),0,0,0,0,(byte)-1);
		char nextc = (char)(((int)filter.charAt(filter.length()-1)) + 1);
		System.out.println("c=" + filter.charAt(filter.length()-1) + " nextc=" + nextc);
		lookforupper.name += nextc;
			
		String [] matches_array =null;
		/* starts with comparator */
		if(!filter.contains("*")){	
			
			int pos = java.util.Arrays.binarySearch(single_index,
					lookfor,
				new Comparator<IndexedRecord>(){
					public int compare(IndexedRecord r1, IndexedRecord r2){
						return r1.name.compareTo(r2.name);
					}
				});
			int upperpos = java.util.Arrays.binarySearch(single_index,
					lookforupper,
				new Comparator<IndexedRecord>(){
					public int compare(IndexedRecord r1, IndexedRecord r2){
						return r1.name.compareTo(r2.name);
					}
				});
			
			if(pos < 0){ //don't care if it is the insertion point
				pos = pos * -1;
				pos--;
			}
			
			if(upperpos < 0){
				upperpos *= -1;
				upperpos --;
			}
			System.out.println("got indx pos: " + pos + " upper:" + upperpos);
			
			/* may need both forward and backwards on this pos */
			int end = limit+pos;
			if(end > upperpos){
				end = upperpos;
			}
			
			matches_array = new String[end-pos];
			System.out.println("end=" + end + " pos=" + pos + " size=" + matches_array.length);
			
			StringBuffer strbuffer2 = new StringBuffer();
			int i;
			int p;
			for(p=0,i=pos;i<end;p++,i++){
				strbuffer2.delete(0,strbuffer2.length());
				strbuffer2.append(single_index[i].name);
				strbuffer2.append(" / ");
				strbuffer2.append(getIndexType(single_index[i].type)); 
				strbuffer2.append(" / found ");
				strbuffer2.append(String.valueOf(single_index[i].record_end-
								single_index[i].record_start+1));
				matches_array[p] = strbuffer2.toString();
			}			
			/* end timer */
		}		
		 	
		return matches_array;				
		
	}
	
	static public IndexedRecord [] filterSpeciesRecords(String filter){
		filter = filter.toLowerCase();
		
		/* expect 
		 * <species name>
		 * or
		 * <species name> / <type>
		 * 
		 */
		String type = null;
		if(filter.contains("/")){
			type = filter.split("/")[1].trim();
			filter = filter.split("/")[0].trim();
		}
		System.out.println(type + ":" + filter);

		loadIndexes();
		
		/* TODO: operate on all indexes, not only the species index */
		if(all_indexes.size() > 0){
			//IndexedRecord [] ir = all_indexes.get(all_indexes.size()-1);
			ArrayList<IndexedRecord> matches = new ArrayList<IndexedRecord>();
			int i = 0;
			for(IndexedRecord [] ir : all_indexes){	
				System.out.println(type + ":"
						+ TabulationSettings.occurances_csv_fields[i]);
				
				if(type == null ||
					type.equals(TabulationSettings.occurances_csv_fields[i])){
					System.out.println("looking for match under: " + type);
			
					for(IndexedRecord r : ir){
						/*
						 * TODO: update to support wildcard matches
						 * 
						 * TODO: make nice output format
						 */
						if(r.name.equals(filter)){
							matches.add(r);
						}
					}
					System.out.println("occurances found: " + matches.size());
				}
				i++;
			}			
			
			if(matches.size() > 0){
				IndexedRecord[] indexedRecord = new IndexedRecord[matches.size()];
				matches.toArray(indexedRecord);
				return indexedRecord;
			}			
		}		
		
		return null;
	}
	
	static void loadIndexes(){
		if(single_index != null){
			return;
		}
		String [] columns = TabulationSettings.occurances_csv_fields;
		
		//only load once, corresponding to column names
		int count = 0;
		try{
			int i;			
			if(all_indexes.size() == 0){
				for(i=0;i<columns.length-2;i++){
					String filename = TabulationSettings.index_path
						+ OTHER_IDX_PREFIX + columns[i] + OTHER_IDX_POSTFIX;
					
					/* rename the species file */
					if(i == columns.length-3){
						filename = TabulationSettings.index_path 
						+ SPECIES_IDX_FILENAME;
					}
					
					System.out.println("opening index file: " + filename);
					
					FileInputStream fis =  new FileInputStream(filename);
			        BufferedInputStream bis = new BufferedInputStream(fis);
			        ObjectInputStream ois = new ObjectInputStream(bis);
			        all_indexes.add((IndexedRecord[]) ois.readObject());
			        
			        System.out.println("records loaded: " + all_indexes.get(all_indexes.size()-1).length);
			        ois.close();	
			        
			        if(all_indexes.get(all_indexes.size()-1) != null){
			        	count += all_indexes.get(all_indexes.size()-1).length;
			        }
				}
			}
			(new SpatialLogger()).log("loadIndexes done");
		}catch (Exception e){
			(new SpatialLogger()).log("loadIndexes",e.toString());
		}
		
		/* put all_indexes into single_index */
		single_index = new IndexedRecord[count];
		int i = 0;
		for(IndexedRecord [] rl : all_indexes){
			for(IndexedRecord r : rl){
				single_index[i++] = r;
			}
		}
		java.util.Arrays.sort(
				single_index,
				new Comparator<IndexedRecord>(){
					public int compare(IndexedRecord r1, IndexedRecord r2){
						return r1.name.compareTo(r2.name);
					}
				});
	}
	
	static public IndexedRecord [] getSpeciesIndex(){
		loadIndexes();
		
		return all_indexes.get(all_indexes.size()-1);
	}
	
	static public IndexedRecord [] getIndex(){
		loadIndexes();
		
		return single_index;
	}
	
	static public String getIndexType(int type){
		return TabulationSettings.occurances_csv_fields[type];
	}
	
	public static String [] getSortedRecords(int file_start, int file_end){
		
		try{
			byte [] data = new byte[file_end-file_start];
			FileInputStream fis = new FileInputStream(
					TabulationSettings.index_path 
					+ SORTED_FILENAME);
			fis.skip(file_start); 			//seek to start
			fis.read(data);					//read what is required
			fis.close();
			
			return (new String(data)).split("\n");		//convert to string			
		}catch(Exception e){
			(new SpatialLogger()).log("getSortedRecords",e.toString());
		}
		/* 
		 * TODO: make safe
		 */
		return null; 		
	}
}
