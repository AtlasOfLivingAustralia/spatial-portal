package org.ala.logger.client;

import au.org.emii.portal.settings.SettingsSupplementary;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;

/**
 *
 * @author ajay
 */
public class RemoteLogger {

    Logger logger = Logger.getLogger(this.getClass());

    // http://localhost:8080/logger-service/log/action?appid=abc123&email=guest@ala.org.au&type=test&name=hello2&layers=a:b:c&status=started
    SettingsSupplementary settingsSupplementary;
    String logger_service = "";
    String appid = "";
    String userip = "";

//    public RemoteLogger() {
//        logger_service = settingsSupplementary.getValue("logging_url");
//
//        appid = settingsSupplementary.getValue("app_id");
//
//        userip = Executions.getCurrent().getHeader("x-forwarded-for");
//        if (StringUtils.isBlank(userip)) {
//            userip = "";
//        }
//    }

    private void init() {
        logger_service = settingsSupplementary.getValue("logging_url");

        appid = settingsSupplementary.getValue("app_id");

        userip = Executions.getCurrent().getHeader("x-forwarded-for");
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        System.out.println("exec.userip: " + userip);
        if (StringUtils.isBlank(userip)) {
            String session_userip = (String)Sessions.getCurrent().getAttribute("userip");
            System.out.println("sessions.userip: " + userip);
            if (StringUtils.isBlank(userip)) {
                userip = "";
            }
        }
        System.out.println("final.userip: " + userip);
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");

    }

    public SettingsSupplementary getSettingsSupplementary() {
        return settingsSupplementary;
    }

    @Required
    public void setSettingsSupplementary(SettingsSupplementary settingsSupplementary) {
        this.settingsSupplementary = settingsSupplementary;
    }

    public void logMapSpecies(String name, String lsid, String area, String extra) {
        logMapSpecies(name, lsid, area, "species", extra);
    }

    public void logMapSpecies(String name, String lsid, String area, String type, String extra) {
//        System.out.println("*************************************************");
//        System.out.println("logging species mapping ");
//        System.out.println(type + " - " + name + " (" + lsid + ") in " + area);
//        System.out.println("*************************************************");

        logger.info("sending log to server");
        int ret = sendToServer(type, name, lsid, area, "", extra, "mapped", "0", "");
        System.out.println("((((((((((((((((( " + ret + " )))))))))))))))))");

    }

    public void logMapArea(String name, String type, String area) {
        logMapArea(name, type, area, "");
    }
    public void logMapArea(String name, String type, String area, String extra) {
        logMapArea(name, type, area, "", "");
    }
    public void logMapArea(String name, String type, String area, String layer, String extra) {
        type = "area - " + type;
//        System.out.println("*************************************************");
//        System.out.println("logging area mapping ");
//        System.out.println(type + " - " + name + " as " + area);
//        System.out.println("*************************************************");

        sendToServer(type, name, "", area, layer, extra, "mapped", "0", "");
    }

    public void logMapAnalysis(String name, String type, String area, String species, String layers, String pid, String options, String status) {
//        System.out.println("*************************************************");
//        System.out.println("logging analysis ");
//        System.out.println(type + " - " + name + " for " + species + " in " + area + " with " + status + " -- " + pid);
//        System.out.println("*************************************************");

        sendToServer(type, name, species, area, layers, options, status, "0", pid);
    }

    public void logMapAnalysisUpdateStatus(String pid, String status) {
//        System.out.println("*************************************************");
//        System.out.println("logging analysis.status update ");
//        System.out.println(pid + " - " + status);
//        System.out.println("*************************************************");
    }

    private int sendToServer(String type, String name, String lsid, String area, String layers, String extra, String status, String privacy, String pid) {
        try {
//            StringBuffer sbProcessUrl = new StringBuffer();
//            sbProcessUrl.append(logger_service);
//            sbProcessUrl.append("/log/action?").append("?");
//            sbProcessUrl.append("email=guest@ala.org.au").append("&");
//            sbProcessUrl.append("appid=").append(appid).append("&");
//            sbProcessUrl.append("userip=").append(userip).append("&");
//            sbProcessUrl.append("type=").append(type).append("&");
//            sbProcessUrl.append("name=").append(name).append("&");
//            sbProcessUrl.append("specieslsid=").append(lsid).append("&");
//            //sbProcessUrl.append("area=").append(area).append("&");
//            sbProcessUrl.append("status=mapped").append("&");
//            sbProcessUrl.append("privacy=0");

            if (StringUtils.isBlank(logger_service)) {
                init(); 
            }
            
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(logger_service + "/log/action");
            post.addRequestHeader("Accept", "application/json");

            post.addParameter("email", "guest@ala.org.au");
            post.addParameter("appid", appid);
            post.addParameter("userip", userip);
            post.addParameter("type", type);
            post.addParameter("name", name);
            post.addParameter("processid", pid);
            post.addParameter("specieslsid", lsid);
            post.addParameter("layers", layers);
            post.addParameter("status", status);
            post.addParameter("privacy", privacy);
            post.addParameter("area", area);

//            System.out.println("*************************************************");
//            System.out.println("logging action to " + logger_service);
//            System.out.println(type + " - " + name + " (" + lsid + ") in " + area);
//            System.out.println("*************************************************");

            return client.executeMethod(post);

        } catch (Exception e) {
            System.out.println("Error sending logging information to server:");
            e.printStackTrace(System.out);
        }

        return -1;
    }

    private int sendToServer(String pid, String status) {
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(logger_service + "/log/update/"+pid+"/"+status);
            get.addRequestHeader("Accept", "application/json");

            return client.executeMethod(get);

        } catch (Exception e) {
            System.out.println("Error sending logging information to server:");
            e.printStackTrace(System.out);
        }

        return -1;
    }
}
