
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
		/*String [] str = OccurancesIndex.filterSpecies(filter,limit);
		if(str != null){
			java.util.Arrays.sort(str);
			return java.util.Arrays.copyOf(str,limit);
		}*/
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
			for(IndexedRecord r : ir){
				columns.clear();
				
				columns.add(OccurancesIndex.getSortedRecords(
						r.file_start, r.file_end));
				
				for(i=0;i<layers.length;i++){
					columns.add(SamplingIndex.getRecords(
							layerDisplayNameToName(layers[i]),
							r.record_start,
							r.record_end));						
				}
				
				/* join for output */
				for(j=0;j<columns.get(0).length;j++){
					for(i=0;i<columns.size();i++){
						if(columns.get(i) != null && j < columns.get(i).length){
							if(!(columns.get(i)[j] == null) && !columns.get(i)[j].equals("NaN")){
								output.append(columns.get(i)[j]);
							}
							if(i < columns.size()-1){
								output.append(",");
							}
						}
					}
					output.append("\r\n");
				}				
			}
			System.out.println("next line, total length:" + output.length());
		}else if(ir != null){
			
			for(IndexedRecord r : ir){
				System.out.println("$" + r.name 
						+ ", file pos " + r.file_start + " to " + r.file_end 
						+ ", for records " + r.record_start + " to " + r.record_end);
				for(String s : OccurancesIndex.getSortedRecords(
						r.file_start, r.file_end)){
					output.append(s);
					output.append("\r\n");
				}				
			}			
		}
		return output.toString();
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