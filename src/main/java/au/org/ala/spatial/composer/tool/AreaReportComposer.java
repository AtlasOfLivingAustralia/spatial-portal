/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.composer.results.AreaReportController;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.SelectedArea;
import org.zkoss.zul.Window;

/**
 * @author ajay
 */
public class AreaReportComposer extends ToolComposer {

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Area report";
        this.totalSteps = 1;

        this.loadAreaLayers();
        this.updateWindowTitle();
    }

    @Override
    public boolean onFinish() {
        //close any existing area report
        Window w = (Window) getPage().getFellowIfAny("popupResults");
        if (w != null) {
            w.detach();
        }
        SelectedArea sa = getSelectedArea();
        String areaName = getSelectedAreaName();
        String areaDisplayName = getSelectedAreaDisplayName();
        boolean includeEndemic = getIsEndemic();
        MapLayer ml = getMapComposer().getMapLayer(areaName);
        double[] bbox = null;
        if (ml != null
                && ml.getMapLayerMetadata().getBbox() != null
                && ml.getMapLayerMetadata().getBbox().size() == 4) {
            bbox = new double[4];
            bbox[0] = ml.getMapLayerMetadata().getBbox().get(0);
            bbox[1] = ml.getMapLayerMetadata().getBbox().get(1);
            bbox[2] = ml.getMapLayerMetadata().getBbox().get(2);
            bbox[3] = ml.getMapLayerMetadata().getBbox().get(3);
        }
        AreaReportController.open(sa, areaName, areaDisplayName,
                (ml == null) ? null : ml.getAreaSqKm(), bbox, includeEndemic);
        detach();

        return true;
    }

    @Override
    void fixFocus() {
        rgArea.setFocus(true);
    }
}
