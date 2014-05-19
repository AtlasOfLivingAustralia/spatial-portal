/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.progress;

import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.composer.UtilityComposer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.*;

import java.net.SocketTimeoutException;

/**
 * @author ajay
 */
public class SitesBySpeciesProgressController extends UtilityComposer {

    private static Logger logger = Logger.getLogger(SitesBySpeciesProgressController.class);

    RemoteLogger remoteLogger;
    Label jobstatus;
    Progressmeter jobprogress;
    Timer timer;
    Textbox tbPid;
    public String pid = null;
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
        if (parent == null) {
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
            getMapComposer().showMessage("Points to grid request does not exist", "");//get("error"));
            this.detach();
            return;
        }

        logger.debug("**************** STATE: " + s);
        remoteLogger.logMapAnalysisUpdateStatus(pid, s);

        String p = get("progress");

        try {
            double d = Double.parseDouble(p);
            jobprogress.setValue((int) (d * 100));
        } catch (Exception ex) {
        }

        if (s.equals("SUCCESSFUL")) {
            timer.stop();
            Events.echoEvent("loadMap", parent, null);
            this.detach();
        } else if (s.startsWith("FAILED")) {
            timer.stop();
            String error_info = get("message");
            if (!error_info.equals("job does not exist")) {
                error_info = " with the following message: \n\n" + error_info;
            } else {
                error_info = "";
            }
            getMapComposer().showMessage("Points to grid failed" + error_info);
            this.detach();
            this.parent.detach();
        } else if (s.equals("CANCELLED")) {
            timer.stop();
            getMapComposer().showMessage("Points to grid cancelled by user");
            this.detach();
        }
    }

    String get(String type) {
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod((CommonData.satServer + "/ws/jobs/") + type + "?pid=" + pid);

            get.addRequestHeader("Accept", "text/plain");

            client.getHttpConnectionManager().getParams().setSoTimeout(timer.getDelay());
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            return slist;
        } catch (SocketTimeoutException e) {
        } catch (Exception e) {
            logger.error("error getting updated job info pid=" + pid, e);
        }
        return "";
    }

    public void onClick$btnCancel(Event e) {
        get("cancel");
        this.detach();
    }

    public void onClick$btnHide(Event e) {
        this.detach();
    }
}
