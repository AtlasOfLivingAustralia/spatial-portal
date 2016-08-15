/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.legend.Facet;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.menu.SelectedArea;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Filedownload;

import java.net.URL;
import java.util.Map;

/**
 * @author ajay
 */
public class ScatterplotListComposer extends ToolComposer {

    private static final Logger LOGGER = Logger.getLogger(ScatterplotListComposer.class);

    private Checkbox chkShowEnvIntersection;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Scatterplot List";
        this.totalSteps = 5;

        this.setIncludeAnalysisLayersForAnyQuery(true);

        this.loadAreaLayers("World");
        this.loadSpeciesLayers();
        this.loadAreaLayersHighlight();
        this.loadGridLayers(true, false, true);
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

        Query lsid = getSelectedSpecies();
        if (lsid == null) {
            getMapComposer().showMessage("There was a problem selecting the species.  Try to select the species again", this);
            return false;
        }
        lsid = lsid.newFacet(new Facet("occurrence_status_s", "absent", false), false);

        SelectedArea filterSa = getSelectedArea();
        SelectedArea highlightSa = getSelectedAreaHighlight();
        Query lsidQuery = QueryUtil.queryFromSelectedArea(lsid, filterSa, false, getGeospatialKosher());

        if (lsidQuery == null || lsidQuery.getOccurrenceCount() == 0) {
            getMapComposer().showMessage("No occurrences found for the selected species in the selected area.");
            return false;
        }

        String pid = "";

        Query backgroundLsid = getSelectedSpeciesBk();
        backgroundLsid = backgroundLsid.newFacet(new Facet("occurrence_status_s", "absent", false), false);
        if (bgSearchSpeciesACComp.hasValidAnnotatedItemSelected()) {
            backgroundLsid = bgSearchSpeciesACComp.getQuery((Map) getMapComposer().getSession().getAttribute(StringConstants.USERPOINTS), false, getGeospatialKosher());
        }

        boolean envGrid = chkShowEnvIntersection.isChecked();

        Query backgroundLsidQuery = null;
        if (backgroundLsid != null) {
            backgroundLsidQuery = QueryUtil.queryFromSelectedArea(backgroundLsid, filterSa, false, getGeospatialKosherBk());
        }

        String sbenvsel = getSelectedLayersWithDisplayNames();
        String[] layers = sbenvsel.split(":");
        if (layers.length > 20) {
            getMapComposer().showMessage(sbenvsel.split(":").length + " layers selected.  Please select fewer than 20 environmental layers in step 1.");
            return false;
        }

        StringBuilder layernames = new StringBuilder();
        String layerunits = "";
        for (int i = 0; i < layers.length; i++) {
            String[] split = layers[i].split("\\|");
            if (layernames.length() > 0) {
                layernames.append(",");
                layerunits += ",";
            }
            layers[i] = split[0];
            String layerDisplayName = split[1];
            layernames.append("\"").append(layerDisplayName.replace("\"", "\"\"").replace("\\", "\\\\")).append("\"");

            String units = "";
            try {
                units = String.valueOf(((JSONObject) CommonData.getLayer(layers[i]).get("layer")).get("environmentalvalueunits"));
            } catch (Exception e) {
            }
            layerunits += units;
        }

        try {
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(CommonData.getSatServer() + "/ws/scatterplotlist");

            //add data parameters
            post.addParameter("layers", getSelectedLayers());
            post.addParameter("layernames", layernames.toString());
            post.addParameter("layerunits", layerunits);
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
                post.addParameter(StringConstants.HIGHLIGHT_WKT, highlightSa.getWkt());
            }

            post.addRequestHeader(StringConstants.ACCEPT, StringConstants.APPLICATION_JSON);

            client.executeMethod(post);
            String hasId = post.getResponseBodyAsString();

            JSONParser jp = new JSONParser();
            JSONObject jo = (JSONObject) jp.parse(hasId);

            String htmlUrl = null;
            String downloadUrl = null;
            if (jo.containsKey(StringConstants.ID)) {
                pid = jo.get(StringConstants.ID).toString();
            }
            if (jo.containsKey("htmlUrl")) {
                htmlUrl = jo.get("htmlUrl").toString();
            }
            if (jo.containsKey("downloadUrl")) {
                downloadUrl = jo.get("downloadUrl").toString();
            }

            if (htmlUrl != null && downloadUrl != null) {
                Events.echoEvent(StringConstants.OPEN_URL, getMapComposer(), htmlUrl);
                try {
                    Filedownload.save(new URL(downloadUrl).openStream(), "application/zip", tToolName.getValue().replaceAll(" ", "_") + ".zip");
                } catch (Exception e) {
                    LOGGER.error("error preparing download for scatterplot data", e);
                }

                try {
                    String extras = "";
                    if (highlightSa != null) {
                        extras += "highlight=" + highlightSa.getWkt();
                    }
                    if (backgroundLsid instanceof BiocacheQuery) {
                        extras += "background=" + ((BiocacheQuery) backgroundLsid).getLsids();
                    } else if (backgroundLsid instanceof UserDataQuery) {
                        extras += "background=" + backgroundLsid.getQ();
                    } else {
                        extras += "background=none";
                    }

                    if (lsidQuery instanceof BiocacheQuery) {
                        BiocacheQuery bq = (BiocacheQuery) lsidQuery;
                        extras = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false) + "|" + extras;
                        remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Scatterplot List", filterSa.getWkt(), bq.getLsids(), sbenvsel, pid, extras, StringConstants.SUCCESSFUL);
                    } else if (lsidQuery instanceof UserDataQuery) {
                        remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Scatterplot List", filterSa.getWkt(), lsidQuery.getQ(), sbenvsel, pid, extras, StringConstants.SUCCESSFUL);
                    } else {
                        remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Scatterplot List", filterSa.getWkt(), "", sbenvsel, pid, extras, StringConstants.SUCCESSFUL);
                    }
                } catch (Exception e) {
                    LOGGER.error("failed to produce a scatterplot list.", e);
                }
            } else {
                LOGGER.error("failed to produce a scatterplot list.  response: " + jo);
            }

        } catch (Exception e) {
            LOGGER.error("error getting a new scatterplot id", e);
        }

        this.detach();

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
                LOGGER.error("invalid step for ScatterplotListComposer: " + currentStep);
        }
    }
}
