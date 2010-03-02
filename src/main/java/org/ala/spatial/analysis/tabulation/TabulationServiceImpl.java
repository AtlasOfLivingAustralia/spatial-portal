package org.ala.spatial.analysis.tabulation;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

import org.ala.spatial.util.*;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * implements a tabulation service that intersects, for species locations
 * in a database, with optional environmental (Grid files) and 
 * contextual (postgis tables)
 * 
 * @author adam
 *
 */
public class TabulationServiceImpl {
	private String output_filename;
	
	/**
	 * loads TabulationSettings and creates class instance objects
	 */
	public TabulationServiceImpl(){
		TabulationSettings.load();
		environmentalLayers = new ArrayList<Layer>();
		contextualLayers = new ArrayList<Layer>();
	}
	
	/**
	 * provides a list of contextual layers in the database
	 * @return List<Layer>
	 */
	public List<Layer> getContextualLayers() {
		
		List<Layer> ret = new ArrayList<Layer>();
		
		for(Layer l : TabulationSettings.geo_tables){
			//System.out.println(l.type + "=\"contextual\" is " + (l.type == "contextual"));
			//System.out.println(l.type + "=\"contextual\" is " + (l.type.equals("contextual")));
			if(l.type.equals("contextual")){
				ret.add(l);
			}
		}
		//System.out.println("contextual layers found=" + ret.size());
		return ret;		
	}

	/**
	 * provides a list of environmental layers available from the database
	 * and grid files
	 * @return List<Layer>
	 */
	public List<Layer> getEnvironmentalLayers() {
		List<Layer> ret = new ArrayList<Layer>();
		for(Layer l : TabulationSettings.geo_tables){
			if(l.type == "environmental"){
				ret.add(l);
			}
		}
		for(Layer l : TabulationSettings.environmental_data_files){
			ret.add(l);
		}
		return ret;
	}

	/**
	 * fills class instance output objects <code>species, contextualData, 
	 * environmentalData</code> with appropriate data.
	 * 
	 * makes use of <code>species</code>, <code>contextualLayers</code> 
	 * and <code>environmentalData</code> class variables to record function
	 * call inputs
	 * 
	 * @param _species species to select as String
	 * @param layers layers by Layer.display_name to include in the output as 
	 * an String []
	 */
	public void tabulate(String _species, String [] layers) {
		//log function call
		SpatialLogger sl = new SpatialLogger();
		String msg = _species;
		for(String l : layers){
			msg = msg + ", " + l;
		}
		sl.log("Sampling request",msg);
		
		//setup
		species = _species;
		
		environmentalLayers.clear();
		contextualLayers.clear();
		
		for(String s : layers){
			//only add environmental layers that appear in the file listing
			for(Layer l : TabulationSettings.environmental_data_files){
				if(l.display_name == s){
					environmentalLayers.add(l);
				}
			}
						
			//not so clear cut, need to add all geo_tables identified
			for(Layer l : TabulationSettings.geo_tables){
				if(l.display_name == s){
					contextualLayers.add(l);
				}
			}
		}		
		
		//run
		connectToDatabase();
		queryLocationWithContextual();
		queryEnvironmentalVariables();	
		
		//save it to a temporary file
		saveResults();
	}

	//parameters for tabulation process
	String species; 
	List<Layer> environmentalLayers;
	List<Layer> contextualLayers;
		
	//data outputs
	double [][] speciesLocations;
	double [][] environmentalData;
	String [][] contextualData;
	String [][] additionalData;
	
	//database connection
	Connection connection;

	/**
	 * postgres db connection load
	 * @return
	 */
	private boolean connectToDatabase(){   	  
		try {
			Class.forName("org.postgresql.Driver");
			connection = DriverManager.getConnection(TabulationSettings.db_connection_string,TabulationSettings.db_username, TabulationSettings.db_password);
		} catch (Exception e) {   
			SpatialLogger sl = new SpatialLogger();
			sl.log("TabulationServiceImpl",e.toString());
			return false;
		}   
		return connection != null;
	} 
	
