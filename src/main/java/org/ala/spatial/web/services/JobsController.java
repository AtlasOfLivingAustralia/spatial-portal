/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.web.services;

import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.util.AnalysisQueue;
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

    @RequestMapping(value = "/message", method = RequestMethod.GET)
    public
    @ResponseBody
    String message(HttpServletRequest req) {
        try {
            String pid = req.getParameter("pid");

            String s = AnalysisQueue.getMessage(pid);
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

    @RequestMapping(value = "/listwaiting", method = RequestMethod.GET)
    public
    @ResponseBody
    String listWaiting(HttpServletRequest req) {
        try {
            String s = AnalysisQueue.listWaiting();
            if(s != null) return s;

        } catch (Exception e){
            e.printStackTrace();
        }

        return "job does not exist";
    }


    @RequestMapping(value = "/listrunning", method = RequestMethod.GET)
    public
    @ResponseBody
    String listRunning(HttpServletRequest req) {
        try {
            String s = AnalysisQueue.listRunning();
            if(s != null) return s;

        } catch (Exception e){
            e.printStackTrace();
        }

        return "job does not exist";
    }

    @RequestMapping(value = "/listfinished", method = RequestMethod.GET)
    public
    @ResponseBody
    String listFinished(HttpServletRequest req) {
        try {
            String s = AnalysisQueue.listFinished();
            if(s != null) return s;

        } catch (Exception e){
            e.printStackTrace();
        }

        return "job does not exist";
    }

    @RequestMapping(value = "/cancel", method = RequestMethod.GET)
    public
    @ResponseBody
    String cancel(HttpServletRequest req) {
        try {
            String pid = req.getParameter("pid");

            AnalysisQueue.cancelJob(pid);
            return "";
        } catch (Exception e){
            e.printStackTrace();
        }

        return "job does not exist";
    }

    @RequestMapping(value = "/inputs", method = RequestMethod.GET)
    public
    @ResponseBody
    String inputs(HttpServletRequest req) {
        try {
            String pid = req.getParameter("pid");

            String s = AnalysisQueue.getInputs(pid);
            return s;
        } catch (Exception e){
            e.printStackTrace();
        }

        return "job does not exist";
    }

    @RequestMapping(value = "/image", method = RequestMethod.GET)
    public
    @ResponseBody
    String image(HttpServletRequest req) {
        try {
            String pid = req.getParameter("pid");

            String s = AnalysisQueue.getImage(pid);
            return s;
        } catch (Exception e){
            e.printStackTrace();
        }

        return "job does not exist";
    }

    @RequestMapping(value = "/copy", method = RequestMethod.GET)
    public
    @ResponseBody
    String copy(HttpServletRequest req) {
        try {
            String pid = req.getParameter("pid");

            String s = AnalysisQueue.copy(pid);

            if(s == null) s = "";
            
            return s;
        } catch (Exception e){
            e.printStackTrace();
        }

        return "job does not exist";
    }
}


