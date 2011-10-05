package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTWriter;
import geo.google.GeoAddressStandardizer;
import geo.google.datamodel.GeoAddress;
import geo.google.datamodel.GeoCoordinate;
import java.util.List;
import javax.measure.converter.UnitConverter;
import javax.measure.unit.Unit;
import org.ala.spatial.util.LayersUtil;
import org.ala.spatial.util.Util;
import org.geotools.factory.Hints;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

/**
 *
 * @author Adam
 */
public class AreaAddressRadiusSelection extends AreaToolComposer {

    private Textbox addressBox;
    Doublebox dRadius;
    Label addressLabel;
    private Textbox displayGeom;
    //String layerName;
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

                MapLayerMetadata md = mapLayer.getMapLayerMetadata();
                if (md == null) {
                    md = new MapLayerMetadata();
                    mapLayer.setMapLayerMetadata(md);
                }
                md.setMoreInfo(LayersUtil.getMetadata(dRadius.getText() + "km radius around " + addressLabel.getValue() + " (" + longitude + ", " + latitude + ")"));

                displayGeom.setText(wkt);
            } catch (Exception e) {
                logger.error("Error adding WKT layer");
            }
        }
    }

    public void onClick$btnFindAddress(Event event) {
        try {
            GeoAddressStandardizer st = new GeoAddressStandardizer("AABBCC");

            List<GeoAddress> addresses = st.standardizeToGeoAddresses(addressBox.getText() + ", Australia");

            addressLabel.setValue(addresses.get(0).getAddressLine());
            dRadius.setDisabled(false);
            btnOk.setDisabled(false);
        } catch (geo.google.GeoException ge) {
            ge.printStackTrace();
        }
    }

    private String radiusFromAddress(String address) {
        try {

            GeoAddressStandardizer st = new GeoAddressStandardizer("AABBCC");

            List<GeoAddress> addresses = st.standardizeToGeoAddresses(address + ", Australia");

            GeoCoordinate gco = addresses.get(0).getCoordinate();

            double radius = dRadius.getValue() * 1000.0;
            longitude = gco.getLongitude();
            latitude = gco.getLatitude();
            //return createCircle(gco.getLongitude(), gco.getLatitude(), radius);
            return Util.createCircleJs(gco.getLongitude(), gco.getLatitude(), radius);

        } catch (geo.google.GeoException ge) {
            return "none";
        }
    }

    private String createCircle(double x, double y, final double RADIUS) {
        return createCircle(x, y, RADIUS, 50);

    }

    private String createCircle(double x, double y, final double RADIUS, int sides) {

        try {
            Hints hints = new Hints(Hints.CRS, DefaultGeographicCRS.WGS84);
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(hints);

            Point point = geometryFactory.createPoint(new Coordinate(x, y));

            Polygon polygon = bufferInKm(point, RADIUS / 1000);

            WKTWriter writer = new WKTWriter();
            String wkt = writer.write(polygon);

            return wkt.replaceAll("POLYGON ", "POLYGON").replaceAll(", ", ",");

        } catch (Exception e) {
            System.out.println("Circle fail!");
            e.printStackTrace();
            return "none";
        }
    }

    public Polygon bufferInKm(Point p, Double radiusKm) {
        Hints hints = new Hints(Hints.CRS, DefaultGeographicCRS.WGS84);
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(hints);
        GeodeticCalculator c = new GeodeticCalculator();
        c.setStartingGeographicPoint(p.getX(), p.getY());
        Unit u = c.getEllipsoid().getAxisUnit();
        Unit km = Unit.valueOf("km");

        Coordinate coords[] = new Coordinate[361];
        for (int i = 0; i < 360; i++) {
            UnitConverter converter = km.getConverterTo(u);
            double converted = converter.convert(radiusKm);

            c.setDirection(i - 180, converted);

            java.awt.geom.Point2D boundaryPoint = c.getDestinationGeographicPoint();

            coords[i] = new Coordinate(boundaryPoint.getX(), boundaryPoint.getY());
        }
        coords[360] = coords[0];
        LinearRing ring = geometryFactory.createLinearRing(coords);
        Polygon polygon = geometryFactory.createPolygon(ring, null);
        return polygon;

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
