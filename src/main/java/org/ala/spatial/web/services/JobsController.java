/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.web.services;

import au.com.bytecode.opencsv.CSVReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Hashtable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.maxent.MaxentServiceImpl;
import org.ala.spatial.analysis.maxent.MaxentSettings;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.util.AnalysisQueue;
import org.ala.spatial.util.GridCutter;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.UploadSpatialResource;
import org.ala.spatial.util.Zipper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
/**
 *
 * @author Adam
 */
@Controller
@RequestMapping("/ws/jobs")
public class JobsController {

    @RequestMapping(value = "/state", method = RequestMethod.GET)
    public
    @ResponseBody
    String state(HttpServletRequest req) {
        try {
            String pid = req.getParameter("pid");

            String s = AnalysisQueue.getState(pid);
            if(s != null) return s;

        } catch (Exception e){
            e.printStackTrace();
        }

        return "job does not exist";
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public
    @ResponseBody
    String status(HttpServletRequest req) {
        try {     
            String pid = req.getParameter("pid");

            String s =  AnalysisQueue.getStatus(pid);
            if(s != null) return s;
        } catch (Exception e){
            e.printStackTrace();
        }

        return "job does not exist";
    }

    @RequestMapping(value = "/progress", method = RequestMethod.GET)
    public
    @ResponseBody
    String progress(HttpServletRequest req) {
        try {
            String pid = req.getParameter("pid");

            String s = AnalysisQueue.getProgress(pid);
            if(s != null) return s;
        } catch (Exception e){
            e.printStackTrace();
        }

        return "job does not exist";
    }

    @RequestMapping(value = "/log", method = RequestMethod.GET)
    public
    @ResponseBody
    String log(HttpServletRequest req) {
        try {
            String pid = req.getParameter("pid");

            String s = AnalysisQueue.getLog(pid);
            if(s != null) return s;
        } catch (Exception e){
            e.printStackTrace();
        }

        return "job does not exist";
    }
}


