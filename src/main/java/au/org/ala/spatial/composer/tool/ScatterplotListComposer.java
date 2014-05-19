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
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Filedownload;

import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * @author ajay
 */
public class ScatterplotListComposer extends ToolComposer {

    private static Logger logger = Logger.getLogger(ScatterplotListComposer.class);


    private static final String ACTIVE_AREA_SERIES = "In Active Area";
    int generation_count = 1;
    ScatterplotData data;
    Checkbox chkShowEnvIntersection;
    Boolean missing_data = false;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Scatterplot List";
        this.totalSteps = 5;

        this.setIncludeAnalysisLayersForAnyQuery(true);
        //this.setIncludeAnalysisLayersForUploadQuery(true);

        this.loadAreaLayers("World");
        this.loadSpeciesLayers();
        this.loadAreaLayersHighlight();
        this.loadGridLayers(true, false, true);
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
        SelectedArea filterSa = getSelectedArea();
        SelectedArea highlightSa = getSelectedAreaHighlight();
        Query lsidQuery = QueryUtil.queryFromSelectedArea(lsid, filterSa, false, getGeospatialKosher());

        String name = getSelectedSpeciesName();

        if (lsidQuery == null || lsidQuery.getOccurrenceCount() == 0) {
            getMapComposer().showMessage("No occurrences found for the selected species in the selected area.");
            return false;
        }

        String pid = "";
        Rectangle2D.Double selection = null;
        boolean enabled = true;

        Query backgroundLsid = getSelectedSpeciesBk();
        if (bgSearchSpeciesACComp.hasValidAnnotatedItemSelected()) {
            backgroundLsid = bgSearchSpeciesACComp.getQuery(getMapComposer(), false, getGeospatialKosher());//QueryUtil.get((String) bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0), getMapComposer(), false, getGeospatialKosher());
        }

        boolean envGrid = chkShowEnvIntersection.isChecked();

        Query backgroundLsidQuery = null;
        if (backgroundLsid != null) {
            backgroundLsidQuery = QueryUtil.queryFromSelectedArea(backgroundLsid, filterSa, false, getGeospatialKosherBk());
        }

        ArrayList<ScatterplotData> datas = new ArrayList<ScatterplotData>();

        String sbenvsel = getSelectedLayers();
        String[] layers = sbenvsel.split(":");
        if (layers.length > 20) {
            getMapComposer().showMessage(sbenvsel.split(":").length + " layers selected.  Please select fewer than 20 environmental layers in step 1.");
            return false;
        }

        String layernames = "";
        for (int i = 0; i < layers.length; i++) {
            if (layernames.length() > 0) {
                layernames += ",";
            }
            layernames += "\"" + CommonData.getLayerDisplayName(layers[i]).replace("\"", "\"\"") + "\"";
        }

        try {
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(CommonData.satServer + "/ws/scatterplotlist");

            //add data parameters
            post.addParameter("layers", getSelectedLayers());
            post.addParameter("layernames", layernames);
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
            String has_id = post.getResponseBodyAsString();

            JSONObject jo = JSONObject.fromObject(has_id);

            String htmlUrl = null;
            String downloadUrl = null;
            if (jo.containsKey("id")) {
                pid = jo.getString("id");
            }
            if (jo.containsKey("htmlUrl")) {
                htmlUrl = jo.getString("htmlUrl");
            }
            if (jo.containsKey("downloadUrl")) {
                downloadUrl = jo.getString("downloadUrl");
            }

            if (htmlUrl != null && downloadUrl != null) {
                Events.echoEvent("openUrl", getMapComposer(), htmlUrl);
                try {
                    Filedownload.save(new URL(downloadUrl).openStream(), "application/zip", tToolName.getValue().replaceAll(" ", "_") + ".zip");
                } catch (Exception e) {
                    logger.error("error preparing download for scatterplot data", e);
                }

                try {
                    String extras = "";
                    if (highlightSa != null) {
                        extras += "highlight=" + highlightSa.getWkt();
                    }
                    if (backgroundLsid != null && backgroundLsid instanceof BiocacheQuery) {
                        extras += "background=" + ((BiocacheQuery) backgroundLsid).getLsids();
                    } else if (backgroundLsid != null && backgroundLsid instanceof UserDataQuery) {
                        extras += "background=" + backgroundLsid.getQ();
                    } else {
                        extras += "background=none";
                    }

                    if (lsidQuery instanceof BiocacheQuery) {
                        BiocacheQuery bq = (BiocacheQuery) lsidQuery;
                        extras = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false) + "|" + extras;
                        remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Scatterplot List", filterSa.getWkt(), bq.getLsids(), sbenvsel, pid, extras, "SUCCESSFUL");
                    } else if (lsidQuery instanceof UserDataQuery) {
                        remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Scatterplot List", filterSa.getWkt(), lsidQuery.getQ(), sbenvsel, pid, extras, "SUCCESSFUL");
                    } else {
                        remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Scatterplot List", filterSa.getWkt(), "", sbenvsel, pid, extras, "SUCCESSFUL");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                logger.error("failed to produce a scatterplot list.  response: " + jo);
            }

        } catch (Exception e) {
            logger.error("error getting a new scatterplot id", e);
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
                //cbLayer2.setFocus(true);
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
