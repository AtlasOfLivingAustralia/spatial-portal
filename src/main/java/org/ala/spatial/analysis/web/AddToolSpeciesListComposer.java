/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import java.util.HashMap;
import org.ala.spatial.util.SelectedArea;
import org.zkoss.zk.ui.Executions;

/**
 *
 * @author ajay
 */
public class AddToolSpeciesListComposer extends AddToolComposer {

    String selectedLayers = "";
    int generation_count = 1;
    String layerLabel = "";
    String legendPath = "";

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Species list";
        this.totalSteps = 1;

        this.loadAreaLayers();
        this.updateWindowTitle();
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
    }

    @Override
    public boolean onFinish() {
        try {
            onClick$btnDownload();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            getMapComposer().showMessage("Unknown error.", this);
        }
        return false;
    }

    public void onClick$btnDownload() {
        SelectedArea sa = getSelectedArea();
        HashMap<String, Object> hm = new HashMap<String, Object>();
        hm.put("selectedarea", sa);
        hm.put("geospatialKosher", getGeospatialKosher());
        hm.put("chooseEndemic", Boolean.valueOf(chkEndemicSpecies.isChecked()));
        SpeciesListResults window = (SpeciesListResults) Executions.createComponents("WEB-INF/zul/AnalysisSpeciesListResults.zul", getMapComposer(), hm);
        try {
            window.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }

        detach();
    }

    @Override
    void fixFocus() {
        rgArea.setFocus(true);
    }
}
