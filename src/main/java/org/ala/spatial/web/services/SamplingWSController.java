package org.ala.spatial.web.services;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.index.OccurrenceRecordNumbers;
import org.ala.spatial.analysis.index.OccurrencesCollection;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.util.AnalysisJobSampling;
import org.ala.spatial.util.AnalysisQueue;
import org.ala.spatial.util.CitationService;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.TabulationSettings;
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

    @RequestMapping(value = "/process/download", method = RequestMethod.POST)
    public
    @ResponseBody
    String process(HttpServletRequest req) {

        try {
            ssets = new SpatialSettings();

            String species = URLDecoder.decode(req.getParameter("taxonid"), "UTF-8").replace("__",".");
            
            System.out.println("species: " + species);
            System.out.println("envlist: " + req.getParameter("envlist"));
            System.out.println("envlist.count: " + req.getParameter("envlist").split(":").length);
            System.out.println("area: " + req.getParameter("area"));

            String area = req.getParameter("area");

            String[] layers = getLayerFiles(URLDecoder.decode(req.getParameter("envlist"), "UTF-8"));
            ArrayList<OccurrenceRecordNumbers> records = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                records = FilteringService.getRecords(req.getParameter("area"));
            } else {
                region = SimpleShapeFile.parseWKT(req.getParameter("area"));
            } 

            String [] n = OccurrencesCollection.getFirstName(species);
            String speciesName;
            if(n != null){
                speciesName = n[0];
            } else {
                speciesName = "";
            }
            
            SamplingService ss = SamplingService.newForLSID(species);
            String datafile = ss.sampleSpeciesAsCSV(species, layers, region, records, ssets.getInt("max_record_count_download"));

            String citationpath = null;
            try {
                citationpath = CitationService.generateCitationDataProviders(datafile);
            }catch (Exception e) {

            }

            Vector<String> vFiles = new Vector<String>();
            vFiles.add(datafile);
            if(citationpath != null){
                vFiles.add(citationpath);
            }

            String[] files = (String[]) vFiles.toArray(new String[vFiles.size()]);

            String[] filenames = new String[vFiles.size()];
            filenames[0] = "samples.csv";
            if(citationpath != null){
                filenames[1] = "citation.csv";
            }
            
            //String[] files = new String[vFiles.size()];
            Iterator it = vFiles.iterator();
            while (it.hasNext()) {
                System.out.println("Adding to download: " + it.next());
            }

            //String currentPath = req.getSession().getServletContext().getRealPath(File.separator);
            //TabulationSettings.load();
            String currentPath = TabulationSettings.base_output_dir;
            long currTime = System.currentTimeMillis();
            String outputpath = currentPath + File.separator + "output" + File.separator + "sampling" + File.separator;
            File fDir = new File(outputpath);
            fDir.mkdir();
            String outfile = fDir.getAbsolutePath() + File.separator + speciesName.replaceAll(" ", "_") + "_sample_" + currTime + ".zip";
            Zipper.zipFiles(files, filenames, outfile);

            return "/output/sampling/" + speciesName.replaceAll(" ", "_") + "_sample_" + currTime + ".zip";

        } catch (Exception e) {
            System.out.println("Error processing Sampling request:");
            e.printStackTrace(System.out);
        }

        return "";

    }

    @RequestMapping(value = "/processq/download", method = RequestMethod.POST)
    public
    @ResponseBody
    String processq(HttpServletRequest req) {

        try {
            ssets = new SpatialSettings();

            String species = URLDecoder.decode(req.getParameter("taxonid"), "UTF-8").replace("__",".");

            System.out.println("species: " + species);
            System.out.println("envlist: " + req.getParameter("envlist"));
            System.out.println("envlist.count: " + req.getParameter("envlist").split(":").length);
            System.out.println("area: " + req.getParameter("area"));

            String area = req.getParameter("area");

           /* String[] layers = getLayerFiles();
            int [] records = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                records = FilteringService.getRecords(req.getParameter("area"));
            } else {
                region = SimpleShapeFile.parseWKT(req.getParameter("area"));
            } */

            //String currentPath = req.getSession().getServletContext().getRealPath(File.separator);
            String currentPath = TabulationSettings.base_output_dir;

            String pid = Long.toString(System.currentTimeMillis());
            AnalysisJobSampling ajs = new AnalysisJobSampling(pid, currentPath, species, req.getParameter("envlist"), area);
            StringBuffer inputs = new StringBuffer();
            inputs.append("pid:").append(pid);
            inputs.append(";taxonid:").append(species);

            String [] n = OccurrencesCollection.getFirstName(species);
            String speciesName;
            if(n != null){
                speciesName = n[0];
                inputs.append(";scientificName:").append(n[0]);
                inputs.append(";taxonRank:").append(n[1]);
            } else {
                speciesName = "";
            }

            inputs.append(";area:").append(area);
            inputs.append(";envlist:").append(req.getParameter("envlist"));
            inputs.append(";output_path:").append("/output/sampling/").append(speciesName.replaceAll(" ", "_")).append("_sample_").append(pid).append(".zip");
            ajs.setInputs(inputs.toString());
            AnalysisQueue.addJob(ajs);

            return pid;

            /*
            SamplingService ss = new SamplingService();
            String datafile = ss.sampleSpeciesAsCSV(species, layers, region, records, ssets.getInt("max_record_count_download"));

            Vector<String> vFiles = new Vector<String>();
            vFiles.add(datafile);

            String[] files = (String[]) vFiles.toArray(new String[vFiles.size()]);

            //String[] files = new String[vFiles.size()];
            Iterator it = vFiles.iterator();
            while (it.hasNext()) {
                System.out.println("Adding to download: " + it.next());
            }

            //String currentPath = req.getSession().getServletContext().getRealPath(File.separator);
            //TabulationSettings.load();
            String currentPath = TabulationSettings.base_output_dir;
            long currTime = System.currentTimeMillis();
            String outputpath = currentPath + File.separator + "output" + File.separator + "sampling" + File.separator;
            File fDir = new File(outputpath);
            fDir.mkdir();
            String outfile = fDir.getAbsolutePath() + File.separator + species.replaceAll(" ", "_") + "_sample_" + currTime + ".zip";
            Zipper.zipFiles(files, outfile);

            return "/output/sampling/" + species.replaceAll(" ", "_") + "_sample_" + currTime + ".zip";*/

        } catch (Exception e) {
            System.out.println("Error processing Sampling request:");
            e.printStackTrace(System.out);
        }

        return "";

    }

    @RequestMapping(value = "/process/preview", method = RequestMethod.POST)
    public
    @ResponseBody
    String preview(HttpServletRequest req) {

        try {

            ssets = new SpatialSettings();

            String species = URLDecoder.decode(req.getParameter("taxonid"), "UTF-8").replace("__",".");
            String[] layers = getLayerFiles(req.getParameter("envlist"));

            System.out.println("species: " + species);
            System.out.println("envlist: " + req.getParameter("envlist"));
            System.out.println("envlist.count: " + req.getParameter("envlist").split(":").length);
            System.out.println("layers:" + layers);
            System.out.println("area: " + req.getParameter("area"));
            
            String area = req.getParameter("area");
            ArrayList<OccurrenceRecordNumbers> records = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                records = FilteringService.getRecords(req.getParameter("area"));
            } else {
                region = SimpleShapeFile.parseWKT(req.getParameter("area"));
            }

            SamplingService ss = SamplingService.newForLSID(species);
            String[][] results = ss.sampleSpecies(species, layers, region, records, 20);

            List rList = new Vector();
            StringBuilder sbResults = new StringBuilder();
            
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

            pathlist[j] = Layers.layerDisplayNameToName(nameslist[j]); 
        }

        return pathlist;
    }

    @RequestMapping(value = "/process/points", method = RequestMethod.POST)
    public
    @ResponseBody
    String processpoints(HttpServletRequest req) {

        try {
            ssets = new SpatialSettings();

            String species = URLDecoder.decode(req.getParameter("taxonid"), "UTF-8").replace("__",".");
            
            System.out.println("species: " + species);

            SamplingService ss = SamplingService.newForLSID(species);
            
            String area = req.getParameter("area");
            ArrayList<OccurrenceRecordNumbers> records = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                records = FilteringService.getRecords(req.getParameter("area"));
            } else {
                region = SimpleShapeFile.parseWKT(req.getParameter("area"));
            }
            double [] points = null;//ss.sampleSpeciesPoints(species, region, records);
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


    /**
     * histogram:
     *
     * lsid for species
     * env layer name
     * number of segments to produce
     *
     * 1. get env layer data for lsid
     * 2. scale between segments
     * 3. return segment counts
     * 4. return segment lower bounds + top bound
     *
     *
     *
     * intersection histogram:
     *
     * lsid for species
     * env layer name
     * number of segments to produce
     * 2nd env layer name
     * 2nd env layer upper bound
     * 2nd env layer lower bound
     *
     * 5. return intersection counts
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/histogram", method = RequestMethod.POST)
    public
    @ResponseBody
    String histogram(HttpServletRequest req) {
        try {
            ssets = new SpatialSettings();

            String species = URLDecoder.decode(req.getParameter("taxonid"), "UTF-8").replace("__",".");
            String[] layers = getLayerFiles(req.getParameter("envlist"));

            System.out.println("species: " + species);
            System.out.println("envlist: " + req.getParameter("envlist"));
            System.out.println("envlist.count: " + req.getParameter("envlist").split(":").length);
            System.out.println("layers:" + layers);
            System.out.println("area: " + req.getParameter("area"));

            String area = req.getParameter("area");
            ArrayList<OccurrenceRecordNumbers> records = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                records = FilteringService.getRecords(req.getParameter("area"));
            } else {
                region = SimpleShapeFile.parseWKT(req.getParameter("area"));
            }

            SamplingService ss = SamplingService.newForLSID(species);
            String[][] results = null;//ss.sampleSpecies(species, layers, region, records, 20);

            List rList = new Vector();
            StringBuilder sbResults = new StringBuilder();

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

            return sbResults.toString();

        } catch (Exception e) {
            System.out.println("Error processing Sampling request:");
            e.printStackTrace(System.out);
        }

        return null;

    }

    /**
     * lsid (mandatory)
     * points or layer name
     * @param req
     * @return
     */
    @RequestMapping(value = "/scatterplot", method = RequestMethod.POST)
    public
    @ResponseBody
    String scatterplot(HttpServletRequest req) {
        try {
            String species = URLDecoder.decode(req.getParameter("taxonid"), "UTF-8").replace("__",".");
            String[] layers = getLayerFiles(req.getParameter("envlist"));

            SamplingService ss = SamplingService.newForLSID(species);
            String[][] results = ss.sampleSpecies(species, layers, null, null, 1000000);

            StringBuilder sbResults = new StringBuilder();

            //last 4 columns; longitude, latitude, layer1, layer2
            for (int i = 0; i < results.length; i++) {
                for (int j = results[i].length - 4; j < results[i].length; j++) {
                    if (results[i][j] != null) {
                        sbResults.append(results[i][j]);
                    }
                    if (j < results[i].length - 1) {
                        sbResults.append(",");
                    }
                }
                sbResults.append("\n");
            }

            return sbResults.toString();

        }catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    @RequestMapping(value = "/scatterplot/register", method = RequestMethod.POST)
    public
    @ResponseBody
    String scatterplotRegister(HttpServletRequest req) {
        try {
            //int highlightLsid(String keyEnd, String lsid, String layer1, double x1, double x2, String layer2, double y1, double y2)

            String pid = String.valueOf(System.currentTimeMillis());

            String species = URLDecoder.decode(req.getParameter("taxonid"), "UTF-8").replace("__",".");
            String[] layers = getLayerFiles(req.getParameter("envlist"));

            String params = URLDecoder.decode(req.getParameter("bounds"), "UTF-8");
            double [] bounds = new double[4];
            String [] aParams = params.split(",");
            for(int i=0;i<aParams.length;i++) {
                bounds[i] = Double.parseDouble(aParams[i]);
            }

            int count = OccurrencesCollection.highlightLsid(pid, species, layers[0], bounds[0], bounds[1], layers[1], bounds[2], bounds[3]);

            return pid + "\n" + String.valueOf(count);
        }catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}


