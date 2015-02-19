/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.progress;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.UtilityComposer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.*;

import java.net.SocketTimeoutException;

/**
 * @author ajay
 */
public class ProgressController extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(ProgressController.class);
    private String pid = null;
    private Window parent = null;
    private String name = "";
    private RemoteLogger remoteLogger;
    private Label jobstatus;
    private Progressmeter jobprogress;
    private Timer timer;
    private Textbox tbPid;
    private Textbox txtLog;
    private String log = "";
    private Button btnHide;

    @Override
    public void afterCompose() {
        super.afterCompose();
        timer.stop();
    }

    public void start(String pid, String name) {
        start(pid, name, true);
    }

    public void start(String pid, String name, boolean background) {
        this.pid = pid;
        tbPid.setValue(pid);
        this.name = name;
        ((Caption) getFellow(StringConstants.CTITLE)).setLabel(name);

        if (background) {
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

        if (jo.containsKey(StringConstants.STATUS)) {
            jobstatus.setValue(jo.get(StringConstants.STATUS).toString());
        }

        String s = jo.get(StringConstants.STATE).toString();
        if (StringConstants.JOB_DOES_NOT_EXIST.equals(s)) {
            timer.stop();
            getMapComposer().showMessage(name + " request does not exist", "");
            this.detach();
            return;
        }

        String p = jo.get(StringConstants.PROGRESS).toString();
        try {
            double d = Double.parseDouble(p);
            jobprogress.setValue((int) (d * 100));
        } catch (Exception ex) {
            LOGGER.error("failed to parse progress %: " + p);
        }

        String l = jo.get("log").toString();
        if (l != null) {
            this.log = reverseLines(l) + this.log;
            if (txtLog != null) {
                txtLog.setValue(this.log);
            }
        }

        remoteLogger.logMapAnalysisUpdateStatus(pid, s);

        if (StringConstants.SUCCESSFUL.equals(s)) {
            timer.stop();
            LOGGER.debug("JOB DONE. Calling loadMap");
            Events.echoEvent("loadMap", parent, null);
            this.detach();
        } else if (s.startsWith(StringConstants.FAILED)) {
            timer.stop();
            String errorInfo = jo.get("message").toString();
            if (!StringConstants.JOB_DOES_NOT_EXIST.equals(errorInfo)) {
                errorInfo = " with the following message: \n\n" + Util.breakString(errorInfo, 64);
            } else {
                errorInfo = "";
            }
            getMapComposer().showMessage(name + " failed" + errorInfo);
            this.detach();
            this.parent.detach();
        } else if (StringConstants.CANCELLED.equals(s)) {
            timer.stop();
            getMapComposer().showMessage(name + " cancelled by user");
            this.detach();
        }

    }

    JSONObject get() {
        try {
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.getSatServer()).append("/ws/job?pid=").append(pid);

            LOGGER.debug("checking status every '" + timer.getDelay() + "' sec: " + sbProcessUrl.toString());

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.APPLICATION_JSON);

            client.getHttpConnectionManager().getParams().setSoTimeout(timer.getDelay());
            int result = client.executeMethod(get);

            if (result == 200) {
                JSONParser jp = new JSONParser();
                return (JSONObject) jp.parse(get.getResponseBodyAsString());
            }
        } catch (SocketTimeoutException e) {
            LOGGER.debug("progress timeout exception, will be trying again.");
        } catch (Exception e) {
            LOGGER.error("error getting updated job info pid=" + pid, e);
        }
        return null;
    }

    public void onClick$btnCancel(Event e) {
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod((CommonData.getSatServer() + "/ws/jobs/cancel?pid=") + pid);

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            client.getHttpConnectionManager().getParams().setSoTimeout(timer.getDelay());
            client.executeMethod(get);
        } catch (Exception ex) {
            LOGGER.error("error getting updated job info pid=" + pid, ex);
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

    public void setParentWindow(Window parentWindow) {
        this.parent = parentWindow;
    }
}
