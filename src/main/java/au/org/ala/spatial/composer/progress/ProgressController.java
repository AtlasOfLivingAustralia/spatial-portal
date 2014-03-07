/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.progress;

import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.UtilityComposer;
import net.sf.json.JSONObject;
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
public class ProgressController extends UtilityComposer {

    private static Logger logger = Logger.getLogger(ProgressController.class);
    RemoteLogger remoteLogger;
    Label jobstatus;
    Progressmeter jobprogress;
    Timer timer;
    Textbox tbPid;
    public String pid = null;
    public Window parent = null;
    public String name = "";
    public boolean background = true;
    Textbox txtLog;
    String log = "";
    Button btnHide;

    @Override
    public void afterCompose() {
        super.afterCompose();
        timer.stop();
    }

    public void start(String pid_, String name) {
        start(pid_, name, true);
    }

    public void start(String pid_, String name, boolean background) {
        pid = pid_;
        tbPid.setValue(pid_);
        this.name = name;
        this.setTitle(name);
        this.background = background;

        if (this.background) {
            //onClick$btnHide(null);
            btnHide.setVisible(true);
        }

        timer.start();

        onTimer$timer(null);
    }

    public void onTimer$timer(Event e) {
        //get status
        if (parent == null) {
            parent = (Window) this.getParent();
        }

        JSONObject jo = get();

        //try again next timeout
        if (jo == null) {
            return;
        }

        if (jo.containsKey("status")) {
            jobstatus.setValue(jo.getString("status"));
        }

        String s = jo.getString("state");
        if (s.equals("job does not exist")) {
            timer.stop();
            getMapComposer().showMessage(name + " request does not exist", "");//get("error"));
            this.detach();
            return;
        }

        String p = jo.getString("progress");
        try {
            double d = Double.parseDouble(p);
            jobprogress.setValue((int) (d * 100));
        } catch (Exception ex) {
        }

        String log = jo.getString("log");
        if (log != null) {
            this.log = reverseLines(log) + this.log;
            if (txtLog != null) {
                txtLog.setValue(this.log);
            }
        }

        remoteLogger.logMapAnalysisUpdateStatus(pid, s);

        if (s.equals("SUCCESSFUL")) {
            timer.stop();
            logger.debug("JOB DONE. Calling loadMap");
            Events.echoEvent("loadMap", parent, null);
            this.detach();
        } else if (s.startsWith("FAILED")) {
            timer.stop();
            String error_info = jo.getString("message");
            if (!error_info.equals("job does not exist")) {
                error_info = " with the following message: \n\n" + Util.breakString(error_info, 64);
            } else {
                error_info = "";
            }
            getMapComposer().showMessage(name + " failed" + error_info);
            this.detach();
            this.parent.detach();
        } else if (s.equals("CANCELLED")) {
            timer.stop();
            getMapComposer().showMessage(name + " cancelled by user");
            this.detach();
        }

    }

    JSONObject get() {
        try {
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.satServer).append("/ws/job?pid=").append(pid);

            logger.debug("checking status every '" + timer.getDelay() + "' sec: " + sbProcessUrl.toString());

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "application/json");

            client.getHttpConnectionManager().getParams().setSoTimeout(timer.getDelay());
            int result = client.executeMethod(get);

            if (result == 200) {
                return JSONObject.fromObject(get.getResponseBodyAsString());
            }
        } catch (SocketTimeoutException e) {
        } catch (Exception e) {
            logger.error("error getting updated job info pid=" + pid, e);
        }
        return null;
    }

    public void onClick$btnCancel(Event e) {
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod((CommonData.satServer + "/ws/jobs/cancel?pid=") + pid);

            get.addRequestHeader("Accept", "text/plain");

            client.getHttpConnectionManager().getParams().setSoTimeout(timer.getDelay());
            int result = client.executeMethod(get);
        } catch (Exception ex) {
            logger.error("error getting updated job info pid=" + pid, ex);
        }
        this.detach();
    }

    public void onClick$btnHide(Event e) {
        showReferenceNumber("Go to the Spatial Portal Dashboard for an update on the status.");
        this.detach();
    }

    void showReferenceNumber() {
        getMapComposer().showMessage("Reference number to retrieve results: " + pid);
    }

    void showReferenceNumber(String message) {
        getMapComposer().showMessage("Reference number to retrieve results: " + pid + "\n" + message);
    }

    private String reverseLines(String log) {
        String[] split = log.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = split.length - 1; i >= 0; i--) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(split[i]);
        }
        return sb.toString();
    }
}
