/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import java.awt.geom.Rectangle2D;
import net.sf.json.JSONObject;
import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryUtil;
import org.ala.spatial.data.UploadQuery;
import org.ala.spatial.util.ScatterplotData;
import org.ala.spatial.util.SelectedArea;
import org.zkoss.zul.Checkbox;

/**
 *
 * @author ajay
 */
public class AddToolScatterplotComposer extends AddToolComposer {

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
        System.out.println("Area: " + getSelectedArea());
        System.out.println("Species: " + getSelectedSpecies());

        Query lsid = getSelectedSpecies();
        String name = getSelectedSpeciesName();

        JSONObject jo = (JSONObject) cbLayer1.getSelectedItem().getValue();
        String lyr1name = cbLayer1.getText();
        String lyr1value = jo.getString("name");

        jo = (JSONObject) cbLayer2.getSelectedItem().getValue();
        String lyr2name = cbLayer2.getText();
        String lyr2value = jo.getString("name");

        String pid = "";
        Rectangle2D.Double selection = null;
        boolean enabled = true;

        Query backgroundLsid = getSelectedSpeciesBk();
        if (bgSearchSpeciesAuto.getSelectedItem() != null
                && bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties() != null
                && bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties().size() > 0) {
            backgroundLsid = QueryUtil.get((String) bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0), getMapComposer(), false);
        }

        SelectedArea filterSa = getSelectedArea();
        SelectedArea highlightSa = getSelectedAreaHighlight();

        boolean envGrid = chkShowEnvIntersection.isChecked();

        Query lsidQuery = QueryUtil.queryFromSelectedArea(lsid, filterSa, false);

        Query backgroundLsidQuery = QueryUtil.queryFromSelectedArea(backgroundLsid, filterSa, false);

        ScatterplotData data = new ScatterplotData(lsidQuery, name, lyr1value,
                lyr1name, lyr2value, lyr2name, pid, selection, enabled,
                (backgroundLsid != null) ? backgroundLsidQuery : null,
                filterSa, highlightSa, envGrid);

        getMapComposer().loadScatterplot(data, tToolName.getValue());

        this.detach();

        try {
            String extras = "";
            if(highlightSa != null) {
                extras += "highlight="+highlightSa.getWkt();
            }
            if (backgroundLsid != null && backgroundLsid instanceof BiocacheQuery) {
                extras += "background="+((BiocacheQuery) backgroundLsid).getLsids();
            } else if (backgroundLsid != null && backgroundLsid instanceof UploadQuery) {
                extras += "background="+((UploadQuery) backgroundLsid).getQ();
            } else {
                extras += "background=none";
            }

            if (lsidQuery instanceof BiocacheQuery) {
                BiocacheQuery bq = (BiocacheQuery) lsidQuery;
                extras = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false) + "|" + extras;
                remoteLogger.logMapAnalysis(tToolName.getValue(), "analysis - scatterplot", filterSa.getWkt(), bq.getLsids(), lyr1value+":"+lyr2value, pid, extras, "SUCCESSFUL");
            } else if (lsidQuery instanceof UploadQuery) {
                remoteLogger.logMapAnalysis(tToolName.getValue(), "analysis - scatterplot", filterSa.getWkt(), ((UploadQuery) lsidQuery).getQ(), "", pid, extras, "SUCCESSFUL");
            } else {
                remoteLogger.logMapAnalysis(tToolName.getValue(), "analysis - scatterplot", filterSa.getWkt(), "", "", pid, extras, "SUCCESSFUL");
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                if(rSpeciesSearch.isChecked()) {
                    searchSpeciesAuto.setFocus(true);
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
                if(rSpeciesSearchBk.isChecked()) {
                    bgSearchSpeciesAuto.setFocus(true);
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
