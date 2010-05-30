package org.ala.spatial.analysis.service;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Comparator;

import javax.imageio.ImageIO;

import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.Tile;

/**
 * for generating layer images from layer filter specifications
 * 
 * @author adam
 *
 */
public class FilteringImage implements Serializable {

	static final long serialVersionUID = 6111740847515567882L;

	
    /**
     * todo: dynamic width
     */
    final int WIDTH = 252;
    
    /**
     * todo: dynamic height
     */
    final int HEIGHT = 210;
    
    /**
     * value of transparent image cell
     */
    final int TRANSPARENT = 0x00000000;
    
    /**
     * hidden colour for current filtered image
     */
    int hidden_colour;
    
    /**
     * filename for current filtered image
     */
    String filename;
    
    /**
     * image bytes for alteration for current filtered image
     */
    int[] image_bytes;
    
    /**
     * current image 
     */
    BufferedImage image;
        
    /**
     * constructor for new, fully hidden, layer/image
     * 
     * TODO: dynamic width, height and resolution
     * 
     * @param filename_ full path to filename where image can be saved
     * @param hidden_colour_ value for the hidden colour in use
     */
    public FilteringImage(String filename_, int hidden_colour_) {
    	TabulationSettings.load();
    	
        filename = filename_;
        hidden_colour = hidden_colour_;

        int i;
     
        /* make image */        
        image = new BufferedImage(WIDTH, HEIGHT,
                BufferedImage.TYPE_4BYTE_ABGR);        
        
        /* get bytes structure */
        image_bytes = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());

        /* init with hidden colour */
        for (i = 0; i < image_bytes.length; i++) {
            image_bytes[i] = hidden_colour; 
        }       
    }


    /**
     * saves the filtered image to the filename from
     * constructor
     */
    public void writeImage() {
        try {
        	/* write back image bytes */
        	image.setRGB(0, 0, image.getWidth(), image.getHeight(),
                     image_bytes, 0, image.getWidth());
        	 
        	/* write image */
            ImageIO.write(image, "png",
                    new File(filename));

        } catch (IOException e) {

        }
    }
   
    /**
     * applies a continous/grid/environmental filter to imagebytes 
     * @param layer_name layer name for source data (grid file/environmental)
     * @param new_min_ minimum filter value
     * @param new_max_ maximum filter value
     */
    public void applyFilter(String layer_name, double new_min_, double new_max_) {    	
    	Tile [] data = null;
    	
        /* load data */       
        try {            
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + "SPL_IMG_T_" + layer_name + ".dat");
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);            
            data = (Tile []) ois.readObject();
           
            ois.close();
        } catch (Exception e) {
        }
        
       
        /* seeking */
        Tile t = new Tile((float)new_min_,0);
        int start = java.util.Arrays.binarySearch(data,t,
            	new Comparator<Tile>(){
    				public int compare(Tile i1, Tile i2) {
    					if (i1.value_ < i2.value_) {
    						return -1;
    					} else if (i1.value_ > i2.value_) {
    						return 1;
    					}
    					return 0;
    				}
    			});

        if (start < 0) {
        	start = start * -1 -1;
        }
        
        t.value_ = (float) new_max_;
        int end = java.util.Arrays.binarySearch(data,t,
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
  
        if (end < 0) {
        	end = end * -1 -1;
        }
        if (end >= data.length) {
        	end = data.length-1;
        }
        
        int i;
        for (i = start; i <= end; i++) {
        	image_bytes[data[i].pos_] = TRANSPARENT;
        } 
    }
    
    /**
     * applies a catagorical/shapefile/contextual filter to imagebytes 
     * 
     * note: hides nothing so cannot be used together with the
     * other applyFilter()
     * 
     * @param layer_name layer name for source data (shapefile/contextual)
     * @param show_list list of indexes to unhide 
     */
    public void applyFilter(String layer_name, int [] show_list) {
        
        Tile [] data = null;
        Boolean has_index;	//checking that index is present
        int [] index = null;
        
        /* load data */
        try {            
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + "SPL_IMG_T_" + layer_name + ".dat");
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);            
            data = (Tile []) ois.readObject();
            has_index = (Boolean) ois.readObject();
            if (has_index) {
            	index = (int[]) ois.readObject();
            } else {
            	//do nothing, layer is not contextual
            	return;
            }
            ois.close();
        } catch (Exception e) {
            
        }
        
        java.util.Arrays.sort(show_list);        
        
        int i,j,end;
        
        /* make imagebytes transparent */
        for (i = 0; i < show_list.length; i++ ) {        	
        	end = index[show_list[i]+1];    
        	for (j = index[show_list[i]]; j < end; j++) {
        		image_bytes[data[j].pos_] = TRANSPARENT;
        	}
        }
    }
}
