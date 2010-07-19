package org.ala.spatial.web.services;

import java.io.File;
import java.io.IOException;
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
            @RequestParam(value = "collection_code", required = false, defaultValue = "") String collection_code,
            @RequestParam(value = "data_provider_id", required = false, defaultValue = "") String data_provider_id,
            @RequestParam(value = "dataset_id", required = false, defaultValue = "") String dataset_id,
            HttpServletRequest request, HttpServletResponse response) {

        String msg = "";

        HttpSession session = request.getSession();
        String baseOutUrl = getBaseUrl(request) + "/output/sampling/";

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

            String currentPath = session.getServletContext().getRealPath("/");
            File baseDir = new File(currentPath + "output/sampling/");
            if (!lsid.equalsIgnoreCase("")) {
                String outputfile = baseDir + "/" + lsid + ".png";
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
                            if ((points.length/2) < 500) {
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
                    }
                }
            } else if (institution_code != null) {
                msg = "generating density insitution code map for: " + institution_code;

                if (collection_code != null) {
                    msg = "generating density insitution/collection code map for: " + institution_code + "/" + collection_code;
                }
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
