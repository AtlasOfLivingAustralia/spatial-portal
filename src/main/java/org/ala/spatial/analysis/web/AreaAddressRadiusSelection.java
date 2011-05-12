package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTWriter;
import geo.google.GeoAddressStandardizer;
import geo.google.datamodel.GeoAddress;
import geo.google.datamodel.GeoCoordinate;
import java.util.List;
import org.ala.spatial.util.LayersUtil;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

/**
 *
 * @author Adam
 */
public class AreaAddressRadiusSelection extends AreaToolComposer {

    private Textbox addressBox;
    Combobox cbRadius;
    Comboitem ci1km;
    Comboitem ci5km;
    Comboitem ci10km;
    Comboitem ci20km;
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
        cbRadius.setReadonly(true);
        cbRadius.setDisabled(true);
        cbRadius.setSelectedItem(ci1km);
        btnOk.setDisabled(true);
        txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));
    }

    public void onClick$btnOk(Event event) {
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
                md.setMoreInfo(LayersUtil.getMetadata(cbRadius.getText() + " radius around " + addressLabel.getValue() + " (" + longitude + ", " + latitude + ")"));

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
            cbRadius.setDisabled(false);
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

            double radius = 1000;
            if (cbRadius.getSelectedItem() == ci1km) {
                radius = 1000;
            }
            if (cbRadius.getSelectedItem() == ci5km) {
                radius = 5000;
            }
            if (cbRadius.getSelectedItem() == ci10km) {
                radius = 10000;
            }
            if (cbRadius.getSelectedItem() == ci20km) {
                radius = 20000;
            }
            longitude = gco.getLongitude();
            latitude = gco.getLatitude();
            return createCircle(gco.getLongitude(), gco.getLatitude(), radius);

        } catch (geo.google.GeoException ge) {
            return "none";
        }
    }

    private String createCircle(double x, double y, final double RADIUS) {
        return createCircle(x, y, RADIUS, 50);

    }

    private String createCircle(double x, double y, final double RADIUS, int sides) {

        try {
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

            String wkt4326 = "GEOGCS[" + "\"WGS 84\"," + "  DATUM[" + "    \"WGS_1984\","
                    + "    SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],"
                    + "    TOWGS84[0,0,0,0,0,0,0]," + "    AUTHORITY[\"EPSG\",\"6326\"]],"
                    + "  PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],"
                    + "  UNIT[\"DMSH\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9108\"]],"
                    + "  AXIS[\"Lat\",NORTH]," + "  AXIS[\"Long\",EAST],"
                    + "  AUTHORITY[\"EPSG\",\"4326\"]]";
            String wkt900913 = "PROJCS[\"WGS84 / Google Mercator\", "
                    + "  GEOGCS[\"WGS 84\", "
                    + "   DATUM[\"World Geodetic System 1984\", "
                    + "   SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], "
                    + "  AUTHORITY[\"EPSG\",\"6326\"]], "
                    + " PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], "
                    + " UNIT[\"degree\", 0.017453292519943295], "
                    + " AXIS[\"Longitude\", EAST], "
                    + " AXIS[\"Latitude\", NORTH], "
                    + " AUTHORITY[\"EPSG\",\"4326\"]], "
                    + " PROJECTION[\"Mercator_1SP\"], "
                    + " PARAMETER[\"semi_minor\", 6378137.0], "
                    + " PARAMETER[\"latitude_of_origin\", 0.0],"
                    + " PARAMETER[\"central_meridian\", 0.0], "
                    + " PARAMETER[\"scale_factor\", 1.0], "
                    + " PARAMETER[\"false_easting\", 0.0], "
                    + " PARAMETER[\"false_northing\", 0.0], "
                    + " UNIT[\"m\", 1.0], "
                    + " AXIS[\"x\", EAST], "
                    + " AXIS[\"y\", NORTH], "
                    + " AUTHORITY[\"EPSG\",\"900913\"]] ";
            CoordinateReferenceSystem wgsCRS = CRS.parseWKT(wkt4326);
            CoordinateReferenceSystem googleCRS = CRS.parseWKT(wkt900913);
            MathTransform transform = CRS.findMathTransform(wgsCRS, googleCRS);
            Point point = geometryFactory.createPoint(new Coordinate(y, x));
            Geometry geom = JTS.transform(point, transform);
            Point gPoint = geometryFactory.createPoint(new Coordinate(geom.getCoordinate()));

            System.out.println("Google point:" + gPoint.getCoordinate().x + "," + gPoint.getCoordinate().y);

            MathTransform reverseTransform = CRS.findMathTransform(googleCRS, wgsCRS);
            final int SIDES = sides;
            Coordinate coords[] = new Coordinate[SIDES + 1];
            for (int i = 0; i < SIDES; i++) {
                double angle = ((double) i / (double) SIDES) * Math.PI * 2.0;
                double dx = Math.cos(angle) * RADIUS;
                double dy = Math.sin(angle) * RADIUS;
                geom = JTS.transform(geometryFactory.createPoint(new Coordinate((double) gPoint.getCoordinate().x + dx, (double) gPoint.getCoordinate().y + dy)), reverseTransform);
                coords[i] = new Coordinate(geom.getCoordinate().y, geom.getCoordinate().x);
            }
            coords[SIDES] = coords[0];

            LinearRing ring = geometryFactory.createLinearRing(coords);
            Polygon polygon = geometryFactory.createPolygon(ring, null);

            WKTWriter writer = new WKTWriter();
            String wkt = writer.write(polygon);
            return wkt.replaceAll("POLYGON ", "POLYGON").replaceAll(", ", ",");

        } catch (Exception e) {
            System.out.println("Circle fail!");
            e.printStackTrace();
            return "none";
        }
    }
}
