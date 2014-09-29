/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.results.SpeciesListResults;
import au.org.emii.portal.menu.SelectedArea;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ajay
 */
public class SpeciesListComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(SpeciesListComposer.class);
    private String extraParams;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Species list";
        this.totalSteps = 1;

        this.loadAreaLayers();
        this.updateWindowTitle();
        extraParams = (String) Executions.getCurrent().getArg().get(StringConstants.EXTRAPARAMS);
    }

    @Override
    public boolean onFinish() {
        try {
            onClick$btnDownload();
            return true;
        } catch (Exception e) {
            LOGGER.error("error attemping to download species list", e);
            getMapComposer().showMessage("Unknown error.", this);
        }
        return false;
    }

    public void onClick$btnDownload() {
        SelectedArea sa = getSelectedArea();
        Map<String, Object> hm = new HashMap<String, Object>();
        hm.put("selectedarea", sa);
        hm.put("geospatialKosher", getGeospatialKosher());
        hm.put(StringConstants.CHOOSEENDEMIC, chkEndemicSpecies.isChecked());

        if (extraParams != null) {
            hm.put(StringConstants.EXTRAPARAMS, extraParams);
        }
        SpeciesListResults window = (SpeciesListResults) Executions.createComponents("WEB-INF/zul/results/AnalysisSpeciesListResults.zul", getMapComposer(), hm);
        try {
            window.doModal();
        } catch (Exception e) {
            LOGGER.error("error opening analysisspecieslistresults.zul", e);
        }
        detach();
    }

    @Override
    void fixFocus() {
        rgArea.setFocus(true);
    }
}
