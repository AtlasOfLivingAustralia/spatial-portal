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
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Window;

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
    //public MaxentWCController parent = null;
    public Window parent = null;

    @Override
    public void afterCompose() {
        super.afterCompose();
        timer.stop();
    }

    public void start(String pid_) {
        pid = pid_;
        tbPid.setValue(pid_);

        timer.start();

        onTimer$timer(null);
    }

    public void onTimer$timer(Event e) {
        if(parent == null) {
            parent = (Window) this.getParent();
        }

        //get status
        String status = get("status");
        if (status.length() > 0) {
            jobstatus.setValue(status);
        }

        String s = get("state");
        if (s.equals("job does not exist")) {
            timer.stop();
            getMapComposer().showMessage("Prediction request does not exist", "");//get("error"));
            this.detach();
            return;
        }

        System.out.println("**************** STATE: " + s);

        String p = get("progress");

        try {
            double d = Double.parseDouble(p);
            jobprogress.setValue((int) (d * 100));
        } catch (Exception ex) {
        }

        if (s.equals("SUCCESSFUL")) {
            timer.stop();
            Events.echoEvent("loadMap", parent, null);
            //parent.loadMap(null);
            this.detach();
        } else if (s.startsWith("FAILED")) {
            timer.stop();
            //String error_info = (s.contains(";")?"\n"+s.substring(s.indexOf(";")+1):"");
            String error_info = get("message");
            if (!error_info.equals("job does not exist")) {
                error_info = " with the following message: \n\n" + error_info;
            } else {
                error_info = "";
            }
            getMapComposer().showMessage("Prediction failed" + error_info);
            this.detach();
            this.parent.detach();
        } else if (s.equals("CANCELLED")) {
            timer.stop();
            getMapComposer().showMessage("Prediction cancelled by user");
            this.detach();
        }
    }

    String get(String type) {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/jobs/").append(type).append("?pid=").append(pid);

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

    public void onClick$btnHide(Event e) {
        showReferenceNumber();
        this.detach();
    }

    void showReferenceNumber() {
        //getMapComposer().showMessage("Reference number to retrieve results: " + pid);
    }
}
