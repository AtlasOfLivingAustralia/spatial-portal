package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
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
    Radio ciWKT, ciUploadKML, ciRegionSelection, ciBoundingBox, ciPolygon, ciPointAndRadius, ciAddressRadiusSelection, ciMapPolygon, ciEnvironmentalEnvelope, ciUploadShapefile, ciBoxAustralia, ciBoxWorld, ciBoxCurrentView;

    @Override
    public void afterCompose() {
        super.afterCompose();

    }

    public void onClick$btnOk(Event event) {
        String windowName = "";
        MapComposer mc = getMapComposer();

        String script = "";
        boolean overlapped = true;
        if (cbAreaSelection.getSelectedItem() == ciBoundingBox) {
            windowName = "WEB-INF/zul/AreaBoundingBox.zul";
            script = mc.getOpenLayersJavascript().addBoxDrawingTool();
        } else if (cbAreaSelection.getSelectedItem() == ciPolygon) {
            windowName = "WEB-INF/zul/AreaPolygon.zul";
            script = mc.getOpenLayersJavascript().addPolygonDrawingTool();
        } else if (cbAreaSelection.getSelectedItem() == ciPointAndRadius) {
            windowName = "WEB-INF/zul/AreaPointAndRadius.zul";
            script = mc.getOpenLayersJavascript().addRadiusDrawingTool();
        } else if (cbAreaSelection.getSelectedItem() == ciRegionSelection) {
            overlapped = false;
            windowName = "WEB-INF/zul/AreaRegionSelection.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciAddressRadiusSelection) {
            overlapped = false;
            windowName = "WEB-INF/zul/AreaAddressRadiusSelection.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciUploadShapefile) {
            windowName = "WEB-INF/zul/AreaUploadShapefile.zul";
            overlapped = false;
        } else if (cbAreaSelection.getSelectedItem() == ciUploadKML) {
            windowName = "WEB-INF/zul/AreaUploadKML.zul";
            overlapped = false;
        } else if (cbAreaSelection.getSelectedItem() == ciMapPolygon) {
            windowName = "WEB-INF/zul/AreaMapPolygon.zul";
            script = mc.getOpenLayersJavascript().addFeatureSelectionTool();
        } else if (cbAreaSelection.getSelectedItem() == ciEnvironmentalEnvelope) {
            windowName = "WEB-INF/zul/AreaEnvironmentalEnvelope.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciBoxAustralia) {
            String wkt = "POLYGON((112.0 -44.0,112.0 -9.0,154.0 -9.0,154.0 -44.0,112.0 -44.0))";
            String layerName = mc.getNextAreaLayerName("Australia Bounding Box");
            MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, layerName);
        } else if (cbAreaSelection.getSelectedItem() == ciBoxWorld) {
            String wkt = "POLYGON((-180 -90,-180 90.0,180.0 90.0,180.0 -90.0,-180.0 -90.0))";
            String layerName = mc.getNextAreaLayerName("World Bounding Box");
            MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, layerName);
        } else if (cbAreaSelection.getSelectedItem() == ciBoxCurrentView) {
            String wkt = mc.getMapComposer().getViewArea();
            String layerName = mc.getNextAreaLayerName("View Area");
            MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, layerName);
        } else if (cbAreaSelection.getSelectedItem() == ciWKT) {
            windowName = "WEB-INF/zul/AreaWKT.zul";
        }
        if (!windowName.contentEquals("")) {
            mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
            Window window = (Window) Executions.createComponents(windowName, getMapComposer(), null);
            try {
                if (overlapped) {
                    window.doOverlapped();
                } else {
                    window.doModal();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }
}
