package org.ala.spatial.web.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.ala.spatial.analysis.service.FilteringImage;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.analysis.index.FilteringIndex;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.Zipper;
//import org.jboss.serial.io.JBossObjectInputStream;
//import org.jboss.serial.io.JBossObjectOutputStream;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws/filtering/")
public class FilteringWSController {

    private SpatialSettings ssets;
    private Map<String, Object> userMap;
    private FilteringImage filteringImage2;
    private List _layer_filters_selected;
    
    int [] colours = {0xFFFF99FF,0XFF99FFFF,0XFF9999FF,0XFF4444FF,0XFF44FFFF,0XFFFF44FF};

    private String outputpath = "/output/filtering/";

    @RequestMapping(value = "/init", method = RequestMethod.GET)
    public
    @ResponseBody
    String doInit(HttpServletRequest req) {
        try {
            TabulationSettings.load();

            String pid = "";
            long currTime = System.currentTimeMillis();
            pid = "" + currTime;

            HttpSession session = req.getSession(true);

            userMap = new Hashtable();

            File workingDir = new File(session.getServletContext().getRealPath(outputpath + currTime + "/"));
            workingDir.mkdirs();

            //File file = File.createTempFile("spl", ".png", workingDir);
            File file = new File(workingDir + "/filtering.png"); 
         //   filteringImage = new FilteringImage(file.getPath());
         //   filteringImage.writeImage();

         //  System.out.println("Created initial image at: " + file.getPath());

            _layer_filters_selected = new ArrayList();

            userMap.put("pid", pid);
            //userMap.put("filteringImage", filteringImage);
            userMap.put("imgpath", file.getPath());
            //userMap.put("selectedLayerFilters", _layer_filters_selected);


            //session.setAttribute(pid, userMap);
        //    writeUserBytes(session.getServletContext().getRealPath("/output/filtering/" + currTime + "/usermap.ser"));
            //writeUserMapXML(session.getServletContext().getRealPath("/output/filtering/" + currTime + "/usermap.xml"));

            return pid;
        } catch (Exception ex) {
            Logger.getLogger(FilteringWSController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "";
    }

    @RequestMapping(value = "/apply4/pid/{pid}/layers/{layers}/types/{types}/val1s/{val1s}/val2s/{val2s}/depth/{depth}", method = RequestMethod.GET)
    public
    @ResponseBody
    String apply4(@PathVariable String pid,
            @PathVariable String layers,
            @PathVariable String types,
            @PathVariable String val1s,
            @PathVariable String val2s,
            @PathVariable String depth,
            HttpServletRequest req) {
        try {
            // Undecode them first
            layers = URLDecoder.decode(layers, "UTF-8");
            types = URLDecoder.decode(types, "UTF-8");
            val1s = URLDecoder.decode(val1s, "UTF-8");
            val2s = URLDecoder.decode(val2s, "UTF-8");
            depth = URLDecoder.decode(depth, "UTF-8");
            int layer_depth = 0;
            try{
            	layer_depth = Integer.parseInt(depth);
            }catch(Exception e){
            	e.printStackTrace();
            }

            // grab and split the layers
            String[] aLayers = layers.split(":");
            String[] aTypes = types.split(":");
            String[] aVal1s = val1s.split(":");
            String[] aVal2s = val2s.split(":");

            // Now lets apply the filters, one at a time
          
            // apply the filters by iterating thru' the layers from client, make spl, should be one layer
            for (int i = 0; i < aLayers.length; i++) {
                String cLayer = Layers.layerDisplayNameToName(aLayers[i]);
                String cType = aTypes[i];
                String cVal1 = aVal1s[i];
                String cVal2 = aVal2s[i];
                
                System.out.println("Applying4 filter for " + cLayer + " with " + cVal1 + " - " + cVal2); 
                
                //get/make layerfilter
                LayerFilter layerfilter = FilteringIndex.getLayerFilter(cLayer);
                if(layerfilter != null){
	                if(layerfilter.layer.type.equalsIgnoreCase("environmental")){
	                	layerfilter.minimum_value = Double.parseDouble(cVal1);
	                	layerfilter.maximum_value = Double.parseDouble(cVal2);
	                }else{
	                	int j;
	                    if(cVal1.length() > 0){
	    	                String [] values_show = cVal1.split(",");
	    	                layerfilter.catagories = new int[values_show.length];
	    	                for(j=0;j<values_show.length;j++){
	    	                	layerfilter.catagories[j] = Integer.parseInt(values_show[j]);
	    	                }
	                    }   
	                }	                
                }   
                FilteringService.applyFilter(pid, layerfilter);
                 
               if(!cType.equalsIgnoreCase("none")){            	   
            	   
                	HttpSession session = req.getSession(true);               	
                	
	                File workingDir = new File(session.getServletContext().getRealPath("/output/filtering/" + pid + "/"));
	                	
	                File file = File.createTempFile("spl", ".png", workingDir);
                        //File file = new File(workingDir + "/filtering.png");

	                filteringImage2 = new FilteringImage(file.getPath(), colours[layer_depth%colours.length]);
	                
	                if (cType.equalsIgnoreCase("environmental")) {	                	
	                	filteringImage2.applyFilter(cLayer, layerfilter.getMinimum_value(), layerfilter.getMaximum_value());
	                } else {                	                	
	                	                	
	                	int j;                	
	                    if(cVal1.length() > 0){
	    	                String [] values_show = cVal1.split(",");
	    	                int [] show_list = new int[values_show.length];
	    	                for(j=0;j<values_show.length;j++){
	    	                	show_list[j] = Integer.parseInt(values_show[j]);
	    	                }
	    	                filteringImage2.applyFilter(cLayer,show_list);	
	                    }                	
	                }
	                
	                filteringImage2.writeImage();
	                
	                return file.getName();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
    
    @RequestMapping(value = "/apply3/pid/{pid}/layers/{layers}/types/{types}/val1s/{val1s}/val2s/{val2s}/depth/{depth}", method = RequestMethod.GET)
    public
    @ResponseBody
    String apply3(@PathVariable String pid,
            @PathVariable String layers,
            @PathVariable String types,
            @PathVariable String val1s,
            @PathVariable String val2s,
            @PathVariable String depth,
            HttpServletRequest req) {
    	 try {
             // Undecode them first
             layers = URLDecoder.decode(layers, "UTF-8");
             types = URLDecoder.decode(types, "UTF-8");
             val1s = URLDecoder.decode(val1s, "UTF-8");
             val2s = URLDecoder.decode(val2s, "UTF-8");
             depth = URLDecoder.decode(depth, "UTF-8");
             int layer_depth = 0;
             try{
             	layer_depth = Integer.parseInt(depth);
             }catch(Exception e){
             	e.printStackTrace();
             }

             // grab and split the layers
             String[] aLayers = layers.split(":");
             String[] aTypes = types.split(":");
             String[] aVal1s = val1s.split(":");
             String[] aVal2s = val2s.split(":");

             // Now lets apply the filters, one at a time
           
             // apply the filters by iterating thru' the layers from client, make spl, should be one layer
             for (int i = 0; i < aLayers.length; i++) {
                 String cLayer = Layers.layerDisplayNameToName(aLayers[i]);
                 String cType = aTypes[i];
                 String cVal1 = aVal1s[i];
                 String cVal2 = aVal2s[i];
                 
                 System.out.println("Applying3 filter for " + cLayer + " with " + cVal1 + " - " + cVal2); 
                 
                 //get/make layerfilter
                 LayerFilter layerfilter = FilteringIndex.getLayerFilter(cLayer);
                 if(layerfilter != null){
 	                if(layerfilter.layer.type.equalsIgnoreCase("environmental")){
 	                	layerfilter.minimum_value = Double.parseDouble(cVal1);
 	                	layerfilter.maximum_value = Double.parseDouble(cVal2);
 	                }else{
 	                	int j;
 	                    if(cVal1.length() > 0){
 	    	                String [] values_show = cVal1.split(",");
 	    	                layerfilter.catagories = new int[values_show.length];
 	    	                for(j=0;j<values_show.length;j++){
 	    	                	layerfilter.catagories[j] = Integer.parseInt(values_show[j]);
 	    	                }
 	                    }   
 	                }
                 }                
                  
                if(!cType.equalsIgnoreCase("none")){            	   
             	   
                 	HttpSession session = req.getSession(true);               	
                 	
 	                File workingDir = new File(session.getServletContext().getRealPath("/output/filtering/" + pid + "/"));
 	                	
 	                File file = File.createTempFile("spl", ".png", workingDir);
                        //File file = new File(workingDir + "/filtering.png");

 	                filteringImage2 = new FilteringImage(file.getPath(), colours[layer_depth%colours.length]);
 	                
 	                if (cType.equalsIgnoreCase("environmental")) {	                	
 	                	filteringImage2.applyFilter(cLayer, layerfilter.getMinimum_value(), layerfilter.getMaximum_value());
 	                } else {                	                	
 	                	                	
 	                	int j;                	
 	                    if(cVal1.length() > 0){
 	    	                String [] values_show = cVal1.split(",");
 	    	                int [] show_list = new int[values_show.length];
 	    	                for(j=0;j<values_show.length;j++){
 	    	                	show_list[j] = Integer.parseInt(values_show[j]);
 	    	                }
 	    	                filteringImage2.applyFilter(cLayer,show_list);	
 	                    }                	
 	                }
 	                
 	                filteringImage2.writeImage();
 	                
 	                return file.getName();
                 }
             }

         } catch (Exception e) {
             e.printStackTrace(System.out);
         }

         return "";
    }
   
    @RequestMapping(value = "/apply/pid/{pid}/species/count/shape/{shape}", method = RequestMethod.GET)
    public
    @ResponseBody
    String getSpeciesCount(@PathVariable String pid, @PathVariable String shape, HttpServletRequest req) {
        try {
        	System.out.println("[[[]]] getcount: " + pid + " " + shape);
        //    String sessionfile = req.getSession().getServletContext().getRealPath("/output/filtering/" + pid + "/usermap.ser");
        //    readUserBytes(sessionfile);
            
            SimpleRegion region = SimpleRegion.parseSimpleRegion(shape);
            
            return String.valueOf(FilteringService.getSpeciesCount(pid, region));
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return "";
    }
    
    /**
     * Returns a list of species as a string delimited by a new line
     * 
     * @param pid
     * @param shape
     * @param req
     * @return
     */
    @RequestMapping(value = "/apply/pid/{pid}/species/list/shape/{shape}", method = RequestMethod.GET)
    public
    @ResponseBody
    String getSpeciesList(@PathVariable String pid, @PathVariable String shape, HttpServletRequest req) {
        try {
        	System.out.println("[[[]]] getlist: " + pid + " " + shape);
     //       String sessionfile = req.getSession().getServletContext().getRealPath("/output/filtering/" + pid + "/usermap.ser");
     //       readUserBytes(sessionfile);
            
            SimpleRegion region = SimpleRegion.parseSimpleRegion(shape);
            
            return FilteringService.getSpeciesList(pid,region);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return "";
    }
    
    /**
     * Returns a relative path to a zip file of the filtered georeferenced data 
     * 
     * @param pid
     * @param shape
     * @param req
     * @return
     */
    @RequestMapping(value = "/apply/pid/{pid}/samples/list/shape/{shape}", method = RequestMethod.GET)
    public
    @ResponseBody
    String getSamplesList(@PathVariable String pid, @PathVariable String shape, HttpServletRequest req) {
        try {
        	System.out.println("[[[]]] getsampleslist: " + pid + " " + shape);
        //    String sessionfile = req.getSession().getServletContext().getRealPath("/output/filtering/" + pid + "/usermap.ser");
       //     readUserBytes(sessionfile);
            
            SimpleRegion region = SimpleRegion.parseSimpleRegion(shape);
            
            String filepath = FilteringService.getSamplesList(pid,region);
            
            /* zipping */
            String [] files = new String[1];
            files[0] = filepath;
            
            String currentPath = req.getSession().getServletContext().getRealPath("/");
            long currTime = System.currentTimeMillis();
            String outputpath = currentPath + "/output/filtering/";
            File fDir = new File(outputpath);
            fDir.mkdir();
            String outfile = fDir.getAbsolutePath() + "/filter_samples_" + currTime + ".zip";
            Zipper.zipFiles(files, outfile);

            return "output/filtering/filter_samples_" + currTime + ".zip";
            
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return "";
    }


    private LayerFilter[] getSelectedFilters() {
        LayerFilter[] f = new LayerFilter[_layer_filters_selected.size()];
        _layer_filters_selected.toArray(f);
        return f;
    }

    private LayerFilter[] getSelectedFilters(List filterNames) {
        LayerFilter[] f = new LayerFilter[_layer_filters_selected.size()];
        _layer_filters_selected.toArray(f);
        return f;
    }

    private void writeUserMap(String filename) {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {

            fos = new FileOutputStream(filename);
            out = new ObjectOutputStream(fos);
            /*
            byte[] b = getByteArrayFromObject(userMap);
            System.out.println("Writing length: " + b.length);
            out.write(b.length);
            out.write(b);
             *
             */
            out.writeObject(userMap);

            out.flush();
            out.close();

            fos.flush();
            fos.close();

        } catch (Exception e) {
            System.out.println("Error writing usermap to filesystem: ");
            e.printStackTrace(System.out);
        }
    }

    private void readUserMap(String filename) {

        FileInputStream fis = null;
        ObjectInputStream in = null;

        try {

            fis = new FileInputStream(filename);
            in = new ObjectInputStream(fis);
            userMap = (Map) in.readObject();
            /*
            int length = in.readInt();
            System.out.println("Got length: " + length);
            byte[] b = new byte[length];
            in.read(b);
            userMap = (Map) getObjectFromByteArray(b);
             */
            in.close();

            fis.close();

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    /*
    private void writeUserMapXML(String filename) {
    try {
    System.out.println("writing out the data");
    XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(filename)));
    encoder.writeObject(userMap);
    encoder.flush();
    encoder.close();
    System.out.println("done writing data");
    } catch (Exception ex) {
    //Logger.getLogger(FilteringWSController.class.getName()).log(Level.SEVERE, null, ex);
    ex.printStackTrace(System.out);
    }
    }

    private void readUserMapXML(String filename) {
    try {
    System.out.println("reading in the data");
    XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(
    new FileInputStream(filename)));
    userMap = (Map) decoder.readObject();
    decoder.close();
    System.out.println("done reading data");
    } catch (Exception ex) {
    ex.printStackTrace(System.out);
    }
    }
     *
     */
   /* private void writeUserBytes(String filename) {
        try {

            // save it to the session
            // userMap.put("pid", pid);
            // userMap.put("filteringImage", filteringImage);
          


            FileOutputStream fos = new FileOutputStream(filename);
            JBossObjectOutputStream out = new JBossObjectOutputStream(fos);
            System.out.println("writing user bytes");
            out.writeObject(userMap);
            out.flush();
            out.close();
            fos.flush();
            fos.close();
            System.out.println("bytes written");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }*/

 /*  private void readUserBytes(String filename) {
        try {
            FileInputStream fis = new FileInputStream(filename);
            JBossObjectInputStream in = new JBossObjectInputStream(fis);
            System.out.println("reading user bytes");
            userMap = (Map) in.readObject();
            in.close();
            fis.close();
            System.out.println("bytes read");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }*/

    /*
     * Converts an object to a serialized byte array.
     *
     * @param obj Object to be converted.
     * @return byte[] Serialized array representing the object.
     */
   /* public byte[] getByteArrayFromObject(Object obj) {
        byte[] result = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new JBossObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            baos.close();
            result = baos.toByteArray();
        } catch (IOException ioEx) {
            ioEx.printStackTrace(System.out);
        }

        return result;
    }*/

    /**
     * Utility method to un-serialize objects from byte arrays.
     *
     * @param bytes The input byte array.
     * @return The output object.
     */
   /* public Object getObjectFromByteArray(byte[] bytes) {
        Object result = null;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new JBossObjectInputStream(bais);
            result = ois.readObject();
            ois.close();
        } catch (IOException ioEx) {
            ioEx.printStackTrace(System.out);
        } catch (ClassNotFoundException cnfEx) {
            cnfEx.printStackTrace(System.out);
        }

        return result;
    }*/
}
