/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.util.AreaReportPDF;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Window;

/**
 * @author ajay
 */
public class AreaReportPDFComposer extends ToolComposer {

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Detailed Area Report (PDF)";
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

        AreaReportPDF af = new AreaReportPDF(ml == null ? sa.getWkt() : ml.getWKT(), areaDisplayName);

        Filedownload.save(af.getPDF(), "application/pdf", "areaReport.pdf");

        detach();

        return true;
    }

    @Override
    void fixFocus() {
        rgArea.setFocus(true);
    }
}
