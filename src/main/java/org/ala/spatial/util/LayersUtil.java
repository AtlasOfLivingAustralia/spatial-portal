package org.ala.spatial.util;

import java.net.URLEncoder;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * joins active layer names to
 * - species names
 * - environmental layer names
 * - contextual layer names
 * 
 * used to autocomplete layer entries
 * 
 * @author adam
 *
 */
public class LayersUtil {
	/**
	 * MapComposer for retrieving active layer names
	 */
	MapComposer mc;
	
	/**
	 * SAT server url
	 */
	String satServer;
	
	/**
	 * list of contextual layer names
	 * 
	 * populated on first getContextualLayers call
	 */
	String [] contextualLayerNames = null;
	
	/**
	 * list of environmental layer names 
	 * 
	 * populated on first getEnvironmentalLayers call
	 */
	String [] environmentalLayerNames = null;
	
	/**
	 * constructor
	 * 
	 * @param mc_ current MapComposer
	 * @param satServer_ SAT server url as String
	 */
	public LayersUtil(MapComposer mc_, String satServer_) {
		mc = mc_;
		satServer = satServer_;
		
		/* do first SAT calls here for retrieving layer names */
		getEnvironmentalLayers();
		getContextualLayers();		
	}
	
	/**
	 * gets first species layer found from active layers list
	 * 
	 * @return species name found as String or null if none found
	 */
	public String getFirstSpeciesLayer(){
		List<MapLayer> activeLayers = mc.getPortalSession().getActiveLayers();
		for (MapLayer ml : activeLayers) {
			if (isSpeciesName(ml.getName())) {
				return ml.getName();
			}
		}
		return null;
	}
   
	/**
	 * gets whole list of environmental or contextual layers
	 * that appear in the active layers list
	 * 
	 * @return list of layer names as String [] or null if none 
	 * found
	 */
   public String [] getActiveEnvCtxLayers(){
  	 List<MapLayer> activeLayers = mc.getPortalSession().getActiveLayers();
  	 ArrayList<String> layers = new ArrayList<String>();
  	 for (MapLayer ml : activeLayers) {
  		System.out.println("active layer: " + ml.getName());
  		 if (isEnvCtxLayer(ml.getName())) {
  			layers.add(ml.getName());
  			System.out.println("match");
  		 }
  	 }
  	 if(layers.size() == 0){
  		 return null;
  	 }
  	 String [] ret = new String[layers.size()];
  	 layers.toArray(ret);
  	 return ret;
  }
   
   /**
    * gets first environmental layer that appears in the
    * active layers list
    * 
    * @return name of first environmental layer as String or null
    * if none found
    */
   public String getFirstEnvLayer(){
     	 List<MapLayer> activeLayers = mc.getPortalSession().getActiveLayers();
     	 for (MapLayer ml : activeLayers) {
     		 if (isEnvLayer(ml.getName())) {
     			 return ml.getName();
     		 }
     	 }
     	 return null;
     }
   
   /**
    * tests if a String is a species name 
    * (or valid autocomplete box higher order value)
    * 
    * uses SAT autocomplete service call
    * 
    * @param val text value to test as String
    * @return true iff val is an exact match with a
    * species autocomplete value.  Note that false is 
    * returned if there is error communicating with SAT  
    */
   public  boolean isSpeciesName(String val) {
     String snUrl = satServer + "/alaspatial/species/taxon/";
       val = val.trim();
       try {           
   
           String nsurl = snUrl + URLEncoder.encode(val, "UTF-8");

           HttpClient client = new HttpClient();
           GetMethod get = new GetMethod(nsurl);
           get.addRequestHeader("Content-type", "text/plain");

           int result = client.executeMethod(get);
           String slist = get.getResponseBodyAsString();

           System.out.println("Response status code: " + result);
           System.out.println("Response: \n" + slist);

           String[] aslist = slist.split("\n");
           
           if (aslist.length > 0) {		//only interested in first match from autocomplete search
				String [] spVal = aslist[0].split("/");
				String taxon = spVal[0].trim();
				if (taxon.equalsIgnoreCase(val)) {
					return true;
				}
           }

       } catch (Exception e) {
               e.printStackTrace(System.out);
       }
       return false;
   }
   
