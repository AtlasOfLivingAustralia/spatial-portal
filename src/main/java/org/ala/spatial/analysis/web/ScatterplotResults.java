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
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListModelArray;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Row;

/**
 *
 * @author adam
 */
public class ScatterplotResults extends UtilityComposer {

    public String pid;
    String shape;
    public String[] results;
    public Button download;
    public Listbox popup_listbox_results;
    public Label results_label;
    private String satServer;
    private SettingsSupplementary settingsSupplementary = null;
    Row rowUpdating;
    Row rowCounts;
    int results_count = 0;
    int results_count_occurrences = 0;
    Listheader header1;
    Listheader header2;
    String data;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }
    boolean addedListener = false;

    public void populateList(String label1, String label2, String data) {
        this.data = data;
        header1.setLabel(label1);
        header2.setLabel(label2);

        results = data.split("\n");

        try {
            String [] tmp = results;
            if (results.length > 200) {
                tmp = java.util.Arrays.copyOf(results, 200);
                results_label.setValue("preview of first 200 scatterplot values");
            } else {
                results_label.setValue("preview of all " + results.length + " scatterplot values");
            }

            popup_listbox_results.setModel(new ListModelArray(tmp, false));
            popup_listbox_results.setItemRenderer(
                    new ListitemRenderer() {

                        public void render(Listitem li, Object data) {
                            String s = (String) data;
                            String[] ss = s.split(",");

                            Listcell lc;

                            for(int i=0;i<ss.length;i++) {
                                lc = new Listcell(ss[i]);
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
        //sb.append(header1.getLabel() + "," + header2.getLabel()).append("\r\n");
        sb.append(data.replace("\n", "\r\n"));

        String spid = pid;
        if (spid == null || spid.equals("none")) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
        String sdate = date.format(new Date());

        Filedownload.save(sb.toString(), "text/plain", "Scatterplot_data_" + sdate + "_" + spid + ".csv");

        //getMapComposer().updateUserLogAnalysis("scatterplot", getMapComposer().getSelectionArea(), "", "Scatterplot_data_" + sdate + "_" + spid + ".csv", pid, "scatterplot download");

        detach();
    }
}
