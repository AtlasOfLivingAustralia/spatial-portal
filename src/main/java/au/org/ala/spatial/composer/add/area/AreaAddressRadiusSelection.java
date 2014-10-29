package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.LayersUtil;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import java.net.URL;
import java.net.URLEncoder;

/**
 * @author Adam
 */
public class AreaAddressRadiusSelection extends AreaToolComposer {

    private static final Logger LOGGER = Logger.getLogger(AreaAddressRadiusSelection.class);
    private Doublebox dRadius;
    private Label addressLabel;
    private Textbox txtLayerName;
    private Button btnOk;
    private Button btnClear;
    private Textbox addressBox;
    private Textbox displayGeom;
    private double longitude;
    private double latitude;

    @Override
    public void afterCompose() {
        super.afterCompose();
        dRadius.setDisabled(true);
        btnOk.setDisabled(true);
        txtLayerName.setValue(getMapComposer().getNextAreaLayerName(CommonData.lang(StringConstants.DEFAULT_AREA_LAYER_NAME)));
    }

    public void onClick$btnOk(Event event) {
        if (!validate()) {
            return;
        }
        createRadiusFromAddress();
        ok = true;
        this.detach();
    }

    public void onClick$btnCreate(Event event) {
        createRadiusFromAddress();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void createRadiusFromAddress() {
        String wkt = radiusFromAddress(addressBox.getText());
        if (!StringConstants.NONE.equalsIgnoreCase(wkt)) {
            try {
                MapComposer mc = getMapComposer();
                layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
                MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, txtLayerName.getValue());

                mapLayer.getMapLayerMetadata()
                        .setMoreInfo(LayersUtil
                                .getMetadata(dRadius.getText() + "km radius around " + addressLabel.getValue() + " (" + longitude + ", " + latitude + ")"));

                displayGeom.setText(wkt);
            } catch (Exception e) {
                LOGGER.error("Error adding WKT layer");
            }
        }
    }

    public String findAddressLine(String text) throws Exception {
        String url = StringConstants.GOOGLE_ADDRESS_LINE + URLEncoder.encode(text, StringConstants.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(new URL(url).openStream());
        return node.get(StringConstants.RESULTS).get(0).get(StringConstants.FORMATTED_ADDRESS).getTextValue();
    }

    public double[] findAddressLatLng(String text) throws Exception {
        String url = StringConstants.GOOGLE_ADDRESS_LINE + URLEncoder.encode(text, StringConstants.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(new URL(url).openStream());
        JsonNode latLngNode = node.get(StringConstants.RESULTS).get(0).get(StringConstants.GEOMETRY).get(StringConstants.LOCATION);
        return new double[]{
                latLngNode.get("lat").getDoubleValue(),
                latLngNode.get("lng").getDoubleValue()
        };
    }

    public void onClick$btnFindAddress(Event event) {
        String address = "";
        try {
            address = findAddressLine(addressBox.getText());
            addressLabel.setValue(address);
            dRadius.setDisabled(false);
            btnOk.setDisabled(false);
        } catch (Exception e) {
            LOGGER.error("error finding address: " + address, e);
        }
    }

    private String radiusFromAddress(String address) {
        try {
            double[] latlng = findAddressLatLng(address);
            double radius = dRadius.getValue() * 1000.0;
            return Util.createCircleJs(latlng[1], latlng[0], radius);
        } catch (Exception ge) {
            return StringConstants.NONE;
        }
    }

    private boolean validate() {
        StringBuilder sb = new StringBuilder();

        double radius = dRadius.getValue();
        if (radius <= 0) {
            sb.append("\n").append(CommonData.lang(StringConstants.ERROR_MESSAGE_RADIUS_0));
        }

        if (sb.length() > 0) {
            getMapComposer().showMessage(sb.toString());
        }

        return sb.length() == 0;
    }
}
