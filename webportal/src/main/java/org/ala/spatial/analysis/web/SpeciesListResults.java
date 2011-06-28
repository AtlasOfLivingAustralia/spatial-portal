package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListModelArray;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Row;

/**
 *
 * @author adam
 */
public class SpeciesListResults extends UtilityComposer {

    public String pid;
    String shape;
    public String[] results;
    public Button download;
    public Listbox popup_listbox_results;
    public Label results_label;
    Row rowUpdating;
    Row rowCounts;
    int results_count = 0;
    int results_count_occurrences = 0;

    public String wkt;

    @Override
    public void afterCompose() {
        super.afterCompose();

        wkt = (String) Executions.getCurrent().getArg().get("wkt");

        populateList();
    }
    boolean addedListener = false;

    public void populateList() {
        updateParameters();

        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/species/list");

            String out = postInfo(sbProcessUrl.toString());
            if (out.length() > 0 && out.charAt(out.length() - 1) == ',') {
                out = out.substring(0, out.length() - 1);
            }
            results = out.split("\\|");
            java.util.Arrays.sort(results);

            if (results.length == 0 || results[0].trim().length() == 0) {
                getMapComposer().showMessage("No species records in the active area.");
                results = null;
                popup_listbox_results.setVisible(false);
                results_label.setVisible(false);
                this.detach();
                return;
            }

            // results should already be sorted: Arrays.sort(results);
            String[] tmp = results;
            if (results.length > 200) {
                tmp = java.util.Arrays.copyOf(results, 200);
                results_label.setValue("preview of first 200 species found");
            } else {
                results_label.setValue("preview of all " + results.length + " species found");
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

                            if (ss.length > 3) {
                                lc = new Listcell(ss[3]);
                                lc.setParent(li);
                            }

                            if (ss.length > 4) {
                                lc = new Listcell(ss[4]);
                                lc.setParent(li);
                            }

                            if (ss.length > 5) {
                                lc = new Listcell(ss[5]);
                                lc.setParent(li);
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$btnDownload() {
        StringBuffer sb = new StringBuffer();
        sb.append("Family Name,Scientific Name,Common name/s,Taxon rank,Scientific Name LSID,Number of Occurrences\r\n");
        for (String s : results) {
            sb.append("\"");
            sb.append(s.replaceAll("\\*", "\",\""));
            sb.append("\"");
            sb.append("\r\n");
        }

        String spid = pid;
        if (spid == null || spid.equals("none")) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
        String sdate = date.format(new Date());

        Filedownload.save(sb.toString(), "text/plain", "Species_list_" + sdate + "_" + spid + ".csv");

        if(wkt == null) {
            wkt = getMapComposer().getViewArea();
        }
        getMapComposer().updateUserLogAnalysis("species list", wkt/*getMapComposer().getSelectionArea()*/, "", "Species_list_" + sdate + "_" + spid + ".csv", pid, "species list download");

        detach();
    }

    private String postInfo(String urlPart) {
        try {
            HttpClient client = new HttpClient();

            PostMethod get = new PostMethod(CommonData.satServer + "/alaspatial/ws" + urlPart); // testurl

            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addParameter("area", shape);

            System.out.println("satServer:" + CommonData.satServer + " ** postInfo:" + urlPart + " ** " + shape);

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

    boolean updateParameters() {
        //extract 'shape' and 'pid' from composer
        String area = wkt;//getMapComposer().getSelectionArea();
        if(area == null) {
            wkt = getMapComposer().getViewArea();
            area = wkt;
        }
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
}