   /**
    * tests if a String is an environmental layer name 
    * 
    * uses SAT environmental list service call 
    * 
    * @param val text value to test as String
    * @return true iff val is an exact match with an
    * environmental layer name. Note that false is 
   * returned if there is error communicating with SAT  
    */
   public boolean isEnvLayer(String val){
   	String [] layers = getEnvironmentalLayers();
   	if (layers == null) {
   		return false;
   	}
   	val = val.trim();
   	for (String s : layers) {   	
   		if (s.equalsIgnoreCase(val)) {
   			return true;
   		}
   	}
   	return false;
   }
   
   /**
   * tests if a String is a contextual layer name 
   * 
   * uses SAT contextual list service call
   * 
   * @param val text value to test as String
   * @return true iff val is an exact match with a
   * contextual layer name. Note that false is 
   * returned if there is error communicating with SAT  
   */
   private boolean isCtxLayer(String val){
   	String [] layers = getContextualLayers();
   	if (layers == null) {
   		return false;
   	}
   	val = val.trim();
   	for (String s : layers) {
   		if (s.equalsIgnoreCase(val)) {
   			return true;
   		}
   	}
   	return false;
   }
   
   /**
   * tests if a String is an environmental 
   * or contextual layer name 
   * 
   * @param val text value to test as String
   * @return true iff val is an exact match with an
   * environmental or a contextual layer name
   */
   public boolean isEnvCtxLayer(String val) {
	 	return isEnvLayer(val) || isCtxLayer(val);
   }
   
   /**
    * gets list of environmental layers from SAT server by 
    * layer name
    * 
    * @return environmental layer names as String[] or null on error
    */
   public String[] getEnvironmentalLayers() {
	   /* return previously generated list if available */
	   if (environmentalLayerNames != null) {
		   return environmentalLayerNames;
	   }
	   
       String[] aslist = null;
       try {
           String envurl = satServer + "/alaspatial/ws/spatial/settings/layers/environmental/string";

           HttpClient client = new HttpClient();
           GetMethod get = new GetMethod(envurl);
           get.addRequestHeader("Content-type", "text/plain");

           int result = client.executeMethod(get);
           String slist = get.getResponseBodyAsString();
           
           aslist = slist.split("\n");
           
           for (int i = 0; i < aslist.length; i++) {
        	   aslist[i] = aslist[i].trim();
           }

       } catch (Exception e) {
           System.out.println("error setting up env list");
           e.printStackTrace(System.out);
       }
       
       /* retain list for future calls */
       environmentalLayerNames = aslist;

       return aslist;
   }

   /**
    * gets list of contextual layers from SAT server by 
    * layer name
    * 
    * @return contextual layer names as String[] or null on error
    */
   public String[] getContextualLayers() {
	   /* return previously generated list if available */
	   if (contextualLayerNames != null) {
		   return contextualLayerNames;
	   }
	   
       String[] aslist = null;
       try {

           String envurl = satServer + "/alaspatial/ws/spatial/settings/layers/contextual/string";

           HttpClient client = new HttpClient();
           GetMethod get = new GetMethod(envurl);
           get.addRequestHeader("Content-type", "text/plain");

           int result = client.executeMethod(get);
           String slist = get.getResponseBodyAsString();

           aslist = slist.split("\n");

           for (int i = 0; i < aslist.length; i++) {
        	   aslist[i] = aslist[i].trim();
           }
           
       } catch (Exception e) {
           System.out.println("error setting up ctx list");
           e.printStackTrace(System.out);
       }
       
       /* retain for future calls */
       contextualLayerNames = aslist;

       return aslist;
   }

}