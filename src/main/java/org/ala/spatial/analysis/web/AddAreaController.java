package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Window;

/**
 *
 * @author ajay
 */
public class AddAreaController extends UtilityComposer {
    
    SettingsSupplementary settingsSupplementary;
    Radiogroup cbAreaSelection;
    Radio ciBoundingBox, ciPolygon, ciPointAndRadius, ciAddressRadiusSelection, ciMapPolygon, ciEnvironmentalEnvelope, ciUploadShapefile, ciBoxAustraia, ciBoxWorld, ciBoxCurrentView;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void onClick$btnOk(Event event) {
        String windowName = "";
        if (cbAreaSelection.getSelectedItem() == ciBoundingBox) {
            windowName = "WEB-INF/zul/AreaBoundingBox.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciPolygon) {
            windowName = "WEB-INF/zul/AreaPolygon.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciPointAndRadius) {
            windowName = "WEB-INF/zul/AreaPointAndRadius.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciAddressRadiusSelection) {
            windowName = "WEB-INF/zul/AreaAddressRadiusSelection.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciUploadShapefile) {
           windowName = "WEB-INF/zul/AreaUploadShapefile.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciMapPolygon) {
            windowName = "WEB-INF/zul/AreaMapPolygon.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciEnvironmentalEnvelope) {
            windowName = "WEB-INF/zul/AreaEnvironmentalEnvelope.zul";
        }

        Window window = (Window) Executions.createComponents(windowName, getMapComposer(), null);
        try {
            window.doOverlapped();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }
}
