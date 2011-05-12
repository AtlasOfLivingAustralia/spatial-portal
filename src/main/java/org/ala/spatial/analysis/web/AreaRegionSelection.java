/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import org.zkoss.zk.ui.event.Event;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import net.sf.json.JSONObject;
import org.ala.spatial.gazetteer.AutoComplete;
import org.ala.spatial.util.CommonData;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;

/**
 *
 * @author angus
 */
public class AreaRegionSelection extends AreaToolComposer {

    Button btnOk;
    Hbox hbRadius;
    private AutoComplete gazetteerAuto;
    Combobox cbRadius;
    Comboitem ci1km;
    Comboitem ci5km;
    Comboitem ci10km;
    Comboitem ci20km;

    @Override
    public void afterCompose() {
        super.afterCompose();
        cbRadius.setSelectedItem(ci1km);
    }

    public void onClick$btnOk(Event event) {
        Comboitem ci = gazetteerAuto.getSelectedItem();

        //exit if no match found
        if (ci == null) {
            return;
        }

        String link = (String) ci.getValue();
        String label = ci.getLabel();

        //add feature to the map as a new layer
        MapLayer mapLayer = getMapComposer().addGeoJSON(label, CommonData.geoServer + link);
        
        JSONObject jo = JSONObject.fromObject(mapLayer.getGeoJSON());
        //if the layer is a point create a radius
        if (jo.getJSONArray("geometries").getJSONObject(0).getString("type").equalsIgnoreCase("point")) {
            
            String coords = jo.getJSONArray("geometries").getJSONObject(0).getString("coordinates").replace("[","").replace("]","");

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

            String wkt = createCircle(Double.parseDouble(coords.split(",")[0]),Double.parseDouble(coords.split(",")[1]),radius);
            getMapComposer().removeLayer(label);
            mapLayer = getMapComposer().addWKTLayer(wkt, label, label);

            //return;
        }
       
        if (mapLayer != null) {  //might be a duplicate layer making mapLayer == null
            String metadatalink = jo.getJSONObject("properties").getString("Layer_Metadata");

            mapLayer.setMapLayerMetadata(new MapLayerMetadata());
            mapLayer.getMapLayerMetadata().setMoreInfo(metadatalink);

            getMapComposer().updateUserLogMapLayer("gaz", label + "|" + CommonData.geoServer + link);

        }

        ok = true;

        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

     /**
     * Adds the currently selected gazetteer feature to the map
     */
    public void onChange$gazetteerAuto() {

        Comboitem ci = gazetteerAuto.getSelectedItem();

        //when no item selected find an exact match from listed items
        if (ci == null) {
            String txt = gazetteerAuto.getText();
            for (Object o : gazetteerAuto.getItems()) {
                Comboitem c = (Comboitem) o;
                if (c.getLabel().equalsIgnoreCase(txt)) {
                    gazetteerAuto.setSelectedItem(c);
                    ci = c;
                    break;
                }
            }
        }

        if(ci == null) {
            btnOk.setDisabled(true);
        } else {
            btnOk.setDisabled(false);
            String json = readGeoJSON(CommonData.geoServer + ci.getValue().toString());
            JSONObject jo = JSONObject.fromObject(json);
            //if the layer is a point create a radius
            if (jo.getJSONArray("geometries").getJSONObject(0).getString("type").equalsIgnoreCase("point"))
                hbRadius.setVisible(true);
            else
                hbRadius.setVisible(false);
//            try {
//                Messagebox.show(ci.getValue().toString());
//            }
//            catch (Exception e) {}
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

     private String readGeoJSON(String feature) {
        StringBuffer content = new StringBuffer();

        try {
            // Construct data

            // Send data
            URL url = new URL(feature);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                content.append(line);
            }
            conn.disconnect();
        } catch (Exception e) {
        }
        return content.toString();
    }
}


