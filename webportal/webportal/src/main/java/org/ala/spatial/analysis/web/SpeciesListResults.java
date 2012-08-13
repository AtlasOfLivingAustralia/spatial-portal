package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.UtilityComposer;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.logger.client.RemoteLogger;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryUtil;
import org.ala.spatial.util.SelectedArea;
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

    RemoteLogger remoteLogger;
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
    SelectedArea selectedArea;
    boolean[] geospatialKosher;

    @Override
    public void afterCompose() {
        super.afterCompose();

        selectedArea = (SelectedArea) Executions.getCurrent().getArg().get("selectedarea");
        geospatialKosher = (boolean[]) Executions.getCurrent().getArg().get("geospatialKosher");

        populateList();
    }
    boolean addedListener = false;

    public void populateList() {
        if (selectedArea == null) {
            selectedArea = new SelectedArea(null, getMapComposer().getViewArea());
        }

        try {
            Query sq = QueryUtil.queryFromSelectedArea(null, selectedArea, false, geospatialKosher);

            if (sq.getSpeciesCount() <= 0) {
                getMapComposer().showMessage("No species records in the active area.");
                results = null;
                popup_listbox_results.setVisible(false);
                results_label.setVisible(false);
                this.detach();
                return;
            }

            //remove header
            String speciesList = sq.speciesList();
            results = speciesList.substring(speciesList.indexOf('\n') + 1).split("\n");

            java.util.Arrays.sort(results);

            // results should already be sorted: Arrays.sort(results);
            String[] tmp = results;
            if (results.length > 200) {
                tmp = java.util.Arrays.copyOf(results, 200);
                results_label.setValue("preview of first 200 of " + results.length + " species found");
            } else {
                results_label.setValue("preview of all " + results.length + " species found");
            }

            popup_listbox_results.setModel(new ListModelArray(tmp, false));
            popup_listbox_results.setItemRenderer(
                    new ListitemRenderer() {

                        public void render(Listitem li, Object data) {
                            String s = (String) data;
                            CSVReader reader = new CSVReader(new StringReader(s));

                            String[] ss = null;
                            try {
                                ss = reader.readNext();
                            } catch (Exception e) {
                                ss = new String[0];
                            }

                            if (ss == null || ss.length == 0) {
                                return;
                            }

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
                            try {
                                reader.close();
                            } catch (IOException ex) {
                                Logger.getLogger(SpeciesListResults.class.getName()).log(Level.SEVERE, null, ex);
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
            sb.append(s);
            sb.append("\r\n");
        }

        String spid = pid;
        if (spid == null || spid.equals("none")) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
        String sdate = date.format(new Date());

        Filedownload.save(sb.toString(), "text/plain", "Species_list_" + sdate + "_" + spid + ".csv");

        if (selectedArea == null) {
            selectedArea = new SelectedArea(null, getMapComposer().getViewArea());
        }
        getMapComposer().updateUserLogAnalysis("Species List", selectedArea.getWkt(), "", "Species_list_" + sdate + "_" + spid + ".csv", pid, "species list download");
        remoteLogger.logMapAnalysis("Species List", "Export - Species List", selectedArea.getWkt(), "", "", spid, "Species_list_" + sdate + "_" + spid + ".csv", "");

        detach();
    }
}
