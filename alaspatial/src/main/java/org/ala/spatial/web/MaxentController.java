package org.ala.spatial.web;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.ala.spatial.analysis.maxent.MaxentServiceImpl;
import org.ala.spatial.analysis.maxent.MaxentSettings;
import org.ala.spatial.dao.SpeciesDAO;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.UploadSpatialResource;
import org.ala.spatial.util.Zipper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * MaxentController provides the url resolutions for the maxent service.
 * Also loads up the output grid into GeoServer for mapping and sends
 * the result to the client
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

    /** 
     * showPage function to return the base page
     * 
     * @param session Current session object
     * @return the VIEW page 
     */
    @RequestMapping(method = RequestMethod.GET)
    public String showPage(HttpSession session) {
        return "maxent";
    }

    /**
     * process method to process the user request to call the maxent service
     * with the various parameters set and eventually sending the output grid
     * to GeoServer. 
     * 
     * @param req
     * @return
     */
    @RequestMapping(value = "/process", method = RequestMethod.POST)
    public @ResponseBody Map process(HttpServletRequest req) {
        System.out.println("Maxent: " + req);
        //System.out.println(.)

        HttpSession session = req.getSession(false);

        //String currentPath = session.getServletContext().getRealPath("/");
        String currentPath = TabulationSettings.base_output_dir;
        String sessionPath = currentPath + "sessiondata/" + session.getId() + "/";

        String query = (String) req.getParameter("spdata");

        String[] spdata = query.split("#");

        MaxentSettings msets = new MaxentSettings();
        msets.setMaxentPath((String)session.getAttribute("maxentCmdPath"));
        msets.setEnvList(Arrays.asList(req.getParameterValues("chkEvars")));
        msets.setRandomTestPercentage(Integer.parseInt(req.getParameter("txtTestPercentage")));
        msets.setEnvPath((String) session.getAttribute("worldClimPresentVars") + "10minutes/");
        msets.setEnvVarToggler("world");
        msets.setEnvPrefix("world_10_bio");
        //msets.setSpeciesFilepath();
        msets.setOutputPath(currentPath + "output/maxent/" + session.getId() + "/");
        if (req.getParameter("chkJackknife") != null) {
            msets.setDoJackknife(true);
        }
        if (req.getParameter("chkResponseCurves") != null) {
            msets.setDoResponsecurves(true);
        }


        MaxentServiceImpl maxent = new MaxentServiceImpl();
        maxent.setMaxentSettings(msets);
        maxent.getMaxentSettings().setSpeciesFilepath(maxent.setupSpecies(speciesDao.getRecordsByNameLevel(spdata[0], spdata[1]), session));
        int exitValue = maxent.process();

        System.out.println("Completed.");

        Hashtable htProcess = new Hashtable();
        if (exitValue == 0) {
            // TODO: Should probably move this part an external "parent"
            // function so can be used by other functions
            // 
            // if generated successfully, then add it to geoserver
            String url = session.getAttribute("geoserver_url") + "/rest/workspaces/ALA/coveragestores/maxent_" + session.getId() + "/file.arcgrid?coverageName=species_" + session.getId();
            String extra = "";
            String username = (String) session.getAttribute("geoserver_username");
            String password = (String) session.getAttribute("geoserver_password");

            // first zip up the file as it's going to be sent as binary
            String ascZipFile = Zipper.zipFile(msets.getOutputPath() + "species.asc");

            // Upload the file to GeoServer using REST calls
            System.out.println("Uploading file: " + ascZipFile + " to \n" + url); 
            UploadSpatialResource.loadResource(url, extra, username, password, ascZipFile);
            
            htProcess.put("status", "success");
            htProcess.put("file", "output/maxent/" + session.getId() + "/species.asc");
            htProcess.put("info","output/maxent/" + session.getId() + "/species.html");
            htProcess.put("map",session.getAttribute("geoserver_url") + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + session.getId() + "&styles=alastyles&bbox=112.0,-44.0,154.0,-9.0&width=700&height=500&srs=EPSG:4326&format=application/openlayers");
            htProcess.put("sid",session.getId());


        } else {
            htProcess.put("status", "failure");
        }

        return htProcess;

    }
}
