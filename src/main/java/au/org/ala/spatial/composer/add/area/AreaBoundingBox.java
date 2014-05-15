package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.util.LayersUtil;
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
public class AreaBoundingBox extends AreaToolComposer {

    private static Logger logger = Logger.getLogger(AreaBoundingBox.class);
    private Textbox displayGeom;
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
        String script = mc.getOpenLayersJavascript().addBoxDrawingTool();
        mc.getOpenLayersJavascript().execute(OpenLayersJavascript.iFrameReferences + script);
        displayGeom.setValue("");
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

    public void onBoxGeom(Event event) {
        String boxGeom = (String) event.getData();
        try {

            if (boxGeom.contains("NaN NaN")) {
                displayGeom.setValue("");
            } else {
                displayGeom.setValue(boxGeom);
            }

            //get the current MapComposer instance
            MapComposer mc = getMapComposer();

            //add feature to the map as a new layer
            layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
            MapLayer mapLayer = mc.addWKTLayer(boxGeom, layerName, txtLayerName.getValue());
            mapLayer.getMapLayerMetadata().setMoreInfo(LayersUtil.getMetadataForWKT("User drawn bounding box", boxGeom));

            btnNext.setDisabled(false);
            btnClear.setDisabled(false);
        } catch (Exception e) {
            logger.error("Error adding user bounding box area", e);
        }

    }
}
