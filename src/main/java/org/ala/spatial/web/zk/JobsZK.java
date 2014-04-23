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
package org.ala.spatial.web.zk;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.ala.spatial.util.AlaspatialProperties;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

public class JobsZK extends GenericForwardComposer {

    //Memory tab
    Label lMemUsage;
    //Analysis tab
    Listbox lbwaiting;
    Listbox lbrunning;
    Listbox lbfinished;
    Textbox selectedJob;
    Textbox joblog;
    Textbox jobparameters;
    Iframe jobimage;
    Textbox imgpth;
    String pid;
    Textbox newjob;
    Textbox cmdtext;
    Textbox txtloggingurl;
    //Other tab
    Label lCachedImageCount;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        System.out.println("***************** ***********");
        setMemoryLabel();

        onClick$refreshButton(null);

        updateCachedImageCount();

    }

    public void onClick$btnProcessCommand(Event e) {
        if (!cmdtext.getValue().trim().equals("")) {
            String[] cmds = cmdtext.getValue().split("\n");

            Map<String, String> cmd = new HashMap<String, String>();
            for (String c : cmds) {
                String key = c.substring(0, c.indexOf("="));
                String value = c.substring(c.indexOf("=") + 1);
                System.out.println("Adding " + key + " => " + value);
                cmd.put(key, value);
            }
        }
    }

    public void onClick$refreshButton(Event e) {
        String[] s = get("listwaiting").split("\n");
        java.util.Arrays.sort(s);
        lbwaiting.setModel(new SimpleListModel(s));
        s = get("listrunning").split("\n");
        java.util.Arrays.sort(s);
        lbrunning.setModel(new SimpleListModel(s));
        s = get("listfinished").split("\n");
        java.util.Arrays.sort(s);
        lbfinished.setModel(new SimpleListModel(s));
    }

    public void onSelect$lbwaiting(Event e) {
        String s = lbwaiting.getSelectedItem().getLabel();
        pid = s.substring(0, s.indexOf(";"));
        selectedJob.setValue(pid);
        refreshInfo();
    }

    public void onSelect$lbrunning(Event e) {
        String s = lbrunning.getSelectedItem().getLabel();
        pid = s.substring(0, s.indexOf(";"));
        selectedJob.setValue(pid);
        refreshInfo();
    }

    public void onSelect$lbfinished(Event e) {
        String s = lbfinished.getSelectedItem().getLabel();
        pid = s.substring(0, s.indexOf(";"));
        selectedJob.setValue(pid);
        refreshInfo();
    }

    public void onClick$lbwaiting(Event e) {
        String s = lbwaiting.getSelectedItem().getLabel();
        pid = s.substring(0, s.indexOf(";"));
        selectedJob.setValue(pid);
        refreshInfo();
    }

    public void onClick$lbrunning(Event e) {
        String s = lbrunning.getSelectedItem().getLabel();
        pid = s.substring(0, s.indexOf(";"));
        selectedJob.setValue(pid);
        refreshInfo();
    }

    public void onClick$lbfinished(Event e) {
        String s = lbfinished.getSelectedItem().getLabel();
        pid = s.substring(0, s.indexOf(";"));
        selectedJob.setValue(pid);
        refreshInfo();
    }

    public void onClick$btnCancel(Event e) {
        get("cancel");
    }

    void refreshInfo() {
        pid = selectedJob.getValue();
        joblog.setValue(get("log"));
        jobparameters.setValue(get("inputs").replace(";", "\r\n"));
        String imgsrc = get("image");
        if (imgsrc == null) {
            imgsrc = "";
        }
        //jobimage.setSrc(TabulationSettings.alaspatial_path + imgsrc);
        imgpth.setText(imgsrc);
        jobimage.setSrc(AlaspatialProperties.getBaseOutputURL() + "/" + imgsrc);
    }

    String get(String type) {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(AlaspatialProperties.getAlaspatialUrl() + "ws/jobs/").append(type).append("?pid=").append(pid);

            System.out.println(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println(slist);
            return slist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void onClick$btnRefreshInfo(Event event) {
        refreshInfo();
    }

    public void onClick$btnCopyJob(Event event) {
        pid = get("copy");
        refreshInfo();
    }

    public void onClick$btnMemoryClean(Event event) {
        System.gc();
        setMemoryLabel();
    }

    void setMemoryLabel() {
        lMemUsage.setValue("Memory usage (total/used/free):" + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB / " + (Runtime.getRuntime().totalMemory() / 1024 / 1024 - Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB / " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB");
    }

    public void onClick$btnNewClassification(Event event) {
        String txt = newjob.getText();

        try {
            int pos = 0;
            while (true) {
                int p1 = txt.indexOf("pid:", pos);
                if (p1 < 0) {
                    break;
                }
                int p2 = txt.indexOf("gc:", pos);
                int p3 = txt.indexOf("area:", pos);
                int p4 = txt.indexOf("envlist:", pos);
                int p5 = txt.indexOf("pid:", p1 + 4);
                if (p5 < 0) {
                    p5 = txt.length();
                }

                pos = p5 - 5;

                String pid = txt.substring(p1 + 4, p2).trim();
                String gc = txt.substring(p2 + 3, p3).trim();
                String area = txt.substring(p3 + 5, p4).trim();
                String envlist = txt.substring(p4 + 8, p5).trim();

                System.out.println("got [" + pid + "][" + gc + "][" + area + "][" + envlist + "]");

                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append(AlaspatialProperties.getAlaspatialUrl() + "ws/aloc/processgeoq?");
                sbProcessUrl.append("gc="
                        + URLEncoder.encode(gc, "UTF-8"));
                sbProcessUrl.append("&envlist="
                        + URLEncoder.encode(envlist, "UTF-8"));

                HttpClient client = new HttpClient();
                PostMethod get = new PostMethod(sbProcessUrl.toString());

                get.addParameter("area", URLEncoder.encode(area, "UTF-8"));

                get.addRequestHeader("Accept", "text/plain");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();

                System.out.println("Got response from ALOCWSController: \n" + slist);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$btnClearCache(Event event) {
        String pth = AlaspatialProperties.getBaseOutputDir() + "output" + File.separator + "sampling" + File.separator;
        File dir = new File(pth);
        String[] f = dir.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith("png");
            }
        });
        for (int i = 0; i < f.length; i++) {
            FileUtils.deleteQuietly(new File(pth + f[i]));
        }

        updateCachedImageCount();
    }

    void updateCachedImageCount() {
        File dir = new File(AlaspatialProperties.getBaseOutputDir() + "output" + File.separator + "sampling" + File.separator);
        String[] f = dir.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith("png");
            }
        });

        if (f == null) {
            lCachedImageCount.setValue("0");
        } else {
            lCachedImageCount.setValue(String.valueOf(f.length));
        }

    }
}
