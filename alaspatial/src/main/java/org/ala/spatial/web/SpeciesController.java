package org.ala.spatial.web;

import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;

import org.ala.spatial.analysis.service.OccurrencesService;
import org.ala.spatial.dao.SpeciesDAO;
import org.ala.spatial.model.CommonName;
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
            }
            System.out.println(">>>>> done <<<<<<<<<<");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return slist.toString();

    }
}
