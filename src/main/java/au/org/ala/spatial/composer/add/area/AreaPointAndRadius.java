package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.util.LayersUtil;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.javascript.OpenLayersJavascript;
import au.org.emii.portal.menu.MapLayer;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Textbox;

/**
 * @author Adam
 */
public class AreaPointAndRadius extends AreaToolComposer {

    private static Logger logger = Logger.getLogger(AreaPointAndRadius.class);
    private Textbox displayGeom;
    //String layerName;
    Textbox txtLayerName;
    Button btnNext;
    Button btnClear;

    @Override
    public void afterCompose() {
        super.afterCompose();

        txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));
    }

    public void onClick$btnNext(Event event) {
        //reapply layer name
        getMapComposer().getMapLayer(layerName).setDisplayName(txtLayerName.getValue());
        getMapComposer().redrawLayersList();

        ok = true;

        this.detach();
    }

    public void onClick$btnClear(Event event) {
        MapComposer mc = getMapComposer();
        if (layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        String script = mc.getOpenLayersJavascript().addRadiusDrawingTool();
        mc.getOpenLayersJavascript().execute(OpenLayersJavascript.iFrameReferences + script);
        displayGeom.setText("");
        btnNext.setDisabled(true);
        btnClear.setDisabled(true);
    }

    public void onClick$btnCancel(Event event) {
        MapComposer mc = getMapComposer();
        if (layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        this.detach();
    }

    /**
     * @param event
     */
    public void onSelectionGeom(Event event) {
        String selectionGeom = (String) event.getData();

        try {

            String wkt = "";
            if (selectionGeom.contains("NaN NaN")) {
                displayGeom.setValue("");
                //  lastTool = null;
            } else if (selectionGeom.startsWith("LAYER(")) {
                //get WKT from this feature
                String v = selectionGeom.replace("LAYER(", "");
                v = v.substring(0, v.length() - 1);

                //for display
                wkt = Util.wktFromJSON(getMapComposer().getMapLayer(v).getGeoJSON());
                displayGeom.setValue(wkt);
            } else {
                wkt = selectionGeom;
                displayGeom.setValue(wkt);
            }

            //get the current MapComposer instance
            MapComposer mc = getMapComposer();

            //add feature to the map as a new layer
            if (wkt.length() > 0) {
                layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
                MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, txtLayerName.getValue());

                mapLayer.getMapLayerMetadata().setMoreInfo(LayersUtil.getMetadataForWKT("User drawn point and radius", wkt));
            }

            btnNext.setDisabled(false);
            btnClear.setDisabled(false);

        } catch (Exception e) {
            logger.error("error mapping point and radius", e);
        }
    }
}
