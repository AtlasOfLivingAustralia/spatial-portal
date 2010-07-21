package org.ala.spatial.web.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.ala.spatial.analysis.heatmap.HeatMap;
import org.ala.spatial.analysis.index.IndexedRecord;
import org.ala.spatial.analysis.index.OccurrencesIndex;
import org.ala.spatial.dao.SpeciesDAO;
import org.ala.spatial.model.ValidTaxonName;
import org.ala.spatial.util.TabulationSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

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
@RequestMapping("/ws/density")
public class WMSController {

    private SpeciesDAO speciesDao;

    @Autowired
    public void setSpeciesDao(SpeciesDAO speciesDao) {
        this.speciesDao = speciesDao;
    }

    @RequestMapping(value = "/map", method = RequestMethod.GET)
    public void getDensityMap(
            @RequestParam(value = "family_lsid", required = false, defaultValue = "") String family_lsid,
            @RequestParam(value = "genus_lsid", required = false, defaultValue = "") String genus_lsid,
            @RequestParam(value = "species_lsid", required = false, defaultValue = "") String species_lsid,
            @RequestParam(value = "subspecies_lsid", required = false, defaultValue = "") String subspecies_lsid,
            @RequestParam(value = "institution_code", required = false, defaultValue = "") String institution_code,
            @RequestParam(value = "collection_code", required = false, defaultValue = "") String collection_code[],
            @RequestParam(value = "data_provider_id", required = false, defaultValue = "") String data_provider_id,
            @RequestParam(value = "dataset_id", required = false, defaultValue = "") String dataset_id,
            HttpServletRequest request, HttpServletResponse response) {

        String msg = "";

        HttpSession session = request.getSession();

        TabulationSettings.load();

        String baseOutUrl = TabulationSettings.base_output_url + "/output/sampling/";

        try {
            String lsid = "";
            if (family_lsid != null) {
                System.out.print("generating family density map for: " + family_lsid);
                msg = "generating density family map for: " + family_lsid;
                lsid = family_lsid;
            } else if (genus_lsid != null) {
                System.out.print("generating genus density map for: " + genus_lsid);
                msg = "generating density genus map for: " + genus_lsid;
                lsid = genus_lsid;
            } else if (species_lsid != null) {
                System.out.print("generating species density map for: " + species_lsid);
                msg = "generating density species map for: " + species_lsid;
                lsid = species_lsid;
            } else if (subspecies_lsid != null) {
                System.out.print("generating subspecies density map for: " + subspecies_lsid);
                msg = "generating density subspecies map for: " + subspecies_lsid;
                lsid = subspecies_lsid;
            }

            //String currentPath = session.getServletContext().getRealPath(File.separator);
            String currentPath = TabulationSettings.base_output_dir;
            File baseDir = new File(currentPath + "output" + File.separator + "sampling" + File.separator);
            if (!lsid.equalsIgnoreCase("")) {
                String outputfile = baseDir + File.separator + lsid + ".png";
                System.out.println("Checking if already present: " + outputfile);
                File imgFile = new File(outputfile);
                if (imgFile.exists()) {
                    System.out.println("File already present, sending that: " + baseOutUrl + lsid + ".png");
                    msg = baseOutUrl + lsid + ".png";
                } else {
                    System.out.println("Starting out search for: " + lsid);

                    List<ValidTaxonName> l = speciesDao.findById(lsid);
                    System.out.println("re-returning " + l.size() + " records");
                    msg += "\nre-returning " + l.size() + " records";
                    if (l.size() > 0) {
                        ValidTaxonName vtn = l.get(0);
                        msg += "\n" + vtn.getScientificname() + " - " + vtn.getRankstring();

                        System.out.println("Have: " + vtn.getScientificname() + " - " + vtn.getRankstring());

                        //File baseDir = new File("/Users/ajay/projects/tmp/heatmap/web/");

                        IndexedRecord[] ir = OccurrencesIndex.filterSpeciesRecords(vtn.getScientificname());
                        if (ir != null) {
                            double[] points = OccurrencesIndex.getPoints(ir[0].record_start, ir[0].record_end);

                            System.out.println("HeatMap.baseDir: " + baseDir.getAbsolutePath());
                            HeatMap hm = new HeatMap(baseDir, vtn.getScientificname());
                            if ((points.length / 2) < 500) {
                                hm.generatePoints(points);
                                hm.drawOuput(outputfile, false);
                            } else {
                                hm.generateClasses(points);
                                hm.drawOuput(outputfile, true);
                            }


                            msg = baseOutUrl + lsid + ".png";
                            System.out.println("Sending out: " + msg);

                        } else {
                            msg = "No species";
                            System.out.println("Empty filter species");
                        }
                    } else {
                        msg = "base/mapaus1_white.png";
                    }
                }
            } else if (institution_code != null) {
                // check if there is a collection code too.
                String baseFilename = institution_code;
                if (collection_code != null) {

                    /*
                     * Enable comma-delimited support if necessary
                     * by uncommenting this part of the code
                     *
                    // check if collection_code is seperated by commas (,)
                    // this should only happen if there is one collection_code
                    // as if several, then we can assume its fine
                    if (collection_code.length == 1) {
                        String[] tmpCollCode = collection_code[0].split(",");

                        // now copy it back into collection_code itself
                        collection_code = tmpCollCode;
                    }
                    *
                    */

                    for (int i = 0; i < collection_code.length; i++) {
                        baseFilename += "_" + collection_code[i];
                    }
                }
                String outputfile = baseDir + File.separator + baseFilename + ".png";
                System.out.println("Checking if already present: " + outputfile);
                File imgFile = new File(outputfile);
                if (imgFile.exists()) {
                    System.out.println("File already present, sending that: " + baseOutUrl + baseFilename + ".png");
                    msg = baseOutUrl + baseFilename + ".png";
                } else {
                    System.out.println("Starting out search for: " + institution_code);
                    msg = "generating density insitution code map for: " + institution_code;
                    int[] recs = OccurrencesIndex.lookup("institutionCode", institution_code);

                    if (recs != null) {

                        System.out.println("Got recs.length: " + recs.length);

                        int[] finalRecs = null;
                        //writeToFile(baseDir.getAbsolutePath() + "/inst_"+institution_code+".txt", recs);

                        if (collection_code != null) {
                            for (int i = 0; i < collection_code.length; i++) {
                                msg = "generating density insitution/collection code map for: " + institution_code + "/" + collection_code[i];
                                int[] recs2 = OccurrencesIndex.lookup("collectionCode", collection_code[i]);
                                if (recs2 != null) {
                                    System.out.println("Got recs2.length: " + recs2.length);
                                    if (finalRecs != null) {
                                        finalRecs = joinIntArrays(finalRecs, getMatchingRecs(recs, recs2));
                                    } else {
                                        finalRecs = getMatchingRecs(recs, recs2);
                                    }

                                    //writeToFile(baseDir.getAbsolutePath() + "/coll" + collection_code[i] + ".txt", recs2);
                                }
                            }
                        } else {
                            finalRecs = recs;
                        }

                        if (finalRecs != null) {
                            System.out.println("Got finalRecs.length: " + finalRecs.length);

                            double[][] pts = OccurrencesIndex.getPointsPairs();
                            double[] points = new double[finalRecs.length * 2];
                            for (int i = 0; i < finalRecs.length * 2; i += 2) {
                                points[i] = pts[finalRecs[i / 2]][0];
                                points[i + 1] = pts[finalRecs[i / 2]][1];
                            }
                            System.out.println("HeatMap.baseDir: " + baseDir.getAbsolutePath());
                            HeatMap hm = new HeatMap(baseDir, baseFilename);
                            if ((points.length / 2) < 500) {
                                hm.generatePoints(points);
                                hm.drawOuput(outputfile, false);
                            } else {
                                hm.generateClasses(points);
                                hm.drawOuput(outputfile, true);
                            }


                            msg = baseOutUrl + baseFilename + ".png";
                            System.out.println("Sending out: " + msg);

                        } else {
                            msg = "base/mapaus1_white.png";
                        }

                    } else {
                        msg = "base/mapaus1_white.png";
                    }
                }
            } else if (data_provider_id != null) {
                msg = baseOutUrl + process(baseDir, "dataProviderId", data_provider_id);
                /*
                String outputfile = baseDir + File.separator + data_provider_id + ".png";
                System.out.println("Checking if already present: " + outputfile);
                File imgFile = new File(outputfile);
                if (imgFile.exists()) {
                System.out.println("File already present, sending that: " + baseOutUrl + data_provider_id + ".png");
                msg = baseOutUrl + data_provider_id + ".png";
                } else {
                System.out.println("Starting out search for: " + data_provider_id);
                msg = "generating density data_provider_id map for: " + data_provider_id;
                int[] recs = OccurrencesIndex.lookup("dataProviderId", data_provider_id);

                int[] finalRecs = recs;

                double[][] pts = OccurrencesIndex.getPointsPairs();
                double[] points = new double[finalRecs.length * 2];
                for (int i = 0; i < finalRecs.length * 2; i += 2) {
                points[i] = pts[finalRecs[i / 2]][0];
                points[i + 1] = pts[finalRecs[i / 2]][1];
                }
                System.out.println("HeatMap.baseDir: " + baseDir.getAbsolutePath());
                HeatMap hm = new HeatMap(baseDir, data_provider_id);
                if ((points.length / 2) < 500) {
                hm.generatePoints(points);
                hm.drawOuput(outputfile, false);
                } else {
                hm.generateClasses(points);
                hm.drawOuput(outputfile, true);
                }


                msg = baseOutUrl + data_provider_id + ".png";
                System.out.println("Sending out: " + msg);
                }
                 * 
                 */
            } else if (dataset_id != null) {
                msg = baseOutUrl + process(baseDir, "dataResourceId", dataset_id);
                /*
                String outputfile = baseDir + File.separator + dataset_id + ".png";
                System.out.println("Checking if already present: " + outputfile);
                File imgFile = new File(outputfile);
                if (imgFile.exists()) {
                System.out.println("File already present, sending that: " + baseOutUrl + dataset_id + ".png");
                msg = baseOutUrl + dataset_id + ".png";
                } else {
                System.out.println("Starting out search for: " + dataset_id);
                msg = "generating density data_provider_id map for: " + dataset_id;
                int[] recs = OccurrencesIndex.lookup("dataResourceId", dataset_id);

                int[] finalRecs = recs;

                double[][] pts = OccurrencesIndex.getPointsPairs();
                double[] points = new double[finalRecs.length * 2];
                for (int i = 0; i < finalRecs.length * 2; i += 2) {
                points[i] = pts[finalRecs[i / 2]][0];
                points[i + 1] = pts[finalRecs[i / 2]][1];
                }
                System.out.println("HeatMap.baseDir: " + baseDir.getAbsolutePath());
                HeatMap hm = new HeatMap(baseDir, dataset_id);
                if ((points.length / 2) < 500) {
                hm.generatePoints(points);
                hm.drawOuput(outputfile, false);
                } else {
                hm.generateClasses(points);
                hm.drawOuput(outputfile, true);
                }


                msg = baseOutUrl + dataset_id + ".png";
                System.out.println("Sending out: " + msg);
                }
                 * 
                 */
            }

        } catch (Exception ex) {
            Logger.getLogger(WMSController.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            response.sendRedirect(msg);
        } catch (IOException ex) {
            Logger.getLogger(WMSController.class.getName()).log(Level.SEVERE, null, ex);
        }

        //return msg;
    }

    private int[] joinIntArrays(int[] a, int[] b) {
        try {

            int[] finalArray = Arrays.copyOf(a, a.length + b.length);
            System.arraycopy(b, 0, finalArray, a.length, b.length);
            return finalArray;
        } catch (Exception e) {
            System.out.println("Error joining arrays: ");
            e.printStackTrace(System.out);
        }

        return null;
    }

    private void writeToFile(String filename, int[] data) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filename));
            for (int i = 0; i < data.length; i++) {
                out.write(data[i] + "\n");
            }

            out.close();
        } catch (IOException e) {
        }
    }

    private int[] getMatchingRecs(int[] source, int[] subset) {
        try {
            int[] tmpArray = new int[source.length];
            int count = 0;

            for (int i = 0; i < subset.length; i++) {
                if (Arrays.binarySearch(source, subset[i]) > 0) {
                    tmpArray[count] = subset[i];
                    count++;
                }
            }

            System.out.println("final count: " + count);

            int[] finalArray = new int[count];
            System.arraycopy(tmpArray, 0, finalArray, 0, finalArray.length);
            return finalArray;

        } catch (Exception e) {
            System.out.println("Error getting matching records:");
            e.printStackTrace(System.out);
        }

        return null;
    }

    private String process(File baseDir, String key, String value) {
        String msg = "";

        String outputfile = baseDir + File.separator + value + ".png";
        System.out.println("Checking if already present: " + outputfile);
        File imgFile = new File(outputfile);
        if (imgFile.exists()) {
            System.out.println("File already present, sending that: " + value + ".png");
            msg = value + ".png";
        } else {
            System.out.println("Starting out search for: " + value);
            msg = "generating density data_provider_id map for: " + value;
            int[] recs = OccurrencesIndex.lookup(key, value);
            if (recs != null) {
                int[] finalRecs = recs;

                double[][] pts = OccurrencesIndex.getPointsPairs();

                double[] points = new double[finalRecs.length * 2];
                for (int i = 0; i < finalRecs.length * 2; i += 2) {
                    points[i] = pts[finalRecs[i / 2]][0];
                    points[i + 1] = pts[finalRecs[i / 2]][1];
                }
                System.out.println("HeatMap.baseDir: " + baseDir.getAbsolutePath());
                HeatMap hm = new HeatMap(baseDir, value);
                if ((points.length / 2) < 500) {
                    hm.generatePoints(points);
                    hm.drawOuput(outputfile, false);
                } else {
                    hm.generateClasses(points);
                    hm.drawOuput(outputfile, true);
                }

                msg = value + ".png";
            } else {
                msg = "base/mapaus1_white.png";
            }
            System.out.println("Sending out: " + msg);
        }

        return msg;

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
}
