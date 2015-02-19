/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.ScatterplotDataDTO;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.menu.SelectedArea;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.zul.Checkbox;

import java.util.Map;

/**
 * @author ajay
 */
public class ScatterplotComposer extends ToolComposer {

    private static final Logger LOGGER = Logger.getLogger(ScatterplotComposer.class);
    private ScatterplotDataDTO data;
    private Checkbox chkShowEnvIntersection;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Scatterplot";
        this.totalSteps = 6;

        this.setIncludeAnalysisLayersForAnyQuery(true);

        this.loadAreaLayers("World");
        this.loadSpeciesLayers();
        this.loadAreaLayersHighlight();
        this.loadSpeciesLayersBk();
        this.updateWindowTitle();

        this.updateName(getMapComposer().getNextAreaLayerName(StringConstants.MY_SCATTERPLOT));
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
        this.updateName(getMapComposer().getNextAreaLayerName(StringConstants.MY_SCATTERPLOT));
    }

    @Override
    public boolean onFinish() {
        LOGGER.debug("Area: " + getSelectedArea());
        LOGGER.debug("Species: " + getSelectedSpecies());

        Query query = getSelectedSpecies();
        if (query == null) {
            getMapComposer().showMessage("There was a problem selecting the species.  Try to select the species again", this);
            return false;
        }

        Query lsid = getSelectedSpecies();
        String name = getSelectedSpeciesName();

        JSONObject jo = cbLayer1.getSelectedItem().getValue();
        String lyr1name = cbLayer1.getText();
        String lyr1value = jo.get(StringConstants.NAME).toString();

        jo = cbLayer2.getSelectedItem().getValue();
        String lyr2name = cbLayer2.getText();
        String lyr2value = jo.get(StringConstants.NAME).toString();

        String pid = "";

        Query backgroundLsid = getSelectedSpeciesBk();
        if (bgSearchSpeciesACComp.hasValidAnnotatedItemSelected()) {
            backgroundLsid = bgSearchSpeciesACComp.getQuery((Map) getMapComposer().getSession().getAttribute(StringConstants.USERPOINTS), false, getGeospatialKosher());
        }

        SelectedArea filterSa = getSelectedArea();
        if (filterSa == null) {
            LOGGER.error("scatterplot area is null");
            return false;
        }
        SelectedArea highlightSa = getSelectedAreaHighlight();

        boolean envGrid = chkShowEnvIntersection.isChecked();

        Query lsidQuery = QueryUtil.queryFromSelectedArea(lsid, filterSa, false, getGeospatialKosher());

        Query backgroundLsidQuery = null;
        if (backgroundLsid != null) {
            backgroundLsidQuery = QueryUtil.queryFromSelectedArea(backgroundLsid, filterSa, false, getGeospatialKosherBk());
        }

        ScatterplotDataDTO d = new ScatterplotDataDTO(lsidQuery, name, lyr1value,
                lyr1name, lyr2value, lyr2name, pid, null, true,
                highlightSa);

        try {
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(CommonData.getSatServer() + "/ws/scatterplot/new");

            //add data parameters

            //colon delimited
            post.addParameter("layers", lyr1value + ":" + lyr2value);
            //CSV format
            post.addParameter("layernames", "\"" + lyr1name.replace("\"", "\"\"").replace("\\", "\\\\") + "\",\"" + lyr2name.replace("\"", "\"\"").replace("\\", "\\\\") + "\"");
            post.addParameter("foregroundOccurrencesQs", lsidQuery.getQ());
            post.addParameter("foregroundOccurrencesBs", lsidQuery.getBS());
            post.addParameter("foregroundName", lsidQuery.getName());

            if (backgroundLsidQuery != null) {
                post.addParameter("backgroundOccurrencesQs", backgroundLsidQuery.getQ());
                post.addParameter("backgroundOccurrencesBs", backgroundLsidQuery.getBS());
                post.addParameter("backgroundName", backgroundLsidQuery.getName());
            }

            if (envGrid) {
                post.addParameter("gridDivisions", "20");
            }

            post.addParameter("filterWkt", filterSa.getWkt());

            //add style parameters (highlight area)
            if (highlightSa != null) {
                post.addParameter(StringConstants.HIGHLIGHT_WKT, highlightSa.getWkt());
            }

            post.addRequestHeader(StringConstants.ACCEPT, StringConstants.APPLICATION_JSON);

            client.executeMethod(post);

            String str = post.getResponseBodyAsString();
            JSONParser jp = new JSONParser();
            JSONObject jsonObject = (JSONObject) jp.parse(str);

            if (jsonObject != null && jsonObject.containsKey(StringConstants.ID)) {
                d.setId(jsonObject.get(StringConstants.ID).toString());
                d.setMissingCount(Integer.parseInt(jsonObject.get("missingCount").toString()));
            } else {
                LOGGER.error("error parsing scatterplot id from: " + str);
            }

        } catch (Exception e) {
            LOGGER.error("error getting a new scatterplot id", e);
        }

        getMapComposer().loadScatterplot(d, tToolName.getValue());

        this.detach();

        try {
            String extras = "";

            if (lsidQuery instanceof BiocacheQuery) {
                BiocacheQuery bq = (BiocacheQuery) lsidQuery;
                extras = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false) + "|" + extras;
                remoteLogger.logMapAnalysis(tToolName.getValue(), StringConstants.TOOL_SCATTERPLOT, filterSa.getWkt(), bq.getLsids(), lyr1value + ":" + lyr2value, pid, extras, StringConstants.SUCCESSFUL);
            } else if (lsidQuery instanceof UserDataQuery) {
                remoteLogger.logMapAnalysis(tToolName.getValue(), StringConstants.TOOL_SCATTERPLOT, filterSa.getWkt(), lsidQuery.getQ(), "", pid, extras, StringConstants.SUCCESSFUL);
            } else {
                remoteLogger.logMapAnalysis(tToolName.getValue(), StringConstants.TOOL_SCATTERPLOT, filterSa.getWkt(), "", "", pid, extras, StringConstants.SUCCESSFUL);
            }
        } catch (Exception e) {
            LOGGER.error("logging error", e);
        }

        return true;
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
                rgAreaHighlight.setFocus(true);
                break;
            case 4:
                cbLayer2.setFocus(true);
                break;
            case 5:
                if (rSpeciesSearchBk.isChecked()) {
                    bgSearchSpeciesACComp.getAutoComplete().setFocus(true);
                } else {
                    rgSpeciesBk.setFocus(true);
                }
                break;
            case 6:
                tToolName.setFocus(true);
                break;
            default:
                LOGGER.error("invalid step for ScatterplotComposer: " + currentStep);
        }
    }
}
