package org.ala.spatial.web.services;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ala.spatial.analysis.cluster.Record;
import org.ala.spatial.analysis.cluster.SpatialCluster3;
import org.ala.spatial.analysis.heatmap.HeatMap;
import org.ala.spatial.analysis.index.IndexedRecord;
import org.ala.spatial.analysis.index.OccurrenceRecordNumbers;
import org.ala.spatial.analysis.index.OccurrencesCollection;
import org.ala.spatial.analysis.index.OccurrencesFilter;
import org.ala.spatial.analysis.index.SpeciesColourOption;
import org.ala.spatial.analysis.legend.Legend;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.dao.SpeciesDAO;
import org.ala.spatial.model.Species;
import org.ala.spatial.util.AndRegion;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Provides an interface to the BIE (or other apps) to display either density
 * or point based images for a given filter:
 *  - family lsid
 *  - genus lsid
 *  - species lsid
 *  - subspecies lsid
 *  - institution code
 *  - institution code & collection code
 *  - data provider id
 *  - dataset id
 *
 * eg:
 *  http://localhost:8080/alaspatial/ws/density/map?species_lsid=urn:lsid:biodiversity.org.au:apni.taxon:295866
 *  http://localhost:8080/alaspatial/ws/density/map?institution_code=WAM&collection_code=MAMM&collection_code=ARACH
 *
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws")
public class WMSController {

    String baseOutputPath = "";
    String baseOutUrl = "/output/sampling/";
    private SpeciesDAO speciesDao;

    @Autowired
    public void setSpeciesDao(SpeciesDAO speciesDao) {
        this.speciesDao = speciesDao;
    }

