package org.ala.spatial.web.services;

import au.com.bytecode.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.ala.spatial.analysis.service.FilteringImage;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.index.FilteringIndex;
import org.ala.spatial.util.CitationService;
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
    private String outputpath = File.separator + "output" + File.separator + "filtering" + File.separator;

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

            File workingDir = new File(session.getServletContext().getRealPath(outputpath + currTime + File.separator));
            //File workingDir = new File(TabulationSettings.base_output_dir + outputpath);
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

                    File workingDir = new File(session.getServletContext().getRealPath(File.separator + "output" + File.separator + "filtering" + File.separator + pid + File.separator));
                    //File workingDir = new File(TabulationSettings.base_output_dir + outputpath);

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

                    //return file.getName();

                    String filenamepart = file.getName();
                    filenamepart = filenamepart.substring(0, filenamepart.lastIndexOf("."));
                    //CoordinateTransformer.generateWorldFiles(workingDir.getAbsolutePath(), filenamepart,
                    //        String.valueOf(TabulationSettings.grd_xdiv), "-" + String.valueOf(TabulationSettings.grd_ydiv),
                    //        String.valueOf(TabulationSettings.grd_xmin), String.valueOf(TabulationSettings.grd_ymin));
                    //CoordinateTransformer.generateWorldFiles(workingDir.getAbsolutePath(), filenamepart,
                    //       String.valueOf((154-122) / 1008.0), "-" + String.valueOf((-9 - (-44)) / 840.0),
                    //       String.valueOf(112), String.valueOf(-9));
                    //String outputfile = CoordinateTransformer.transformToGoogleMercator(file.getAbsolutePath());
                    //return outputfile.substring(outputfile.lastIndexOf(File.separator)+1);
                    return file.getName();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    @RequestMapping(value = "/merge/pid/{pid}", method = RequestMethod.GET)
    public
    @ResponseBody
    String merge(@PathVariable String pid,
            HttpServletRequest req) {
        FilteringImage filteringImage2;
        try {
            HttpSession session = req.getSession(true);

            File workingDir = new File(session.getServletContext().getRealPath(File.separator + "output" + File.separator + "filtering" + File.separator + pid + File.separator));

            File file = File.createTempFile("spl", ".png", workingDir);

            //setup for accumulative image
            filteringImage2 = new FilteringImage(file.getPath(), 0x00000000);

            // apply the filters by iterating thru' the layers from client, make spl, should be one layer
            LayerFilter [] lf = FilteringService.getFilters(pid);
            for (int i = 0; i < lf.length; i++) {  
                if (lf[i].getLayer().type.equalsIgnoreCase("environmental")) {
                    filteringImage2.applyFilterAccumulative(lf[i].getLayer().name, lf[i].getMinimum_value(), lf[i].getMaximum_value());
                } else {
                    if (lf[i].getCatagories().length > 0) {
                        filteringImage2.applyFilterAccumulative(lf[i].getLayer().name, lf[i].getCatagories());
                    }
                }
            }

            String boundingBoxString = filteringImage2.writeImageAccumulative(0xFFFF0000); //red

            String filenamepart = file.getName();
            filenamepart = filenamepart.substring(0, filenamepart.lastIndexOf("."));
            return file.getName() + "\n" + boundingBoxString;
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

                    File workingDir = new File(session.getServletContext().getRealPath(File.separator + "output" + File.separator + "filtering" + File.separator + pid + File.separator));
                    //File workingDir = new File(TabulationSettings.base_output_dir + outputpath);

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

                    //return file.getName();

                    /*String filenamepart = file.getName();
                    filenamepart = filenamepart.substring(0, filenamepart.lastIndexOf(".")); 
                    //CoordinateTransformer.generateWorldFiles(workingDir.getAbsolutePath(), filenamepart,
                    //        String.valueOf(TabulationSettings.grd_xdiv), "-" + String.valueOf(TabulationSettings.grd_ydiv),
                    //        String.valueOf(TabulationSettings.grd_xmin), String.valueOf(TabulationSettings.grd_ymin));
                    CoordinateTransformer.generateWorldFiles(workingDir.getAbsolutePath(), filenamepart,
                    String.valueOf((154-122) / 1008.0), "-" + String.valueOf((-9 - (-44)) / 840.0),
                    String.valueOf(112), String.valueOf(-9));
                    String outputfile = CoordinateTransformer.transformToGoogleMercator(file.getAbsolutePath());
                    return outputfile.substring(outputfile.lastIndexOf(File.separator)+1);
                     * *
                     */
                    return file.getName();
                }
            }

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return "";
    }

    @RequestMapping(value = "/apply/pid/{pid}/species/count", method = RequestMethod.POST)
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
                shape = URLDecoder.decode(shape, "UTF-8");
            }
            if (shape.equals("none") && pid.equals("none")) {
                return "";  //error
            }

            SimpleRegion region = SimpleShapeFile.parseWKT(shape);

            int[] counts = FilteringService.getSpeciesCount(pid, region);
            String count = String.valueOf(counts[0] + "\n" + counts[1]);

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
                shape = URLDecoder.decode(shape, "UTF-8");
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
                shape = URLDecoder.decode(shape, "UTF-8");
            }
            if (shape.equals("none") && pid.equals("none")) {
                return "";  //error
            }

            System.out.println("[[[]]] getsampleslist: " + pid + " " + shape);

            SimpleRegion region = SimpleShapeFile.parseWKT(shape);

            String filepath = FilteringService.getSamplesList(pid, region, TabulationSettings.MAX_RECORD_COUNT);

            String citationpath = CitationService.generateCitationDataProviders(filepath);

            /* zipping */
            String[] files = new String[2];
            files[0] = filepath;
            files[1] = citationpath;

            String[] filenames = new String[2];
            filenames[0] = "samples.csv";
            filenames[1] = "citation.csv";

            String currentPath = req.getSession().getServletContext().getRealPath(File.separator);
            //String currentPath = TabulationSettings.base_output_dir;
            long currTime = System.currentTimeMillis();
            String outputpath = currentPath + File.separator + "output" + File.separator + "filtering" + File.separator;
            File fDir = new File(outputpath);
            fDir.mkdir();
            SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
            String sdate = date.format(new Date());
            String outfile = fDir.getAbsolutePath() + File.separator + "Sample_" + sdate + "_" + currTime + ".zip";
            Zipper.zipFiles(files, filenames, outfile);

            return "output/filtering/" + "Sample_" + sdate + "_" + currTime + ".zip";

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return "";
    }

    /**
     * Returns string of samples preview
     *
     * @param pid
     * @param shape
     * @param req
     * @return
     */
    @RequestMapping(value = "/apply/pid/{pid}/samples/list/preview", method = RequestMethod.POST)
    public
    @ResponseBody
    String getSamplesListPreview(@PathVariable String pid, HttpServletRequest req) {
        TabulationSettings.load();

        try {
            String shape = req.getParameter("area");
            if (shape == null) {
                shape = "none";
            } else {
                shape = URLDecoder.decode(shape, "UTF-8");
            }
            if (shape.equals("none") && pid.equals("none")) {
                return "";  //error
            }

            System.out.println("[[[]]] getsampleslist: " + pid + " " + shape);

            SimpleRegion region = SimpleShapeFile.parseWKT(shape);

            String filepath = FilteringService.getSamplesList(pid, region, 20);

            //read in file
            StringBuffer sbResults = new StringBuffer();
            try {
                CSVReader reader = new CSVReader(new FileReader(filepath));
                List<String[]> contents = reader.readAll();
                for (int i = 0; i < contents.size(); i++) {
                    String[] results = contents.get(i);
                    for (int j = 0; j < results.length; j++) {
                        if (results[j] != null) {
                            sbResults.append(results[j]);
                        }
                        if (j < results.length - 1) {
                            sbResults.append("~");
                        }
                    }
                    sbResults.append(";");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return sbResults.toString();

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
    @RequestMapping(value = "/apply/pid/{pid}/samples/geojson", method = RequestMethod.GET)
    public
    @ResponseBody
    String getSamplesListAsGeoJSON(@PathVariable String pid, HttpServletRequest req) {
        TabulationSettings.load();

        try {
            String shape = req.getParameter("area");
            if (shape == null) {
                shape = "none";
            } else {
                shape = URLDecoder.decode(shape, "UTF-8");
            }
            if (shape.equals("none") && pid.equals("none")) {
                return "";  //error
            }

            System.out.println("[[[]]] getsampleslist: " + pid + " " + shape);

            SimpleRegion region = null;
            ArrayList<Integer> records = null;
            shape = URLDecoder.decode(shape, "UTF-8");
            if (shape != null && shape.startsWith("ENVELOPE")) {
                records = FilteringService.getRecords(shape);
            } else {
                region = SimpleShapeFile.parseWKT(shape);
            }


            String currentPath = req.getSession().getServletContext().getRealPath(File.separator);
            String outputpath = currentPath + File.separator + "output" + File.separator + "filtering" + File.separator;
            File fDir = new File(outputpath);
            fDir.mkdir();

            String gjsonFile = FilteringService.getSamplesListAsGeoJSON("none" /*TODO: allow pid here*/, region, records, fDir);

            System.out.println("getSamplesListAsGeoJSON:" + gjsonFile);
            return "output/filtering/" + gjsonFile;
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return "";
    }
}
