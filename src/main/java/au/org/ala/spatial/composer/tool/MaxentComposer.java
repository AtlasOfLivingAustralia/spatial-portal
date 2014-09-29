/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.progress.ProgressController;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Textbox;

import java.net.URL;
import java.net.URLEncoder;

/**
 * @author ajay
 */
public class MaxentComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(MaxentComposer.class);
    private SelectedArea sa = null;
    private Query query = null;
    private String sbenvsel = "";
    private Checkbox chkJackknife;
    private Checkbox chkRCurves;
    private Textbox txtTestPercentage;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = StringConstants.PREDICTION;
        this.totalSteps = 5;

        this.setIncludeAnalysisLayersForAnyQuery(true);

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.loadGridLayers(true, true, true);
        this.updateWindowTitle();

    }

    public void onClick$btnClearSelectionCtx(Event event) {
        // check if lbListLayers is empty as well,
        // if so, then disable the next button
        if (lbListLayers.getSelectedCount() == 0) {
            btnOk.setDisabled(true);
        }
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();

        this.updateName(getMapComposer().getNextAreaLayerName("My Prediction"));
    }

    @Override
    public boolean onFinish() {

        if (!hasEstimated && !isUserLoggedIn()) {
            checkEstimate();
            return false;
        }

        Query q = getSelectedSpecies();
        if (q == null) {
            getMapComposer().showMessage("There is a problem selecting the species.  Try to select the species again", this);
            return false;
        }
        if (searchSpeciesACComp.getAutoComplete().getSelectedItem() != null) {
            getMapComposer().mapSpeciesFromAutocompleteComponent(searchSpeciesACComp, getSelectedArea(), getGeospatialKosher(), false);
        } else if (rgSpecies.getSelectedItem() != null && StringConstants.MULTIPLE.equals(rgSpecies.getSelectedItem().getValue())) {
            getMapComposer().mapSpecies(q, StringConstants.SPECIES_ASSEMBLAGE, StringConstants.SPECIES, 0, LayerUtilitiesImpl.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), false);
        }

        LOGGER.debug("Maxent Selected layers:");
        LOGGER.debug(getSelectedLayers());

        return runmaxent();
    }

    private void setupData() throws Exception {
        if (query == null) {
            sa = getSelectedArea();
            query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false, getGeospatialKosher());

            sbenvsel = getSelectedLayers();
        }
    }

    @Override
    public long getEstimate() {
        try {
            setupData();

            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.getSatServer()).append("/ws/maxent/estimate?");
            sbProcessUrl.append("taxonid=").append(URLEncoder.encode(query.getName(), StringConstants.UTF_8));
            sbProcessUrl.append("&taxonlsid=").append(URLEncoder.encode(query.getQ(), StringConstants.UTF_8));
            sbProcessUrl.append("&envlist=").append(URLEncoder.encode(sbenvsel, StringConstants.UTF_8));

            sbProcessUrl.append("&speciesq=").append(URLEncoder.encode(QueryUtil.queryFromSelectedArea(query, sa, false, getGeospatialKosher()).getQ(), StringConstants.UTF_8));
            sbProcessUrl.append("&bs=").append(URLEncoder.encode(query.getBS(), StringConstants.UTF_8));

            if (chkJackknife.isChecked()) {
                sbProcessUrl.append("&chkJackknife=on");
            }
            if (chkRCurves.isChecked()) {
                sbProcessUrl.append("&chkResponseCurves=on");
            }
            sbProcessUrl.append("&txtTestPercentage=").append(txtTestPercentage.getValue());

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());

            String area;
            if (sa.getMapLayer() != null && sa.getMapLayer().getEnvelope() != null) {
                area = StringConstants.ENVELOPE + "(" + sa.getMapLayer().getEnvelope() + ")";
            } else {
                area = sa.getWkt();
            }
            if (getSelectedArea() != null) {
                get.addParameter(StringConstants.AREA, area);
            }

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            client.executeMethod(get);
            String estimate = get.getResponseBodyAsString();

            return Long.valueOf(estimate);

        } catch (Exception e) {
            LOGGER.error("Unable to get estimates", e);
        }
        return -1;
    }

    public boolean runmaxent() {
        try {

            setupData();

            LOGGER.debug("Selected species: " + query.getName());
            LOGGER.debug("Selected species query: " + query.getQ());
            LOGGER.debug("Selected env vars");
            LOGGER.debug(sbenvsel);
            LOGGER.debug("Selected options: ");
            LOGGER.debug("Jackknife: " + chkJackknife.isChecked());
            LOGGER.debug("Response curves: " + chkRCurves.isChecked());
            LOGGER.debug("Test per: " + txtTestPercentage.getValue());

            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.getSatServer()).append("/ws/maxent?");
            sbProcessUrl.append("taxonid=").append(URLEncoder.encode(query.getName(), StringConstants.UTF_8));
            sbProcessUrl.append("&taxonlsid=").append(URLEncoder.encode(query.getQ(), StringConstants.UTF_8));
            sbProcessUrl.append("&envlist=").append(URLEncoder.encode(sbenvsel, StringConstants.UTF_8));
            sbProcessUrl.append("&speciesq=").append(URLEncoder.encode(QueryUtil.queryFromSelectedArea(query, sa, false, getGeospatialKosher()).getQ(), StringConstants.UTF_8));
            sbProcessUrl.append("&bs=").append(URLEncoder.encode(query.getBS(), StringConstants.UTF_8));

            if (chkJackknife.isChecked()) {
                sbProcessUrl.append("&chkJackknife=on");
            }
            if (chkRCurves.isChecked()) {
                sbProcessUrl.append("&chkResponseCurves=on");
            }
            sbProcessUrl.append("&txtTestPercentage=").append(txtTestPercentage.getValue());

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());

            String area;
            if (sa.getMapLayer() != null && sa.getMapLayer().getEnvelope() != null) {
                area = StringConstants.ENVELOPE + "(" + sa.getMapLayer().getEnvelope() + ")";
            } else {
                area = sa.getWkt();
            }
            if (getSelectedArea() != null) {
                get.addParameter(StringConstants.AREA, area);
            }

            LOGGER.debug("Getting species data");

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            client.executeMethod(get);
            pid = get.getResponseBodyAsString();

            openProgressBar();

            try {
                String options = "";
                options += "Jackknife: " + chkJackknife.isChecked();
                options += ";Response curves: " + chkRCurves.isChecked();
                options += ";Test per: " + txtTestPercentage.getValue();
                if (query instanceof BiocacheQuery) {
                    BiocacheQuery bq = (BiocacheQuery) query;
                    options = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false) + "|" + options;
                    remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Prediction", area, bq.getLsids(), sbenvsel, pid, options, StringConstants.STARTED);
                } else {
                    remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Prediction", area, query.getName() + "__" + query.getQ(), sbenvsel, pid, options, StringConstants.STARTED);
                }
            } catch (Exception e) {
                LOGGER.error("error requesting maxent", e);
            }

            this.setVisible(false);

            return true;

        } catch (Exception e) {
            LOGGER.error("Maxent error: ", e);
            getMapComposer().showMessage("Unknown error.", this);
        }
        return false;
    }

    void openProgressBar() {
        ProgressController window = (ProgressController) Executions.createComponents("WEB-INF/zul/progress/AnalysisProgress.zul", getMapComposer(), null);
        window.setParentWindow(this);
        window.start(pid, StringConstants.PREDICTION, isBackgroundProcess);
        try {
            window.doModal();
        } catch (Exception e) {
            LOGGER.error("error opening AnaysisProgress.zul for Prediction", e);
        }
    }

    public void loadMap(Event event) {

        String mapurl = CommonData.getGeoServer() + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + pid + "&styles=alastyles&FORMAT=image%2Fpng";

        String legendurl = CommonData.getGeoServer()
                + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                + "&LAYER=ALA:species_" + pid
                + "&STYLE=alastyles";

        LOGGER.debug(legendurl);

        //get job inputs
        String layername = tToolName.getValue();
        getMapComposer().addWMSLayer("species_" + pid, layername, mapurl, (float) 0.5, null, legendurl, LayerUtilitiesImpl.MAXENT, null, null);
        MapLayer ml = getMapComposer().getMapLayer("species_" + pid);
        ml.setPid(pid);
        String infoUrl = CommonData.getSatServer() + "/output/maxent/" + pid + "/species.html";
        MapLayerMetadata md = ml.getMapLayerMetadata();

        md.setMoreInfo(infoUrl + "\nMaxent Output\npid:" + pid);
        md.setId(Long.valueOf(pid));

        try {
            // set off the download as well
            String fileUrl = CommonData.getSatServer() + "/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", tToolName.getValue().replaceAll(" ", "_") + ".zip");
        } catch (Exception e) {
            LOGGER.error("Error generating download for prediction model:", e);
        }

        this.detach();
    }

    @Override
    void fixFocus() {
        switch (currentStep) {
            case 1:
                rgArea.setFocus(true);
                break;
            case 2:
                if (rSpeciesSearch.isChecked()) {
                    searchSpeciesACComp.getAutoComplete().setFocus(true);
                } else {
                    rgSpecies.setFocus(true);
                }
                break;
            case 3:
                lbListLayers.setFocus(true);
                break;
            case 4:
                chkJackknife.setFocus(true);
                break;
            case 5:
                tToolName.setFocus(true);
                break;
            default:
                LOGGER.error("invalid step for MaxentComposer: " + currentStep);
        }
    }
}
