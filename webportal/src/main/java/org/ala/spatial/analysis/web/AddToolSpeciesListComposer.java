/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import java.util.HashMap;
import org.ala.spatial.util.SelectedArea;
import org.zkoss.zk.ui.Executions;

/**
 *
 * @author ajay
 */
public class AddToolSpeciesListComposer extends AddToolComposer {

    String selectedLayers = "";
    int generation_count = 1;
    String layerLabel = "";
    String legendPath = "";
    private String[] results;
    String pid = "none";

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Species list";
        this.totalSteps = 1;

        this.loadAreaLayers();
        this.updateWindowTitle();
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
    }

    @Override
    public void onFinish() {
        try {
            onClick$btnDownload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$btnDownload() {
        SelectedArea sa = getSelectedArea();
        HashMap<String, Object> hm = new HashMap<String, Object>();
        hm.put("selectedarea", sa);
        SpeciesListResults window = (SpeciesListResults) Executions.createComponents("WEB-INF/zul/AnalysisSpeciesListResults.zul", getMapComposer(), hm);
        try {
            window.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        StringBuffer sb = new StringBuffer();
//        sb.append("Family Name,Scientific Name,Common name/s,Taxon rank,Scientific Name LSID,Number of Occurrences\r\n");
//        for (String s : results) {
//            sb.append("\"");
//            sb.append(s.replaceAll("\\*", "\",\""));
//            sb.append("\"");
//            sb.append("\r\n");
//        }
//
//        String spid = pid;
//        if (spid == null || spid.equals("none")) {
//            spid = String.valueOf(System.currentTimeMillis());
//        }
//
//        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
//        String sdate = date.format(new Date());
//
//        Filedownload.save(sb.toString(), "text/plain", "Species_list_" + sdate + "_" + spid + ".csv");

//        if(wkt == null) {
//            wkt = getMapComposer().getViewArea();
//        }
//        getMapComposer().updateUserLogAnalysis("species list", wkt, "", "Species_list_" + sdate + "_" + spid + ".csv", pid, "species list download");

        detach();
    }
}
