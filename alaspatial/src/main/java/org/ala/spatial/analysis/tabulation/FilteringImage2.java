package org.ala.spatial.analysis.tabulation;

import java.util.*;

import org.ala.spatial.analysis.*;
import org.ala.spatial.util.*;

import java.awt.image.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.imageio.ImageIO;

public class FilteringImage2 implements Serializable {

    private static final long serialVersionUID = -2431102028701859952L;

    final int WIDTH = 252;
    final int HEIGHT = 210;
    final int TRANSPARENT = 0x00000000;
    
    int hidden_colour;
    String filename;
    int[] image_bytes;
    
    BufferedImage image;
        
    public FilteringImage2(String filename_, int hidden_colour_) {
    	//System.out.println("        FilteringImage:" + filename_ + " 0x" + Integer.toHexString(hidden_colour_));
        filename = filename_;
        hidden_colour = hidden_colour_;

        int i;
        
        TabulationSettings.load();
     
        /* make image */        
        image = new BufferedImage(WIDTH, HEIGHT,
                BufferedImage.TYPE_4BYTE_ABGR);        
        
        image_bytes = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());

        for (i = 0; i < image_bytes.length; i++) {
            image_bytes[i] = hidden_colour; 
        }       
    }


    public void writeImage() {
        try {
        	image.setRGB(0, 0, image.getWidth(), image.getHeight(),
                     image_bytes, 0, image.getWidth());
        	 
            ImageIO.write(image, "png",
                    new File(filename));
            
            System.out.println("writing image:" + filename);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
   
    public void applyFilter(String layer_name, double new_min_, double new_max_) {
    //    System.out.println("applyFilter(" + layer_name + "," + new_min_ + "," + new_max_ + "): ");
       
        /* load data */
        Tile [] data = null;
        
        try {            
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + "SPL_IMG_T_" + layer_name + ".dat");
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);            
            data = (Tile []) ois.readObject();
           
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
       
        /* seeking */
        Tile t = new Tile((float)new_min_,0);
        int start = java.util.Arrays.binarySearch(data,t,
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
    //    System.out.print("[" + start + "," + t.value_ + "]");
        if(start < 0){
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
   //     System.out.print("[" + end + "," + t.value_ + "]");
        if(end < 0){
        	end = end * -1 -1;
        }
        if(end >= data.length){
        	end = data.length-1;
        }
     //   System.out.println("start=" + start + " end=" + end + " #show count=" + (end-start+1));
        
        int i;
        for(i=start;i<=end;i++){
        	image_bytes[data[i].pos_] = TRANSPARENT;
        } 
    }
    
    public void applyFilter(String layer_name, int [] show_list) {
     //   System.out.println("applyFilter(" + layer_name + ",show_count=" + show_list.length + "): ");
       
        /* load data */
        Tile [] data = null;
        Boolean has_index;
        int [] index = null;
        try {            
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + "SPL_IMG_T_" + layer_name + ".dat");
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);            
            data = (Tile []) ois.readObject();
            has_index = (Boolean) ois.readObject();
            if(has_index){
            	index = (int[]) ois.readObject();
            }
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        java.util.Arrays.sort(show_list);
        
        int i,j,end;
        for(i=0;i<show_list.length;i++){
        	j = index[show_list[i]];
        	end = index[show_list[i]+1];        	
      //  	System.out.print("{" + j + " " + end + "}");
        	for(;j<end;j++){
        		image_bytes[data[j].pos_] = TRANSPARENT;
        	}
        }
    }
}
