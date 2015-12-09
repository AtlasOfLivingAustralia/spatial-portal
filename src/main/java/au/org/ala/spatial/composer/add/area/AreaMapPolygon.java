package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.gazetteer.GazetteerPointSearch;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;

import java.util.List;
import java.util.Map;

/**
 * @author Adam
 */
public class AreaMapPolygon extends AreaToolComposer {

    private static final Logger LOGGER = Logger.getLogger(AreaMapPolygon.class);
    private Textbox txtLayerName;
    private Button btnOk;
    private Button btnClear;
    private Radiogroup rgPolygonLayers;
    private Textbox displayGeom;

    @Override
    public void afterCompose() {
        super.afterCompose();
        txtLayerName.setValue(getMapComposer().getNextAreaLayerName(CommonData.lang(StringConstants.DEFAULT_AREA_LAYER_NAME)));
        btnOk.setDisabled(true);
        btnClear.setDisabled(true);
        loadLayerSelection();
        Clients.evalJavaScript("mapFrame.toggleClickHandler(false);");
    }

    public void onClick$btnOk(Event event) {
        MapLayer ml = getMapComposer().getMapLayer(layerName);
        ml.setDisplayName(txtLayerName.getValue());
        getMapComposer().redrawLayersList();
        ok = true;
        Clients.evalJavaScript("mapFrame.toggleClickHandler(true);");

        String activeLayerName = StringConstants.NONE;
        if (ml.getUri() != null) {
            activeLayerName = ml.getUri().replaceAll("^.*&style=", "").replaceAll("&.*", "").replaceAll("_style", "");
        }
        getMapComposer().setAttribute("activeLayerName", activeLayerName);

        try {
            if (rgPolygonLayers.getSelectedItem() == null) {
                if (rgPolygonLayers.getItemCount() > 0) {
                    getMapComposer().setAttribute("mappolygonlayer", rgPolygonLayers.getItemAtIndex(0).getValue());
                }
            } else {
                getMapComposer().setAttribute("mappolygonlayer", rgPolygonLayers.getSelectedItem().getValue());
            }
        } catch (Exception e) {
            LOGGER.error("failed to set map area polygon selected by the radio button selection", e);
        }

        this.detach();
    }

    public void onClick$btnClear(Event event) {
        MapComposer mc = getMapComposer();
        if (layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        String script = mc.getOpenLayersJavascript().addFeatureSelectionTool();
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().getIFrameReferences() + script);
        displayGeom.setValue("");
        btnOk.setDisabled(true);
        btnClear.setDisabled(true);
    }

    public void onClick$btnCancel(Event event) {
        MapComposer mc = getMapComposer();
        if (layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        Clients.evalJavaScript("mapFrame.toggleClickHandler(true);");
        this.detach();
    }

    public void onCheck$rgPolygonLayers(Event event) {

        Radio selectedItem = rgPolygonLayers.getSelectedItem();

        //Add and remove layer to set as top layer
        String layerName = selectedItem.getValue();
        MapComposer mc = getMapComposer();
        MapLayer ml = mc.getMapLayer(layerName);
        mc.removeLayer(layerName);
        mc.activateLayer(ml, true);

    }

    /**
     * Searches the gazetter at a given point and then maps the polygon feature
     * found at the location (for the current top contextual layer).
     *
     * @param event
     */
    public void onSearchPoint(Event event) {
        String searchPoint = (String) event.getData();
        String lon = searchPoint.split(",")[0];
        String lat = searchPoint.split(",")[1];

        LOGGER.debug("*************************************");
        LOGGER.debug("CommonData.getLayerList");
        LOGGER.debug("*************************************");

        JSONArray layerlist = CommonData.getLayerListJSONArray();
        MapComposer mc = getMapComposer();

        List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
        Boolean searchComplete = false;
        for (int i = 0; i < activeLayers.size(); i++) {
            MapLayer ml = activeLayers.get(i);

            String activeLayerName = StringConstants.NONE;
            if (ml.getUri() != null) {
                activeLayerName = ml.getUri().replaceAll("^.*&style=", "").replaceAll("&.*", "").replaceAll("_style", "");
            }
            LOGGER.debug("ACTIVE LAYER: " + activeLayerName);
            if (ml.isDisplayed() && ml.isContextualLayer()) {
                for (int j = 0; j < layerlist.size() && !searchComplete; j++) {
                    JSONObject field = (JSONObject) layerlist.get(j);
                    JSONObject layer = (JSONObject) field.get("layer");
                    if (layer.get(StringConstants.TYPE) != null
                            && layer.get(StringConstants.TYPE).toString().length() > 0
                            && StringConstants.CONTEXTUAL.equalsIgnoreCase(layer.get(StringConstants.TYPE).toString())
                            && field.get(StringConstants.ID).toString().equalsIgnoreCase(activeLayerName)) {

                        LOGGER.debug(ml.getName());
                        Map<String, String> feature = GazetteerPointSearch.pointSearch(lon, lat, activeLayerName, CommonData.getGeoServer());
                        if (feature == null || !feature.containsKey(StringConstants.PID)) {
                            continue;
                        }

                        layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
                        getMapComposer().addObjectByPid(feature.get(StringConstants.PID), layerName, 1);

                        //found the object on the layer
                        btnOk.setDisabled(false);
                        btnClear.setDisabled(false);

                        mc.updateLayerControls();

                        searchComplete = true;
                        displayGeom.setValue("layer: " + feature.get(StringConstants.LAYERNAME) + "\r\n" + "area: " + feature.get(StringConstants.VALUE));

                        return;
                    }
                }
            }
        }
    }

    public void loadLayerSelection() {
        try {

            Radio rSelectedLayer = (Radio) getFellowIfAny("rSelectedLayer");

            List<MapLayer> layers = getMapComposer().getContextualLayers();

            if (!layers.isEmpty()) {

                for (int i = 0; i < layers.size(); i++) {

                    MapLayer lyr = layers.get(i);
                    Radio rAr = new Radio(lyr.getDisplayName());
                    rAr.setId(lyr.getDisplayName().replaceAll(" ", ""));
                    rAr.setValue(lyr.getDisplayName());
                    rAr.setParent(rgPolygonLayers);

                    if (i == 0) {
                        rAr.setSelected(true);
                    }
                    rgPolygonLayers.insertBefore(rAr, rSelectedLayer);
                }
                rSelectedLayer.setSelected(true);
            }
        } catch (Exception e) {
        }
    }
}
