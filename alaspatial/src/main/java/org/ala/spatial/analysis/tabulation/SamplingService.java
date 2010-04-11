
package org.ala.spatial.analysis.tabulation;

import org.ala.spatial.util.*;

import java.io.*;
import java.util.*;

public class SamplingService {
	public SamplingService(){
		TabulationSettings.load();
	}

	public String [] listLayers(){
		String [] layers = new String[
			TabulationSettings.environmental_data_files.length
			+ TabulationSettings.geo_tables.length];

		int i = 0;
		for(Layer l : TabulationSettings.environmental_data_files){
			layers[i++] = l.display_name;
		}

		for(Layer l : TabulationSettings.geo_tables){
			layers[i++] = l.display_name;
		}

		return layers;
	}

	public String [] filterSpecies(String filter,int limit){

		return OccurancesIndex.filterIndex(filter,limit);
		//return null;
	}

	public String sampleSpecies(String filter, String [] layers){
		System.out.println("sampleSpecies(" + filter);

		StringBuffer output = new StringBuffer();

		for(String s : TabulationSettings.occurances_csv_fields){
			output.append(s);
			output.append(",");
		}

		if(layers != null){
			for(String l : layers){
				output.append(layerNameToDisplayName(l));
				output.append(",");
				System.out.print(l + ",");
			}

			System.out.print("]");
		}else{
			System.out.print(")");
		}

		/* tidy up header */
		output.deleteCharAt(output.length()-1); //take off end ','
		output.append("\r\n");

		IndexedRecord [] ir = OccurancesIndex.filterSpeciesRecords(filter);
		int i,j;

		if(ir != null && layers != null && layers.length > 0){
			ArrayList<String[]> columns = new ArrayList<String[]>(layers.length+1);

			/*
			 * TODO: make split safe
			 */
			System.out.println("recordsets found: " + ir.length);

			try{
				File temporary_file = java.io.File.createTempFile("sample",".csv");
				FileWriter fw = new FileWriter(temporary_file);

				fw.append(output.toString());


				for(IndexedRecord r : ir){
					columns.clear();

					/*
					 * cap the number of records per read
					 */

					int step = 5000000; //max characters to read
					int rstart = r.file_start;
					int rend;

					rend = rstart + step;
					if(rend > r.file_end){
						rend = r.file_end;
					}

					String [] sortedrecords;
					int recordstart = r.record_start;
					int recordend;
					System.out.println("$$ extents: " + r.record_start + "," + r.record_end + "," + r.file_start + "," + r.file_end);
					System.out.println("$$ pos: " + recordstart + "," + ":" + rstart + "," + rend);
					String lastpart = "";

					while(rend <= r.file_end){

						columns.clear();

						sortedrecords = OccurancesIndex.getSortedRecords(rstart, rend);
						sortedrecords[0] = lastpart + sortedrecords[0];

						columns.add(sortedrecords);

						if(rend == r.file_end){
							//do all records
							recordend = r.record_end;
							lastpart = "";
							System.out.println("no last part");
						}else{
							//do up to last record
							recordend = recordstart + sortedrecords.length-2;
							lastpart = sortedrecords[sortedrecords.length-1];
							System.out.println("got last part: " + lastpart);
						}

						System.out.println("$$ position: " + recordstart + "," + recordend + "," + rstart + "," + rend + " len=" + sortedrecords.length);

						for(i=0;i<layers.length;i++){
							columns.add(SamplingIndex.getRecords(
									layerDisplayNameToName(layers[i]),
									recordstart,
									recordend));
						}

						/* join for output */
						int len = columns.get(1).length;
						System.out.println("len of speciesinfo=" + columns.get(0).length + ", len of records=" + len);

						for(j=0;j<len;j++){
					//		System.out.println("**>" + columns.get(0)[0]);
							for(i=0;i<columns.size();i++){
								if(columns.get(i) != null && j < columns.get(i).length){
									if(!(columns.get(i)[j] == null) && !columns.get(i)[j].equals("NaN")){
										fw.append(columns.get(i)[j]);
									}
									if(i < columns.size()-1){
										fw.append(",");
									}
								}
							}

							fw.append("\r\n");
						}

						/* adjust for next loop */
						recordstart = recordend+1; 		//this was inclusive
						if(rend < r.file_end){
							rstart = rend;				//this is not inclusive
							rend = rstart + step;
							if(rend > r.file_end){
								rend = r.file_end;
							}
						}else{
							rend = r.file_end+1;
						}

					}
				}

				fw.close();
				System.out.println("created sample file: " +temporary_file.getPath());
				return temporary_file.getPath();
			}catch (Exception e){
				System.out.println("dumping records to a file error: " + e.toString());
			}
		}else if(ir != null){
			try{
				File temporary_file = java.io.File.createTempFile("sample",".csv");
				FileWriter fw = new FileWriter(temporary_file);

				fw.append(output.toString());

				for(IndexedRecord r : ir){
					System.out.println("$ sample, no layers: " + r.name
							+ ", file pos " + r.file_start + " to " + r.file_end
							+ ", for records " + r.record_start + " to " + r.record_end);

					int step = 10000000;
					String s = "";
					for(i=r.file_start;i<r.file_end-step;i+=step){
						System.out.println("$ getting: " + i + " to " + i+step);
						fw.append(OccurancesIndex.getSortedRecordsString(
								i,i+step));
					}
					System.out.println("$ getting: " + i + " to " + r.file_end);
					fw.append(OccurancesIndex.getSortedRecordsString(
							i,r.file_end));

				}
				fw.close();
				System.out.println("created sample file: " +temporary_file.getPath());
				return temporary_file.getPath();
			}catch(Exception e){
				System.out.println("output sample, no layers: " + e.toString());
			}
		}

		return null;
	}

