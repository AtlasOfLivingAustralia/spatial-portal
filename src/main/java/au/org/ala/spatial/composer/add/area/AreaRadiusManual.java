package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.LayersUtil;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.value.BoundingBox;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Textbox;

/**
 * @author Adam
 */
public class AreaRadiusManual extends AreaToolComposer {

    private static final Logger LOGGER = Logger.getLogger(AreaRadiusManual.class);
    private Doublebox dLongitude;
    private Doublebox dLatitude;
    private Doublebox dRadius;
    private Textbox txtLayerName;
    private Button btnOk;
    private Button btnClear;
    private Textbox displayGeom;

    @Override
    public void afterCompose() {
        super.afterCompose();
        txtLayerName.setValue(getMapComposer().getNextAreaLayerName(CommonData.lang(StringConstants.DEFAULT_AREA_LAYER_NAME)));
        BoundingBox bb = getMapComposer().getLeftmenuSearchComposer().getViewportBoundingBox();
        dLongitude.setValue(Math.round((bb.getMinLongitude() + (bb.getMaxLongitude() - bb.getMinLongitude()) / 2.0) * 100) / 100.0);
        dLatitude.setValue(Math.round((bb.getMinLatitude() + (bb.getMaxLatitude() - bb.getMinLatitude()) / 2.0) * 100) / 100.0);
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
        if (!StringConstants.NONE.equalsIgnoreCase(wkt)) {
            try {
                MapComposer mc = getMapComposer();
                layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
                MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, txtLayerName.getValue());

                mapLayer.getMapLayerMetadata().setMoreInfo(LayersUtil.getMetadata(dRadius.getText() + "km radius around longitude " + dLongitude.getText() + ", latitude " + dLatitude.getText()));

                displayGeom.setText(wkt);
            } catch (Exception e) {
                LOGGER.error("Error adding WKT layer", e);
            }
        }
    }

    private boolean validate() {
        StringBuilder sb = new StringBuilder();

        //test longitude
        double longitude = dLongitude.getValue();
        if (longitude < -180 || longitude > 360) {
            sb.append("\n").append(CommonData.lang(StringConstants.ERROR_INVALID_LONGITUDE));
        }

        double latitude = dLatitude.getValue();
        if (latitude < -90 || latitude > 90) {
            sb.append("\n").append(CommonData.lang(StringConstants.ERROR_INVALID_LATITUDE));
        }

        double radius = dRadius.getValue();
        if (radius <= 0) {
            sb.append("\n").append(CommonData.lang(StringConstants.ERROR_INVALID_RADIUS));
        }

        if (sb.length() > 0) {
            getMapComposer().showMessage(sb.toString());
        }

        return sb.length() == 0;
    }
}
