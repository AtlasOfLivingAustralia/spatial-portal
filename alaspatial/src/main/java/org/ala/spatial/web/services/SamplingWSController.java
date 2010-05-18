package org.ala.spatial.web.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.tabulation.SamplingService;
import org.ala.spatial.analysis.tabulation.SpeciesListIndex;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.Zipper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws/sampling/")
public class SamplingWSController {

    private SpatialSettings ssets;
    private List alllayers;

    @RequestMapping(value = "/process/download", method = RequestMethod.GET)
    public
    @ResponseBody
    String process(HttpServletRequest req) {

        try {
            ssets = new SpatialSettings();

            String species = req.getParameter("taxonid");
            String[] layers = getLayerFiles(req.getParameter("envlist"));

            System.out.println("species: " + species);
            System.out.println("envlist: " + req.getParameter("envlist"));
            System.out.println("envlist.count: " + req.getParameter("envlist").split(":").length);
            System.out.println("points: " + req.getParameter("points"));
            
            SimpleRegion region = SimpleRegion.parseSimpleRegion(req.getParameter("points"));

            SamplingService ss = new SamplingService();
            String datafile = ss.sampleSpecies(species, layers, region);

            Vector<String> vFiles = new Vector<String>();
            vFiles.add(datafile);

            /* add each catagorical layer */
            if (layers != null) {
                for (String s : layers) {
                    String csv = SpeciesListIndex.getLayerExtents(s);
                    if (csv != null) {
                        System.out.println("csv:\n " + csv);
                    }
                    if (csv != null && csv.length() > 0
                            && csv.contains("<br>")) {

                        File tmpLayerFile = File.createTempFile(SamplingService.layerNameToDisplayName(s) + "_lookup_values", ".csv");
                        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tmpLayerFile)));
                        out.write(csv.replace("<br>", ""));
                        out.close();
                        vFiles.add(tmpLayerFile.getAbsolutePath());
                    }
                }
            }

            String[] files = (String[]) vFiles.toArray(new String[vFiles.size()]);

            //String[] files = new String[vFiles.size()];
            Iterator it = vFiles.iterator();
            while (it.hasNext()) {
                System.out.println("Adding to download: " + it.next());
            }

            String currentPath = req.getSession().getServletContext().getRealPath("/");
            long currTime = System.currentTimeMillis();
            String outputpath = currentPath + "/output/sampling/";
            File fDir = new File(outputpath);
            fDir.mkdir();
            String outfile = fDir.getAbsolutePath() + "/" + species.replaceAll(" ", "_") + "_sample_" + currTime + ".zip";
            Zipper.zipFiles(files, outfile);

            return "/output/sampling/" + species.replaceAll(" ", "_") + "_sample_" + currTime + ".zip";

        } catch (Exception e) {
            System.out.println("Error processing Sampling request:");
            e.printStackTrace(System.out);
        }

        return "";

    }

    @RequestMapping(value = "/process/preview", method = RequestMethod.GET)
    public
    @ResponseBody
    String preview(HttpServletRequest req) {

        try {

            ssets = new SpatialSettings();

            String species = req.getParameter("taxonid");
            String[] layers = getLayerFiles(req.getParameter("envlist"));

            System.out.println("species: " + species);
            System.out.println("envlist: " + req.getParameter("envlist"));
            System.out.println("envlist.count: " + req.getParameter("envlist").split(":").length);
            System.out.println("layers:" + layers);
            System.out.println("points: " + req.getParameter("points"));
            
            SimpleRegion region = SimpleRegion.parseSimpleRegion(req.getParameter("points"));

            SamplingService ss = new SamplingService();
            String[][] results = ss.sampleSpecies(species, layers, region, 20);

            List rList = new Vector();
            StringBuilder sbResults = new StringBuilder();

            System.out.println("results: " + results.length + " - " + results[0].length);
            for (int i = 0; i < results.length; i++) {
                //System.out.println(results[i]);
                //System.out.println("");
                Hashtable htRecs = new Hashtable();
                for (int j = 0; j < results[i].length; j++) {
                    System.out.print("|" + results[i][j]);
                    if (results[i][j] != null) {
                        htRecs.put(j, results[i][j]);
                        sbResults.append(results[i][j]);
                    }
                    if (j < results[i].length - 1) {
                        sbResults.append("~");
                    }
                }
                System.out.println("|");
                sbResults.append(";");
                rList.add(htRecs);
            }

            //return results;
            //return rList;
            return sbResults.toString();

        } catch (Exception e) {
            System.out.println("Error processing Sampling request:");
            e.printStackTrace(System.out);
        }

        return null;

    }

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public
    @ResponseBody
    Map test(HttpServletRequest req) {

        Map list = null;

        try {

            list = new Hashtable();
            list.put("firstname", "Ajay");
            list.put("lastname", "Ranipeta");
            list.put("address", "Sydney");

        } catch (Exception e) {
            System.out.println("Error processing Sampling request:");
            e.printStackTrace(System.out);
        }

        return list;

    }

    private String[] getLayerFiles(String envNames) {
    	if(envNames.equals("none")){
    		return null;
    	}
        String[] nameslist = envNames.split(":");
        String[] pathlist = new String[nameslist.length];

        System.out.println("Got envlist.count: " + nameslist.length);

        for (int j = 0; j < nameslist.length; j++) {

            //Layer[] _layerlist1 = ssets.getEnvironmentalLayers();
            //Layer[] _layerlist2 = ssets.getContextualLayers();

            pathlist[j] = SamplingService.layerDisplayNameToName(nameslist[j]); 
            /*
            for (int i = 0; i < _layerlist1.length; i++) {
                if (_layerlist1[i].display_name.equalsIgnoreCase(nameslist[j])) {
                    pathlist[j] = _layerlist1[i].name;
                    continue;
                }
            }

            for (int i = 0; i < _layerlist2.length; i++) {
                if (_layerlist2[i].display_name.equalsIgnoreCase(nameslist[j])) {
                    pathlist[j] = _layerlist2[i].name;
                    continue;
                }
            }
            *
            */
        }

        return pathlist;
    }

    private void writeAsJSON() {
    }
    
    @RequestMapping(value = "/process/points", method = RequestMethod.GET)
    public
    @ResponseBody
    String processpoints(HttpServletRequest req) {

        try {
            ssets = new SpatialSettings();

            String species = req.getParameter("taxonid");
            
            System.out.println("species: " + species);

            SamplingService ss = new SamplingService();
            
            SimpleRegion region = SimpleRegion.parseSimpleRegion(req.getParameter("points"));
            
            double [] points = ss.sampleSpeciesPoints(species,region);
            StringBuffer sb = new StringBuffer();
            for(int i=0;i<points.length;i+=2){
            	sb.append(points[i]);
            	sb.append(":");
            	sb.append(points[i+1]);
            	if(i < points.length-1){
            		sb.append(",");
            	}
            }

            return sb.toString();

        } catch (Exception e) {
            System.out.println("Error processing Sampling request:");
            e.printStackTrace(System.out);
        }

        return "";

    }

}
