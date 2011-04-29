/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.analysis.web;

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
        if(w != null) {
            w.detach();
        }
        String area = getSelectedArea();
        String areaName = getSelectedAreaName();
        String areaDisplayName = getSelectedAreaDisplayName();
        FilteringResultsWCController.open(area, areaName, areaDisplayName);
        detach();
    }
}