	/**
	 * with longitude and latitude in class variable <code>speciesLocations</code>
	 * all values at intersection with .gri/.grd files listed in class 
	 * array <code>environmentalLayers</code> are entered into class
	 * array <code>environmentalData</code>
	 * 
	 */
	private void queryEnvironmentalVariables(){
		if(speciesLocations == null ||
				speciesLocations.length == 0 ||
				environmentalLayers == null ||
				environmentalLayers.size() == 0)
		{
			return; 	//nothing to do
		}
		double [][] ret = new double[speciesLocations.length][environmentalLayers.size()];
		
		//open layer by grid name and get all variables @ locations
		int i,j;
		Grid grid;
		for(i=0;i<environmentalLayers.size();i++){			
			grid = new Grid(TabulationSettings.environmental_data_path + environmentalLayers.get(i).name);			
			double [] env = grid.getValues(speciesLocations);
			for(j=0;j<speciesLocations.length;j++){
				ret[j][i] = env[j];
			}	
		}

		environmentalData = ret;
	}
	
	/**
	 * queries the database <code>connection</code> with left join on 
	 * class variable <code>species</code> longitude and latitude with 
	 * contextual and environmental shape files in the database as
	 * found in class array <code>contextualLayers</code>
	 * 
	 *  results of query are stored in arrays <code>speciesLocations</code> and
	 *  <code>contextualData</code>
	 */
	private void queryLocationWithContextual(){		
		int i,j;
	
		//for sql query formation
		String q1,q2,q3,i1,i2,i3,layerShortName,query;

		//when point_type == true use the_geom object in the table
		if(TabulationSettings.point_type){
			q1 = "SELECT ST_X(o.the_geom), ST_Y(o.the_geom)";
		}else{
			q1 = "SELECT o." + TabulationSettings.longitude_field + ", o." + TabulationSettings.latitude_field;
		}
		
		//include any additional fields from the points table
		for(i=0;i<TabulationSettings.additional_fields.length;i++){
			q1 = q1 + ", o." + TabulationSettings.additional_fields[i].name;
		}
		
		i1 = "";
		q2 = " FROM " + TabulationSettings.source_table_name + " o";
		i2 = "";
		q3 = " WHERE " + TabulationSettings.key_field + "=" + 
			TabulationSettings.key_value_prefix + species + 
			TabulationSettings.key_value_postfix;
		i3 = "";
		for(i=0;i<contextualLayers.size();i++){
			//table alias
			layerShortName = "c" + i;
			
			//output variables for this contextual layer
			for(j=0;j<contextualLayers.get(i).fields.length;j++){
				i1 = i1 + ", " + layerShortName + "." + contextualLayers.get(i).fields[j].name;
			}

			//left join with occurances table
			if(TabulationSettings.point_type){
				i2 = i2 + " LEFT JOIN " + contextualLayers.get(i).name + " " + 
					layerShortName + " ON (ST_Within(o.the_geom," + 
					layerShortName + ".the_geom))";
			} else {
				i2 = i2 + " LEFT JOIN " + contextualLayers.get(i).name + " " + 
					layerShortName + " ON (ST_Within(ST_MakePoint(to_number(o.longitude),to_number(o.latitude))," + 
					layerShortName + ".the_geom))";
			}
			
			//placeholder for filtering
			//i3 = i3 + "";
		}

		query = q1 + i1 + q2 + i2 + q3 + i3 + ";";

		//for lat/lng
		java.util.List<Double> d = new java.util.ArrayList<Double>();

		//for contextual variables
		java.util.List<String> str = new java.util.ArrayList<String>();

		try{		
			Statement s = connection.createStatement();
			ResultSet r = s.executeQuery(query);
		
			j = 0;
			while(r.next()){				
				d.add(new Double(r.getDouble(1)));//lng
				d.add(new Double(r.getDouble(2)));//lat
				
				//additional fields from points table are first
				for(i=0;i<TabulationSettings.additional_fields.length;i++){
					str.add(r.getString(3+i));
				}

				//the rest
				for(i=0;i<contextualLayers.size();i++){
					str.add(r.getString(3+i+TabulationSettings.additional_fields.length));
				}
				j++;				
				
			}

			//writeback
			speciesLocations = new double[d.size()/2][2];
			for(i=0;i<d.size();i+=2){		
				speciesLocations[i/2][0] = d.get(i).doubleValue();
				speciesLocations[i/2][1] = d.get(i+1).doubleValue();
			}
			
			//contextual data store includes additional fields from points table
			if(contextualLayers.size()+TabulationSettings.additional_fields.length > 0 && str.size() > 0){
				contextualData = new String[str.size()/(contextualLayers.size()+TabulationSettings.additional_fields.length)][contextualLayers.size()+TabulationSettings.additional_fields.length];
				for(i=0;i<str.size();i+=contextualLayers.size() + TabulationSettings.additional_fields.length){
					for(j=0;j<contextualLayers.size();j++){
						contextualData[i/contextualLayers.size()][j] = str.get(i+j);
					}
				}
			}
		}catch(Exception e){
			SpatialLogger sl = new SpatialLogger();
			sl.log("TabulationServiceImpl","db query " + e.toString());
		}				
	}

