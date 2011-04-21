/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.analysis.web;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.ala.spatial.util.CommonData;
import org.zkoss.zul.Filedownload;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

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
        String area = getSelectedArea();

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

            onClick$btnDownload();

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

//        if(wkt == null) {
//            wkt = getMapComposer().getViewArea();
//        }
//        getMapComposer().updateUserLogAnalysis("species list", wkt, "", "Species_list_" + sdate + "_" + spid + ".csv", pid, "species list download");

        detach();
    }

    private String postInfo(String urlPart) {
        try {
            HttpClient client = new HttpClient();

            PostMethod get = new PostMethod(CommonData.satServer + "/alaspatial/ws" + urlPart); // testurl

            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addParameter("area", URLEncoder.encode(getSelectedArea(), "UTF-8"));

            System.out.println("satServer:" + CommonData.satServer + " ** postInfo:" + urlPart + " ** " + getSelectedArea());

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
