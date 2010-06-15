package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import org.ala.spatial.util.SPLFilter;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.ListModelArray;

/**
 *
 * @author adam
 */
public class FilteringResultsWCController extends UtilityComposer {

    private static final String SAT_URL = "sat_url";
    public Textbox popup_results_seek;
    public Button download;
    public Button downloadsamples;
    public Listbox popup_listbox_results;
    public Popup popup_results;
    public Button results_prev;
    public Button results_next;
    public Label results_label;
    public int results_pos;
    public String[] results = null;
    public String pid;
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
        } else {
            //TODO: error message
        }

        pid = (String) (Executions.getCurrent().getArg().get("pid"));
  
        try {
            long t1 = System.currentTimeMillis();
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/species/list");
            sbProcessUrl.append("/shape/none");
            results = getInfo(sbProcessUrl.toString()).split("\r\n");
            long t2 = System.currentTimeMillis();

            // results should already be sorted: Arrays.sort(results);
            int length = results.length;
            if (results.length > 200) {
                String [] tmp = java.util.Arrays.copyOf(results, 200);
                results = tmp;
            }
            long t3 = System.currentTimeMillis();

            popup_listbox_results.setModel(new ListModelArray(results,false));
            if(length < 200){
                results_label.setValue("species found: " + length);
            } else {
                results_label.setValue("species found: " + length + " (first 200 in this list)");
            }
            long t4 = System.currentTimeMillis();

            System.out.println("predisplay filtering result timings: sz=" + results.length + " timing: " + (t2-t1) + " " + (t3-t1) + " " + (t4-t1));
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

            sbProcessUrl.append("/shape/none");
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
