package org.ala.spatial.analysis.tabulation;

import java.io.Serializable;

import org.ala.spatial.util.*;

public class SPLFilter extends Object implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public int count = 0;
	
	public Layer layer;
	public String layername = "";
	
	public int [] catagories = null;		//use to maintain Set validity
	public String [] catagory_names = null;
	
	public double minimum_value = 0;
	
	public double maximum_value = 0;
	
	public double minimum_initial = 0;
	public double maximum_initial = 0;	
	
	public SPLFilter(){}
	
	public SPLFilter(SPLFilter copy){		
		count = copy.count;
		layer = copy.layer;
		layername = copy.layername;
		if(catagories == null){
			catagories = null;
		}else{
			catagories = copy.catagories.clone();
		}
		if(catagory_names == null){
			catagory_names = null;
		}else{
			catagory_names = copy.catagory_names.clone();
		}
		minimum_value = copy.minimum_value;
		maximum_value = copy.maximum_value;
		minimum_initial = copy.minimum_initial;
		maximum_initial = copy.maximum_initial;
	}
	
	public SPLFilter(Layer _layer, 
			int[] _catagories, String [] _catagory_names,
			double _minimum, double _maximum){
		layer = _layer;
		if(layer != null){
			layername = _layer.name;
		}
		catagories = _catagories;
		catagory_names = _catagory_names;
		minimum_value = _minimum;
		maximum_value = _maximum;	
		
		minimum_initial = _minimum;
		maximum_initial = _maximum;	
	}
	
	
	public Layer getLayer() {
		return layer;
	}

	public String getLayername() {
		return layername;
	}

	public int[] getCatagories() {
		return catagories;
	}

	public String[] getCatagory_names() {
		return catagory_names;
	}

	public double getMinimum_value() {
		return minimum_value;
	}

	public double getMaximum_value() {
		return maximum_value;
	}

	public double getMinimum_initial() {
		return minimum_initial;
	}

	public double getMaximum_initial() {
		return maximum_initial;
	}


	
	/* only works if created with constructor and 'catagories' Set validity is maintained */
	public boolean isChanged(){
		if(minimum_value != minimum_initial
				|| maximum_value != maximum_initial
				|| (catagories != null && catagory_names != null 
						&& catagories.length != catagory_names.length)){
				return true;
		}
		return false;				
	}
	
	public boolean equals(SPLFilter f){
		if(layername != f.layername ||
				(catagories != null && f.catagories != null &&
						catagories.length != f.catagories.length)
				|| minimum_value != f.minimum_value 
				|| maximum_value != f.maximum_value){
			return false;
		}
		return true;		
	} 
	
	public String getFilterString(){
		/* TODO finish */
		
		/*
		 * cases:
		 * 
		 * catagorical
		 * - include all
		 * - include none
		 * - only; comma separated list of names
		 * 		 
		 * continous
		 * - include all
		 * - include none
		 * - between x and y 
		 * 
		 */
		
		if(catagory_names != null){
			if(catagories == null || catagories.length == 0){
				return "include none";
			}else if(catagories.length == catagory_names.length){
				return "include all";
			}else{
				StringBuffer string = new StringBuffer();
				for(int i : catagories){
					if(string.length() == 0){
						string.append("only; ");
						string.append(catagory_names[i]);
					}else{
						string.append(", ");
						string.append(catagory_names[i]);
					}					
				}
				return string.toString();
			}
		}else{
			/*if(minimum_value <= minimum_initial 
					&& maximum_value >= maximum_initial){
				return "include all";
			}else if(minimum_value > maximum_value
					|| maximum_value < minimum_initial 
					|| minimum_value > maximum_initial){
				return "include none";
			}else*/{
				return "between " + ((float)minimum_value) 
				+ " and " + ((float)maximum_value);
			}
		}
	
	}
	
}