package org.ala.spatial.web;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.ala.spatial.analysis.index.OccurrencesIndex;

import org.ala.spatial.analysis.service.OccurrencesService;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.dao.SpeciesDAO;
import org.ala.spatial.model.CommonName;
import org.ala.spatial.model.ValidTaxonName;
import org.ala.spatial.util.TabulationSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author ajayr
 */
@Controller
@RequestMapping("/species")
public class SpeciesController {

    private SpeciesDAO speciesDao;

    @Autowired
    public void setSpeciesDao(SpeciesDAO speciesDao) {
        this.speciesDao = speciesDao;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView showPage(HttpSession session) {
        System.out.println("species page method called");

        //return new ModelAndView("species", "message", "Add method called");

        ModelMap modelMap = new ModelMap();
        //modelMap.addAttribute("dsourceList", dataDao.listByUser(user.getId()));
        modelMap.addAttribute("message", "Generating species list");
        modelMap.addAttribute("spList", speciesDao.getSpecies());
        return new ModelAndView("species", modelMap);

    }

    @RequestMapping(value = "/records", method = RequestMethod.GET)
    public ModelAndView getNames(@RequestParam String query) {
        System.out.println("Looking up list for: " + query);
        String[] spdata = query.split("#");
        ModelMap modelMap = new ModelMap();
        modelMap.addAttribute("message", "Generating species list");
        modelMap.addAttribute("spList", speciesDao.getRecordsByNameLevel(spdata[0], spdata[1]));
        return new ModelAndView("species/records", modelMap);

    }

    @RequestMapping(value = "/names", method = RequestMethod.GET)
    public
    @ResponseBody
    Map getNames2(@RequestParam String q, @RequestParam int s, @RequestParam int p) {
        System.out.println("Looking up names for: " + q);

        /*
        List lsp = speciesDao.findByName(q, s, p);
        //List lsp = speciesDao.getNames();
        Hashtable<String, Object> hout = new Hashtable();
        hout.put("totalCount", lsp.size());
        hout.put("taxanames", lsp);
        return hout;
         *
         */

        return speciesDao.findByName(q, s, p);

    }

    @RequestMapping(value = "/taxon", method = RequestMethod.GET)
    public
    @ResponseBody
    String getTaxonNamesEmpty() {
        return "";
    }

    @RequestMapping(value = "/taxon/{name}", method = RequestMethod.GET)
    public
    @ResponseBody
    String getTaxonNames(@PathVariable("name") String name) {
        StringBuffer slist = new StringBuffer();
        try {

            System.out.println("Looking up names for: " + name);

            TabulationSettings.load();

            name = URLDecoder.decode(name, "UTF-8");

            String[] aslist = OccurrencesService.filterSpecies(name, 40);
            if (aslist == null) {
                aslist = new String[1];
                aslist[0] = "";
            }

            for (String s : aslist) {                
                slist.append(s).append("\n");
            }

            //System.out.println(">>>>> dumping out s.names <<<<<<<<<<");
            //System.out.println(slist);
            //System.out.println(">>>>> dumping out c.names <<<<<<<<<<");
//long t1 = System.currentTimeMillis();
/*
           List<CommonName> clist = speciesDao.getCommonNames(name);
            Iterator<CommonName> it = clist.iterator();
            String previousScientificName = "";
            while (it.hasNext()) {
                CommonName cn = it.next();
                //System.out.println("> " + cn.getCommonname() + " -- " + cn.getScientificname());
                
                //only add if different from previous (query is sorted by scientificName)
                if (!previousScientificName.equals(cn.getScientificname())) {
                    int records = OccurrencesService.getSpeciesCount(cn.getScientificname());
                    slist.append(cn.getCommonname()).append(" / Scientific name: ").append(cn.getScientificname()).append(" / found ").append(records).append("\n");
                    previousScientificName = cn.getScientificname();
                }
            }*/
//long t2 = System.currentTimeMillis();
 
            String s = OccurrencesIndex.getCommonNames(name);
            slist.append(s);

//long t3 = System.currentTimeMillis();

//System.out.println("timings: DAO=" + (t2 - t1) + "ms; OI=" + (t3-t2) + "ms");

            //System.out.println(">>>>> done <<<<<<<<<<");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return slist.toString();

    }

    @RequestMapping(value = "/lsid/{lsid}", method = RequestMethod.GET)
    public
    @ResponseBody
    List<ValidTaxonName> getTaxonByLsid(@PathVariable("lsid") String lsid) {
        try {
            lsid = URLDecoder.decode(lsid, "UTF-8");
            lsid=lsid.replaceAll("__",".");
            System.out.println("Starting out search for: " + lsid); 
            List l = speciesDao.findById(lsid);
            System.out.println("re-returning " + l.size() + " records");

            /*
            int[] recs = OccurrencesIndex.lookup("institutionCode","WAM");
            int[] recs2 = OccurrencesIndex.lookup("collectionCode","MAMM");
            int[] recs3 = OccurrencesIndex.lookup("collectionCode","ARACH");

            int[] recs23 = recs2+recs3;
            sort(recs);
            sort(recs23);
            int[] finalRecs = removeNotInList(recs,recs23);

            double[][] pts = OccurrencesIndex.getPointsPairs();
            for (i=0;i<finalRecs.length;i++) {
                pts[finalRecs[i]][0];
                pts[finalRecs[i]][1];
            }
             */


            return l;
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SpeciesController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Returns a relative path to a zip file of the filtered georeferenced data
     *
     * @param pid
     * @param shape
     * @param req
     * @return
     */
    @RequestMapping(value = "/lsid/{lsid}/geojson", method = RequestMethod.POST)
    public
    @ResponseBody
    String getSamplesListAsGeoJSON(@PathVariable String lsid, HttpServletRequest req) {
        TabulationSettings.load();

        try {

            lsid = URLDecoder.decode(lsid, "UTF-8");
            lsid=lsid.replaceAll("__",".");

            String currentPath = req.getSession().getServletContext().getRealPath(File.separator);
            //String currentPath = TabulationSettings.base_output_dir;
            long currTime = System.currentTimeMillis();
            String outputpath = currentPath + File.separator + "output" + File.separator + "sampling" + File.separator + currTime + File.separator;
            File fDir = new File(outputpath);
            fDir.mkdir();

            String gjsonFile = SamplingService.getLSIDAsGeoJSON(lsid, fDir);

            return "output/sampling/" + currTime + "/" + gjsonFile;
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return "";
    }
}
