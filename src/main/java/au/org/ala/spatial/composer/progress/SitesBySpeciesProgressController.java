/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.progress;

import au.org.ala.spatial.StringConstants;
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

    private static final Logger LOGGER = Logger.getLogger(SitesBySpeciesProgressController.class);
    private String pid = null;
    private Window parent = null;
    private RemoteLogger remoteLogger;
    private Label jobstatus;
    private Progressmeter jobprogress;
    private Timer timer;
    private Textbox tbPid;

    @Override
    public void afterCompose() {
        super.afterCompose();
        timer.stop();
    }

    public void start(String pid) {
        this.pid = pid;
        tbPid.setValue(pid);

        timer.start();

        onTimer$timer(null);
    }

    public void onTimer$timer(Event e) {
        if (parent == null) {
            parent = (Window) this.getParent();
        }

        //get status
        String status = get(StringConstants.STATUS);
        if (status.length() > 0) {
            jobstatus.setValue(status);
        }

        String s = get(StringConstants.STATE);
        if (StringConstants.JOB_DOES_NOT_EXIST.equals(s)) {
            timer.stop();
            getMapComposer().showMessage("Points to grid request does not exist", "");
            this.detach();
            return;
        }

        LOGGER.debug("**************** STATE: " + s);
        remoteLogger.logMapAnalysisUpdateStatus(pid, s);

        String p = get(StringConstants.PROGRESS);

        try {
            double d = Double.parseDouble(p);
            jobprogress.setValue((int) (d * 100));
        } catch (Exception ex) {
            LOGGER.error("failed to parse progress %: " + p);
        }

        if (StringConstants.SUCCESSFUL.equals(s)) {
            timer.stop();
            Events.echoEvent("loadMap", parent, null);
            this.detach();
        } else if (s.startsWith(StringConstants.FAILED)) {
            timer.stop();
            String errorInfo = get("message");
            if (!StringConstants.JOB_DOES_NOT_EXIST.equals(errorInfo)) {
                errorInfo = " with the following message: \n\n" + errorInfo;
            } else {
                errorInfo = "";
            }
            getMapComposer().showMessage("Points to grid failed" + errorInfo);
            this.detach();
            this.parent.detach();
        } else if (StringConstants.CANCELLED.equals(s)) {
            timer.stop();
            getMapComposer().showMessage("Points to grid cancelled by user");
            this.detach();
        }
    }

    String get(String type) {
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod((CommonData.getSatServer() + "/ws/jobs/") + type + "?pid=" + pid);

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            client.getHttpConnectionManager().getParams().setSoTimeout(timer.getDelay());

            client.executeMethod(get);
            return get.getResponseBodyAsString();
        } catch (SocketTimeoutException e) {
            LOGGER.debug("progress timeout exception, will be trying again.");
        } catch (Exception e) {
            LOGGER.error("error getting updated job info pid=" + pid, e);
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
