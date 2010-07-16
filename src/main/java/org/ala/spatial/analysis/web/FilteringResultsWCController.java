package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
import org.ala.spatial.util.SPLFilter;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Popup;
import org.zkoss.zul.ListModelArray;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Textbox;

/**
 *
 * @author adam
 */
public class FilteringResultsWCController extends UtilityComposer {

    private static final String SAT_URL = "sat_url";
    public Button download;
    public Button downloadsamples;
    public Button refreshButton2;
    public Listbox popup_listbox_results;
    public Label results_label;
    public int results_pos;
    public String[] results = null;
    public String pid;
    String shape;
    private String satServer;
    private SettingsSupplementary settingsSupplementary = null;
    int results_count = 0;

    @Override
    public void afterCompose() {
        super.afterCompose();

        // onClick$refreshButton2();
    }
    boolean addedListener = false;

    @Override
    public void redraw(Writer out) throws java.io.IOException {
        super.redraw(out);

        System.out.println("redraw:filteringresultswccontroller");
        if (!addedListener) {
            addedListener = true;
            //register for viewport changes
            EventListener el = new EventListener() {

                public void onEvent(Event event) throws Exception {
                    // refresh count may be required if area is
                    // not an envelope.
                    String area = getMapComposer().getSelectionArea();
                    if (!area.contains("ENVELOPE(")) {
                        refreshCount();
                    }
                }
            };
            getMapComposer().getLeftmenuSearchComposer().addViewportEventListener("filteringResults", el);

        }
    }

    public void populateList() {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/species/list");

            String out = postInfo(sbProcessUrl.toString());
            //remove trailing ','
            if (out.length() > 0 && out.charAt(out.length() - 1) == ',') {
                out = out.substring(0, out.length() - 1);
            }
            results = out.split(",");
            java.util.Arrays.sort(results);

            if (results.length == 0 || results[0].trim().length() == 0) {
                results_label.setValue("no species in area");
                results = null;
                popup_listbox_results.setVisible(false);
                refreshButton2.setVisible(true);
                return;
            }

            // results should already be sorted: Arrays.sort(results);
            int length = results.length;
            String[] tmp = results;
            if (results.length > 200) {
                tmp = java.util.Arrays.copyOf(results, 200);
            }

            popup_listbox_results.setModel(new ListModelArray(tmp, false));
            popup_listbox_results.setItemRenderer(
                    new ListitemRenderer() {

                        public void render(Listitem li, Object data) {
                            String s = (String) data;
                            String[] ss = s.split("[*]");


                            Listcell lc = new Listcell(ss[0]);
                            lc.setParent(li);

                            if (ss.length > 1) {
                                lc = new Listcell(ss[1]);
                                lc.setParent(li);
                            }

                            if (ss.length > 2) {
                                lc = new Listcell(ss[2]);
                                lc.setParent(li);
                            }
                        }
                    });
            if (length < 200) {
                results_label.setValue("species in active area: " + length);
            } else {
                results_label.setValue("species in active area: " + length + " (first 200 listed)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean isTabOpen() {
        try {
            Tabbox analysisOptsTabbox =
                    (Tabbox) getParent() //htmlmacrocomponent
                    .getParent() //tabpanel
                    .getParent() //tabpanels
                    .getParent(); //tabbox

            if (analysisOptsTabbox.getSelectedTab().getIndex() == 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public void refreshCount() {
        //check if tab is open
        
        if (!isTabOpen() || !updateParameters()) {
            return;
        }


        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/species/count");

            String out = postInfo(sbProcessUrl.toString());
            results_count = Integer.parseInt(out);
            if (results_count == 0) {
                results_label.setValue("no species in active area");
                results = null;
                popup_listbox_results.setVisible(false);
                refreshButton2.setVisible(true);
                return;
            }

            results_label.setValue("species in active area: " + results_count);

            //hide results list, show 'preview list' button
            popup_listbox_results.setVisible(false);
            refreshButton2.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$download() {
        //update 'results'
        if (updateParameters()
                || !popup_listbox_results.isVisible()) {
            populateList();
        }

        StringBuffer sb = new StringBuffer();
        sb.append("family name,species name,common name\r\n");
        for (String s : results) {
            sb.append(s.replace('*', ','));
            sb.append("\r\n");
        }
        Filedownload.save(sb.toString(), "text/plain", "filter.csv");
    }

    public void onClick$downloadsamples() {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(SAT_URL);
        }

        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/samples/list");

            String samplesfile = postInfo(sbProcessUrl.toString());

            URL u = new URL(satServer + "/alaspatial/" + samplesfile);
            Filedownload.save(u.openStream(), "application/zip", "filter_samples_" + pid + ".zip");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getInfo(String urlPart) {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(SAT_URL);
        }

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

    private String postInfo(String urlPart) {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(SAT_URL);
        }
        try {
            HttpClient client = new HttpClient();

            PostMethod get = new PostMethod(satServer + "/alaspatial/ws" + urlPart); // testurl

            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addParameter("area", URLEncoder.encode(shape, "UTF-8"));

            System.out.println("satServer:" + satServer + " ** postInfo:" + urlPart + " ** " + shape);

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

    public void onClick$refreshButton2() {
        if (!isTabOpen()) {
            return;
        }
        if (updateParameters()
                || !popup_listbox_results.isVisible()) {
            populateList();

            //show update list if count > 0
            refreshButton2.setVisible(false);
            popup_listbox_results.setVisible(true);
        }
    }

    boolean updateParameters() {
        //extract 'shape' and 'pid' from composer
        String area = getMapComposer().getSelectionArea();

        if (area.contains("ENVELOPE(")) {
            shape = "none";
            pid = area.substring(9, area.length() - 1);
            return true;
        } else {
            pid = "none";
            if (shape != area) {
                shape = area;
                return true;
            } else {
                return false;
            }
        }
    }

    static public void open() {
        FilteringResultsWCController win = (FilteringResultsWCController) Executions.createComponents(
                "/WEB-INF/zul/AnalysisFilteringResults.zul", null, null);
        try {
            win.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void refreshCount(int newCount) {
        results_count = newCount;
        if (results_count == 0) {
            results_label.setValue("no species in active area");
            results = null;
            popup_listbox_results.setVisible(false);
            refreshButton2.setVisible(true);
        }

        results_label.setValue("species in active area: " + results_count);

        //hide results list, show 'preview list' button
        popup_listbox_results.setVisible(false);
        refreshButton2.setVisible(true);
    }
}
