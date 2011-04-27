/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.analysis.web;

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
        String area = getSelectedArea();
        String areaName = getSelectedAreaName();
        FilteringResultsWCController.open(area, areaName);
        detach();
    }
}
