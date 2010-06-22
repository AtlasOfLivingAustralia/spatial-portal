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
   
    @RequestMapping(value = "/process", method = RequestMethod.GET)
    public
    @ResponseBody
    String process(HttpServletRequest req) {
        String pid = "";
        try {
            TabulationSettings.load();

            long currTime = System.currentTimeMillis();

            String currentPath = req.getSession(true).getServletContext().getRealPath("/");
            String outputpath = currentPath + "output/aloc/" + currTime + "/";
            String outputfile = outputpath + "aloc.png";
            File fDir = new File(outputpath);
            fDir.mkdir();

            ssets = new SpatialSettings();

            String groupCount = req.getParameter("gc");
            Layer[] envList = getEnvFilesAsLayers(req.getParameter("envlist"));

            AlocService.run(outputfile, envList, Integer.parseInt(groupCount), null, null, Long.toString(currTime));

            /* register with LayerImgService */
            String extents = "252\n210\n112.083333333335\n-9.083333333335\n154.083333333335\n-44.083333333335";
            StringBuffer legend = new StringBuffer();
            System.out.println("legend path:" + outputpath + "aloc.png.csv");
            BufferedReader flegend = new BufferedReader(new FileReader(outputpath + "aloc.png.csv"));
            String line;
            while ((line = flegend.readLine()) != null) {
                legend.append(line);
                legend.append("\r\n");
            }
            flegend.close();
            System.out.println("registering layer image (A)");
            if (!LayerImgService.registerLayerImage(currentPath, pid, outputfile, extents, legend.toString())) {
                //error
            }

            pid = "" + currTime;
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return pid;
    }

    @RequestMapping(value = "/processgeo", method = RequestMethod.POST)
    public
    @ResponseBody
    String processgeo(HttpServletRequest req) {
        String pid = "";
        try {
            TabulationSettings.load();

            long currTime = System.currentTimeMillis();

            String currentPath = req.getSession(true).getServletContext().getRealPath("/");
            String outputpath = currentPath + "output/aloc/" + currTime + "/";
            String outputfile = outputpath + "aloc.png";
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
            
            /* register with LayerImgService */

            //TODO: get extents from aloc run
            String extents = "252\n210\n112.083333333335\n-9.083333333335\n154.083333333335\n-44.083333333335";

            StringBuffer legend = new StringBuffer();
            System.out.println("legend path:" + outputpath + "aloc.png.csv");
            BufferedReader flegend = new BufferedReader(new FileReader(outputpath + "aloc.png.csv"));
            String line;
            while ((line = flegend.readLine()) != null) {
                legend.append(line);
                legend.append("\r\n");
            }
            flegend.close();
            System.out.println("registering layer image (A): pid=" + currTime);
            if (!LayerImgService.registerLayerImage(currentPath, "" + currTime, outputfile, extents, legend.toString())) {
                //error
            }

            pid = "" + currTime;
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

        for (int j = 0; j < nameslist.length; j++) {
            for (int i = 0; i < _layerlist.length; i++) {
                if (_layerlist[i].display_name.equalsIgnoreCase(nameslist[j])) {
                    sellayers[j] = _layerlist[i];

                    System.out.println("Adding layer for ALOC: " + sellayers[j].name);
                    continue;
                }
            }
        }       

        return sellayers;
    }
}
