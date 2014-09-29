package au.org.ala.spatial.logger;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;

import javax.servlet.http.HttpSession;
import java.net.URLEncoder;

/**
 * Helper class to send logging information to the logger-service
 *
 * @author ajay
 */
public class RemoteLogger {

    private static final Logger LOGGER = Logger.getLogger(RemoteLogger.class);

    private String loggerService = "";
    private String appid = "";

    private void init() {
        loggerService = CommonData.getSettings().getProperty("logging_url");
        appid = CommonData.getSettings().getProperty("app_id");
    }

    public void logMapSpecies(String name, String lsid, String area, String extra) {
        logMapSpecies(name, lsid, area, "Species", extra);
    }

    public void logMapSpecies(String name, String lsid, String area, String type, String extra) {
        sendToServer(type, name, lsid, area, "", extra, "mapped", "0", "");
    }

    public void logMapArea(String name, String type, String area) {
        logMapArea(name, type, area, "");
    }

    public void logMapArea(String name, String type, String area, String extra) {
        logMapArea(name, type, area, "", "");
    }

    public void logMapArea(String name, String type, String area, String layer, String extra) {
        sendToServer(type, name, "", area, layer, extra, "mapped", "0", "");
    }

    public void logMapAnalysis(String name, String type, String area, String species, String layers, String pid, String options, String status) {
        sendToServer(type, name, species, area, layers, options, status, "0", pid);
    }

    public void logMapAnalysisUpdateStatus(String pid, String status) {
        sendToServer(pid, status);
    }

    private int sendToServer(String type, String name, String lsid, String area, String layers, String extra, String status, String privacy, String pid) {
        try {
            if (StringUtils.isBlank(loggerService)) {
                init();
            }

            String sessionid = ((HttpSession) Sessions.getCurrent().getNativeSession()).getId();

            String userip = Executions.getCurrent().getHeader("x-forwarded-for");
            if (StringUtils.isBlank(userip)) {
                userip = Executions.getCurrent().getRemoteAddr();
                if (StringUtils.isBlank(userip)) {
                    userip = "";
                }
            }

            String useremail = Util.getUserEmail();

            LOGGER.debug("Sending log to: " + loggerService + "/log/action");
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(loggerService + "/log/action");
            post.addRequestHeader(StringConstants.ACCEPT, StringConstants.APPLICATION_JSON);

            String category1, category2 = "";
            String[] types = type.split("-");
            category1 = StringUtils.capitalize(types[0].trim());
            if (types.length > 1) {
                category2 = StringUtils.capitalize(types[1].trim());
            }

            String newLsid = lsid;
            if (StringUtils.isBlank(newLsid)) {
                newLsid = "";
            }
            String newPid = pid;
            if (StringUtils.isBlank(newPid)) {
                newPid = "";
            }
            String newArea = area;
            if (StringUtils.isBlank(newArea)) {
                newArea = "";
            }

            post.addParameter("email", useremail);
            post.addParameter("appid", appid);
            post.addParameter("userip", userip);
            post.addParameter("sessionid", sessionid);
            post.addParameter(StringConstants.TYPE, type);
            post.addParameter("category1", category1);
            post.addParameter("category2", category2);
            post.addParameter(StringConstants.NAME, name);
            post.addParameter("processid", newPid);
            post.addParameter("specieslsid", newLsid);
            post.addParameter("layers", layers);
            post.addParameter(StringConstants.STATUS, status);
            post.addParameter("privacy", privacy);
            post.addParameter(StringConstants.AREA, newArea);
            post.addParameter("extra", extra);

            LOGGER.debug("logging " + type + " action for user session " + sessionid + " for user " + useremail + " from " + userip);
            return client.executeMethod(post);

        } catch (Exception e) {
            LOGGER.error("Error sending logging information to server:", e);
        }

        return -1;
    }

    private int sendToServer(String pid, String status) {
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(loggerService + "/log/update/" + pid + "/" + status);
            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.APPLICATION_JSON);

            LOGGER.debug("logging status update on " + pid);
            return client.executeMethod(get);

        } catch (Exception e) {
            LOGGER.error("Error sending logging information to server:", e);
        }

        return -1;
    }

    public JSONObject getLogCSV() {
        init();

        try {
            if (Util.isLoggedIn()) {
                String url = loggerService + "/app/types/tool.json?"
                        + "email=" + URLEncoder.encode(Util.getUserEmail(), StringConstants.UTF_8)
                        + "&appid=" + URLEncoder.encode(appid, StringConstants.UTF_8)
                        + "&api_key=" + URLEncoder.encode(CommonData.getSettings().getProperty("api_key"), StringConstants.UTF_8);

                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(url);

                get.addRequestHeader(StringConstants.ACCEPT, StringConstants.APPLICATION_JSON);

                client.executeMethod(get);

                LOGGER.debug("get: " + url + ", response: " + get.getResponseBodyAsString());

                return JSONObject.fromObject(get.getResponseBodyAsString());
            }
        } catch (Exception e) {
            LOGGER.error("Error getting logging information from server:", e);
        }

        return null;
    }

    public JSONObject getLogItem(String logId) {
        init();

        try {

            if (Util.isLoggedIn()) {
                String url = loggerService + "/app/view/"
                        + logId + ".json"
                        + "?appid=" + URLEncoder.encode(appid, StringConstants.UTF_8)
                        + "&api_key=" + URLEncoder.encode(CommonData.getSettings().getProperty("api_key"), StringConstants.UTF_8);


                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(url);

                get.addRequestHeader(StringConstants.ACCEPT, StringConstants.APPLICATION_JSON);

                client.executeMethod(get);

                LOGGER.debug("get: " + url + ", response: " + get.getResponseBodyAsString());

                return JSONObject.fromObject(get.getResponseBodyAsString());
            }

        } catch (Exception e) {
            LOGGER.error("Error getting logging information from server:", e);
        }

        return null;
    }
}
