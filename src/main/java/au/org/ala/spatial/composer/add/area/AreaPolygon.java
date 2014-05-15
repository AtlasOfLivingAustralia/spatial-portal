package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.util.LayersUtil;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.javascript.OpenLayersJavascript;
import au.org.emii.portal.menu.MapLayer;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

/**
 * @author Adam
 */
public class AreaPolygon extends AreaToolComposer {

    private static Logger logger = Logger.getLogger(AreaPolygon.class);
    private Textbox displayGeom;
    Button btnNext;

    Label invalidWKT;
    Textbox txtLayerName;
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
        String script = mc.getOpenLayersJavascript().addPolygonDrawingTool();
        mc.getOpenLayersJavascript().execute(OpenLayersJavascript.iFrameReferences + script);
        displayGeom.setValue("");
        btnNext.setDisabled(true);
        btnClear.setDisabled(true);
        invalidWKT.setValue("");
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

            } /*else if (selectionGeom.startsWith("LAYER(")) {

                //get WKT from this feature
                String v = selectionGeom.replace("LAYER(", "");

                v = v.substring(0, v.length() - 1);

                wkt = Util.wktFromJSON(getMapComposer().getMapLayer(v).getGeoJSON());
                displayGeom.setValue(wkt);

            } */ else {
                wkt = selectionGeom;
                displayGeom.setValue(wkt);
            }

            //get the current MapComposer instance
            MapComposer mc = getMapComposer();

            //add feature to the map as a new layer
            if (wkt.length() > 0) {
                layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
                MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, txtLayerName.getValue());
                mapLayer.getMapLayerMetadata().setMoreInfo(LayersUtil.getMetadataForWKT("User drawn polygon", wkt));

                if (!validWKT(wkt)) {
                    btnNext.setDisabled(true);
                    btnClear.setDisabled(false);
                } else {
                    btnClear.setDisabled(false);
                    btnNext.setDisabled(false);
                }
            }

        } catch (Exception e) {
            logger.error("error mapping user polygon", e);
            btnNext.setDisabled(true);
            btnClear.setDisabled(false);
        }
    }

    public boolean validWKT(String wkt) {
        if (wkt.replaceAll(" ", "").isEmpty()) {
            invalidWKT.setValue("WKT is Invalid");
            return false;
        } else {
            invalidWKT.setValue("");
        }
        try {
            WKTReader wktReader = new WKTReader();
            com.vividsolutions.jts.geom.Geometry g = wktReader.read(wkt);
            //NC 20130319: Ensure that the WKT is valid according to the WKT standards.
            //logger.debug("GEOMETRY TYPE: " + g.getGeometryType());
            IsValidOp op = new IsValidOp(g);
            if (!op.isValid()) {
                invalidWKT.setValue("WKT is invalid. " + op.getValidationError().getMessage());
                logger.warn("WKT is invalid." + op.getValidationError().getMessage());
                //TODO Fix invalid WKT text using https://github.com/tudelft-gist/prepair maybe???
            } else if (g.isRectangle()) {
                //NC 20130319: When the shape is a rectangle ensure that the points a specified in the correct order.
                //get the new WKT for the rectangle will possibly need to change the order.

                com.vividsolutions.jts.geom.Envelope envelope = g.getEnvelopeInternal();
                String wkt2 = "POLYGON(("
                        + envelope.getMinX() + " " + envelope.getMinY() + ","
                        + envelope.getMaxX() + " " + envelope.getMinY() + ","
                        + envelope.getMaxX() + " " + envelope.getMaxY() + ","
                        + envelope.getMinX() + " " + envelope.getMaxY() + ","
                        + envelope.getMinX() + " " + envelope.getMinY() + "))";
                if (!wkt.equals(wkt2)) {
                    logger.debug("NEW WKT for Rectangle: " + wkt);
                    invalidWKT.setValue("WKT for Rectangle is in incorrect order. We have automatically fixed this. Press next to accept this value.");
                    displayGeom.setValue(wkt2);
                    return false;
                }

            }
            return op.isValid();
        } catch (ParseException parseException) {
            invalidWKT.setValue("WKT is Invalid. " + parseException.getMessage());
            return false;
        }
    }
}
