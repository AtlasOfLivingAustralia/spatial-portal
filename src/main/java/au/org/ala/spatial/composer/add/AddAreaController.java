package au.org.ala.spatial.composer.add;


import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.tool.ToolComposer;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.LayersUtil;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Window;

import java.util.List;
import java.util.Map;

/**
 * @author ajay
 */
public class AddAreaController extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(AddAreaController.class);

    private RemoteLogger remoteLogger;
    private Radiogroup cbAreaSelection;
    private Radio ciWKT, ciUploadKML, ciRegionSelection, ciBoundingBox, ciPolygon, ciPointAndRadius, ciAddressRadiusSelection, ciMapPolygon, ciEnvironmentalEnvelope, ciUploadShapefile, ciBoxAustralia, ciBoxWorld, ciBoxCurrentView, ciRadiusManualSelection, ciMergeAreas;
    private Button btnOk;
    private Map args;

    @Override
    public void afterCompose() {
        super.afterCompose();
        args = Executions.getCurrent().getArg();
    }

    public void onClick$btnOk(Event event) {
        if (btnOk.isDisabled()) {
            return;
        }
        String windowName = "";
        MapComposer mc = getMapComposer();

        String script = "";
        boolean overlapped = true;
        if (cbAreaSelection.getSelectedItem() == ciBoundingBox) {
            windowName = "WEB-INF/zul/add/area/AreaBoundingBox.zul";
            script = mc.getOpenLayersJavascript().addBoxDrawingTool();
        } else if (cbAreaSelection.getSelectedItem() == ciPolygon) {
            windowName = "WEB-INF/zul/add/area/AreaPolygon.zul";
            script = mc.getOpenLayersJavascript().addPolygonDrawingTool();
        } else if (cbAreaSelection.getSelectedItem() == ciPointAndRadius) {
            windowName = "WEB-INF/zul/add/area/AreaPointAndRadius.zul";
            script = mc.getOpenLayersJavascript().addRadiusDrawingTool();
        } else if (cbAreaSelection.getSelectedItem() == ciRegionSelection) {
            overlapped = false;
            windowName = "WEB-INF/zul/add/area/AreaRegionSelection.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciAddressRadiusSelection) {
            overlapped = false;
            windowName = "WEB-INF/zul/add/area/AreaAddressRadiusSelection.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciRadiusManualSelection) {
            overlapped = false;
            windowName = "WEB-INF/zul/add/area/AreaRadiusManualSelection.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciUploadShapefile) {
            windowName = "WEB-INF/zul/add/area/AreaUploadShapefile.zul";
            overlapped = false;
        } else if (cbAreaSelection.getSelectedItem() == ciUploadKML) {
            windowName = "WEB-INF/zul/add/area/AreaUploadKML.zul";
            overlapped = false;
        } else if (cbAreaSelection.getSelectedItem() == ciMapPolygon) {
            List<MapLayer> layers = getMapComposer().getContextualLayers();
            boolean visibleLayers = false;
            for (MapLayer ml : layers) {
                if (ml.isDisplayed()) {
                    visibleLayers = true;
                    break;
                }
            }
            if (!visibleLayers) {
                //present layer selection window
                windowName = "WEB-INF/zul/layer/ContextualLayerSelection.zul";
                overlapped = false;
            } else {
                windowName = "WEB-INF/zul/add/area/AreaMapPolygon.zul";
                script = mc.getOpenLayersJavascript().addFeatureSelectionTool();
            }
        } else if (cbAreaSelection.getSelectedItem() == ciEnvironmentalEnvelope) {
            windowName = "WEB-INF/zul/add/area/AreaEnvironmentalEnvelope.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciBoxAustralia) {
            String wkt = CommonData.getSettings().getProperty(CommonData.AUSTRALIA_WKT);
            String layerName = mc.getNextAreaLayerName(CommonData.lang(StringConstants.AUSTRALIA_BOUNDING_BOX));
            MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, layerName);
            mapLayer.getMapLayerMetadata().setMoreInfo(LayersUtil.getMetadata(CommonData.lang(StringConstants.AUSTRALIA_BOUNDING_BOX) + " " + wkt));
            remoteLogger.logMapArea(layerName, "Area - BoxAustralia", wkt);
        } else if (cbAreaSelection.getSelectedItem() == ciBoxWorld) {
            String wkt = CommonData.WORLD_WKT;
            String layerName = mc.getNextAreaLayerName(CommonData.lang(StringConstants.WORLD_BOUNDING_BOX));
            MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, layerName);
            mapLayer.getMapLayerMetadata().setMoreInfo(LayersUtil.getMetadata(CommonData.lang(StringConstants.WORLD_BOUNDING_BOX) + " " + wkt));
            remoteLogger.logMapArea(layerName, "Area - BoxWorld", wkt);
        } else if (cbAreaSelection.getSelectedItem() == ciBoxCurrentView) {
            String wkt = mc.getMapComposer().getViewArea();
            String layerName = mc.getNextAreaLayerName(CommonData.lang(StringConstants.CURRENT_VIEW_AREA));
            MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, layerName);
            mapLayer.getMapLayerMetadata().setMoreInfo(LayersUtil.getMetadata(CommonData.lang(StringConstants.CURRENT_VIEW_AREA) + " " + wkt));
            remoteLogger.logMapArea(layerName, "Area - BoxCurrentView", wkt);
        } else if (cbAreaSelection.getSelectedItem() == ciWKT) {
            windowName = "WEB-INF/zul/add/area/AreaWKT.zul";
        } else if (cbAreaSelection.getSelectedItem() == ciMergeAreas) {
            windowName = "WEB-INF/zul/add/area/AreaMerge.zul";
            overlapped = false;
        }
        if (!windowName.isEmpty()) {
            mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().getIFrameReferences() + script);
            Window window = (Window) Executions.createComponents(windowName, this.getParent(), args);
            try {
                if (overlapped) {
                    window.doOverlapped();
                } else {
                    window.setParent(this.getParent());
                    window.doModal();
                }
            } catch (Exception e) {
                LOGGER.error("error opening window: " + windowName, e);
            }
        } else if (StringConstants.ADDTOOLWINDOW.equals(getParent().getId())) {
            ToolComposer analysisParent = (ToolComposer) this.getParent();
            analysisParent.resetWindow(getMapComposer().getNextAreaLayerName(StringConstants.MY_AREA));
        } else if (StringConstants.ADDFACETWINDOW.equals(getParent().getId())) {
            AddFacetController analysisParent = (AddFacetController) this.getParent();
            analysisParent.resetWindow(getMapComposer().getNextAreaLayerName(StringConstants.MY_AREA));
        }

        if (cbAreaSelection.getSelectedItem() != null) {
            mc.setAttribute(StringConstants.ADDAREAWINDOW, cbAreaSelection.getSelectedItem().getId());
        }
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        if (StringConstants.ADDTOOLWINDOW.equals(getParent().getId())) {
            ToolComposer analysisParent = (ToolComposer) this.getParent();
            analysisParent.resetWindow(null);
        }

        this.detach();
    }
}
