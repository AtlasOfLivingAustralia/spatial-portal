/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.progress.ProgressController;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import au.org.emii.portal.wms.WMSStyle;
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

    private static final Logger LOGGER = Logger.getLogger(ALOCComposer.class);
    private String selectedLayers = "";
    private int generationCount = 1;
    private String layerLabel = "";
    private String legendPath = "";
    private Intbox groupCount;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = StringConstants.CLASSIFICATION;
        this.totalSteps = 4;

        this.setIncludeAnalysisLayersForAnyQuery(true);

        this.loadAreaLayers();
        this.loadGridLayers(true, false, true);
        this.updateWindowTitle();

        this.updateName(getMapComposer().getNextAreaLayerName(StringConstants.MY_CLASSIFICATION));
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();

        this.updateName(getMapComposer().getNextAreaLayerName(StringConstants.MY_CLASSIFICATION));
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
                return -1;
            }

            SelectedArea sa = getSelectedArea();

            //estimate analysis size in bytes
            double[][] bbox = null;
            List<Double> bb;
            if (sa.getMapLayer() != null) {
                bb = sa.getMapLayer().getMapLayerMetadata().getBbox();
            } else {
                bb = Util.getBoundingBox(sa.getWkt());
            }
            bbox = new double[][]{{bb.get(0), bb.get(1)}, {bb.get(2), bb.get(3)}};

            long cellsInBBox = (long) ((bbox[1][0] - bbox[0][0]) / 0.01 * (bbox[1][1] - bbox[0][1]) / 0.01);
            long size = (groupCount.getValue() + sbenvsel.split(":").length + 2) * cellsInBBox * 4;
            LOGGER.debug("ALOC estimate size in MB, cells=" + cellsInBBox
                    + ", bbox=" + bbox[0][0] + "," + bbox[0][1] + "," + bbox[1][0] + "," + bbox[1][1]
                    + ", groups=" + groupCount.getValue()
                    + ", layers=" + sbenvsel.split(":").length
                    + ", size=" + size / 1024 / 1024
                    + ", max size=" + CommonData.getSettings().getProperty("aloc_size_limit_in_mb"));
            if (size / 1024 / 1024 > Integer.parseInt(CommonData.getSettings().getProperty("aloc_size_limit_in_mb"))) {
                getMapComposer().showMessage("Analysis is too large.  Reduce the number of groups, number of layers or area.", this);
                return -1;
            }

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod((CommonData.getSatServer() + "/ws/aloc/estimate?") + "gc=" + URLEncoder.encode(String.valueOf(groupCount.getValue()), StringConstants.UTF_8) + "&envlist=" + URLEncoder.encode(sbenvsel, StringConstants.UTF_8));
            String area;
            if (sa.getMapLayer() != null && sa.getMapLayer().getEnvelope() != null) {
                area = StringConstants.ENVELOPE + "(" + sa.getMapLayer().getEnvelope() + ")";
            } else {
                area = sa.getWkt();
            }
            get.addParameter(StringConstants.AREA, area);
            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            client.executeMethod(get);
            String estimate = get.getResponseBodyAsString();

            return Long.valueOf(estimate);

        } catch (Exception e) {
            LOGGER.error("Unable to get estimates", e);
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
            default:
                LOGGER.error("invalid step for ALOCComposer: " + currentStep);
        }
    }

    public void loadMap(Event event) {
        //get job inputs
        try {
            for (String s : getJob().split(";")) {
                if (s.startsWith("gc")) {
                    layerLabel = tToolName.getValue();
                    generationCount++;
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("error getting ALOC job info", e);
        }

        if (layerLabel == null) {
            layerLabel = "My Classification " + generationCount;
            generationCount++;
        }

        String mapurl = CommonData.getGeoServer() + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:aloc_" + pid + "&FORMAT=image%2Fpng";
        String legendurl = CommonData.getGeoServer()
                + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                + "&LAYER=ALA:aloc_" + pid;
        LOGGER.debug(legendurl);
        legendPath = legendurl;
        getMapComposer().addWMSLayer("aloc_" + pid, layerLabel, mapurl, (float) 0.5, null, legendurl, LayerUtilitiesImpl.ALOC, null, null);
        MapLayer mapLayer = getMapComposer().getMapLayer("aloc_" + pid);
        if (mapLayer != null) {
            mapLayer.setPid(pid);
            WMSStyle style = new WMSStyle();
            style.setName(StringConstants.DEFAULT);
            style.setDescription("Default style");
            style.setTitle(StringConstants.DEFAULT);
            style.setLegendUri(legendurl);

            LOGGER.debug("legend:" + legendPath);
            mapLayer.addStyle(style);
            mapLayer.setSelectedStyleIndex(1);

            MapLayerMetadata md = mapLayer.getMapLayerMetadata();

            String infoUrl = CommonData.getSatServer() + "/output/aloc/" + pid + "/classification.html" + "\nClassification output\npid:" + pid;
            md.setMoreInfo(infoUrl);
            md.setId(Long.valueOf(pid));

            getMapComposer().updateLayerControls();

            try {
                // set off the download as well
                String fileUrl = CommonData.getSatServer() + "/ws/download/" + pid;
                Filedownload.save(new URL(fileUrl).openStream(), "application/zip", tToolName.getValue().replaceAll(" ", "_") + ".zip");
            } catch (Exception ex) {
                LOGGER.error("Error generating download for classification model:", ex);
            }
        }

        this.detach();
    }

    String getJob() {
        try {
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.getSatServer()).append("/ws/jobs/").append("inputs").append("?pid=").append(pid);

            LOGGER.debug(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            LOGGER.debug(slist);
            return slist;
        } catch (Exception e) {
            LOGGER.error("error getting gob info pid=" + pid, e);
        }
        return "";
    }

    public boolean runclassification() {
        try {
            layerLabel = tToolName.getValue();

            String sbenvsel = getSelectedLayers();
            if (sbenvsel.split(":").length > 50) {
                getMapComposer().showMessage(sbenvsel.split(":").length + " layers selected.  Please select fewer than 50 environmental layers in step 1.");
                return false;
            }

            if (groupCount.getValue() <= 1 || groupCount.getValue() > 200) {
                getMapComposer().showMessage("Please enter the number of groups to generate (2 to 200) in step 2.");
                return false;
            }

            SelectedArea sa = getSelectedArea();

            //estimate analysis size in bytes
            double[][] bbox = null;
            //NQ: 20131219 Added extra check because BB is not supplied in all situations with the metadata (ie WKT has been drawn etc) http://code.google.com/p/ala/issues/detail?id=475
            if (sa.getMapLayer() != null && sa.getMapLayer().getMapLayerMetadata().getBbox() != null) {
                List<Double> bb = sa.getMapLayer().getMapLayerMetadata().getBbox();
                bbox = new double[][]{{bb.get(0), bb.get(1)}, {bb.get(2), bb.get(3)}};

            } else {
                List<Double> bb = Util.getBoundingBox(sa.getWkt());
                bbox = new double[][]{{bb.get(0), bb.get(1)}, {bb.get(2), bb.get(3)}};
            }
            if (bbox == null) {
                bbox = new double[][]{{-180, -90}, {180, 90}};
            }

            long cellsInBBox = (long) ((bbox[1][0] - bbox[0][0]) / 0.01 * (bbox[1][1] - bbox[0][1]) / 0.01);
            long size = (groupCount.getValue() + sbenvsel.split(":").length + 2) * cellsInBBox * 4;
            LOGGER.debug("ALOC estimate size in MB, cells=" + cellsInBBox
                    + ", bbox=" + bbox[0][0] + "," + bbox[0][1] + "," + bbox[1][0] + "," + bbox[1][1]
                    + ", groups=" + groupCount.getValue()
                    + ", layers=" + sbenvsel.split(":").length
                    + ", size=" + size / 1024 / 1024
                    + ", max size=" + CommonData.getSettings().getProperty("aloc_size_limit_in_mb"));
            if (size / 1024 / 1024 > Integer.parseInt(CommonData.getSettings().getProperty("aloc_size_limit_in_mb"))) {
                getMapComposer().showMessage("Analysis is too large.  Reduce the number of groups, number of layers or area.", this);
                return false;
            }

            HttpClient client = new HttpClient();

            PostMethod get = new PostMethod((CommonData.getSatServer() + "/ws/aloc?") + "gc=" + URLEncoder.encode(String.valueOf(groupCount.getValue()), StringConstants.UTF_8) + "&envlist=" + URLEncoder.encode(sbenvsel, StringConstants.UTF_8));
            String area;
            if (sa.getMapLayer() != null && sa.getMapLayer().getEnvelope() != null) {
                area = StringConstants.ENVELOPE + "(" + sa.getMapLayer().getEnvelope() + ")";
            } else {
                area = sa.getWkt();
            }
            get.addParameter(StringConstants.AREA, area);
            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            generationCount++;
            pid = slist;

            legendPath = "/WEB-INF/zul/legend/LayerLegendClassification.zul?pid=" + pid + "&layer=" + URLEncoder.encode(layerLabel, StringConstants.UTF_8);

            try {
                remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Classification", area, "", sbenvsel, pid, "gc: " + groupCount.getValue(), StringConstants.STARTED);
            } catch (Exception e) {
                LOGGER.error("error with remote logger", e);
            }

            ProgressController window = (ProgressController) Executions.createComponents("WEB-INF/zul/progress/AnalysisProgress.zul", getMapComposer(), null);
            window.setParentWindow(this);
            window.start(pid, StringConstants.CLASSIFICATION);
            window.doModal();

            this.setVisible(false);

            return true;

        } catch (Exception ex) {
            LOGGER.error("error opening AnalysisProgress.zul for classification: " + pid, ex);
            getMapComposer().showMessage("Unknown error.", this);
        }

        return false;
    }
}
