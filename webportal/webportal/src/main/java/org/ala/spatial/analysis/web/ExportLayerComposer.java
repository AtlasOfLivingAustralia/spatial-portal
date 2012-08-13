/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.menu.MapLayer;
import java.util.List;
import org.zkoss.zul.Div;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Radiogroup;

/**
 *
 * @author ajay
 */
public class ExportLayerComposer extends AddToolComposer {

    Radiogroup exportFormat;
    //Button btnExportCancel;
    //Button btnExportOk;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Export area";
        this.totalSteps = 2;

        this.loadAreaLayers();
        this.updateWindowTitle();

        this.updateName(getMapComposer().getNextAreaLayerName("My Export Area"));
    }

    //@Override
    public void loadAreaLayersChecks() {
        String selectedLayerName = (String) params.get("polygonLayerName");
        Div areachks = (Div) getFellowIfAny("areachks");

        List<MapLayer> layers = getMapComposer().getPolygonLayers();
        for (int i = 0; i < layers.size(); i++) {
            MapLayer lyr = layers.get(i);

            Checkbox cAr = new Checkbox(lyr.getDisplayName());
            cAr.setValue(lyr.getWKT());

            areachks.insertBefore(cAr, null);
        }

    }

    public void onCheck$exportFormat(Event event) {
        btnOk.setDisabled(false);
    }

    @Override
    public boolean onFinish() {
        getMapComposer().exportAreaAs(exportFormat.getSelectedItem().getValue(), rAreaSelected.getLabel(), getSelectedArea());
        detach();
        return true;
    }

     @Override
    void fixFocus() {
         switch(currentStep) {
             case 1:
                 rgArea.setFocus(true);
                 break;
             case 2:
                 exportFormat.setFocus(true);
                 break;
         }
    }
}
