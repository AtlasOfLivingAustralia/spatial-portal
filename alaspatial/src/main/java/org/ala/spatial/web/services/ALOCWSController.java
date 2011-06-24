package org.ala.spatial.web.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.service.AlocService;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.analysis.service.LayerImgService;
import org.ala.spatial.util.AnalysisJobAloc;
import org.ala.spatial.util.AnalysisQueue;
import org.ala.spatial.util.CoordinateTransformer;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.TabulationSettings;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws/aloc/")
public class ALOCWSController {

    private SpatialSettings ssets;
   
    @RequestMapping(value = "/processgeo", method = RequestMethod.POST)
    public
    @ResponseBody
    String processgeo(HttpServletRequest req) {
        String pid = "";
        try {
            TabulationSettings.load();

            long currTime = System.currentTimeMillis();

            //String currentPath = req.getSession(true).getServletContext().getRealPath(File.separator);
            String currentPath = TabulationSettings.base_output_dir;
            String outputpath = currentPath + "output" + File.separator + "aloc" + File.separator + currTime + File.separator;
            String outputfile = outputpath + "aloc.png";
            String outputfile_orig = outputfile;
            File fDir = new File(outputpath);
            fDir.mkdir();

            ssets = new SpatialSettings();

            String groupCount = req.getParameter("gc");
            Layer[] envList = getEnvFilesAsLayers(req.getParameter("envlist"));

            String area = req.getParameter("area");

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = FilteringService.getFilters(req.getParameter("area"));
            } else {
                region = SimpleShapeFile.parseWKT(req.getParameter("area"));
            }

            AlocService.run(outputfile, envList, Integer.parseInt(groupCount), region, filter, Long.toString(currTime));

            String line;

            //get extents from aloc run
            StringBuffer extents = new StringBuffer();
            BufferedReader br = new BufferedReader(new FileReader(outputfile_orig + "extents.txt"));
            int width = Integer.parseInt(br.readLine());
            int height = Integer.parseInt(br.readLine());
            double xmin = Double.parseDouble(br.readLine());
            double ymin = Double.parseDouble(br.readLine());
            double xmax = Double.parseDouble(br.readLine());
            double ymax = Double.parseDouble(br.readLine());
            br.close();
            br = new BufferedReader(new FileReader(outputfile_orig + "extents.txt"));
            while((line = br.readLine()) != null) {
                extents.append(line).append("\n");
            }
            br.close();

            CoordinateTransformer.generateWorldFiles(outputpath, "aloc",
                    String.valueOf((xmax-xmin)/(double)width),
                    "-" + String.valueOf((ymax-ymin)/(double)height),
                    String.valueOf(xmin),
                    String.valueOf(ymax));
            System.out.println("OUT1: " + outputfile);
            outputfile=CoordinateTransformer.transformToGoogleMercator(outputfile);
            System.out.println("OUT2: " + outputfile);
            
            /* register with LayerImgService */


            StringBuffer legend = new StringBuffer();
            System.out.println("legend path:" + outputpath + "classification_means.csv");
            BufferedReader flegend = new BufferedReader(new FileReader(outputpath + "classification_means.csv"));
            while ((line = flegend.readLine()) != null) {
                legend.append(line);
                legend.append("\r\n");
            }
            flegend.close();

            StringBuffer metadata = new StringBuffer();
            System.out.println("meatadata path:" + outputpath + "classification.html");
            BufferedReader fmetadata = new BufferedReader(new FileReader(outputpath + "classification.html"));
            while ((line = fmetadata.readLine()) != null) {
                metadata.append(line);
                metadata.append("\n");
            }
            fmetadata.close();

            System.out.println("registering layer image (A): pid=" + currTime);
            if (!LayerImgService.registerLayerImage(currentPath, "" + currTime, outputfile, extents.toString(), legend.toString(), metadata.toString())) {
                //error
            }

            pid = "" + currTime;
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return pid;
    }

    @RequestMapping(value = "/processgeoq", method = RequestMethod.POST)
    public
    @ResponseBody
    String processgeoq(HttpServletRequest req) {
        String pid = "";
        try {
            TabulationSettings.load();

            long currTime = System.currentTimeMillis();

            //String currentPath = req.getSession(true).getServletContext().getRealPath(File.separator);
            String currentPath = TabulationSettings.base_output_dir;
           
            ssets = new SpatialSettings();

            String groupCount = req.getParameter("gc");
            Layer[] envList = getEnvFilesAsLayers(req.getParameter("envlist"));

            String area = req.getParameter("area");

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = FilteringService.getFilters(req.getParameter("area"));
            } else {
                region = SimpleShapeFile.parseWKT(req.getParameter("area"));
            }

            pid = Long.toString(currTime);
            AnalysisJobAloc aja = new AnalysisJobAloc(pid, currentPath, envList, Integer.parseInt(groupCount), region, filter, area);
            StringBuffer inputs = new StringBuffer();
            inputs.append("pid:").append(pid);
            inputs.append(";gc:").append(groupCount);
            inputs.append(";area:").append(area);
            inputs.append(";envlist:").append(req.getParameter("envlist"));
            aja.setInputs(inputs.toString());
            AnalysisQueue.addJob(aja);
            
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return pid;
    }

    private Layer[] getEnvFilesAsLayers(String envNames) {
        try {
            envNames = URLDecoder.decode(envNames, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace(System.out);
        }
        String[] nameslist = envNames.split(":");
        Layer[] sellayers = new Layer[nameslist.length];

        Layer[] _layerlist = ssets.getEnvironmentalLayers();
        int nulls = 0;
        for (int j = 0; j < nameslist.length; j++) {
            int i;
            for (i = 0; i < _layerlist.length; i++) {
                if (_layerlist[i].display_name.equalsIgnoreCase(nameslist[j])
                        || _layerlist[i].name.equalsIgnoreCase(nameslist[j])) {
                    sellayers[j] = _layerlist[i];

                    System.out.println("Adding layer for ALOC: " + sellayers[j].name);
                    break;
                }
            }
            if(i == _layerlist.length){
                System.out.println("Cannot add layer: " + nameslist[j]);
                nulls++;
            }
        }

        //remove nulls
        Layer[] sellayersNoNulls = new Layer[sellayers.length - nulls];
        int pos = 0;
        for(int j=0;j<sellayers.length;j++){
            if(sellayers[j] != null) {
                sellayersNoNulls[pos++] = sellayers[j];
            }
        }

        return sellayers;
    }
}
