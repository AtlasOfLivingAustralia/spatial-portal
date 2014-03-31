/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.composer.progress.ProgressController;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.SelectedArea;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.wms.WMSStyle;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;

import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * @author ajay
 */
public class ALOCComposer extends ToolComposer {
    private static Logger logger = Logger.getLogger(ALOCComposer.class);
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
        if (!hasEstimated && !isUserLoggedIn()) {
            checkEstimate();
            return false;
        }

        return runclassification();
    }

    @Override
    public long getEstimate() {
        try {
            String sbenvsel = getSelectedLayers();
            if (sbenvsel.split(":").length > 50) {
                getMapComposer().showMessage(sbenvsel.split(":").length + " layers selected.  Please select fewer than 50 environmental layers in step 1.");
                return -1;
            }

            if (groupCount.getValue() <= 1 || groupCount.getValue() > 200) {
                getMapComposer().showMessage("Please enter the number of groups to generate (2 to 200) in step 2.");
                //highlight step 2
//                tabboxclassification.setSelectedIndex(1);
                return -1;
            }

            SelectedArea sa = getSelectedArea();//getMapComposer().getSelectionArea();

            //estimate analysis size in bytes
            double[][] bbox = null;
            if (sa.getMapLayer() != null) {
                List<Double> bb = sa.getMapLayer().getMapLayerMetadata().getBbox();
                bbox = new double[][]{{bb.get(0), bb.get(1)}, {bb.get(2), bb.get(3)}};
            } else {
                SimpleRegion sr = SimpleShapeFile.parseWKT(sa.getWkt());
                if (sr != null) {
                    bbox = sr.getBoundingBox();
                }
            }
            if (bbox == null) {
                bbox = new double[][]{{-180, -90}, {180, 90}};
            }

            long cellsInBBox = (long) ((bbox[1][0] - bbox[0][0]) / 0.01 * (bbox[1][1] - bbox[0][1]) / 0.01);
            long size = (groupCount.getValue() + sbenvsel.split(":").length + 2) * cellsInBBox * 4;
            logger.debug("ALOC estimate size in MB, cells=" + cellsInBBox
                    + ", bbox=" + bbox[0][0] + "," + bbox[0][1] + "," + bbox[1][0] + "," + bbox[1][1]
                    + ", groups=" + groupCount.getValue()
                    + ", layers=" + sbenvsel.split(":").length
                    + ", size=" + size / 1024 / 1024
                    + ", max size=" + settingsSupplementary.getValueAsInt("aloc_size_limit_in_mb"));
            if (size / 1024 / 1024 > settingsSupplementary.getValueAsInt("aloc_size_limit_in_mb")) {
                getMapComposer().showMessage("Analysis is too large.  Reduce the number of groups, number of layers or area.", this);
                return -1;
            }

            HttpClient client = new HttpClient();
            //GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            PostMethod get = new PostMethod((CommonData.satServer + "/ws/aloc/estimate?") + "gc=" + URLEncoder.encode(String.valueOf(groupCount.getValue()), "UTF-8") + "&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
            String area;
            if (sa.getMapLayer() != null && sa.getMapLayer().getEnvelope() != null) {
                area = "ENVELOPE(" + sa.getMapLayer().getEnvelope() + ")";
            } else {
                area = sa.getWkt();
            }
            get.addParameter("area", area);
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String estimate = get.getResponseBodyAsString();

            return Long.valueOf(estimate);

        } catch (Exception e) {
            logger.error("Unable to get estimates", e);
        }

        return -1;
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
            for (String s : getJob().split(";")) {
                if (s.startsWith("gc")) {
                    //layerLabel = "Classification #" + generation_count + " - " + s.split(":")[1] + " groups";
                    layerLabel = tToolName.getValue();
                    generation_count++;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("error getting ALOC job info", e);
        }

        if (layerLabel == null) {
            layerLabel = "My Classification " + generation_count;
            generation_count++;
        }

        String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:aloc_" + pid + "&FORMAT=image%2Fpng";
        String legendurl = CommonData.geoServer
                + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                + "&LAYER=ALA:aloc_" + pid;
        logger.debug(legendurl);
        legendPath = legendurl;
        getMapComposer().addWMSLayer("aloc_" + pid, layerLabel, mapurl, (float) 0.5, null, legendurl, LayerUtilities.ALOC, null, null);
        MapLayer mapLayer = getMapComposer().getMapLayer("aloc_" + pid);
        if (mapLayer != null) {
            mapLayer.setPid(pid);
            WMSStyle style = new WMSStyle();
            style.setName("Default");
            style.setDescription("Default style");
            style.setTitle("Default");
            style.setLegendUri(legendurl);

            logger.debug("legend:" + legendPath);
            mapLayer.addStyle(style);
            mapLayer.setSelectedStyleIndex(1);

            MapLayerMetadata md = mapLayer.getMapLayerMetadata();

            String infoUrl = CommonData.satServer + "/output/aloc/" + pid + "/classification.html" + "\nClassification output\npid:" + pid;
            md.setMoreInfo(infoUrl);
            md.setId(Long.valueOf(pid));

            getMapComposer().updateLayerControls();

            try {
                // set off the download as well
                String fileUrl = CommonData.satServer + "/ws/download/" + pid;
                Filedownload.save(new URL(fileUrl).openStream(), "application/zip", tToolName.getValue().replaceAll(" ", "_") + ".zip"); // "ALA_Prediction_"+pid+".zip"
            } catch (Exception ex) {
                logger.error("Error generating download for classification model:", ex);
            }

            //Events.echoEvent("openUrl", this.getMapComposer(), infoUrl);

            //perform intersection on user uploaded layers so you can facet on this layer
        }

        this.detach();
    }

    String getJob() {
        try {
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.satServer + "/ws/jobs/").append("inputs").append("?pid=").append(pid);

            logger.debug(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            logger.debug(slist);
            return slist;
        } catch (Exception e) {
            logger.error("error getting gob info pid=" + pid, e);
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
            double[][] bbox = null;
            //NQ: 20131219 Added extra check because BB is not supplied in all situations with the metadata (ie WKT has been drawn etc) http://code.google.com/p/ala/issues/detail?id=475
            if (sa.getMapLayer() != null && sa.getMapLayer().getMapLayerMetadata().getBbox() != null) {
                List<Double> bb = sa.getMapLayer().getMapLayerMetadata().getBbox();
                bbox = new double[][]{{bb.get(0), bb.get(1)}, {bb.get(2), bb.get(3)}};

            } else {
                SimpleRegion sr = SimpleShapeFile.parseWKT(sa.getWkt());
                if (sr != null) {
                    bbox = sr.getBoundingBox();
                }
            }
            if (bbox == null) {
                bbox = new double[][]{{-180, -90}, {180, 90}};
            }

            long cellsInBBox = (long) ((bbox[1][0] - bbox[0][0]) / 0.01 * (bbox[1][1] - bbox[0][1]) / 0.01);
            long size = (groupCount.getValue() + sbenvsel.split(":").length + 2) * cellsInBBox * 4;
            logger.debug("ALOC estimate size in MB, cells=" + cellsInBBox
                    + ", bbox=" + bbox[0][0] + "," + bbox[0][1] + "," + bbox[1][0] + "," + bbox[1][1]
                    + ", groups=" + groupCount.getValue()
                    + ", layers=" + sbenvsel.split(":").length
                    + ", size=" + size / 1024 / 1024
                    + ", max size=" + settingsSupplementary.getValueAsInt("aloc_size_limit_in_mb"));
            if (size / 1024 / 1024 > settingsSupplementary.getValueAsInt("aloc_size_limit_in_mb")) {
                getMapComposer().showMessage("Analysis is too large.  Reduce the number of groups, number of layers or area.", this);
                return false;
            }

            HttpClient client = new HttpClient();
            //GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            PostMethod get = new PostMethod((CommonData.satServer + "/ws/aloc?") + "gc=" + URLEncoder.encode(String.valueOf(groupCount.getValue()), "UTF-8") + "&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
            String area;
            if (sa.getMapLayer() != null && sa.getMapLayer().getEnvelope() != null) {
                area = "ENVELOPE(" + (String) sa.getMapLayer().getEnvelope() + ")";
            } else {
                area = sa.getWkt();
            }
            get.addParameter("area", area);
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            generation_count++;
            pid = slist;

            legendPath = "/WEB-INF/zul/legend/LayerLegendClassification.zul?pid=" + pid + "&layer=" + URLEncoder.encode(layerLabel, "UTF-8");

            try {
                remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Classification", area, "", sbenvsel.toString(), pid, "gc: " + groupCount.getValue(), "STARTED");
            } catch (Exception e) {
                logger.error("error with remote logger", e);
            }

            ProgressController window = (ProgressController) Executions.createComponents("WEB-INF/zul/progress/AnalysisProgress.zul", getMapComposer(), null);
            window.parent = this;
            window.start(pid, "Classification");
            window.doModal();

            this.setVisible(false);

            return true;

        } catch (Exception ex) {
            logger.error("error opening AnalysisProgress.zul for classification: " + pid, ex);
            getMapComposer().showMessage("Unknown error.", this);
        }

        return false;
    }
}
