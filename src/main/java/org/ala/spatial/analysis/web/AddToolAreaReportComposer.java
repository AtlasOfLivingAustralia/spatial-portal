/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.menu.MapLayer;
import org.zkoss.zul.Window;

/**
 *
 * @author ajay
 */
public class AddToolAreaReportComposer extends AddToolComposer {

    String selectedLayers = "";
    int generation_count = 1;
    String layerLabel = "";
    String legendPath = "";

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Area report";
        this.totalSteps = 1;

        this.loadAreaLayers();
        this.updateWindowTitle();
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
    }

    @Override
    public void onFinish() {
        //close any existing area report
        Window w = (Window) getPage().getFellowIfAny("popup_results");
        if (w != null) {
            w.detach();
        }
        String area = getSelectedArea();
        String areaName = getSelectedAreaName();
        String areaDisplayName = getSelectedAreaDisplayName();
        MapLayer ml = getMapComposer().getMapLayer(area);
        double[] bbox = null;
        if (ml != null && ml.getMapLayerMetadata() != null
                && ml.getMapLayerMetadata().getBbox() != null
                && ml.getMapLayerMetadata().getBbox().size() == 4) {
            bbox = new double[4];
            bbox[0] = ml.getMapLayerMetadata().getBbox().get(0);
            bbox[1] = ml.getMapLayerMetadata().getBbox().get(1);
            bbox[2] = ml.getMapLayerMetadata().getBbox().get(2);
            bbox[3] = ml.getMapLayerMetadata().getBbox().get(3);
        }
        FilteringResultsWCController.open(area, areaName, areaDisplayName,
                (String) ((ml == null) ? null : ml.getData("area")), bbox);
        detach();
    }
}
