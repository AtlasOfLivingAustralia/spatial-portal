package org.ala.spatial.web.services;

import java.io.File;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.util.AnalysisJobSitesBySpecies;
import org.ala.spatial.util.AnalysisQueue;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.TabulationSettings;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws/sitesbyspecies/")
public class SitesBySpeciesWSController {

    @RequestMapping(value = "/processgeoq", method = RequestMethod.POST)
    public
    @ResponseBody
    String processgeoq(HttpServletRequest req) {

        try {
            TabulationSettings.load();

            long currTime = System.currentTimeMillis();

            String currentPath = TabulationSettings.base_output_dir + "output" + File.separator + "sitesbyspecies";
            String qname = URLDecoder.decode(req.getParameter("qname"), "UTF-8").replace("__", ".");
            String speciesq = URLDecoder.decode(req.getParameter("speciesq"), "UTF-8").replace("__", ".");
            String area = req.getParameter("area");
            double gridsize = Double.parseDouble(req.getParameter("gridsize"));
            int movingAverageSize = Integer.parseInt(req.getParameter("movingaveragesize"));
            boolean occurrencedensity = req.getParameter("occurrencedensity") != null;
            boolean speciesdensity = req.getParameter("speciesdensity") != null;
            boolean sitesbyspecies = req.getParameter("sitesbyspecies") != null;

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = FilteringService.getFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }

            String pid = Long.toString(currTime);

            AnalysisJobSitesBySpecies sbs = new AnalysisJobSitesBySpecies(pid, currentPath, qname, speciesq, gridsize, region, filter, sitesbyspecies, occurrencedensity, speciesdensity, movingAverageSize);

            StringBuffer inputs = new StringBuffer();
            inputs.append("pid:").append(pid);
            inputs.append(";qname:").append(qname);
            inputs.append(";speciesq:").append(speciesq);
            inputs.append(";gridsize:").append(gridsize);
            inputs.append(";area:").append(area);
            if(occurrencedensity) {
                inputs.append(";occurrencedensity:true").append(area);
            }
            if(speciesdensity) {
                inputs.append(";speciesdensity:true").append(area);
            }
            if(sitesbyspecies) {
                inputs.append(";sitesbyspecies:true").append(area);
            }
            sbs.setInputs(inputs.toString());
            AnalysisQueue.addJob(sbs);

            return pid;

        } catch (Exception e) {
            System.out.println("Error processing SitesBySpecies request:");
            e.printStackTrace(System.out);
        }

        return "";
    }
}
