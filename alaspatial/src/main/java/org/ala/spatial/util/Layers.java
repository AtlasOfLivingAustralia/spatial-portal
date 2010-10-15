package org.ala.spatial.util;

import java.io.File;
import java.io.FileInputStream;

import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;

/**
 * set of functions for accessing layers
 * 
 * @author adam
 *
 */
public class Layers {
	
	/**
	 * constructor
	 */
	public Layers(){
		TabulationSettings.load();
	}
	
	/**
	 * gets list of all layers with grid/environmental then 
	 * shape/contextual then tabulation_settings.xml order
	 * 
	 * @return layer display names as String []
	 */
	static public String [] listLayers(){
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
	
	/**
	 * gets layer name from layer display name
	 * 
	 * if not found, display name input is returned
	 * 
	 * @param display_name layer display name
	 * @return layer name if display name match found, otherwise input
	 * display name is returned
	 */
	public static String layerDisplayNameToName(String display_name){
		TabulationSettings.load();

		/* convert layer name to TabulationSettings.Layers name */
		String layer_name = display_name;
		for(Layer l : TabulationSettings.geo_tables){
			if(l.display_name.equals(display_name)
                                || l.name.equals(display_name)){
				layer_name = l.name;
                                break;
			}
		}
		for(Layer l : TabulationSettings.environmental_data_files){
			if(l.display_name.equals(display_name)
                                || l.name.equals(display_name)){
				layer_name = l.name;
                                break;
			}
		}

		return layer_name;
	}
	
	/**
	 * gets layer display name from layer name
	 * 
	 * if not found, name input is returned
	 * 
	 * @param name layer name
	 * @return layer display name if name match found, otherwise input
	 * name is returned
	 */
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
	
	/**
	 * gets a layer from either a layer display name or layer name
	 * @param name name or display name of layer to be returned
	 * @return layer found as Layer or null if not found
	 */
	public static Layer getLayer(String name){
		TabulationSettings.load();

		/* convert layer name to TabulationSettings.Layers name */
		for(Layer l : TabulationSettings.geo_tables){
			if(l.name.equalsIgnoreCase(name) || l.display_name.equalsIgnoreCase(name)){
				return l;
			}
		}

		for(Layer l : TabulationSettings.environmental_data_files){
			if(l.name.equalsIgnoreCase(name) || l.display_name.equalsIgnoreCase(name)){
				return l;
			}
		}
		return null;
	}

	/**
	 * gets layer metadata 
	 * 
	 * TODO: get layer meta data instead of what is being used now
	 * 
	 * @param layer_name layer name of data to be returned
	 * @return metadata as String
	 */
	static public String getLayerMetaData(String layer_name){
		/* environmental layers first */
		for(Layer l : TabulationSettings.environmental_data_files){
			/* check for layer name match */
			if(l.name.equals(layer_name)){
				/* return meta data e.g. for grid files _name_.gri */
				File file = new File(
						TabulationSettings.environmental_data_path
						+ layer_name + ".grd");

				try{
					FileInputStream fis = new FileInputStream(file);
					byte [] data = new byte[(int)file.length()];
					fis.read(data);
					fis.close();
					return new String(data);

				}catch(Exception e){
					SpatialLogger.log("getLayerMetaData(" + layer_name + ")",
						e.toString());
				}
			}
		}
		
		/* contextual layers search if env layers search did not return */
		for(Layer l : TabulationSettings.geo_tables){
			if(l.name.equals(layer_name)){
				/* catagorical data match off the table */
				return "TODO: set/get real catagorical layer metadata.";
			}
		}
		return "";
	}

}