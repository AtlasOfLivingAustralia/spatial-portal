/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;

/**
 *
 * @author ajay
 */
public class SamplingProgressWCController extends UtilityComposer {

    private static final String SAT_URL = "sat_url";
    Label jobstatus;
    Progressmeter jobprogress;
    Timer timer;
    Textbox tbPid;
    public String pid = null;
    private String satServer = "";
    private SettingsSupplementary settingsSupplementary = null;
    public SamplingWCController parent = null;

    @Override
    public void afterCompose() {
        super.afterCompose();

        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(SAT_URL);
        }

        timer.stop();
    }

    public void start(String pid_) {
        pid = pid_;
        tbPid.setValue(pid_);

        timer.start();

        onTimer$timer(null);
    }

    public void onTimer$timer(Event e) {
        //get status

        jobstatus.setValue(get("status"));

        String s = get("state");
        if(s.equals("job does not exist")){
            timer.stop();
            getMapComposer().showMessage("Sampling request does not exist","");//get("error"));
            this.detach();
            return;
        }

        String p = get("progress");
        System.out.println("prog:" + p);
        try {
            double d = Double.parseDouble(p);
            jobprogress.setValue((int) (d * 100));
        } catch (Exception ex) {
        }

        if (s.equals("SUCCESSFUL")) {
            timer.stop();
            Events.echoEvent("downloadSampling",parent,null);
            showReferenceNumber();
            this.detach();
        } else if(s.equals("FAILED")) {
            timer.stop();
            getMapComposer().showMessage("Sampling failed");//get("error"));
            this.detach();
        } else if(s.equals("CANCELLED")){
            timer.stop();
            getMapComposer().showMessage("Sampling cancelled by user");
            this.detach();
        }

    }

    String get(String type) {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/alaspatial/ws/jobs/").append(type).append("?pid=").append(pid);

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

    public void onClick$btnCancel(Event e) {
        get("cancel");
        this.detach();
    }

    public void onClick$btnHide(Event e){
        showReferenceNumber();
        this.detach();
    }

    void showReferenceNumber(){
        getMapComposer().showMessage("Reference number to retrieve results: " + pid);
    }
}
