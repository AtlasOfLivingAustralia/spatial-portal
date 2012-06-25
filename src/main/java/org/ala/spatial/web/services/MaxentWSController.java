/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package org.ala.spatial.web.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.util.AnalysisJobMaxent;
import org.ala.spatial.util.AnalysisQueue;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.ala.spatial.util.AlaspatialProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
public class MaxentWSController {

    @RequestMapping(value = "/ws/maxent", method = RequestMethod.POST)
    public @ResponseBody
    String maxent(HttpServletRequest req) {

        try {
            long currTime = System.currentTimeMillis();

            String currentPath = AlaspatialProperties.getBaseOutputDir();
            String taxon = URLDecoder.decode(req.getParameter("taxonid"), "UTF-8").replace("__", ".");
            String taxonlsid = URLDecoder.decode(req.getParameter("taxonlsid"), "UTF-8").replace("__", ".");
            String species = req.getParameter("species");
            String removedSpecies = req.getParameter("removedspecies");
            String area = req.getParameter("area");
            String envlist = req.getParameter("envlist");
            String txtTestPercentage = req.getParameter("txtTestPercentage");
            String chkJackknife = req.getParameter("chkJackknife");
            String chkResponseCurves = req.getParameter("chkResponseCurves");

            String resolution = req.getParameter("res");
            if (resolution == null) {
                resolution = "0.01";
            }

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = LayerFilter.parseLayerFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }

            String pid = Long.toString(currTime);
            writeFile(species, currentPath + "output" + File.separator + "maxent" + File.separator + pid + File.separator, "species_points.csv");
            if (removedSpecies != null) {
                writeFile(removedSpecies, currentPath + "output" + File.separator + "maxent" + File.separator + pid + File.separator, "Prediction_removedSpecies.txt");
            }
            AnalysisJobMaxent ajm = new AnalysisJobMaxent(pid, currentPath, taxon, envlist, region, filter, txtTestPercentage, chkJackknife, chkResponseCurves, resolution);
            StringBuffer inputs = new StringBuffer();
            inputs.append("pid:").append(pid);
            inputs.append(";taxonid:").append(taxon);
            inputs.append(";taxonlsid:").append(taxonlsid);

            inputs.append(";area:").append(area);
            inputs.append(";envlist:").append(envlist);
            inputs.append(";txtTestPercentage:").append(txtTestPercentage);
            inputs.append(";chkJackknife:").append(chkJackknife);
            inputs.append(";chkResponseCurves:").append(chkResponseCurves);
            inputs.append(";resolution:").append(resolution);
            ajm.setInputs(inputs.toString());
            AnalysisQueue.addJob(ajm);

            return pid;

        } catch (Exception e) {
            System.out.println("Error processing Maxent request:");
            e.printStackTrace(System.out);
        }

        return "";

    }

    private String writeFile(String contents, String outputpath, String filename) {
        try {
            File fDir = new File(outputpath);
            fDir.mkdir();

            File spFile = new File(fDir, filename);
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(spFile)));

            spWriter.write(contents);
            spWriter.close();

            return spFile.getAbsolutePath();
        } catch (IOException ex) {
            System.out.println("error writing species file:");
            ex.printStackTrace(System.out);
        }

        return null;
    }

    @RequestMapping(value = "/ws/maxent/estimate", method = RequestMethod.POST)
    public @ResponseBody
    String maxentEstimate(HttpServletRequest req) {

        try {
            long currTime = System.currentTimeMillis();

            String currentPath = AlaspatialProperties.getBaseOutputDir();
            String taxon = URLDecoder.decode(req.getParameter("taxonid"), "UTF-8").replace("__", ".");
            String taxonlsid = URLDecoder.decode(req.getParameter("taxonlsid"), "UTF-8").replace("__", ".");
            String species = req.getParameter("species");
            String removedSpecies = req.getParameter("removedspecies");
            String area = req.getParameter("area");
            String envlist = req.getParameter("envlist");
            String txtTestPercentage = req.getParameter("txtTestPercentage");
            String chkJackknife = req.getParameter("chkJackknife");
            String chkResponseCurves = req.getParameter("chkResponseCurves");

            String resolution = req.getParameter("res");
            if (resolution == null) {
                resolution = "0.01";
            }

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = LayerFilter.parseLayerFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }

            String pid = Long.toString(currTime);
            writeFile(species, currentPath + "output" + File.separator + "maxent" + File.separator + pid + File.separator, "species_points.csv");
            if (removedSpecies != null) {
                writeFile(removedSpecies, currentPath + "output" + File.separator + "maxent" + File.separator + pid + File.separator, "Prediction_removedSpecies.txt");
            }
            AnalysisJobMaxent ajm = new AnalysisJobMaxent(pid, currentPath, taxon, envlist, region, filter, txtTestPercentage, chkJackknife, chkResponseCurves, resolution);
            StringBuffer inputs = new StringBuffer();
            inputs.append("pid:").append(pid);
            inputs.append(";taxonid:").append(taxon);
            inputs.append(";taxonlsid:").append(taxonlsid);

            inputs.append(";area:").append(area);
            inputs.append(";envlist:").append(envlist);
            inputs.append(";txtTestPercentage:").append(txtTestPercentage);
            inputs.append(";chkJackknife:").append(chkJackknife);
            inputs.append(";chkResponseCurves:").append(chkResponseCurves);
            inputs.append(";resolution:").append(resolution);
            ajm.setInputs(inputs.toString());
            //AnalysisQueue.addJob(ajm);

            return String.valueOf(ajm.getEstimate());

        } catch (Exception e) {
            System.out.println("Error processing Maxent request:");
            e.printStackTrace(System.out);
        }

        return "";

    }
}
