/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;

/**
 *
 * @author ajay
 */
public class MaxentProgressWCController extends UtilityComposer {

    Label jobstatus;
    Progressmeter jobprogress;
    Timer timer;
    Textbox tbPid;
    public String pid = null;
    private String satServer = "";
    private SettingsSupplementary settingsSupplementary = null;
    public MaxentWCController parent = null;

    @Override
    public void afterCompose() {
        super.afterCompose();

        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
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
            getMapComposer().showMessage("Prediction request does not exist","");//get("error"));
            this.detach();
            return;
        }

        String p = get("progress");
        
        try {
            double d = Double.parseDouble(p);
            jobprogress.setValue((int) (d * 100));
        } catch (Exception ex) {
        }

        if (s.equals("SUCCESSFUL")) {
            timer.stop();            
            //Events.echoEvent("loadMap", parent, null);
            parent.loadMap(null);
            this.detach();
        } else if(s.equals("FAILED")) {
            timer.stop();
            getMapComposer().showMessage("Prediction failed");
            this.detach();
        } else if(s.equals("CANCELLED")){
            timer.stop();
            getMapComposer().showMessage("Prediction cancelled by user");
            this.detach();
        }
    }

    String get(String type) {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/alaspatial/ws/jobs/").append(type).append("?pid=").append(pid);
            
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            client.getHttpConnectionManager().getParams().setSoTimeout(timer.getDelay());
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
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
