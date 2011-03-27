package org.ala.spatial.web.services;

import java.awt.Color;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ala.spatial.analysis.cluster.Record;
import org.ala.spatial.analysis.index.FilteringIndex;
import org.ala.spatial.analysis.index.OccurrenceRecordNumbers;
import org.ala.spatial.analysis.index.OccurrencesCollection;
import org.ala.spatial.analysis.index.OccurrencesFilter;
import org.ala.spatial.analysis.index.SpeciesColourOption;
import org.ala.spatial.analysis.index.SpeciesIndex;
import org.ala.spatial.analysis.legend.Legend;
import org.ala.spatial.analysis.legend.LegendEvenInterval;
import org.ala.spatial.analysis.legend.LegendEvenIntervalLog10;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.util.AnalysisJobSampling;
import org.ala.spatial.util.AnalysisQueue;
import org.ala.spatial.util.CitationService;
import org.ala.spatial.util.Grid;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.OccurrencesFieldsUtil;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.Zipper;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleAnchor;
import org.springframework.web.bind.annotation.RequestParam;

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
            if(species.length() == 0 || species.equals("null")) {
                species = null;
            }
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

            String colourMode = req.getParameter("colourmode");

            String areaRestrict = req.getParameter("arearestrict");
            ArrayList<OccurrenceRecordNumbers> recordsR = null;
            SimpleRegion regionR = null;
            if(areaRestrict != null) {
                if (areaRestrict != null && areaRestrict.startsWith("ENVELOPE")) {
                    recordsR = FilteringService.getRecords(areaRestrict);
                } else {
                    regionR= SimpleShapeFile.parseWKT(areaRestrict);
                }
            }

            String areaHighlight = req.getParameter("areahighlight");
            ArrayList<OccurrenceRecordNumbers> recordsH = null;
            SimpleRegion regionH = null;
            if(areaHighlight != null) {
                if (areaHighlight != null && areaHighlight.startsWith("ENVELOPE")) {
                    recordsH = FilteringService.getRecords(areaHighlight);
                } else {
                    regionH = SimpleShapeFile.parseWKT(areaHighlight);
                }
            }

            SamplingService ss = SamplingService.newForLSID(species);
            String[][] results = ss.sampleSpecies(species, layers, regionR, recordsR, 10000000);

            long [] recordsHids = null;
            if(recordsH != null) {
                //get occurrenceID
                Vector v = OccurrencesCollection.getRecords(new OccurrencesFilter(species, regionR, recordsH, 10000000));
                recordsHids = new long[v.size()];
                for(int i=0;i<v.size();i++) {
                    recordsHids[i] = ((Record) v.get(i)).getId();
                }
                java.util.Arrays.sort(recordsHids);
            }

            StringBuilder sbResults = new StringBuilder();
            StringBuilder sbResultsHighlight = new StringBuilder();

            SpeciesColourOption sco = null;
            if(colourMode != null) {
                sco = SpeciesColourOption.fromName(colourMode, false);
            }

            String speciesName = "";
            if(species != null) {
                speciesName = SpeciesIndex.getScientificName(SpeciesIndex.findLSID(species));
            }

            //last 2 columns; layer1, layer2
            for (int i = 0; i < results.length; i++) {
                //include occurrenceID
                sbResults.append(results[i][TabulationSettings.occurrences_csv_twos_names.length]).append(",");

                //is there a series (colourMode) ?
                if(sco != null) {
                    sbResults.append(results[i][sco.getPos()]).append(",");
                } else {
                    sbResults.append(speciesName).append(",");
                }

                //is there an area highlight?
                boolean highlightRecord = false;
                if(i > 0) {
                    if(regionH != null) {
                        double longitude = Double.parseDouble(results[i][results[i].length - 4]);
                        double latitude = Double.parseDouble(results[i][results[i].length - 3]);
                        if(regionH.isWithin(longitude, latitude)) {
                            sbResultsHighlight.append(results[i][TabulationSettings.occurrences_csv_twos_names.length]).append(",In Active Area,");
                            highlightRecord = true;
                        }
                    } else if(recordsH != null) {
                        if(java.util.Arrays.binarySearch(recordsHids, Long.parseLong((results[i][TabulationSettings.occurrences_csv_twos_names.length]))) >= 0) {
                            sbResultsHighlight.append(results[i][TabulationSettings.occurrences_csv_twos_names.length]).append(",In Active Area,");
                            highlightRecord = true;
                        }
                    }
                }

                for (int j = results[i].length - 2; j < results[i].length; j++) {
                    if (results[i][j] != null) {
                        sbResults.append(results[i][j]);
                        if(highlightRecord) {
                            sbResultsHighlight.append(results[i][j]);
                        }
                    }
                    if (j < results[i].length - 1) {
                        sbResults.append(",");
                        if(highlightRecord) {
                            sbResultsHighlight.append(",");
                        }
                    }
                }
                sbResults.append("\n");
                if(highlightRecord) {
                    sbResultsHighlight.append("\n");
                }
            }

            sbResults.append(sbResultsHighlight.toString());

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

            String species = URLDecoder.decode(req.getParameter("lsid"), "UTF-8").replace("__",".");

            //count parameters
            int paramCount = 0;
            while(req.getParameter("param" + (paramCount)) != null) {
                paramCount++;
            }
            Object [] filters = new Object[paramCount];
            for(int i=0;i<paramCount;i++) {
                String param = req.getParameter("param" + i);
                String [] parts = param.split(",");
                Object [] o = null;
                //type is first, then name, then any values
                if(parts[1].equalsIgnoreCase("string")) {
                    o = new Object[3];
                    o[0] = parts[0];
                    o[1] = parts[1];
                    o[2] = java.util.Arrays.copyOfRange(parts, 2, parts.length);
                } else if(parts[1].equalsIgnoreCase("integer")) {
                    o = new Object[4];
                    o[0] = parts[0];
                    o[1] = parts[2];
                    try {
                        if(parts[3].equalsIgnoreCase("NaN")) {
                            o[2] = Integer.MIN_VALUE;
                        } else {
                            o[2] = Integer.parseInt(parts[3]);
                        }
                        if(parts[4].equalsIgnoreCase("NaN")) {
                            o[3] = Integer.MIN_VALUE;
                        } else {
                            o[3] = Integer.parseInt(parts[4]);
                        }
                    } catch (Exception e) {
                        o = null;
                    }
                } else if(parts[1].equalsIgnoreCase("double")) {
                    o = new Object[4];
                    o[0] = parts[0];
                    o[1] = parts[2];
                    try {
                        if(parts[3].equalsIgnoreCase("NaN")) {
                            o[2] = Double.NaN;
                        } else {
                            o[2] = Double.parseDouble(parts[3]);
                        }
                        if(parts[4].equalsIgnoreCase("NaN")) {
                            o[3] = Double.NaN;
                        } else {
                            o[3] = Double.parseDouble(parts[4]);
                        }
                    } catch (Exception e) {
                        o = null;
                    }
                } else if(parts[1].equalsIgnoreCase("boolean")) {
                    o = new Object[3];
                    o[1] = parts[0];
                    o[2] = parts[2];
                    try {
                        o[3] = Boolean.parseBoolean(parts[3]);
                    } catch (Exception e) {
                        o = null;
                    }
                }

                if(o == null) {
                    System.out.println("invalid scatterplot param" + (i+1) + ": " + param);
                    return null;
                } else {
                    filters[i] = o;
                }
            }
            
            int count = OccurrencesCollection.highlightLsid(pid, species, filters);

            return pid + "\n" + String.valueOf(count);
        }catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * chart basis: occurrence or area
     * filter: [lsid] + [area]
     * series: [taxon level] or [string attribute] or [boolean attribute] or [shape file layer]
     * xaxis: [taxon level] or [attribute] or [layer]
     *        Optional ',' + min + ' ' max
     *        Optional ',' + categories delimited by '\t'
     * yaxis: [taxon level] or [attribute] or [layer] or [count type=countspecies, countoccurrence, size of area]
     *        Optional ',' + min + ' ' max
     *        Optional ',' + categories delimited by '\t'
     * zaxis: [count type=species, occurrence, size of area]
     *
     * Types of chart basis:
     * bA. Occurrence locations.
     * bB. Layer intersections.
     *
     * Types of variables:
     * tA. catagorical (e.g. STRING ATTRIBUTES[bA], SHAPE FILE INTERSECTION, TAXON NAMES[bA])
     * tB. continous (e.g. INT or DOUBLE ATTRIBUTES[bA] and GRID FILE INTERSECTION)
     * tC. presence (e.g. BOOLEAN ATTRIBUTES[bA], BOOLEAN=TRUE COUNT[bA], SPECIES COUNT[bA] and OCCURRENCE COUNT[bA], INTERSECTION AREA SIZE[bB])
     *
     * Valid variables for Filtering:
     * fZ. none.
     * fA. catagorical (e.g. STRING ATTRIBUTES[bA], SHAPE FILE INTERSECTION, TAXON NAMES[bA])
     * fB. continous (e.g. INT or DOUBLE ATTRIBUTES[bA] and GRID FILE INTERSECTION)
     * fC. presence (e.g. BOOLEAN ATTRIBUTES[bA])
     * fD. Active Area
     *
     * Valid variables for Series:
     * sZ. none
     * sA. catagorical (e.g. STRING ATTRIBUTES[bA], SHAPE FILE INTERSECTION, TAXON NAMES[bA])
     * sB. presence (e.g. BOOLEAN ATTRIBUTES[bA])
     *
     * Valid variables for Y-Axis:
     * yA. catagorical (e.g. STRING ATTRIBUTES[bA], SHAPE FILE INTERSECTION, TAXON NAMES[bA])
     * yB. continous (e.g. INT or DOUBLE ATTRIBUTES[bA] and GRID FILE INTERSECTION)
     * yC. presence (e.g. BOOLEAN=TRUE COUNT[bA], SPECIES COUNT[bA] and OCCURRENCE COUNT[bA], INTERSECTION AREA SIZE[bB])
     *
     * Valid variables for X-Axis:
     * tA. catagorical (e.g. STRING ATTRIBUTES[bA], SHAPE FILE INTERSECTION, TAXON NAMES[bA])
     * tB. continous (e.g. INT or DOUBLE ATTRIBUTES[bA] and GRID FILE INTERSECTION)
     *
     * Valid variables for Z-Axis:
     * zZ. none.
     * zA. presence (e.g. BOOLEAN=TRUE COUNT[bA], SPECIES COUNT[bA] and OCCURRENCE COUNT[bA], INTERSECTION AREA SIZE[bB])
     *
     *
     * Axis Combinations
     *
     * table for bA
     * yA			yB					yC
     * tA	(1) XYBlockChart	Box & Whisker				Histogram
     * tB	(2)			Scatterplot or (1) XYBlockChart		Histogram
     *
     * table for bB
     * yA			yB					yC
     * tA	(1) XYBlockChart	Box & Whisker				(2)
     * tB	(2)			(1) XYBlockChart			(2)
     *
     * (1) requires Z-Axis variable.
     * (2) not allowed
     *
     * 
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/chart", method = RequestMethod.GET)
    public
    @ResponseBody
    String chart(HttpServletRequest req) {
        try {
            String basis = URLDecoder.decode(req.getParameter("basis"), "UTF-8");
            String filter = URLDecoder.decode(req.getParameter("filter"), "UTF-8");
            String [] fetch = new String[4];
            try {
                fetch[0] = URLDecoder.decode(req.getParameter("series"), "UTF-8");
            } catch (Exception e) {
                fetch[0] = "";
            }
            fetch[1] = URLDecoder.decode(req.getParameter("xaxis"), "UTF-8");
            fetch[2] = URLDecoder.decode(req.getParameter("yaxis"), "UTF-8");
            try {
                fetch[3] = URLDecoder.decode(req.getParameter("zaxis"), "UTF-8");
            } catch (Exception e) {
                fetch[3] = "";
            }            

            String filterLsid = getParam("lsid", filter).replace("__", ".");
            String filterArea = getParam("area", filter);
            String filterArea2 = getParam("area2", filter);
            
            ArrayList<OccurrenceRecordNumbers> records = null;
            SimpleRegion region = null;
            if (filterArea != null && filterArea.startsWith("ENVELOPE")) {
                records = FilteringService.getRecords(filterArea);
            } else {
                region = SimpleShapeFile.parseWKT(filterArea);
            }

            SimpleRegion region2 = null;
            ArrayList<OccurrenceRecordNumbers> records2 = null;
            if (filterArea2 != null && filterArea2.startsWith("ENVELOPE")) {
                records2 = FilteringService.getRecords(filterArea2);
            } else {
                region2 = SimpleShapeFile.parseWKT(filterArea2);
            }

            //if one axis has count type speces, make sure to evaluate a
            //'unique species value' column
            boolean countOccurrences = false;
            boolean countSpecies = false;
            boolean sumArea = false;
            for(int i=0;i<fetch.length;i++) {
                if(fetch[i].equalsIgnoreCase("countOccurrences")) {
                    countOccurrences = true;
                } else if(fetch[i].equalsIgnoreCase("countSpecies")) {
                    countSpecies = true;
                } else if(fetch[i].equalsIgnoreCase("sumArea")) {
                    sumArea = true;
                }
            }
            
            if(basis.equalsIgnoreCase("occurrence")) {
                //bA

                //add series and axis info as layers
                String [] layers = new String[4];
                int layerCount = 0;
                Layer layer;
                String [] fetchFullNames = new String[4];
                for(int i=0;i<fetch.length;i++) {
                    if(fetch[i] != null && (layer = Layers.getLayer(fetch[i])) != null) {
                        layers[layerCount] = layer.name;
                        fetchFullNames[i] = layer.display_name;
                        layerCount++;
                    }
                }

                SamplingService ss = SamplingService.newForLSID(filterLsid);
                String[][] results = ss.sampleSpecies(filterLsid, layers, region, records, 1000000);

                //first record in return results is a header
                if(results == null || results.length <= 1) {
                    return null;
                }

                //identify columns to keep
                int [] fetchColumns = new int[fetch.length];
                int fetchColumnsCount = 0;
                for(int i=0;i<results[0].length;i++) {
                    //test layers
                    if(fetchColumnsCount < fetch.length) {
                        for(int j=0;j<fetch.length;j++) {
                            if((fetch[j] != null && fetch[j].equalsIgnoreCase(results[0][i]))
                                ||    (fetchFullNames[j] != null && fetchFullNames[j].equalsIgnoreCase(results[0][i]))) {
                                fetchColumns[fetchColumnsCount] = i;
                                fetchColumnsCount++;
                                break;
                            }
                        }
                    }
                }

                //clean up null to emptystring from columns to keep
                for(int i=0;i<results.length;i++){
                    for(int j=0;j<fetchColumnsCount;j++) {
                        if(results[i][fetchColumns[j]] == null) {
                            results[i][fetchColumns[j]] = "";
                        }
                    }
                }

                //generate countSpecies column
                int [] countSpeciesUniqueValue = null;
                if(countSpecies) {
                    OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();
                    ofu.load();                   
                    
                    countSpeciesUniqueValue = new int[results.length];
                    String [] names = new String[results.length];
                    //make species names list
                    for(int i=0;i<results.length;i++) {
                        StringBuilder sb = new StringBuilder();
                        for(int j=0;j<ofu.onetwoCount;j++) {
                            sb.append(results[i][j]);
                        }
                        names[i] = sb.toString();
                    }

                    //get unique list
                    String [] namesCopy = names.clone();
                    java.util.Arrays.sort(namesCopy);
                    int numberOfUnique = 1;
                    for(int i=1;i<namesCopy.length;i++) {
                        if(!namesCopy[i].equals(namesCopy[i-1])) {
                            namesCopy[numberOfUnique] = namesCopy[i];
                            numberOfUnique++;
                        }
                    }
                    namesCopy = java.util.Arrays.copyOf(namesCopy, numberOfUnique);
                        
                    //position in unique list is indicated of species number
                    for(int i=1;i<results.length;i++) {
                        countSpeciesUniqueValue[i] = java.util.Arrays.binarySearch(namesCopy, names[i]);
                    }
                }

                //sort by fetch column values
                class ReorderedColumns implements Comparator<String[]> {
                    public int [] colOrder;
                    
                    public int compare(String [] s1, String [] s2) {
                        int c;
                        for(int i=0;i<colOrder.length;i++) {
                            if(s1[colOrder[i]] == null && s2[colOrder[i]] == null) {
                                return 0;
                            } else if (s1[colOrder[i]] == null) {
                                return "".compareTo(s2[colOrder[i]]);
                            } else if (s2[colOrder[i]] == null) {
                                return s1[colOrder[i]].compareTo("");
                            }
                            c = s1[colOrder[i]].compareTo(s2[colOrder[i]]);
                            if(c != 0) {
                                return c;
                            }                            
                        }
                        return 0;
                    }
                };             
                ReorderedColumns c = new ReorderedColumns();                
                c.colOrder = java.util.Arrays.copyOf(fetchColumns, fetchColumnsCount);;
                //remove header
                results = java.util.Arrays.copyOfRange(results, 1, results.length);
                java.util.Arrays.sort(results, c);

                //format for output as csv
                StringBuilder csv = new StringBuilder();

                String nextLine = "";
                BitSet countSpeciesValue = new BitSet();
                int countOccurrencesValue = 0;
                String currentLine;
                for(int i=0;i<results.length;i++) {
                    //form current line
                    currentLine = results[i][fetchColumns[0]];
                    for(int j=1;j<fetchColumnsCount;j++) {
                        currentLine += "," + results[i][fetchColumns[j]];
                    }
                    if(i == 0){
                        nextLine = currentLine;
                    }

                    if(i > 0 && !currentLine.equals(nextLine)) {
                        //if this is a new line, append nextLine to csv

                        //new line
                        if(csv.length() > 0) {
                            csv.append("\n");
                        }

                        //append
                        csv.append(nextLine);
                        if(countSpecies) {
                            csv.append(",").append(countBitSetFlags(countSpeciesValue));
                        }
                        if(countOccurrences) {
                            csv.append(",").append(countOccurrencesValue);
                        }

                        //reset
                        nextLine = currentLine;
                        countSpeciesValue = new BitSet();
                        countOccurrencesValue = 0;
                    }

                    //same line, inc counters
                    if(countSpecies) {
                        countSpeciesValue.set(countSpeciesUniqueValue[i]);
                    }
                    if(countOccurrences) {
                        countOccurrencesValue++;
                    }
                }
                //add last line
                if(csv.length() > 0) {
                    csv.append("\n");
                }
                //append
                csv.append(nextLine);
                if(countSpecies) {
                    csv.append(",").append(countBitSetFlags(countSpeciesValue));
                }
                if(countOccurrences) {
                    csv.append(",").append(countOccurrencesValue);
                }

                return csv.toString();                
            } else {
                //bB

                //TODO: more than env layers
                Layer [] layers = new Layer[2];
                double [][] extents = new double[2][];
                for(int i=1;i<3;i++) {
                    String [] s = fetch[i].split(",");
                    layers[i-1] = Layers.getLayer(s[0]);
                    if(s.length > 2) {
                        double [] e = new double[2];
                        e[0] = Double.parseDouble(s[1]);
                        e[1] = Double.parseDouble(s[2]);
                        extents[i-1] = e;
                    }
                }
                
                Legend [] legends = new Legend[2];

                float [][] data = new float[2][];
                float [][] cutoffs = new float[2][];
               
                //determine scales
                Grid g = null;

                for(int i=0;i<layers.length;i++) {
                    g = Grid.getGrid(TabulationSettings.environmental_data_path + layers[i].name);

                    float [] f = g.getGrid().clone();   //Aligned grid

                    //are extents present?
                    if(extents[i] != null) {
                        for(int j=0;j<f.length;j++) {
                            if(Float.isNaN(f[j]) || f[j] < extents[i][0] || f[j] > extents[i][1]) {
                                f[j] = Float.NaN;
                            }
                        }
                    }
                    data[i] = f.clone();
                    java.util.Arrays.sort(f);

                    Legend lei = new LegendEvenInterval();
                    lei.generate(f, 20);

                    //Legend leil10 = new LegendEvenIntervalLog10();
                    //leil10.generate(f);
                    //if(lei.evaluateStdDevArea(data[i]) < leil10.evaluateStdDevArea(f)) {
                    //    legends[i] = lei;
                    //} else {
                    //    legends[i] = leil10;
                    //}
                    legends[i] = lei;

                    cutoffs[i] = legends[i].getCutoffFloats();
                }

                //build
                int len = data[0].length;
                int divs = 20;  //same as number of cutpoints in Legend
                double [][] area = new double[divs][divs];

                for(int i=0;i<cutoffs[0].length;i++) {
                    System.out.println(cutoffs[0][i] + "," + cutoffs[1][i]);
                }

                for(int i=0;i<len;i++) {
                    if(Float.isNaN(data[0][i]) || Float.isNaN(data[1][i])) {
                        continue;
                    }
                    int x = getPos(data[0][i], cutoffs[0]);
                    int y = getPos(data[1][i], cutoffs[1]);

                    area[x][y] += cellArea(g.nrows - 1 - (i / g.ncols));
                }
                
                //to csv
                StringBuilder sb = new StringBuilder();
                sb.append(",").append(layers[1].display_name).append("\n").append(layers[0].display_name);
                for(int j=0;j<divs;j++) {
                    sb.append(",").append(cutoffs[1][j]);
                }
                for(int i=0;i<divs;i++) {
                    sb.append("\n").append(cutoffs[0][i]);
                    for(int j=0;j<divs;j++) {
                        sb.append(",").append(area[i][j]);
                    }
                }

                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    int getPos(float d, float[] cutoffs) {
        int pos = java.util.Arrays.binarySearch(cutoffs, d);
        if(pos < 0) {
            pos = (pos * -1) - 1;
        }

        return pos;
    }

    static double [] commonGridLatitudeArea = null;
    double cellArea(int commonGridY) {
        if(commonGridLatitudeArea == null) {
            commonGridLatitudeArea = FilteringIndex.getCommonGridLatitudeArea();
        }
        if(commonGridY == commonGridLatitudeArea.length) {
            commonGridY--;
        }
        return commonGridLatitudeArea[commonGridY];
    }

    /**
     * get values from string of the form:
     * 
     * name1:value2;name2;value2
     * 
     * ';' is not a valid value character and any recognised ';'+name+':'
     * must be absent from values
     * 
     * @param get   name to look for as String
     * @param list  String with name and value pairs
     * @return value as String for the name, or null
     */
    private String getParam(String get, String list) {
        //get name pos
        int p1 = 0;
        
        while(p1 >= 0 && p1 < list.length()) {
            p1 = list.indexOf(get + ":", p1);
            //failed to find it if -1
            if(p1 < 0) {
                return null;
            } else if(p1 == 0 || list.charAt(p1-1) == ';'){
                //found it, extract value
                int start = p1 + get.length() + 1;
                int end = list.indexOf(';', start);
                if(end < 0) {
                    end = list.length();
                }
                if(start >= 0 && start < list.length()
                        && end >= start && end <= list.length() ){
                    return list.substring(start, end);
                }
            } else {
                //contine after moving p1 forward
                p1 += get.length() + 1;
            }
        }

        return null;
    }

    int countBitSetFlags(BitSet bs) {
        int count = 0;
        for(int i=0;i<bs.length();i++) {
            count += bs.get(i)?1:0;
        }
        return count;
    }



    @RequestMapping(value = "/wms/scatterplot", method = RequestMethod.GET)
    public void getScatterplotImage(
            @RequestParam(value = "LSID", required = true, defaultValue = "") String species,
            @RequestParam(value = "ENVLIST", required = true, defaultValue = "") String envlist,
            @RequestParam(value = "WIDTH", required = false, defaultValue = "640") String widthString,
            @RequestParam(value = "HEIGHT", required = false, defaultValue = "480") String heightString,
            HttpServletRequest request, HttpServletResponse response) {

        long start = System.currentTimeMillis();

        response.setHeader("Cache-Control", "max-age=86400"); //age == 1 day
        response.setContentType("image/png"); //only png images generated

        try {
            species = species.replace("__",".");
            int width = Integer.parseInt(widthString);
            int height = Integer.parseInt(heightString);
            
            //get data
            String[] layers = getLayerFiles(envlist);

            SamplingService ss = SamplingService.newForLSID(species);
            String[][] results = ss.sampleSpecies(species, layers, null, null, 1000000);

            DefaultXYDataset xyDataset = new DefaultXYDataset();
            int p1 = results[0].length-2;
            int p2 = results[0].length-1;
            double[][] dbl = new double[2][results.length - 1];
            for (int i = 1; i < results.length; i++) {   //skip header
                try {
                    dbl[0][i - 1] = Double.parseDouble(results[i][p1]);
                    dbl[1][i - 1] = Double.parseDouble(results[i][p2]);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }

            xyDataset.addSeries("lsid", dbl);

            JFreeChart jChart;
            XYPlot plot;
            ChartRenderingInfo chartRenderingInfo;

            jChart = ChartFactory.createScatterPlot(
                    SpeciesIndex.getScientificName(SpeciesIndex.findLSID(species))
                    , Layers.layerNameToDisplayName(layers[0])
                    , Layers.layerNameToDisplayName(layers[1])
                    , xyDataset, PlotOrientation.HORIZONTAL, false, false, false);
            plot = (XYPlot) jChart.getPlot();
            plot.setForegroundAlpha(0.5f);
            chartRenderingInfo = new ChartRenderingInfo();

            BufferedImage bi = jChart.createBufferedImage(width, height, BufferedImage.TRANSLUCENT, chartRenderingInfo);
            byte[] bytes = EncoderUtil.encode(bi, ImageFormat.PNG, true);

            ServletOutputStream outStream = response.getOutputStream();
            outStream.write(bytes);
            outStream.flush();
            outStream.close();

            System.out.println("[scatterplot " + height + "," + width + "," + species + "," + envlist + ": " + (System.currentTimeMillis() - start) + "ms]");

        } catch (Exception e) {
            e.printStackTrace();
        }     
    }

    @RequestMapping(value = "/wms/block", method = RequestMethod.GET)
    public void getBlockImage(
            @RequestParam(value = "ENVLIST", required = true, defaultValue = "") String envlist,
            @RequestParam(value = "WIDTH", required = false, defaultValue = "640") String widthString,
            @RequestParam(value = "HEIGHT", required = false, defaultValue = "480") String heightString,
            @RequestParam(value = "DIVS", required = false, defaultValue = "10") String divsString,
            HttpServletRequest request, HttpServletResponse response) {

        long start = System.currentTimeMillis();

        response.setHeader("Cache-Control", "max-age=86400"); //age == 1 day
        response.setContentType("image/png"); //only png images generated

        try {
            int divs = Integer.parseInt(divsString);
            int width = Integer.parseInt(widthString);
            int height = Integer.parseInt(heightString);

            //get data
            String[] fetch = envlist.split(":");

                Layer [] layers = new Layer[2];
                double [][] extents = new double[2][];
                for(int i=0;i<2;i++) {
                    String [] s = fetch[i].split(",");
                    layers[i] = Layers.getLayer(s[0]);
                    if(s.length > 2) {
                        double [] e = new double[2];
                        e[0] = Double.parseDouble(s[1]);
                        e[1] = Double.parseDouble(s[2]);
                        extents[i] = e;
                    }
                }

                Legend [] legends = new Legend[2];

                float [][] data = new float[2][];
                float [][] cutoffs = new float[2][];

                //determine scales
                Grid g = null;

                for(int i=0;i<layers.length;i++) {
                    g = Grid.getGrid(TabulationSettings.environmental_data_path + layers[i].name);

                    float [] f = g.getGrid().clone();   //Aligned grid

                    //are extents present?
                    if(extents[i] != null) {
                        for(int j=0;j<f.length;j++) {
                            if(Float.isNaN(f[j]) || f[j] < extents[i][0] || f[j] > extents[i][1]) {
                                f[j] = Float.NaN;
                            }
                        }
                    }
                    data[i] = f.clone();
                    java.util.Arrays.sort(f);

                    Legend lei = new LegendEvenInterval();
                    lei.generate(f);

                    legends[i] = lei;

                    //cutoffs[i] = legends[i].getCutoffFloats();
                    float min = legends[i].getMinMax()[0];
                    float max = legends[i].getMinMax()[1];
                    cutoffs[i] = new float[divs];
                    for(int j=1;j<=divs;j++) {
                        if(j == divs) {
                            cutoffs[i][j-1] = max;
                        } else {
                            cutoffs[i][j-1] = (float) ((j/(float)divs * (max-min)) + min);
                        }
                    }
                }

                //build
                int len = data[0].length;
                double [][] area = new double[divs][divs];

                for(int i=0;i<len;i++) {
                    if(Float.isNaN(data[0][i]) || Float.isNaN(data[1][i])) {
                        continue;
                    }
                    int x = getPos(data[0][i], cutoffs[0]);
                    int y = getPos(data[1][i], cutoffs[1]);

                    area[x][y] += cellArea(g.nrows - 1 - (i / g.ncols));
                }

                //normalize
                double min, max;
                min = max = area[0][0];
                for(int i=0;i<divs;i++) {
                    for(int j=0;j<divs;j++) {
                        if(min > area[i][j]) {
                            min = area[i][j];
                        }
                        if(max < area[i][j]) {
                            max = area[i][j];
                        }
                    }
                }
                for(int i=0;i<divs;i++) {
                    for(int j=0;j<divs;j++) {
                        if(max != min) {
                            area[i][j] /= (max - min);
                        } else {
                            area[i][j] = 1;
                        }
                    }
                }

            double xdiv = (cutoffs[0][1] - cutoffs[0][0]);
            double ydiv = (cutoffs[1][1] - cutoffs[1][0]);

            NumberAxis x = new NumberAxis(layers[0].display_name);
            x.setUpperBound(legends[0].getMinMax()[1]);
            x.setLowerBound(legends[0].getMinMax()[0]);
            x.setTickUnit(new NumberTickUnit(xdiv*20));
            NumberAxis y = new NumberAxis(layers[1].display_name);
            y.setUpperBound(legends[1].getMinMax()[1]);
            y.setLowerBound(legends[1].getMinMax()[0]);
            y.setTickUnit(new NumberTickUnit(ydiv*20));
            DefaultXYZDataset defaultXYZDataset = new DefaultXYZDataset();

            double [][] dat = new double[3][area.length * area.length];
            int pos = 0;
            
            for(int i=0;i<area.length;i++) {
                for(int j=0;j<area.length;j++) {
                    if(area[i][j] > 0) {
                        dat[0][pos] = cutoffs[0][i] - xdiv;
                        dat[1][pos] = cutoffs[1][j] - ydiv;
                        dat[2][pos] = area[i][j];
                        pos++;
                    }
                }
            }
            defaultXYZDataset.addSeries("",dat);
            XYBlockRenderer xyBlockRenderer = new XYBlockRenderer();
            LookupPaintScale ramp = new LookupPaintScale();
            for(int i=0;i<=10;i++) {
                ramp.add(i/10.0,new Color(Legend.colours[i]));
            }
            xyBlockRenderer.setPaintScale(ramp);
            xyBlockRenderer.setBlockHeight(ydiv);
            xyBlockRenderer.setBlockWidth(xdiv);
            xyBlockRenderer.setBlockAnchor(RectangleAnchor.BOTTOM_LEFT);
            XYPlot plot = new XYPlot(defaultXYZDataset,x,y, xyBlockRenderer);
            JFreeChart jChart = new JFreeChart("Intersecting Area", plot);

            plot = (XYPlot) jChart.getPlot();
            ChartRenderingInfo chartRenderingInfo = new ChartRenderingInfo();
            jChart.removeLegend();
            jChart.setBackgroundPaint(Color.white);

            BufferedImage bi = jChart.createBufferedImage(width, height, BufferedImage.TRANSLUCENT, chartRenderingInfo);
            byte[] bytes = EncoderUtil.encode(bi, ImageFormat.PNG, true);

            ServletOutputStream outStream = response.getOutputStream();
            outStream.write(bytes);
            outStream.flush();
            outStream.close();

            System.out.println("[block " + height + "," + width + "," + envlist + ": " + (System.currentTimeMillis() - start) + "ms]");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