	/*
	 * returns a String[][] of results, no more than max_rows, possibly less if it is a bit big
	 */
	public String[][] sampleSpecies(String filter, String [] layers, int max_rows){
		String [][] results = null;

		System.out.println("sampleSpecies(" + filter);

		StringBuffer output = new StringBuffer();

		int number_of_columns = TabulationSettings.occurances_csv_fields.length;

		for(String s : TabulationSettings.occurances_csv_fields){
			output.append(s);
			output.append(",");
		}

		if(layers != null){
			for(String l : layers){
				output.append(layerNameToDisplayName(l));
				output.append(",");
				System.out.print(l + ",");
			}
			number_of_columns += layers.length;

			System.out.print("]");
		}else{
			System.out.print(")");
		}
		System.out.print("rows limit=" + max_rows);

		/* tidy up header */
		output.deleteCharAt(output.length()-1); //take off end ','
		output.append("\r\n");

		IndexedRecord [] ir = OccurancesIndex.filterSpeciesRecords(filter);
		int i,j;


		ArrayList<String[]> columns = new ArrayList<String[]>();


		/*
		 * TODO: make split safe
		 */
		System.out.println("recordsets found: " + ir.length);

		try{
			for(IndexedRecord r : ir){
				columns.clear();

				/*
				 * cap the number of records per read
				 */

				int step = 50000; //max characters to read
				int rstart = r.file_start;
				int rend;

				rend = rstart + step;
				if(rend > r.file_end){
					rend = r.file_end;
				}

				String [] sortedrecords;
				int recordstart = r.record_start;
				int recordend;
				System.out.println("$$ extents: " + r.record_start + "," + r.record_end + "," + r.file_start + "," + r.file_end);
				System.out.println("$$ pos: " + recordstart + "," + ":" + rstart + "," + rend);
				String lastpart = "";

				/*
				 * single record retrieval pass
				 */
				if(rend <= r.file_end){

					columns.clear();

					sortedrecords = OccurancesIndex.getSortedRecords(rstart, rend);
					sortedrecords[0] = lastpart + sortedrecords[0];

					columns.add(sortedrecords);

					if(rend == r.file_end){
						//do all records
						recordend = r.record_end;
						lastpart = "";
						System.out.println("no last part");
					}else{
						//do up to last record
						recordend = recordstart + sortedrecords.length-2;
						lastpart = sortedrecords[sortedrecords.length-1];
						System.out.println("got last part: " + lastpart);
					}
					/* cap results to max_rows */
					if(recordend-recordstart+1 > max_rows){
						recordend = recordstart + max_rows;
					}

					System.out.println("$$ position: " + recordstart + "," + recordend + "," + rstart + "," + rend + " len=" + sortedrecords.length);

					if(layers != null){
						for(i=0;i<layers.length;i++){
							columns.add(SamplingIndex.getRecords(
									layerDisplayNameToName(layers[i]),
									recordstart,
									recordend));

							System.out.println("adding column at end: " + columns.size());
						}
					}

					/* join for output */
					int len;
					if(columns.size() > 1){
						len = columns.get(1).length;
					}else{
						len = sortedrecords.length-1;
					}
					if(len > max_rows){
						len = max_rows;
					}
					System.out.println("len of speciesinfo=" + columns.get(0).length + ", len of records=" + len);

					/* output structure */
					results = new String[len+1][number_of_columns+1];

					int coloffset = 0;
					String [] row = output.toString().split(",");
					for(j=0;j<row.length;j++){
						results[0][j] = row[j];
					}
					for(j=0;j<len;j++){
						coloffset = 0;
						for(i=0;i<columns.size();i++){
							if(columns.get(i) != null && j < columns.get(i).length){
								if(i==0){
									row = columns.get(i)[j].split(",");
									System.out.println(">" + columns.get(i)[j]);
									System.out.println(row.length);
									for(int k=0;k<row.length;k++){
										results[j+1][k] = row[k];
									}
									coloffset = row.length-1;
								}else if(!(columns.get(i)[j] == null) && !columns.get(i)[j].equals("NaN")){
									results[j+1][coloffset] = columns.get(i)[j];
									System.out.println("adding from col: " + i);
								}else{
									results[j+1][coloffset] = "missing";
								}
								coloffset++;

							}
						}
					}
				}
			}

			System.out.println("created sample output: " + results.length + " x " + results[0].length);
			return results;
		}catch (Exception e){
			e.printStackTrace();
		}

		return null;
	}

