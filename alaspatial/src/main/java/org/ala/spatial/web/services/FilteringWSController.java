package org.ala.spatial.web.services;

import java.io.File;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.ala.spatial.analysis.service.FilteringImage;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.index.FilteringIndex;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.Zipper;
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

    int[] colours = {0xFFFF99FF, 0XFF99FFFF, 0XFF9999FF, 0XFF4444FF, 0XFF44FFFF, 0XFFFF44FF};
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

            File workingDir = new File(session.getServletContext().getRealPath(outputpath + currTime + "/"));
            workingDir.mkdirs();

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
        FilteringImage filteringImage2;
        try {
            // Undecode them first
            layers = URLDecoder.decode(layers, "UTF-8");
            types = URLDecoder.decode(types, "UTF-8");
            val1s = URLDecoder.decode(val1s, "UTF-8");
            val2s = URLDecoder.decode(val2s, "UTF-8");
            depth = URLDecoder.decode(depth, "UTF-8");
            int layer_depth = 0;
            try {
                layer_depth = Integer.parseInt(depth);
            } catch (Exception e) {
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
                if (layerfilter != null) {
                    if (layerfilter.layer.type.equalsIgnoreCase("environmental")) {
                        layerfilter.minimum_value = Double.parseDouble(cVal1);
                        layerfilter.maximum_value = Double.parseDouble(cVal2);
                    } else {
                        int j;
                        if (cVal1.length() > 0) {
                            String[] values_show = cVal1.split(",");
                            layerfilter.catagories = new int[values_show.length];
                            for (j = 0; j < values_show.length; j++) {
                                layerfilter.catagories[j] = Integer.parseInt(values_show[j]);
                            }
                        }
                    }
                }
                FilteringService.applyFilter(pid, layerfilter);

                if (!cType.equalsIgnoreCase("none")) {

                    HttpSession session = req.getSession(true);

                    File workingDir = new File(session.getServletContext().getRealPath("/output/filtering/" + pid + "/"));

                    File file = File.createTempFile("spl", ".png", workingDir);
                    //File file = new File(workingDir + "/filtering.png");

                    filteringImage2 = new FilteringImage(file.getPath(), colours[layer_depth % colours.length]);

                    if (cType.equalsIgnoreCase("environmental")) {
                        filteringImage2.applyFilter(cLayer, layerfilter.getMinimum_value(), layerfilter.getMaximum_value());
                    } else {

                        int j;
                        if (cVal1.length() > 0) {
                            String[] values_show = cVal1.split(",");
                            int[] show_list = new int[values_show.length];
                            for (j = 0; j < values_show.length; j++) {
                                show_list[j] = Integer.parseInt(values_show[j]);
                            }
                            filteringImage2.applyFilter(cLayer, show_list);
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
        FilteringImage filteringImage2;
        try {
            // Undecode them first
            layers = URLDecoder.decode(layers, "UTF-8");
            types = URLDecoder.decode(types, "UTF-8");
            val1s = URLDecoder.decode(val1s, "UTF-8");
            val2s = URLDecoder.decode(val2s, "UTF-8");
            depth = URLDecoder.decode(depth, "UTF-8");
            int layer_depth = 0;
            try {
                layer_depth = Integer.parseInt(depth);
            } catch (Exception e) {
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
                if (layerfilter != null) {
                    if (layerfilter.layer.type.equalsIgnoreCase("environmental")) {
                        layerfilter.minimum_value = Double.parseDouble(cVal1);
                        layerfilter.maximum_value = Double.parseDouble(cVal2);
                    } else {
                        int j;
                        if (cVal1.length() > 0) {
                            String[] values_show = cVal1.split(",");
                            layerfilter.catagories = new int[values_show.length];
                            for (j = 0; j < values_show.length; j++) {
                                layerfilter.catagories[j] = Integer.parseInt(values_show[j]);
                            }
                        }
                    }
                }

                if (!cType.equalsIgnoreCase("none")) {

                    HttpSession session = req.getSession(true);

                    File workingDir = new File(session.getServletContext().getRealPath("/output/filtering/" + pid + "/"));

                    File file = File.createTempFile("spl", ".png", workingDir);
                    //File file = new File(workingDir + "/filtering.png");

                    filteringImage2 = new FilteringImage(file.getPath(), colours[layer_depth % colours.length]);

                    if (cType.equalsIgnoreCase("environmental")) {
                        filteringImage2.applyFilter(cLayer, layerfilter.getMinimum_value(), layerfilter.getMaximum_value());
                    } else {

                        int j;
                        if (cVal1.length() > 0) {
                            String[] values_show = cVal1.split(",");
                            int[] show_list = new int[values_show.length];
                            for (j = 0; j < values_show.length; j++) {
                                show_list[j] = Integer.parseInt(values_show[j]);
                            }
                            filteringImage2.applyFilter(cLayer, show_list);
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

    @RequestMapping(value = "/apply/pid/{pid}/species/count", method = RequestMethod.GET)
    public
    @ResponseBody
    String getSpeciesCount(@PathVariable String pid, HttpServletRequest req) {

        TabulationSettings.load();
        long starttime = System.currentTimeMillis();
        try {
            String shape = req.getParameter("area");
            if (shape == null) {
                shape = "none";
            } else {
                shape = URLDecoder.decode(shape,"UTF-8");
            }
            if (shape.equals("none") && pid.equals("none")) {
                return "";  //error
            }
            
            SimpleRegion region = SimpleShapeFile.parseWKT(shape);

            String count = String.valueOf(FilteringService.getSpeciesCount(pid, region));

            long endtime = System.currentTimeMillis();
            System.out.println("getSpeciesCount()=" + count + " in " + (endtime - starttime) + "ms");

            return count;
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
    @RequestMapping(value = "/apply/pid/{pid}/species/list", method = RequestMethod.POST)
    public
    @ResponseBody
    String getSpeciesList(@PathVariable String pid, HttpServletRequest req) {
        TabulationSettings.load();

        long starttime = System.currentTimeMillis();
        try {
            String shape = req.getParameter("area");
            if (shape == null) {
                shape = "none";
            } else {
                shape = URLDecoder.decode(shape,"UTF-8");
            }
            if (shape.equals("none") && pid.equals("none")) {
                return "";  //error
            }

            System.out.println("[[[]]] getlist: " + pid + " " + shape);

            SimpleRegion region = SimpleShapeFile.parseWKT(shape);

            String list = FilteringService.getSpeciesList(pid, region);
            long endtime = System.currentTimeMillis();
            System.out.println("getSpeciesCount().length=" + list.length() + " in " + (endtime - starttime) + "ms");

            return list;
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
    @RequestMapping(value = "/apply/pid/{pid}/samples/list", method = RequestMethod.POST)
    public
    @ResponseBody
    String getSamplesList(@PathVariable String pid, HttpServletRequest req) {
        TabulationSettings.load();

        try {
            String shape = req.getParameter("area");
            if (shape == null) {
                shape = "none";
            } else {
                shape = URLDecoder.decode(shape,"UTF-8");                
            }
            if (shape.equals("none") && pid.equals("none")) {
                return "";  //error
            }

            System.out.println("[[[]]] getsampleslist: " + pid + " " + shape);
            
            SimpleRegion region = SimpleShapeFile.parseWKT(shape);

            String filepath = FilteringService.getSamplesList(pid, region);

            /* zipping */
            String[] files = new String[1];
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
}
