package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.value.BoundingBox;
import org.ala.spatial.util.LayersUtil;
import org.ala.spatial.util.Util;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Textbox;

/**
 *
 * @author Adam
 */
public class AreaRadiusManual extends AreaToolComposer {

    Doublebox dLongitude;
    Doublebox dLatitude;
    Doublebox dRadius;
    private Textbox displayGeom;
    Textbox txtLayerName;
    Button btnOk;
    Button btnClear;

    @Override
    public void afterCompose() {
        super.afterCompose();
        txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));
        BoundingBox bb = getMapComposer().getLeftmenuSearchComposer().getViewportBoundingBox();
        dLongitude.setValue(Math.round((bb.getMinLongitude() + (bb.getMaxLongitude() - bb.getMinLongitude())/2) * 100) / 100.0);
        dLatitude.setValue(Math.round((bb.getMinLatitude() + (bb.getMaxLatitude() - bb.getMinLatitude())/2) * 100) / 100.0);
    }

    public void onClick$btnOk(Event event) {
        if (!validate()) {
            return;
        }
        createCircle();
        ok = true;
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void createCircle() {
        String wkt = Util.createCircleJs(dLongitude.getValue(), dLatitude.getValue(), dRadius.getValue() * 1000.0);
        if (wkt.contentEquals("none")) {
            return;
        } else {
            try {
                MapComposer mc = getMapComposer();
                layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
                MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, txtLayerName.getValue());

                MapLayerMetadata md = mapLayer.getMapLayerMetadata();
                if (md == null) {
                    md = new MapLayerMetadata();
                    mapLayer.setMapLayerMetadata(md);
                }
                md.setMoreInfo(LayersUtil.getMetadata(dRadius.getText() + "km radius around longitude " + dLongitude.getText() + ", latitude " + dLatitude.getText()));

                displayGeom.setText(wkt);
            } catch (Exception e) {
                logger.error("Error adding WKT layer");
            }
        }
    }

    private boolean validate() {
        StringBuilder sb = new StringBuilder();

        //test longitude
        double longitude = dLongitude.getValue();
        if (longitude < -180 || longitude > 360) {
            sb.append("\nLongitude must be between -180 and 180.");
        }

        double latitude = dLatitude.getValue();
        if (latitude < -90 || latitude > 90) {
            sb.append("\nLatitude must be between -90 and 90.");
        }

        double radius = dRadius.getValue();
        if (radius <= 0) {
            sb.append("\nRadius must be greater than 0.");
        }

        if (sb.length() > 0) {
            getMapComposer().showMessage(sb.toString());
        }

        return sb.length() == 0;
    }
}
