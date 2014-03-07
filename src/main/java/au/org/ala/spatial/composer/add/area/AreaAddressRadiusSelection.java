package au.org.ala.spatial.composer.add.area;

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

    private static Logger logger = Logger.getLogger(AreaAddressRadiusSelection.class);

    private Textbox addressBox;
    Doublebox dRadius;
    Label addressLabel;
    private Textbox displayGeom;
    Textbox txtLayerName;
    Button btnOk;
    Button btnClear;
    private double longitude;
    private double latitude;

    @Override
    public void afterCompose() {
        super.afterCompose();
        dRadius.setDisabled(true);
        btnOk.setDisabled(true);
        txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));
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
        MapComposer mc = getMapComposer();
        this.detach();
    }

    public void createRadiusFromAddress() {
        String wkt = radiusFromAddress(addressBox.getText());
        if (wkt.contentEquals("none")) {
            return;
        } else {
            try {
                MapComposer mc = getMapComposer();
                layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
                MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, txtLayerName.getValue());

                mapLayer.getMapLayerMetadata()
                        .setMoreInfo(LayersUtil
                                .getMetadata(dRadius.getText() + "km radius around " + addressLabel.getValue() + " (" + longitude + ", " + latitude + ")"));

                displayGeom.setText(wkt);
            } catch (Exception e) {
                logger.error("Error adding WKT layer");
            }
        }
    }

    public String findAddressLine(String text) throws Exception {
        String url = "http://maps.google.com/maps/api/geocode/json?components=locality&sensor=false&address=" + URLEncoder.encode(text, "UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(new URL(url));
        return node.get("results").get(0).get("formatted_address").getTextValue();
    }

    public double[] findAddressLatLng(String text) throws Exception {
        String url = "http://maps.google.com/maps/api/geocode/json?components=locality&sensor=false&address=" + URLEncoder.encode(text, "UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(new URL(url));
        JsonNode latLngNode = node.get("results").get(0).get("geometry").get("location");
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
            logger.error("error finding address: " + address, e);
        }
    }

    private String radiusFromAddress(String address) {
        try {
            double[] latlng = findAddressLatLng(address);
            double radius = dRadius.getValue() * 1000.0;
            return Util.createCircleJs(latlng[1], latlng[0], radius);
        } catch (Exception ge) {
            return "none";
        }
    }

    private boolean validate() {
        StringBuilder sb = new StringBuilder();

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
