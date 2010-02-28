package org.ala.spatial.web;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.ala.spatial.analysis.maxent.MaxentServiceImpl;
import org.ala.spatial.analysis.maxent.MaxentSettings;
import org.ala.spatial.dao.SpeciesDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajayr
 */
@Controller
@RequestMapping("/maxent")
public class MaxentController {

    private SpeciesDAO speciesDao;

    @Autowired
    public void setSpeciesDao(SpeciesDAO speciesDao) {
        this.speciesDao = speciesDao;
    }

    @RequestMapping(method = RequestMethod.GET)
    public String showPage(HttpSession session) {
        return "maxent";
    }

    @RequestMapping(value = "/process", method = RequestMethod.POST)
    public 
    @ResponseBody
    Map process(HttpServletRequest req) {
        System.out.println("Maxent: " + req);
        //System.out.println(.)

        HttpSession session = req.getSession(false);

        String currentPath = session.getServletContext().getRealPath("/");
        String sessionPath = currentPath + "sessiondata";

        String query = (String) req.getParameter("spdata");

        String[] spdata = query.split("#");

        MaxentSettings msets = new MaxentSettings();
        msets.setEnvList(Arrays.asList(req.getParameterValues("chkEvars")));
        msets.setRandomTestPercentage(Integer.parseInt(req.getParameter("txtTestPercentage")));
        msets.setEnvPath((String) session.getAttribute("worldClimPresentVars") + "10minutes/");
        msets.setEnvVarToggler("world");
        msets.setEnvPrefix("world_10_bio");
        //msets.setSpeciesFilepath();
        msets.setOutputPath(currentPath + "output/maxent/");
        if (req.getParameter("chkJackknife") != null) {
            msets.setDoJackknife(true);
        }
        if (req.getParameter("chkResponseCurves") != null) {
            msets.setDoResponsecurves(true);
        }


        MaxentServiceImpl maxent = new MaxentServiceImpl();
        msets.setSpeciesFilepath(maxent.setupSpecies(speciesDao.getRecordsByNameLevel(spdata[0], spdata[1]), session));
        maxent.setMaxentSettings(msets);
        int exitValue = maxent.process();

        System.out.println("Completed.");

        Hashtable htProcess = new Hashtable();
        if (exitValue == 0) {
            htProcess.put("status", "success");
            htProcess.put("file", "output/maxent/species.asc");

            // if generated successfully, then add it to geoserver
            

        } else {
            htProcess.put("status", "failure");
        }




        //return "maxent";

        return htProcess;

    }
}
