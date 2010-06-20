package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URL;
import java.net.URLEncoder;
import org.ala.spatial.util.SPLFilter;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Popup;
import org.zkoss.zul.ListModelArray;

/**
 *
 * @author adam
 */
public class FilteringResultsWCController extends UtilityComposer {

    private static final String SAT_URL = "sat_url";

    public Button download;
    public Button downloadsamples;
    public Listbox popup_listbox_results;
    public Popup popup_results;
    public Label results_label;

    public int results_pos;
    public String[] results = null;
    public String pid;
    String shape;
    private String satServer;
    private SettingsSupplementary settingsSupplementary = null;
    /**
     * for functions in popup box
     */
    SPLFilter popup_filter;
    Listcell popup_cell;
    Listitem popup_item;

    @Override
    public void afterCompose() {
        super.afterCompose();

        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(SAT_URL);
        }

        pid = (String) (Executions.getCurrent().getArg().get("pid"));
        shape = (String) (Executions.getCurrent().getArg().get("shape"));
        String manual = (String) (Executions.getCurrent().getArg().get("manual"));

        /* set nulls to none
         *
         * in case of a service update, do not set both to null or everything
         * will be returned.
         */
        if (pid == null) {
            pid = "none";
        } else if (shape == null) {
            shape = "none";
        }
        if (pid.equals("none") && shape.equals("none")) {
            return;
        }

        if (manual == null) {
            populateList();
        }
    }

    public void populateList() {

        try {
            System.out.println("resuts:" + results);
            if (results == null) {
                long t1 = System.currentTimeMillis();
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append("/filtering/apply");
                sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
                sbProcessUrl.append("/species/list");
                sbProcessUrl.append("/shape/" + URLEncoder.encode(shape, "UTF-8"));
                String out = getInfo(sbProcessUrl.toString());
                //remove trailing ','
                if (out.length() > 0 && out.charAt(out.length() - 1) == ',') {
                    out = out.substring(0, out.length() - 1);
                }
                results = out.split(",");
                long t2 = System.currentTimeMillis();

                if (results.length == 0) {
                    results_label.setValue("no species found");
                    return;
                }

                // results should already be sorted: Arrays.sort(results);
                int length = results.length;
                String[] tmp = results;
                if (results.length > 200) {
                    tmp = java.util.Arrays.copyOf(results, 200);
                }
                long t3 = System.currentTimeMillis();

                popup_listbox_results.setModel(new ListModelArray(tmp, false));
                if (length < 200) {
                    results_label.setValue("species found: " + length);
                } else {
                    results_label.setValue("species found: " + length + " (first 200 in this list)");
                }
                long t4 = System.currentTimeMillis();

                System.out.println("predisplay filtering result timings: sz=" + results.length + " timing: " + (t2 - t1) + " " + (t3 - t1) + " " + (t4 - t1));
            } else {
                int length = results.length;
                String[] tmp = results;

                popup_listbox_results.setModel(new ListModelArray(tmp, false));
                results_label.setValue("species found: " + length);
                System.out.println("using provided species list");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$download() {
        StringBuffer sb = new StringBuffer();
        for (String s : results) {
            sb.append(s);
            sb.append("\r\n");
        }
        Filedownload.save(sb.toString(), "text/plain", "filter.csv");
    }

    public void onClick$downloadsamples() {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/samples/list");

            sbProcessUrl.append("/shape/" + URLEncoder.encode(shape, "UTF-8"));
            String samplesfile = getInfo(sbProcessUrl.toString());

            URL u = new URL(satServer + "/alaspatial/" + samplesfile);
            Filedownload.save(u.openStream(), "application/zip", "filter_samples_" + pid + ".zip");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getInfo(String urlPart) {
        try {
            HttpClient client = new HttpClient();

            GetMethod get = new GetMethod(satServer + "/alaspatial/ws" + urlPart); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(get);

            //TODO: confirm result
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception ex) {
            //TODO: error message
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }
}
