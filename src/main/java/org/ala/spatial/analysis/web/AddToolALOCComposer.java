/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.analysis.web;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.wms.WMSStyle;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Messagebox;

/**
 *
 * @author ajay
 */
public class AddToolALOCComposer extends AddToolComposer {

    private Intbox groupCount;
    String selectedLayers = "";
    int generation_count = 1;
    String layerLabel = ""; 
    String legendPath = "";

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Classification";
        this.totalSteps = 4;

        this.loadAreaLayers();
        this.loadGridLayers(true);
        this.updateWindowTitle();
        //this.updateName("Classification #" + generation_count + " - " + groupCount.getValue() + " groups");
        //this.updateName("My Classification #" + generation_count);
        this.updateName(getMapComposer().getNextAreaLayerName("My Classification"));
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
        //this.updateName("My Classification #" + generation_count + " - " + groupCount.getValue() + " groups");
        this.updateName(getMapComposer().getNextAreaLayerName("My Classification"));
    }

    @Override
    public void onFinish() {
        //super.onFinish();

        runclassification();
        lbListLayers.clearSelection();
    }

    @Override
    public void loadMap(Event event) {
        String uri = CommonData.satServer + "/alaspatial/output/layers/" + pid + "/img.png";
        float opacity = Float.parseFloat("0.75");

        List<Double> bbox = new ArrayList<Double>();

        double[] d = getExtents();
        bbox.add(d[2]);
        bbox.add(d[3]);
        bbox.add(d[4]);
        bbox.add(d[5]);

        //get job inputs
        try {
            for (String s : getJob("inputs").split(";")) {
                if (s.startsWith("gc")) {
                    //layerLabel = "Classification #" + generation_count + " - " + s.split(":")[1] + " groups";
                    layerLabel = tToolName.getValue();
                    generation_count++;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (layerLabel == null) {
            layerLabel = "My Classification " + generation_count;
            generation_count++;
        }
        legendPath = "";
        try {
            legendPath = "/WEB-INF/zul/AnalysisClassificationLegend.zul?pid=" + pid + "&layer=" + URLEncoder.encode(layerLabel, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        getMapComposer().addImageLayer(pid, layerLabel, uri, opacity, bbox, LayerUtilities.ALOC);
        MapLayer mapLayer = getMapComposer().getMapLayer(layerLabel);
        if (mapLayer != null) {
            WMSStyle style = new WMSStyle();
            style.setName("Default");
            style.setDescription("Default style");
            style.setTitle("Default");
            style.setLegendUri(legendPath);

            System.out.println("legend:" + legendPath);
            mapLayer.addStyle(style);
            mapLayer.setSelectedStyleIndex(1);

            MapLayerMetadata md = mapLayer.getMapLayerMetadata();
            if (md == null) {
                md = new MapLayerMetadata();
            }

            String infoUrl = CommonData.satServer + "/alaspatial/output/layers/" + pid + "/metadata.html" + "\nClassification output\npid:"+pid;
            md.setMoreInfo(infoUrl);
            md.setId(Long.valueOf(pid));

        try {
            // set off the download as well
            String fileUrl = CommonData.satServer + "/alaspatial/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip",tToolName.getValue().replaceAll(" ", "_")+".zip"); // "ALA_Prediction_"+pid+".zip"
        } catch (Exception ex) {
            System.out.println("Error generating download for classification model:");
            ex.printStackTrace(System.out);
        }


            //Events.echoEvent("openUrl", this.getMapComposer(), infoUrl);
        }
        
        this.detach();
    }

    double[] getExtents() {
        double[] d = new double[6];
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/alaspatial/output/aloc/" + pid + "/aloc.pngextents.txt");

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("getExtents:" + slist);

            String[] s = slist.split("\n");
            for (int i = 0; i < 6 && i < s.length; i++) {
                d[i] = Double.parseDouble(s[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return d;
    }

    String getJob(String type) {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/alaspatial/ws/jobs/").append(type).append("?pid=").append(pid);

            System.out.println(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println(slist);
            return slist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void runclassification() {
        try {

            //layerLabel = "Classification #" + generation_count + " - " + groupCount.getValue() + " groups";
            layerLabel = tToolName.getValue();

            String sbenvsel = getSelectedLayers();
            if (sbenvsel.split(":").length > 50) {
                Messagebox.show(sbenvsel.split(":").length + " layers selected.  Please select fewer than 50 environmental layers in step 1.", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                return;
            }

            if (groupCount.getValue() <= 1 || groupCount.getValue() > 200) {
                Messagebox.show("Please enter the number of groups to generate (2 to 200) in step 2.", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                //highlight step 2
//                tabboxclassification.setSelectedIndex(1);
                return;
            }

            String area = getSelectedArea();//getMapComposer().getSelectionArea();

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/alaspatial/ws/aloc/processgeoq?");
            sbProcessUrl.append("gc=" + URLEncoder.encode(String.valueOf(groupCount.getValue()), "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));

            HttpClient client = new HttpClient();
            //GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            PostMethod get = new PostMethod(sbProcessUrl.toString());
            get.addParameter("area", area);
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            generation_count++;
            pid = slist;

            legendPath = "/WEB-INF/zul/AnalysisClassificationLegend.zul?pid=" + pid + "&layer=" + URLEncoder.encode(layerLabel, "UTF-8");

            getMapComposer().updateUserLogAnalysis("Classification", "gc: " + groupCount.getValue() + ";area: " + area, sbenvsel.toString(), slist, pid, layerLabel);

            ALOCProgressWCController window = (ALOCProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisALOCProgress.zul", this, null);
            window.parent = this;
            window.start(pid);
            window.doModal();

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }

}
