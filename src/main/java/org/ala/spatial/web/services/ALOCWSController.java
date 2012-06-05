package org.ala.spatial.web.services;

import javax.servlet.http.HttpServletRequest;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.util.AlaspatialProperties;
import org.ala.spatial.util.AnalysisJobAloc;
import org.ala.spatial.util.AnalysisQueue;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
public class ALOCWSController {

    @RequestMapping(value = "/ws/aloc", method = RequestMethod.POST)
    public
    @ResponseBody
    String aloc(HttpServletRequest req) {
        String pid = "";
        try {

            long currTime = System.currentTimeMillis();

            String currentPath = AlaspatialProperties.getBaseOutputDir();
            String groupCount = req.getParameter("gc");
            String area = req.getParameter("area");

            String resolution = req.getParameter("res");
            if (resolution == null) {
                resolution = "0.01";
            }

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = LayerFilter.parseLayerFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(req.getParameter("area"));
            }

            pid = Long.toString(currTime);
            AnalysisJobAloc aja = new AnalysisJobAloc(pid, currentPath, req.getParameter("envlist"), Integer.parseInt(groupCount), region, filter, area, resolution);
            StringBuffer inputs = new StringBuffer();
            inputs.append("pid:").append(pid);
            inputs.append(";gc:").append(groupCount);
            inputs.append(";area:").append(area);
            inputs.append(";envlist:").append(req.getParameter("envlist"));
            inputs.append(";resolution:").append(resolution);
            aja.setInputs(inputs.toString());
            AnalysisQueue.addJob(aja);

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return pid;
    }

    @RequestMapping(value = "/ws/aloc/estimate", method = {RequestMethod.POST, RequestMethod.GET})
    public
    @ResponseBody
    String alocEstimate(HttpServletRequest req) {
        String pid = "";
        try {

            long currTime = System.currentTimeMillis();

            String currentPath = AlaspatialProperties.getBaseOutputDir();
            String groupCount = req.getParameter("gc");
            String area = req.getParameter("area");

            String resolution = req.getParameter("res");
            if (resolution == null) {
                resolution = "0.01";
            }

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = LayerFilter.parseLayerFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(req.getParameter("area"));
            }

            pid = Long.toString(currTime);
            AnalysisJobAloc aja = new AnalysisJobAloc(pid, currentPath, req.getParameter("envlist"), Integer.parseInt(groupCount), region, filter, area, resolution);
            StringBuffer inputs = new StringBuffer();
            inputs.append("pid:").append(pid);
            inputs.append(";gc:").append(groupCount);
            inputs.append(";area:").append(area);
            inputs.append(";envlist:").append(req.getParameter("envlist"));
            inputs.append(";resolution:").append(resolution);
            aja.setInputs(inputs.toString());
            //AnalysisQueue.addJob(aja);
            
            return String.valueOf(aja.getEstimate()); 

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return "";
    }
}