    @RequestMapping(value = "/density/map", method = RequestMethod.GET)
    public
    @ResponseBody
    Map getDensityMap(
            @RequestParam(value = "spname", required = false, defaultValue = "") String spname,
            @RequestParam(value = "tcid", required = false, defaultValue = "") String tcid,
            @RequestParam(value = "family_lsid", required = false, defaultValue = "") String family_lsid,
            @RequestParam(value = "genus_lsid", required = false, defaultValue = "") String genus_lsid,
            @RequestParam(value = "species_lsid", required = false, defaultValue = "") String species_lsid,
            @RequestParam(value = "subspecies_lsid", required = false, defaultValue = "") String subspecies_lsid,
            @RequestParam(value = "institutionUid", required = false, defaultValue = "") String institutionUid,
            @RequestParam(value = "collectionUid", required = false, defaultValue = "") String collectionUid,
            @RequestParam(value = "dataProviderUid", required = false, defaultValue = "") String dataProviderUid,
            @RequestParam(value = "dataResourceUid", required = false, defaultValue = "") String dataResourceUid,
            HttpServletRequest request, HttpServletResponse response) {

        String msg = "";
        Map output = new HashMap();
        boolean isHeatmap = true;

        String area = "";
        area += "POLYGON((";
        area += "110.911 -44.778,";
        area += "110.911 -9.221,";
        area += "156.113 -9.221,";
        area += "156.113 -44.778,";
        area += "110.911 -44.778";
        area += "))";
        SimpleRegion region = SimpleShapeFile.parseWKT(area);

        baseOutUrl = TabulationSettings.base_output_url + "/output/sampling/";

        try {
            String lsid = "";
            if (family_lsid != null) {
                SpatialLogger.log("generating family density map for: " + family_lsid);
                //msg = "generating density family map for: " + family_lsid;
                lsid = family_lsid;
            } else if (genus_lsid != null) {
                SpatialLogger.log("generating genus density map for: " + genus_lsid);
                //msg = "generating density genus map for: " + genus_lsid;
                lsid = genus_lsid;
            } else if (species_lsid != null) {
                SpatialLogger.log("generating species density map for: " + species_lsid);
                //msg = "generating density species map for: " + species_lsid;
                lsid = species_lsid;
            } else if (subspecies_lsid != null) {
                SpatialLogger.log("generating subspecies density map for: " + subspecies_lsid);
                //msg = "generating density subspecies map for: " + subspecies_lsid;
                lsid = subspecies_lsid;
            } else if (tcid != null) {
                SpatialLogger.log("generating taxonconceptid density map for: " + tcid);
                //msg = "generating density subspecies map for: " + subspecies_lsid;
                //lsid = tcid;
            }

            //String currentPath = session.getServletContext().getRealPath(File.separator);
            String currentPath = TabulationSettings.base_output_dir;
            baseOutputPath = TabulationSettings.base_output_dir;
            File baseDir = new File(currentPath + "output" + File.separator + "sampling" + File.separator);
            if (!lsid.equalsIgnoreCase("")) {
                String outputfile = baseDir + File.separator + lsid.replace(":", "_") + ".png";
                SpatialLogger.log("Checking if already present: " + outputfile);
                File imgFile = new File(outputfile);
                if (imgFile.exists()) {
                    SpatialLogger.log("File already present, sending that: " + baseOutUrl + lsid.replace(":", "_") + ".png");
                    //msg = baseOutUrl + lsid.replace(":", "_") + ".png";

                    // output = generateOutput(baseOutUrl + lsid.replace(":", "_") + ".png", baseOutUrl + "legend_" + lsid.replace(":", "_") + ".png", isHeatmap);
                    output = generateOutput(lsid.replace(":", "_") + ".png", "legend_" + lsid.replace(":", "_") + ".png", "check");

                } else {
                    SpatialLogger.log("Starting out search for: " + lsid);

                    //     IndexedRecord[] ir = OccurrencesIndex.filterSpeciesRecords(vtn.getScientificname());
                    //IndexedRecord[] ir = OccurrencesIndex.filterSpeciesRecords(lsid);
                    //if (ir != null) {
                    //    if (ir != null) {
                    //        double[] points = OccurrencesIndex.getPoints(ir[0].record_start, ir[0].record_end);
                    SamplingService ss = SamplingService.newForLSID(lsid);
                    double[] points = ss.sampleSpeciesPoints(lsid, region, null);
                    if (points != null && points.length > 0) {
                        System.out.println("HeatMap.baseDir: " + baseDir.getAbsolutePath());
                        //HeatMap hm = new HeatMap(baseDir, vtn.getScientificname());
                        HeatMap hm = new HeatMap(baseDir, lsid.replace(":", "_"));
                        if ((points.length / 2) < 500) {
                            hm.generatePoints(points);
                            hm.drawOuput(outputfile, false);
                            isHeatmap = false;
                        } else {
                            hm.generateClasses(points);
                            hm.drawOuput(outputfile, true);
                            isHeatmap = true;
                        }

                        //msg = baseOutUrl + lsid.replace(":", "_") + ".png";
                        //output = generateOutput(baseOutUrl + lsid.replace(":", "_") + ".png", baseOutUrl + "legend_" + lsid.replace(":", "_") + ".png", isHeatmap);
                        output = generateOutput(lsid.replace(":", "_") + ".png", "legend_" + lsid.replace(":", "_") + ".png", ((isHeatmap) ? "heatmap" : "points"));
                        System.out.println("Sending out: " + msg);

                    } else {
                        //msg = "No species";
                        //msg = baseOutUrl + "base/mapaus1_white.png";
                        output = generateOutput("base/mapaus1_white.png", "", "blank");
                        System.out.println("Empty filter species");
                    }
                    //} else {
                    //msg = baseOutUrl + "base/mapaus1_white.png";
                    //    output = generateOutput("base/mapaus1_white.png", "", "blank");
                    // }

//                    SpatialLogger.log("Looking for " + lsid + " within region: " + area);
//                    SamplingService ss = SamplingService.newForLSID(lsid);
//                    double[] p = ss.sampleSpeciesPoints(lsid, region, null);
//                    if (p != null) {
//                        SpatialLogger.log("HeatMap.baseDir: " + baseDir.getAbsolutePath());
//                        //HeatMap hm = new HeatMap(baseDir, vtn.getScientificname());
//                        HeatMap hm = new HeatMap(baseDir, lsid.replace(":", "_"));
//                        if ((p.length / 2) < 500) {
//                            hm.generatePoints(p);
//                            hm.drawOuput(outputfile, false);
//                            isHeatmap = false;
//                        } else {
//                            hm.generateClasses(p);
//                            hm.drawOuput(outputfile, true);
//                            isHeatmap = true;
//                        }
//
//                        //msg = baseOutUrl + lsid.replace(":", "_") + ".png";
//                        output = generateOutput(baseOutUrl + lsid.replace(":", "_") + ".png", baseOutUrl + "legend_" + lsid.replace(":", "_") + ".png", isHeatmap);
//                        SpatialLogger.log("Sending out: " + msg);
//                    } else {
//                        output = generateOutput("base/mapaus1_white.png", "", "blank");
//                    }

                }
            } else if (institutionUid != null) {
                //msg = baseOutUrl + process(baseDir, "institution_code_uid", institutionUid);
                output = process(baseDir, "institution_code_uid", institutionUid);
            } else if (collectionUid != null) {
                //msg = baseOutUrl + process(baseDir, "collection_code_uid", collectionUid);
                output = process(baseDir, "collection_code_uid", collectionUid);
            } else if (dataProviderUid != null) {
                //msg = baseOutUrl + process(baseDir, "data_provider_uid", dataProviderUid);
                output = process(baseDir, "data_provider_uid", dataProviderUid);
            } else if (dataResourceUid != null) {
                //msg = baseOutUrl + process(baseDir, "data_resource_uid", dataResourceUid);
                output = process(baseDir, "data_resource_uid", dataResourceUid);
            } else if (tcid != null) {
                //msg = baseOutUrl + process(baseDir, "taxonConceptId", tcid);
                output = process(baseDir, "taxonConceptId", tcid);
            } else if (spname != null) {
                SpatialLogger.log("Mapping via speciesname: " + spname);
                String outputfile = baseDir + File.separator + spname + ".png";
                //IndexedRecord[] ir = OccurrencesIndex.filterSpeciesRecords(spname);
                IndexedRecord[] ir = null;
                if (ir != null) {
                    double[] points = OccurrencesCollection.getPoints(new OccurrencesFilter(spname, TabulationSettings.MAX_RECORD_COUNT_CLUSTER));

                    SpatialLogger.log("HeatMap.baseDir: " + baseDir.getAbsolutePath());
                    HeatMap hm = new HeatMap(baseDir, spname);
                    if ((points.length / 2) < 500) {
                        hm.generatePoints(points);
                        hm.drawOuput(outputfile, false);
                        isHeatmap = false;
                    } else {
                        hm.generateClasses(points);
                        hm.drawOuput(outputfile, true);
                        isHeatmap = true;
                    }


                    //msg = baseOutUrl + spname + ".png";
                    //output = generateOutput(baseOutUrl + spname + ".png", baseOutUrl + "legend_" + spname + ".png", isHeatmap);
                    output = generateOutput(spname + ".png", "legend_" + spname + ".png", ((isHeatmap) ? "heatmap" : "points"));
                    SpatialLogger.log("Sending out: " + msg);

                } else {
                    SpatialLogger.log("getting " + spname + " from db");
                    List<Species> species = speciesDao.getRecordsById(spname);

                    if (species != null) {
                        double[] points = new double[species.size() * 2];
                        int pi = 0;
                        for (Species sp : species) {
                            SpatialLogger.log("sp: " + sp.getSpecies() + " at " + sp.getLongitude() + ", " + sp.getLatitude());
                            points[pi] = Double.parseDouble(sp.getLongitude());
                            points[pi + 1] = Double.parseDouble(sp.getLatitude());
                            pi += 2;
                        }
                        HeatMap hm = new HeatMap(baseDir, spname);
                        if ((points.length / 2) < 500) {
                            hm.generatePoints(points);
                            hm.drawOuput(outputfile, false);
                            isHeatmap = false;
                        } else {
                            hm.generateClasses(points);
                            hm.drawOuput(outputfile, true);
                            isHeatmap = true;
                        }

                        //msg = baseOutUrl + spname + ".png";
                        //output = generateOutput(baseOutUrl + spname + ".png", baseOutUrl + "legend_" + spname + ".png", isHeatmap);
                        output = generateOutput(spname + ".png", "legend_" + spname + ".png", ((isHeatmap) ? "heatmap" : "points"));
                        SpatialLogger.log("Sending out: " + msg);

                    } else {
                        //msg = baseOutUrl + "base/mapaus1_white.png";
                        output = generateOutput("base/mapaus1_white.png", "", "blank");
                        SpatialLogger.log("Empty filter species");
                    }


                    //msg = "No species";
                    //msg = "base/mapaus1_white.png";
                    //System.out.println("Empty filter species");
                }

            }

        } catch (Exception ex) {
            Logger.getLogger(WMSController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return output;
    }

    private Map process(File baseDir, String key, String value) {
        String msg = "";
        Map output = new HashMap();
        boolean isHeatmap = true;

        String outputfile = baseDir + File.separator + value + ".png";
        SpatialLogger.log("Checking if already present: " + outputfile);
        File imgFile = new File(outputfile);
        if (imgFile.exists()) {
            SpatialLogger.log("File already present, sending that: " + value + ".png");
            //msg = value + ".png";
            //output = generateOutput(baseOutUrl + value + ".png", baseOutUrl + "legend_" + value + ".png", isHeatmap);
            output = generateOutput(value + ".png", "legend_" + value + ".png", "check");
        } else {
            SpatialLogger.log("Starting out search for: " + value);
            //msg = "generating density data_provider_id map for: " + value;
            ArrayList<OccurrenceRecordNumbers> recs = OccurrencesCollection.lookup(key, value);
            if (recs != null) {
                ArrayList<OccurrenceRecordNumbers> finalRecs = recs;

                double[] points = OccurrencesCollection.getPoints(new OccurrencesFilter(recs, TabulationSettings.MAX_RECORD_COUNT_CLUSTER));

                SpatialLogger.log("HeatMap.baseDir: " + baseDir.getAbsolutePath());
                HeatMap hm = new HeatMap(baseDir, value);
                if ((points.length / 2) < 500) {
                    hm.generatePoints(points);
                    hm.drawOuput(outputfile, false);
                    isHeatmap = false;
                } else {
                    hm.generateClasses(points);
                    hm.drawOuput(outputfile, true);
                    isHeatmap = true;
                }

                //msg = value + ".png";
                output = generateOutput(value + ".png", "legend_" + value + ".png", ((isHeatmap) ? "heatmap" : "points"));
            } else {
                //msg = "base/mapaus1_white.png";
                output = generateOutput("base/mapaus1_white.png", "", "blank");
            }
            SpatialLogger.log("Sending out: " + msg);
        }

        return output;

    }

    public static String getBaseUrl(HttpServletRequest request) {
        if ((request.getServerPort() == 80)
                || (request.getServerPort() == 443)) {
            return request.getScheme() + "://"
                    + request.getServerName()
                    + request.getContextPath();
        } else {
            return request.getScheme() + "://"
                    + request.getServerName() + ":" + request.getServerPort()
                    + request.getContextPath();
        }
    }

    private Map generateOutput(String mapUrl, String legUrl, String type) {

        File baseDir = new File(baseOutputPath + "output" + File.separator + "sampling" + File.separator);

        // check if the legend file exists,
        // if so, then its a heatmap
        // else it's a point map
        if (type.equalsIgnoreCase("check")) {
            String legfile = baseDir + File.separator + "legend_" + mapUrl;
            File legFile = new File(legfile);
            if (legFile.exists()) {
                //legUrl = baseOutUrl + legUrl;
                type = "heatmap";
            } else {
                legUrl = "";
                type = "points";
            }
        }

//        if (type.equalsIgnoreCase("")) {
//            type = "points";
//        }
        Map output = new HashMap();
        output.put("mapUrl", baseOutUrl + mapUrl);
        output.put("legendUrl", baseOutUrl + legUrl);
        output.put("type", type);

        return output;
    }

    private Map generateOutput(String mapUrl, String legUrl, boolean heatmap) {

        Map output = new HashMap();

//        StringBuffer output = new StringBuffer();
//
//        output.append("{");
//
//        // the map image url
//        output.append("mapUrl:");
//        output.append("'").append(mapUrl).append("'");
//
//        output.append(",");
//
//        // the legend image url
//        output.append("legendUrl:");
//        output.append("'").append(legUrl).append("'");
//
//        output.append(",");
//
//        output.append("type:");
//        if (heatmap) {
//            output.append("'heatmap'");
//        } else {
//            output.append("'points'");
//        }
//
//        output.append("}");

        String type = "heatmap";
        if (!heatmap) {
            type = "points";
            legUrl = "";
        }
        output.put("mapUrl", mapUrl);
        output.put("legendUrl", legUrl);
        output.put("type", type);

        return output;
    }

    public String generateMapLSID(String lsid) {


        String value = lsid;

        List<Species> species = speciesDao.getRecordsById(lsid);
        SpatialLogger.log("Found " + species.size() + " records via db");
        double[] points = null;
        String outputfile = "";
        File baseDir = null;
        for (Species sp : species) {
        }

        HeatMap hm = new HeatMap(baseDir, value);
        if ((points.length / 2) < 500) {
            hm.generatePoints(points);
            hm.drawOuput(outputfile, false);
        } else {
            hm.generateClasses(points);
            hm.drawOuput(outputfile, true);
        }

        return value + ".png";

    }

    /**
     * Action call to get a layer info based on it's ID
     *
     * @param id
     * @return LayerInfo layer
     */
    /*
    //@RequestMapping(method = RequestMethod.GET)
    public
    @ResponseBody
    String getSpeciesDensityMap(@RequestParam String species_lsid) {
    System.out.print("generating density map for: " + species_lsid);
    return "generating density map for: " + species_lsid;
    }

    //@RequestMapping(method = RequestMethod.GET)
    public
    @ResponseBody
    String getFamilyDensityMap(@RequestParam String family_lsid) {
    System.out.print("generating density map for: " + family_lsid);
    return "generating density map for: " + family_lsid;
    }
     *
     */
    public static void main(String[] args) {
    }


    /*
    http://spatial.ala.org.au/geoserver/wms/reflect?styles=&format=image/png&
    layers=ALA:occurrences&transparent=true&
    CQL_FILTER=speciesconceptid='urn:lsid:biodiversity.org.au:afd.taxon:cd149740-87b2-4da2-96dc-e1aa1f693438'&SRS=EPSG%3A900913&
    ENV=color%3A1dd183%3Bname%3Acircle%3Bsize%3A8%3Bopacity%3A0.8&
    VERSION=1.1.0&
    SERVICE=WMS&REQUEST=GetMap&
    EXCEPTIONS=application%2Fvnd.ogc.se_inimage&
    BBOX=15654303.7292,-1408886.9659,15810846.7631,-1252343.932&
    WIDTH=256&
    HEIGHT=256
     *
     */
    @RequestMapping(value = "/wms/reflect", method = RequestMethod.GET)
    public void getPointsMap(
            @RequestParam(value = "CQL_FILTER", required = false, defaultValue = "") String cql_filter,
            @RequestParam(value = "ENV", required = false, defaultValue = "") String env,
            @RequestParam(value = "BBOX", required = false, defaultValue = "") String bboxString,
            @RequestParam(value = "WIDTH", required = false, defaultValue = "") String widthString,
            @RequestParam(value = "HEIGHT", required = false, defaultValue = "") String heightString,
            HttpServletRequest request, HttpServletResponse response) {

        //grid redirect
        if(env.contains("grid")) {
            getGridMap(cql_filter, env, bboxString, widthString, heightString,request, response);
            return;
        }

        response.setHeader("Cache-Control", "max-age=86400"); //age == 1 day
        response.setContentType("image/png"); //only png images generated

        int width = 256, height = 256;
        try {
            width = Integer.parseInt(widthString);
            height = Integer.parseInt(heightString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            env = URLDecoder.decode(env, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WMSController.class.getName()).log(Level.SEVERE, null, ex);
        }
        int red = 0, green = 0, blue = 0, alpha = 0;
        String name = "circle";
        int size = 4;
        boolean uncertainty = false;
        String highlight = null;
        String colourMode = null;
        for (String s : env.split(";")) {
            String[] pair = s.split(":");
            if (pair[0].equals("color")) {
                while (pair[1].length() < 6) {
                    pair[1] = "0" + pair[1];
                }
                red = Integer.parseInt(pair[1].substring(0, 2), 16);
                green = Integer.parseInt(pair[1].substring(2, 4), 16);
                blue = Integer.parseInt(pair[1].substring(4), 16);
            } else if (pair[0].equals("name")) {
                name = pair[1];
            } else if (pair[0].equals("size")) {
                size = Integer.parseInt(pair[1]);
            } else if (pair[0].equals("opacity")) {
                alpha = (int) (255 * Double.parseDouble(pair[1]));
            } else if (pair[0].equals("uncertainty")) {
                uncertainty = true;
            } else if (pair[0].equals("sel")) {
                highlight = pair[1];
            } else if (pair[0].equals("colormode")) {
                colourMode = pair[1];
            }
        }

        double[] bbox = new double[4];
        int i;
        i = 0;
        for (String s : bboxString.split(",")) {
            try {
                bbox[i] = Double.parseDouble(s);
                i++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//adjust bbox extents with half pixel width/height
        double pixelWidth = (bbox[2] - bbox[0]) / width;
        double pixelHeight = (bbox[3] - bbox[1]) / height;
        bbox[0] += pixelWidth / 2;
        bbox[2] -= pixelWidth / 2;
        bbox[1] += pixelHeight / 2;
        bbox[3] -= pixelHeight / 2;

//offset for points bounding box by size
        double xoffset = (bbox[2] - bbox[0]) / (double) width * size;
        double yoffset = (bbox[3] - bbox[1]) / (double) height * size;

//check offset for points bb by maximum uncertainty (?? 30k ??)
        if (uncertainty) {
            double xuoffset = 30000;
            double yuoffset = 30000;
            if (xoffset < xuoffset) {
                xoffset = xuoffset;
            }
            if (yoffset < yuoffset) {
                yoffset = yuoffset;
            }
        }

//adjust offset for pixel height/width
        xoffset += pixelWidth;
        yoffset += pixelHeight;

        SpatialCluster3 sc = new SpatialCluster3();
        SimpleRegion region = new SimpleRegion();
        region.setBox(sc.convertMetersToLng(bbox[0] - xoffset), sc.convertMetersToLat(bbox[1] - yoffset), sc.convertMetersToLng(bbox[2] + xoffset), sc.convertMetersToLat(bbox[3] + yoffset));

        double[] pbbox = new double[4]; //pixel bounding box
        pbbox[0] = sc.convertLngToPixel(sc.convertMetersToLng(bbox[0]));
        pbbox[1] = sc.convertLatToPixel(sc.convertMetersToLat(bbox[1]));
        pbbox[2] = sc.convertLngToPixel(sc.convertMetersToLng(bbox[2]));
        pbbox[3] = sc.convertLatToPixel(sc.convertMetersToLat(bbox[3]));

        String lsid = null;
        SimpleRegion r = null;
        ArrayList<OccurrenceRecordNumbers> records = null;
        int p1 = cql_filter.indexOf("id='") + 4;
        if (p1 > 4) {
            int p2 = cql_filter.indexOf('\'', p1 + 1);
            lsid = cql_filter.substring(p1, p2);
        } else { //expect area=' for SimpleRegion construction
            p1 = cql_filter.indexOf("area='") + 6;
            int p2 = cql_filter.indexOf('\'', p1 + 1);
            String a = cql_filter.substring(p1, p2);
            if (a != null && a.startsWith("ENVELOPE")) {
                records = FilteringService.getRecords(a);
            } else {
                r = SimpleShapeFile.parseWKT(a);
            }
            if (r != null) {
                AndRegion ar = new AndRegion();
                ArrayList<SimpleRegion> sr = new ArrayList<SimpleRegion>(2);
                sr.add(region);
                sr.add(r);
                ar.setSimpleRegions(sr);

                region = ar;
            }
        }

        /* TODO: buffering for sampleSpeciesPoints */
        double[] points = null;

        int[] uncertainties = null;
        short[] uncertaintiesType = null;

        boolean[] listHighlight = null;

        int[] colours = null;

        if (lsid != null) {
            SamplingService ss = SamplingService.newForLSID(lsid);
            ArrayList<SpeciesColourOption> other = new ArrayList<SpeciesColourOption>();
            if (uncertainty) {
                other.add(SpeciesColourOption.fromName("u", false));
            }
            if (highlight != null) {
                other.add(SpeciesColourOption.fromHighlight(highlight, true));
            }
            if (colourMode != null) {
                other.add(SpeciesColourOption.fromName(colourMode, true));
            }
            points = ss.sampleSpeciesPoints(lsid, region, records, other);
            if (points != null && points.length > 0) {
                for (int j = 0; j < other.size(); j++) {
                    if (!other.get(j).isColourMode() && other.get(j).getName().equals("u")) {
                        uncertainties = other.get(j).getIntArray();
                    } else if (other.get(j).isHighlight()) {
                        listHighlight = other.get(j).getBoolArray();
                    } else {
                        //colour mode!
                        colours = other.get(j).getColours(lsid);
                    }
                }
            }
        } else {
            //lsid mandatory.
            //use species/{type}/register service to create 'dummy'


//TODO: create new function for sampling, allowing for 'other' fields
            Vector<Record> v = null;
            try {
                v = OccurrencesCollection.getRecords(new OccurrencesFilter(null /*lsid == null*/, region, records, TabulationSettings.MAX_RECORD_COUNT_CLUSTER));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (v == null || v.size() == 0) {
                points = null;
            } else {
                if (uncertainty) {
                    points = new double[v.size() * 2];
                    uncertainties = new int[v.size()];
                    uncertaintiesType = new short[v.size()];
                    for (int j = 0; j < v.size(); j++) {
                        points[j * 2] = v.get(j).getLongitude();
                        points[j * 2 + 1] = v.get(j).getLatitude();

                        uncertainties[j] = v.get(j).getUncertainity();
                    }
                } else {
                    points = new double[v.size() * 2];
                    for (int j = 0; j < v.size(); j++) {
                        points[j * 2] = v.get(j).getLongitude();
                        points[j * 2 + 1] = v.get(j).getLatitude();
                    }
                }
            }
        }

        if (points == null || points.length == 0) {
//TODO: make dynamic instead of fixed 256x256
            setImageBlank(response);
            //System.out.print("[wms blank: " + (System.currentTimeMillis() - start) + "ms]");
            return;
        }


//fix uncertanties to max 30000 and alter colours
        if (uncertainty) {
            uncertaintiesType = new short[uncertainties.length];
            for (int j = 0; j < uncertainties.length; j++) {
                if (Integer.MIN_VALUE == uncertainties[j]) {
                    uncertaintiesType[j] = 1;
                    uncertainties[j] = 30000;
                } else if (uncertainties[j] > 30000) {
                    uncertaintiesType[j] = 2;
                    uncertainties[j] = 30000;
                }/* else {
                uncertaintiesType[j] = 0;
                }*/
            }
        }

        /* TODO: make this a copy instead of create */
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
//g.setClip(-width,-height,width*3,height*3);
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(red, green, blue, alpha));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x, y;
        int pointWidth = size * 2;
        double width_mult = (width / (pbbox[2] - pbbox[0]));
        double height_mult = (height / (pbbox[1] - pbbox[3]));

//circle type
        if (name.equals("circle")) {
            if (colours == null) {
                for (i = 0; i < points.length; i += 2) {
                    x = (int) ((sc.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                    y = (int) ((sc.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                    g.fillOval(x - size, y - size, pointWidth, pointWidth);
                }
            } else {
                int prevColour = colours[0] + 1;    //!= colours[0]
                for (i = 0; i < points.length; i += 2) {
                    if (colours[i / 2] != prevColour) {
                        g.setColor(new Color(colours[i / 2]));
                    }
                    x = (int) ((sc.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                    y = (int) ((sc.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                    g.fillOval(x - size, y - size, pointWidth, pointWidth);
                }
            }
        }

        if (highlight != null) {
            g.setColor(new Color(255, 0, 0, alpha));
            int sz = size + 3;
            int w = sz * 2;
            for (i = 0; i < points.length; i += 2) {
                if (listHighlight[i / 2]) {
                    x = (int) ((sc.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                    y = (int) ((sc.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                    g.drawOval(x - sz, y - sz, w, w);
                }
            }
        }

//uncertainty
        if (uncertainty) {
            int uncertaintyRadius;

//white, uncertainty value
            g.setColor(new Color(255, 255, 255, alpha));
            double hmult = (height / (bbox[3] - bbox[1]));
            for (i = 0; i < points.length; i += 2) {
                if (uncertaintiesType[i / 2] == 0) {
                    x = (int) ((sc.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                    y = (int) ((sc.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                    uncertaintyRadius = (int) Math.ceil(uncertainties[i / 2] * hmult);
                    g.drawOval(x - uncertaintyRadius, y - uncertaintyRadius, uncertaintyRadius * 2, uncertaintyRadius * 2);
                }
            }

//yellow, undefined uncertainty value
            g.setColor(new Color(255, 255, 100, alpha));
            uncertaintyRadius = (int) Math.ceil(30000 * hmult);
            for (i = 0; i < points.length; i += 2) {
                if (uncertaintiesType[i / 2] == 1) {
                    x = (int) ((sc.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                    y = (int) ((sc.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                    g.drawOval(x - uncertaintyRadius, y - uncertaintyRadius, uncertaintyRadius * 2, uncertaintyRadius * 2);
                }
            }

//green, capped uncertainty value
            g.setColor(new Color(100, 255, 100, alpha));
            uncertaintyRadius = (int) Math.ceil(30000 * hmult);
            for (i = 0; i < points.length; i += 2) {
                if (uncertaintiesType[i / 2] == 2) {
                    x = (int) ((sc.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
                    y = (int) ((sc.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
                    g.drawOval(x - uncertaintyRadius, y - uncertaintyRadius, uncertaintyRadius * 2, uncertaintyRadius * 2);
                }
            }
        }

        g.dispose();

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(img, "png", outputStream);
            ServletOutputStream outStream = response.getOutputStream();
            outStream.write(outputStream.toByteArray());
            outStream.flush();
            outStream.close();
        } catch (IOException ex) {
            Logger.getLogger(WMSController.class.getName()).log(Level.SEVERE, null, ex);
        }

//System.out.println("[wms tile: " + (System.currentTimeMillis() - start) + "ms]");
    }
    //256x256 transparent image
    static Object blankImageObject = new Object();
    static byte[] blankImageBytes = null;

    private void setImageBlank(HttpServletResponse response) {
        if (blankImageBytes == null && blankImageObject != null) {
            synchronized (blankImageObject) {
                if (blankImageBytes == null) {
                    try {
                        RandomAccessFile raf = new RandomAccessFile(WMSController.class.getResource("/blank.png").getFile(), "r");
                        blankImageBytes = new byte[(int) raf.length()];
                        raf.read(blankImageBytes);
                        raf.close();
                    } catch (IOException ex) {
                        Logger.getLogger(WMSController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        if (blankImageObject != null) {
            response.setContentType("image/png");
            try {
                ServletOutputStream outStream = response.getOutputStream();
                outStream.write(blankImageBytes);
                outStream.flush();
                outStream.close();
            } catch (IOException ex) {
                Logger.getLogger(WMSController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @RequestMapping(value = "/wms/reflect2", method = RequestMethod.GET)
    public void getGridMap(
            @RequestParam(value = "CQL_FILTER", required = false, defaultValue = "") String cql_filter,
            @RequestParam(value = "ENV", required = false, defaultValue = "") String env,
            @RequestParam(value = "BBOX", required = false, defaultValue = "") String bboxString,
            @RequestParam(value = "WIDTH", required = false, defaultValue = "") String widthString,
            @RequestParam(value = "HEIGHT", required = false, defaultValue = "") String heightString,
            HttpServletRequest request, HttpServletResponse response) {

        int divs = 16; //number of x & y divisions in the WIDTH/HEIGHT

        response.setHeader("Cache-Control", "max-age=86400"); //age == 1 day
        response.setContentType("image/png"); //only png images generated

        int width = 256, height = 256;
        try {
            width = Integer.parseInt(widthString);
            height = Integer.parseInt(heightString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            env = URLDecoder.decode(env, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WMSController.class.getName()).log(Level.SEVERE, null, ex);
        }
        int red = 0, green = 0, blue = 0, alpha = 0;
        for (String s : env.split(";")) {
            String[] pair = s.split(":");
            if (pair[0].equals("color")) {
                while (pair[1].length() < 6) {
                    pair[1] = "0" + pair[1];
                }
                red = Integer.parseInt(pair[1].substring(0, 2), 16);
                green = Integer.parseInt(pair[1].substring(2, 4), 16);
                blue = Integer.parseInt(pair[1].substring(4), 16);
            }
        }

        double[] bbox = new double[4];
        int i;
        i = 0;
        for (String s : bboxString.split(",")) {
            try {
                bbox[i] = Double.parseDouble(s);
                i++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//adjust bbox extents with half pixel width/height
        double pixelWidth = (bbox[2] - bbox[0]) / width;
        double pixelHeight = (bbox[3] - bbox[1]) / height;
        bbox[0] += pixelWidth / 2;
        bbox[2] -= pixelWidth / 2;
        bbox[1] += pixelHeight / 2;
        bbox[3] -= pixelHeight / 2;

        SpatialCluster3 sc = new SpatialCluster3();
        SimpleRegion region = new SimpleRegion();
        region.setBox(sc.convertMetersToLng(bbox[0]), sc.convertMetersToLat(bbox[1]), sc.convertMetersToLng(bbox[2]), sc.convertMetersToLat(bbox[3]));

        double[] pbbox = new double[4]; //pixel bounding box
        pbbox[0] = sc.convertLngToPixel(sc.convertMetersToLng(bbox[0]));
        pbbox[1] = sc.convertLatToPixel(sc.convertMetersToLat(bbox[1]));
        pbbox[2] = sc.convertLngToPixel(sc.convertMetersToLng(bbox[2]));
        pbbox[3] = sc.convertLatToPixel(sc.convertMetersToLat(bbox[3]));

        String lsid = null;
        SimpleRegion r = null;
        ArrayList<OccurrenceRecordNumbers> records = null;
        int p1 = cql_filter.indexOf("id='") + 4;
        if (p1 > 4) {
            int p2 = cql_filter.indexOf('\'', p1 + 1);
            lsid = cql_filter.substring(p1, p2);
        } else { //expect area=' for SimpleRegion construction
            p1 = cql_filter.indexOf("area='") + 6;
            int p2 = cql_filter.indexOf('\'', p1 + 1);
            String a = cql_filter.substring(p1, p2);
            if (a != null && a.startsWith("ENVELOPE")) {
                records = FilteringService.getRecords(a);
            } else {
                r = SimpleShapeFile.parseWKT(a);
            }
            if (r != null) {
                AndRegion ar = new AndRegion();
                ArrayList<SimpleRegion> sr = new ArrayList<SimpleRegion>(2);
                sr.add(region);
                sr.add(r);
                ar.setSimpleRegions(sr);

                region = ar;
            }
        }

        /* TODO: buffering for sampleSpeciesPoints */
        double[] points = null;

        if (lsid != null) {
            SamplingService ss = SamplingService.newForLSID(lsid);
            points = ss.sampleSpeciesPoints(lsid, region, records, null);
        } else {
            //lsid mandatory.
            //use species/{type}/register service to create 'dummy'

//TODO: create new function for sampling, allowing for 'other' fields
            Vector<Record> v = null;
            try {
                v = OccurrencesCollection.getRecords(new OccurrencesFilter(null /*lsid == null*/, region, records, TabulationSettings.MAX_RECORD_COUNT_CLUSTER));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (v == null || v.size() == 0) {
                points = null;
            } else {
                points = new double[v.size() * 2];
                for (int j = 0; j < v.size(); j++) {
                    points[j * 2] = v.get(j).getLongitude();
                    points[j * 2 + 1] = v.get(j).getLatitude();
                }
            }
        }

        if (points == null || points.length == 0) {
//TODO: make dynamic instead of fixed 256x256
            setImageBlank(response);
            return;
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(red, green, blue, alpha));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x, y;
        double width_mult = (width / (pbbox[2] - pbbox[0])) / (256/divs);
        double height_mult = (height / (pbbox[1] - pbbox[3])) / (256/divs);

        //count
        int [][] gridCounts = new int[divs][divs];

        for (i = 0; i < points.length; i += 2) {
            x = (int) ((sc.convertLngToPixel(points[i]) - pbbox[0]) * width_mult);
            y = (int) ((sc.convertLatToPixel(points[i + 1]) - pbbox[3]) * height_mult);
            if(x >= 0 && x < divs && y >= 0 && y < divs) {
                gridCounts[x][y]++;
            }
        }
        int xstep = 256 / divs;
        int ystep = 256 / divs;
        for(x=0;x<divs;x++) {
            for(y=0;y<divs;y++) {
                int v = gridCounts[x][y];
                if(v > 0) {
                    if(v > 500) v = 500;
                    int colour = Legend.getLinearColour(v, 0, 500, 0xFFFFFF00, 0xFFFF0000);
                    g.setColor(new Color(colour));
                    g.fillRect(x*xstep, y*ystep, xstep, ystep);
                }
            }
        }

        g.dispose();

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(img, "png", outputStream);
            ServletOutputStream outStream = response.getOutputStream();
            outStream.write(outputStream.toByteArray());
            outStream.flush();
            outStream.close();
        } catch (IOException ex) {
            Logger.getLogger(WMSController.class.getName()).log(Level.SEVERE, null, ex);
        }

//System.out.println("[wms tile: " + (System.currentTimeMillis() - start) + "ms]");
    }
}
