package org.ala.spatial.web.services;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.service.AlocService;
import org.ala.spatial.analysis.service.LayerImgService;
import org.ala.spatial.util.Grid;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.UploadSpatialResource;
import org.ala.spatial.util.Zipper;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * web service for image, 
 * path from pid exists in output/layer 
 * 
 * original image: srcimg.png
 * current image: img.png
 * 
 * original legend: srclegend.txt
 * legend: legend.txt
 * (header) name, red, green, blue CR
 * (records) string, 0-255, 0-255, 0-255 CR
 * 
 * extents: extents.txt
 * width in px
 * height in px
 * min longitude
 * min latitude
 * max longitude
 * max latitude 
 * 
 * @author adam
 */
@Controller
@RequestMapping("/ws/layer/")
public class LayerImgWSController {

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    public
    @ResponseBody
    String get(HttpServletRequest req) {
        String pid = "";
        try {
            pid = req.getParameter("pid");

            String currentPath = req.getSession(true).getServletContext().getRealPath(File.separator);
            //TabulationSettings.load();
            //String currentPath = TabulationSettings.base_output_dir;
            String outputpath = /* currentPath + */ "output/layers/" + pid + "/";
            String layerimage = outputpath + "img.png";            
            String layerlegend = outputpath + "legend.txt";
            String layerextents = outputpath + "extents.txt";
        
            return layerimage + "\r\n" +  layerlegend + "\r\n" + layerextents;
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return pid;
    }
    
    @RequestMapping(value = "/set", method = RequestMethod.GET)
    public
    @ResponseBody
    String set(HttpServletRequest req) {
        String pid = "";
        try {
            pid = req.getParameter("pid");
            String idx = req.getParameter("idx");		// 0..n for colours in image
            String red = req.getParameter("red");		// 0-255
            String green = req.getParameter("green");	// 0-255
            String blue = req.getParameter("blue");		// 0-255

            String currentPath = req.getSession(true).getServletContext().getRealPath(File.separator);
            //TabulationSettings.load();
            //String currentPath = TabulationSettings.base_output_dir;
            String outputpath = currentPath + "output" + File.separator + "layers" + File.separator + pid + File.separator;
            String layerimage = outputpath + "img.png";
            
            LayerImgService.changeColour(outputpath,Integer.parseInt(idx),Integer.parseInt(red),Integer.parseInt(green),Integer.parseInt(blue));
            
            /* copy img.png to new filename */
            String id = "" +  System.currentTimeMillis();
            String layerimagenew = outputpath + "img" + id + ".png";            
            FileUtils.copyFile(new File(layerimage), new File(layerimagenew));
            
            String layerlegend = "output/layers/" + pid + "/" + "legend.txt";
            String layerextents = "output/layers/" + pid + "/" + "extents.txt";
            
            //redo layerimagenew path
            layerimagenew = "output/layers/" + pid + "/img" + id + ".png";           
        
            return layerimagenew + "\r\n" +  layerlegend + "\r\n" + layerextents;
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return pid;
    }    
    
    
   
}