	public static String layerDisplayNameToName(String display_name){
		TabulationSettings.load();

		/* convert layer name to TabulationSettings.Layers name */
		String layer_name = display_name;
		for(Layer l : TabulationSettings.geo_tables){
			if(l.display_name.equals(display_name)){
				layer_name = l.name;
			}
		}
		for(Layer l : TabulationSettings.environmental_data_files){
			if(l.display_name.equals(display_name)){
				layer_name = l.name;
			}
		}

		return layer_name;
	}
	public static String layerNameToDisplayName(String name){
		TabulationSettings.load();

		/* convert layer name to TabulationSettings.Layers name */
		String layer_name = name;
		for(Layer l : TabulationSettings.geo_tables){
			if(l.name.equals(name)){
				layer_name = l.display_name;
			}
		}

		for(Layer l : TabulationSettings.environmental_data_files){
			if(l.name.equals(name)){
				layer_name = l.display_name;
			}
		}
		return layer_name;
	}

	static public String getLayerMetaData(String layer_name){
		for(Layer l : TabulationSettings.environmental_data_files){
			if(l.name.equals(layer_name)){
				/* return meta data e.g. for grid files _name_.gri */
				File file = new File(
						TabulationSettings.environmental_data_path
						+ layer_name + ".grd");
				/*try{
					BufferedReader br = new BufferedReader(new FileReader(file));

					String str;
					StringBuffer sb = new StringBuffer();

					while((str = br.readLine()) != null) {
						sb.append(str + "\r\n");
					}
					br.close();

					return sb.toString();*/
				try{
					FileInputStream fis = new FileInputStream(file);
					byte [] data = new byte[(int)file.length()];
					fis.read(data);
					fis.close();
					return new String(data);

				}catch(Exception e){
					(new SpatialLogger()).log("getLayerMetaData(" + layer_name + ")",
						e.toString());
				}
			}
		}
		for(Layer l : TabulationSettings.geo_tables){
			if(l.name.equals(layer_name)){
				/* catagorical data match off the table */
				return "TODO: set/get real catagorical layer metadata.";
			}
		}
		return "";
	}
}