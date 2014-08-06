/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.data.*;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.SelectedArea;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.zkoss.zul.Checkbox;

import java.awt.geom.Rectangle2D;
import java.net.URLEncoder;

/**
 * @author ajay
 */
public class ScatterplotComposer extends ToolComposer {

    private static Logger logger = Logger.getLogger(ScatterplotComposer.class);
    int generation_count = 1;
    ScatterplotData data;
    Checkbox chkShowEnvIntersection;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Scatterplot";
        this.totalSteps = 6;

        this.setIncludeAnalysisLayersForAnyQuery(true);
        //this.setIncludeAnalysisLayersForUploadQuery(true);

        this.loadAreaLayers("World");
        this.loadSpeciesLayers();
        this.loadAreaLayersHighlight();
        this.loadSpeciesLayersBk();
        this.updateWindowTitle();

        this.updateName(getMapComposer().getNextAreaLayerName("My Scatterplot"));
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
        this.updateName(getMapComposer().getNextAreaLayerName("My Scatterplot"));
    }

    @Override
    public boolean onFinish() {
        logger.debug("Area: " + getSelectedArea());
        logger.debug("Species: " + getSelectedSpecies());

        Query query = getSelectedSpecies();
        if (query == null) {
            getMapComposer().showMessage("There was a problem selecting the species.  Try to select the species again", this);
            return false;
        }

        Query lsid = getSelectedSpecies();
        String name = getSelectedSpeciesName();

        JSONObject jo = cbLayer1.getSelectedItem().getValue();
        String lyr1name = cbLayer1.getText();
        String lyr1value = jo.getString("name");

        jo = cbLayer2.getSelectedItem().getValue();
        String lyr2name = cbLayer2.getText();
        String lyr2value = jo.getString("name");

        String pid = "";
        Rectangle2D.Double selection = null;
        boolean enabled = true;

        Query backgroundLsid = getSelectedSpeciesBk();
        if (bgSearchSpeciesACComp.hasValidAnnotatedItemSelected()) {
            backgroundLsid = bgSearchSpeciesACComp.getQuery(getMapComposer(), false, getGeospatialKosher());//QueryUtil.get((String) bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0), getMapComposer(), false, getGeospatialKosher());
        }

        SelectedArea filterSa = getSelectedArea();
        SelectedArea highlightSa = getSelectedAreaHighlight();

        boolean envGrid = chkShowEnvIntersection.isChecked();

        Query lsidQuery = QueryUtil.queryFromSelectedArea(lsid, filterSa, false, getGeospatialKosher());

        Query backgroundLsidQuery = null;
        if (backgroundLsid != null) {
            backgroundLsidQuery = QueryUtil.queryFromSelectedArea(backgroundLsid, filterSa, false, getGeospatialKosherBk());
        }

        ScatterplotData data = new ScatterplotData(lsidQuery, name, lyr1value,
                lyr1name, lyr2value, lyr2name, pid, selection, enabled,
                backgroundLsidQuery,
                filterSa, highlightSa, envGrid);

        try {
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(CommonData.satServer + "/ws/scatterplot/new");

            //add data parameters
            post.addParameter("layers", lyr1value + ":" + lyr2value);   //colon delimited
            post.addParameter("layernames", "\"" + lyr1name.replace("\"", "\"\"") + "\",\"" + lyr2name.replace("\"", "\"\"") + "\"");  //CSV format
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

            if (filterSa != null) {
                post.addParameter("filterWkt", filterSa.getWkt());
            }

            //add style parameters (highlight area)
            if (highlightSa != null) {
                post.addParameter("highlightWkt", highlightSa.getWkt());
            }

            post.addRequestHeader("Accept", "application/json");

            int result = client.executeMethod(post);

            String str = post.getResponseBodyAsString();
            JSONObject jsonObject = JSONObject.fromObject(str);

            if (jsonObject != null && jsonObject.containsKey("id")) {
                data.setId(jsonObject.getString("id"));
                data.setMissingCount((jsonObject.getInt("missingCount")));
            } else {
                logger.error("error parsing scatterplot id from: " + str);
            }

        } catch (Exception e) {
            logger.error("error getting a new scatterplot id", e);
        }

        getMapComposer().loadScatterplot(data, tToolName.getValue());

        this.detach();

        try {
            String extras = "";

            if (lsidQuery instanceof BiocacheQuery) {
                BiocacheQuery bq = (BiocacheQuery) lsidQuery;
                extras = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false) + "|" + extras;
                remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Scatterplot", filterSa.getWkt(), bq.getLsids(), lyr1value + ":" + lyr2value, pid, extras, "SUCCESSFUL");
            } else if (lsidQuery instanceof UserDataQuery) {
                remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Scatterplot", filterSa.getWkt(), lsidQuery.getQ(), "", pid, extras, "SUCCESSFUL");
            } else {
                remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Scatterplot", filterSa.getWkt(), "", "", pid, extras, "SUCCESSFUL");
            }
        } catch (Exception e) {
            logger.error("logging error", e);
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
        }
    }
}
