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
import org.ala.spatial.sampling.SimpleRegion;
import org.ala.spatial.sampling.SimpleShapeFile;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.SelectedArea;
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

        this.setIncludeAnalysisLayersForAnyQuery(true);

        this.loadAreaLayers();
        this.loadGridLayers(true, false, true);
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
    public boolean onFinish() {
        return runclassification();
    }

    @Override
    void fixFocus() {
        switch (currentStep) {
            case 1:
                rgArea.setFocus(true);
                break;
            case 2:
                lbListLayers.setFocus(true);
                break;
            case 3:
                groupCount.setFocus(true);
                break;
            case 4:
                tToolName.setFocus(true);
                break;
        }
    }

    @Override
    public void loadMap(Event event) {
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

        String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:aloc_" + pid + "&FORMAT=image%2Fpng";
        String legendurl = CommonData.geoServer
                + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                + "&LAYER=ALA:aloc_" + pid;
        System.out.println(legendurl);
        legendPath = legendurl;
        getMapComposer().addWMSLayer(pid, layerLabel, mapurl, (float) 0.5, null, legendurl, LayerUtilities.ALOC, null, null);
        
        MapLayer mapLayer = getMapComposer().getMapLayer(pid);
        if (mapLayer != null) {
            mapLayer.setData("pid", pid);
            WMSStyle style = new WMSStyle();
            style.setName("Default");
            style.setDescription("Default style");
            style.setTitle("Default");
            style.setLegendUri(legendurl);

            System.out.println("legend:" + legendPath);
            mapLayer.addStyle(style);
            mapLayer.setSelectedStyleIndex(1);

            MapLayerMetadata md = mapLayer.getMapLayerMetadata();
            if (md == null) {
                md = new MapLayerMetadata();
            }

            String infoUrl = CommonData.satServer + "/output/aloc/" + pid + "/classification.html" + "\nClassification output\npid:" + pid;
            md.setMoreInfo(infoUrl);
            md.setId(Long.valueOf(pid));

            getMapComposer().updateLayerControls();

            try {
                // set off the download as well
                String fileUrl = CommonData.satServer + "/ws/download/" + pid;
                Filedownload.save(new URL(fileUrl).openStream(), "application/zip", tToolName.getValue().replaceAll(" ", "_") + ".zip"); // "ALA_Prediction_"+pid+".zip"
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
            sbProcessUrl.append(CommonData.satServer + "/output/aloc/" + pid + "/aloc.pngextents.txt");

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
            sbProcessUrl.append(CommonData.satServer + "/ws/jobs/").append(type).append("?pid=").append(pid);

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

    public boolean runclassification() {
        try {

            //layerLabel = "Classification #" + generation_count + " - " + groupCount.getValue() + " groups";
            layerLabel = tToolName.getValue();

            String sbenvsel = getSelectedLayers();
            if (sbenvsel.split(":").length > 50) {
                getMapComposer().showMessage(sbenvsel.split(":").length + " layers selected.  Please select fewer than 50 environmental layers in step 1.");
                return false;
            }

            if (groupCount.getValue() <= 1 || groupCount.getValue() > 200) {
                getMapComposer().showMessage("Please enter the number of groups to generate (2 to 200) in step 2.");
                //highlight step 2
//                tabboxclassification.setSelectedIndex(1);
               return false;
            }

            SelectedArea sa = getSelectedArea();//getMapComposer().getSelectionArea();

            //estimate analysis size in bytes
            SimpleRegion sr = SimpleShapeFile.parseWKT(sa.getWkt());
            double [][] bbox;
            if(sr != null) {
                bbox = sr.getBoundingBox();
            } else {
                bbox = new double[][] {{-180,-90},{180,90}};
            }
            //analysis currently restricted to australian extents and 0.01 degree grid
            if(bbox[0][0] < 112) bbox[0][0] = 112;
            if(bbox[1][0] > 155) bbox[1][0] = 155;
            if(bbox[0][1] < -44) bbox[0][1] = -44;
            if(bbox[1][1] > -9) bbox[1][1] = -9;

            long cellsInBBox = (long)((bbox[1][0] - bbox[0][0]) / 0.01 * (bbox[1][1] - bbox[0][1]) / 0.01);
            long size = (groupCount.getValue() + sbenvsel.split(":").length + 2) * cellsInBBox * 4;
            System.out.println("ALOC estimate size in MB, cells=" + cellsInBBox
                    + ", bbox=" + bbox[0][0] + "," + bbox[0][1] + "," + bbox[1][0] + "," + bbox[1][1]
                    + ", groups=" + groupCount.getValue()
                    + ", layers=" + sbenvsel.split(":").length
                    + ", size=" + size/1024/1024
                    + ", max size=" + settingsSupplementary.getValueAsInt("aloc_size_limit_in_mb"));
            if(size /1024/1024 > settingsSupplementary.getValueAsInt("aloc_size_limit_in_mb")) {
                getMapComposer().showMessage("Analysis is too large.  Reduce the number of groups, number of layers or area.", this);
                return false;
            }

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/aloc?");
            sbProcessUrl.append("gc=" + URLEncoder.encode(String.valueOf(groupCount.getValue()), "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));

            HttpClient client = new HttpClient();
            //GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            PostMethod get = new PostMethod(sbProcessUrl.toString());
            String area;
            if (sa.getMapLayer() != null && sa.getMapLayer().getData("envelope") != null) {
                area = "ENVELOPE(" + (String) sa.getMapLayer().getData("envelope") + ")";
            } else {
                area = sa.getWkt();
            }
            get.addParameter("area", area);
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            generation_count++;
            pid = slist;

            legendPath = "/WEB-INF/zul/AnalysisClassificationLegend.zul?pid=" + pid + "&layer=" + URLEncoder.encode(layerLabel, "UTF-8");

            getMapComposer().updateUserLogAnalysis("Classification", "gc: " + groupCount.getValue() + ";area: " + area, sbenvsel.toString(), slist, pid, layerLabel);
            
            try {
                remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Classification", area, "", sbenvsel.toString(), pid, "gc: " + groupCount.getValue(), "STARTED");
            } catch (Exception e) {
                e.printStackTrace();
            }

            ALOCProgressWCController window = (ALOCProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisALOCProgress.zul", null, null);
            window.parent = this;
            window.start(pid);
            window.doModal();

            this.setVisible(false);

            return true;

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
             getMapComposer().showMessage("Unknown error.", this);
        }

        return false;
    }
}
