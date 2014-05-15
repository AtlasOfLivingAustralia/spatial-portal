package au.org.ala.spatial.composer.add.area;

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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author Adam
 */
public class AreaWKT extends AreaToolComposer {

    private static Logger logger = Logger.getLogger(AreaWKT.class);

    private Textbox displayGeom;
    Textbox txtLayerName;
    Label invalidWKT;
    Button btnOk;
    Button btnClear;

    @Override
    public void afterCompose() {
        super.afterCompose();
        txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));
    }

    public void onClick$btnOk(Event event) {
        String wkt = displayGeom.getText();
        if (validWKT(wkt)) {

            MapComposer mc = getMapComposer();

            layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
            MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, txtLayerName.getValue());

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());

            String metadata = "";
            metadata += "User pasted WKT \n";
            metadata += "Name: " + layerName + " <br />\n";
            metadata += "Date: " + formatter.format(calendar.getTime()) + " <br />\n";

            mapLayer.getMapLayerMetadata().setMoreInfo(metadata);

            //reapply layer name
            getMapComposer().getMapLayer(layerName).setDisplayName(txtLayerName.getValue());
            getMapComposer().redrawLayersList();

            ok = true;

            this.detach();
        } else {
            invalidWKT.setVisible(true);
        }
    }

    public boolean validWKT(String wkt) {
        if (wkt.replaceAll(" ", "").isEmpty()) {
            invalidWKT.setValue("WKT is Invalid");
            return false;
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

    public void onClick$btnClear(Event event) {
        MapComposer mc = getMapComposer();
        if (layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        String script = mc.getOpenLayersJavascript().addBoxDrawingTool();
        mc.getOpenLayersJavascript().execute(OpenLayersJavascript.iFrameReferences + script);
        displayGeom.setValue("");

        invalidWKT.setVisible(false);
    }

    public void onClick$btnCancel(Event event) {
        MapComposer mc = getMapComposer();
        if (layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        this.detach();
    }

}
