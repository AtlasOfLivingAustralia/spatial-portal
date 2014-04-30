/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.composer.results.SpeciesListResults;
import au.org.ala.spatial.util.SelectedArea;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;

import java.util.HashMap;

/**
 * @author ajay
 */
public class SpeciesListComposer extends ToolComposer {
    private static Logger logger = Logger.getLogger(SpeciesListComposer.class);
    String selectedLayers = "";
    int generation_count = 1;
    String layerLabel = "";
    String legendPath = "";
    String extraParams;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Species list";
        this.totalSteps = 1;

        this.loadAreaLayers();
        this.updateWindowTitle();
        extraParams = (String) Executions.getCurrent().getArg().get("extraParams");
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
            logger.error("error attemping to download species list", e);
            getMapComposer().showMessage("Unknown error.", this);
        }
        return false;
    }

    public void onClick$btnDownload() {
        SelectedArea sa = getSelectedArea();
        HashMap<String, Object> hm = new HashMap<String, Object>();
        hm.put("selectedarea", sa);
        hm.put("geospatialKosher", getGeospatialKosher());
        hm.put("chooseEndemic", chkEndemicSpecies.isChecked());

        if (extraParams != null) {
            hm.put("extraParams", extraParams);
        }
        SpeciesListResults window = (SpeciesListResults) Executions.createComponents("WEB-INF/zul/results/AnalysisSpeciesListResults.zul", getMapComposer(), hm);
        try {
            window.doModal();
        } catch (Exception e) {
            logger.error("error opening analysisspecieslistresults.zul", e);
        }
        detach();
    }

    @Override
    void fixFocus() {
        rgArea.setFocus(true);
    }
}