	/**
	 * CSV style export of <code>tabulation( ... )</code> results.  
	 * @return String url to temporary filename with the stored results
	 */
	private void saveResults() {
		//TODO, text qualifers on export	
		
		SpatialLogger sl = new SpatialLogger();

		//create random file name/location		
		File file = null;
				
		try{
			//TODO, use better output file 
			file = java.io.File.createTempFile("sampling",".csv");			
			java.io.FileWriter filewriter = new java.io.FileWriter(file);
						
			String line;
			String header;
						
			int i,j;
			
			header = "species,longitude,latitude";
			for(i=0;i<contextualLayers.size();i++){
				header = header + "," + contextualLayers.get(i).display_name;
			}
			for(i=0;i<environmentalLayers.size();i++){
				header = header + "," + environmentalLayers.get(i).display_name;
			}
			
			//output = header;
			filewriter.write(header.toCharArray());
			
			for(i=0;i<speciesLocations.length;i++){
				line = "\r\n" + species + ",";
				if(!Double.isNaN(speciesLocations[i][0])){
					line = line + speciesLocations[i][0] + "," + speciesLocations[i][1];
				} else {
					line = line + ",";
				}
				
				//contextual, if any
				if(contextualData != null && contextualData.length > i){
					for(j=0;j<contextualData[i].length;j++){
						line = line + "," 
							+ ((contextualData[i][j] == null) ? "" : contextualData[i][j]);
					}
				}		
				
				if(environmentalData != null && environmentalData.length > i){				
					for(j=0;j<environmentalData[i].length;j++){
						line = line + ","
							+ ((Double.isNaN(environmentalData[i][j])) ? "" : environmentalData[i][j]);					
					}
				}
				filewriter.write(line.toCharArray());
			}
			filewriter.close();

		}catch (Exception e){			
			sl.log("TabulationServiceImpl","getResults:" + e.toString());
		}
		
		if(file != null){
			sl.log("TabulationServiceImpl","output file: " + file.getAbsolutePath());
			output_filename = file.getAbsolutePath();
		}else{
			output_filename = "";
		}
		
		//free data stored
		environmentalLayers = new ArrayList<Layer>();
		contextualLayers = new ArrayList<Layer>();
		System.gc();
	}
	
	public char [] getResults(){
		//provide results back
		try{
			java.io.File file = new File(output_filename);
			java.io.FileReader file_reader = new java.io.FileReader(file);
			char [] results = new char[(int)file.length()];
			file_reader.read(results,0,(int)file.length());
			return results;
		}catch (Exception e){
			SpatialLogger sl = new SpatialLogger();
			sl.log("TabulationServiceImpl","getResults: " + e.toString());
		}
		return null;		
		
	}
}
