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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.ala.spatial.util.AnalysisQueue;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Analysis jobs general webservices.
 *
 * @author Adam
 */
@Controller
public class JobsController {

    static HashMap<String, Integer> lastLogPos = new HashMap<String, Integer>();

    @RequestMapping(value = "/ws/job", method = RequestMethod.GET)
    public
    @ResponseBody
    Map job(@RequestParam(value = "pid", required = true, defaultValue = "") String pid, HttpServletRequest req) {
        String fullLog = req.getParameter("log");
        Map m = new HashMap<String, String>();
        m.put("state", "job does not exist");
        try {
            String state = AnalysisQueue.getState(pid);
            if (state != null) {
                m.put("state", state);
                m.put("message", AnalysisQueue.getMessage(pid));
                m.put("progress", AnalysisQueue.getProgress(pid));
                m.put("status", AnalysisQueue.getStatus(pid));

                if (fullLog == null) {
                    Integer pos = lastLogPos.containsKey(pid) ? lastLogPos.get(pid) : 0;
                    String log = AnalysisQueue.getLog(pid);
                    if (log == null) {
                        log = "";
                    }
                    m.put("log", log.substring(pos));
                    lastLogPos.put(pid, log.length());
                } else {
                    m.put("log", AnalysisQueue.getLog(pid));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return m;
    }

    @RequestMapping(value = "/ws/jobs/listwaiting", method = RequestMethod.GET)
    public
    @ResponseBody
    String listWaiting() {
        try {
            String s = AnalysisQueue.listWaiting();
            if (s != null) {
                return s;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "job does not exist";
    }

    @RequestMapping(value = "/ws/jobs/listrunning", method = RequestMethod.GET)
    public
    @ResponseBody
    String listRunning() {
        try {
            String s = AnalysisQueue.listRunning();
            if (s != null) {
                return s;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "job does not exist";
    }

    @RequestMapping(value = "/ws/jobs/listfinished", method = RequestMethod.GET)
    public
    @ResponseBody
    String listFinished() {
        try {
            String s = AnalysisQueue.listFinished();
            if (s != null) {
                return s;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "job does not exist";
    }

    @RequestMapping(value = "/ws/jobs/cancel", method = RequestMethod.GET)
    public
    @ResponseBody
    String cancel(@RequestParam(value = "pid", required = true, defaultValue = "") String pid) {
        try {

            AnalysisQueue.cancelJob(pid);
            return "";
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "job does not exist";
    }

    @RequestMapping(value = "/ws/jobs/inputs", method = RequestMethod.GET)
    public
    @ResponseBody
    String inputs(@RequestParam(value = "pid", required = true, defaultValue = "") String pid) {
        try {
            String s = AnalysisQueue.getInputs(pid);
            return s;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "job does not exist";
    }

    @RequestMapping(value = "/ws/jobs/image", method = RequestMethod.GET)
    public
    @ResponseBody
    String image(@RequestParam(value = "pid", required = true, defaultValue = "") String pid) {
        try {
            String s = AnalysisQueue.getImage(pid);
            return s;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "job does not exist";
    }
}
