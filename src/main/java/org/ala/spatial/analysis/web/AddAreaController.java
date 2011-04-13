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
    Radio ciBoundingBox, ciPolygon, ciPointAndRadius, ciAddressRadiusSelection, ciMapPolygon, ciEnvironmentalEnvelope, ciUploadShapefile, ciBoxAustralia, ciBoxWorld, ciBoxCurrentView;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void onClick$btnOk(Event event) {
        String windowName = "";
        MapComposer mc = getMapComposer();

	String script = "";
        if (cbAreaSelection.getSelectedItem() == ciBoundingBox) {
            windowName = "WEB-INF/zul/AreaBoundingBox.zul";
	    script = mc.getOpenLayersJavascript().addBoxDrawingTool();
        } else if (cbAreaSelection.getSelectedItem() == ciPolygon) {
            windowName = "WEB-INF/zul/AreaPolygon.zul";
	    script = mc.getOpenLayersJavascript().addPolygonDrawingTool();
        } else if (cbAreaSelection.getSelectedItem() == ciPointAndRadius) {
            windowName = "WEB-INF/zul/AreaPointAndRadius.zul";
	    script = mc.getOpenLayersJavascript().addRadiusDrawingTool();
        } else if (cbAreaSelection.getSelectedItem() == ciAddressRadiusSelection) {
            windowName = "WEB-INF/zul/AreaAddressRadiusSelection.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciUploadShapefile) {
           windowName = "WEB-INF/zul/AreaUploadShapefile.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciMapPolygon) {
            windowName = "WEB-INF/zul/AreaMapPolygon.zul";
            script = mc.getOpenLayersJavascript().addFeatureSelectionTool();
        } else if (cbAreaSelection.getSelectedItem() == ciEnvironmentalEnvelope) {
            windowName = "WEB-INF/zul/AreaEnvironmentalEnvelope.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciBoxAustralia) {
            String wkt = "POLYGON((112.0 -44.0,112.0 -9.0,154.0 -9.0,154.0 -44.0,112.0 -44.0))";
            MapLayer mapLayer = mc.addWKTLayer(wkt, "Active Area");
        } else if (cbAreaSelection.getSelectedItem() == ciBoxWorld) {
            String wkt = "POLYGON((-180 -90,-180 90.0,180.0 90.0,180.0 -90.0,-180.0 -90.0))";
            MapLayer mapLayer = mc.addWKTLayer(wkt, "Active Area");
        } else if (cbAreaSelection.getSelectedItem() == ciBoxCurrentView) {
            
        }
        if (!windowName.contentEquals("")) {
            mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
            Window window = (Window) Executions.createComponents(windowName, getMapComposer(), null);
            try {
                window.doOverlapped();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

     /**
     * Gets the main pages controller so we can add a
     * drawing tool to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }
}
