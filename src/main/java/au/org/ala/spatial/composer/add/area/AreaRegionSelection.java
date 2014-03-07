/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.composer.gazetteer.GazetteerAutoComplete;
import au.org.ala.spatial.data.Facet;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import net.sf.json.JSONObject;
import org.ala.layers.intersect.SimpleShapeFile;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.*;

import java.util.ArrayList;

/**
 * @author angus
 */
public class AreaRegionSelection extends AreaToolComposer {

    private static Logger logger = Logger.getLogger(AreaRegionSelection.class);
    Button btnOk;
    Hbox hbRadius;
    private GazetteerAutoComplete gazetteerAuto;
    Doublebox dRadius;
    Checkbox displayAsWms;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void onClick$btnOk(Event event) {
        Comboitem ci = gazetteerAuto.getSelectedItem();

        if (!validate()) {
            return;
        }

        //exit if no match found
        if (ci == null) {
            return;
        }

        JSONObject jo = ci.getValue();
        JSONObject obj = JSONObject.fromObject(Util.readUrl(CommonData.layersServer + "/object/" + jo.getString("pid")));
        String label = ci.getLabel();

        //add feature to the map as a new layer
        MapLayer mapLayer;
        if (displayAsWms.isChecked()) {
            logger.info(label + " | " + obj.getString("wmsurl"));
            mapLayer = getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(label), label, obj.getString("wmsurl"), 0.6f, /*metadata url*/ null,
                    null, LayerUtilities.WKT, null, null);
            if (mapLayer == null) {
                return;
            }

            //TODO: move large WKT objects into envelope form
            //grid class layers best operate as an envelope
            if (jo.getString("pid").contains(":")) {
                String envelope = "cl" + jo.getString("pid");
                mapLayer.setWKT("ENVELOPE(" + envelope + ")");
                mapLayer.setEnvelope(envelope);
                ArrayList<Facet> facets = new ArrayList<Facet>();
                facets.add(new Facet(envelope.split(":")[0], jo.getString("name"), true));
                mapLayer.setFacets(facets);
            } else {
                mapLayer.setWKT(Util.readUrl(CommonData.layersServer + "/shape/wkt/" + jo.getString("pid")));
            }
            mapLayer.setPolygonLayer(true);
        } else {
            mapLayer = getMapComposer().addGeoJSON(label, CommonData.layersServer + "/shape/geojson/" + jo.getString("pid"));
            if (mapLayer == null) {
                return;
            }
        }
        this.layerName = mapLayer.getName();

        //if the layer is a point create a radius
        boolean point = false;
        if (mapLayer.getWKT().startsWith("POINT")) {
            point = true;
            String coords = mapLayer.getWKT().replace("POINT(", "").replace(")", "");

            double radius = dRadius.getValue() * 1000.0;

            String wkt = Util.createCircleJs(Double.parseDouble(coords.split(" ")[0]), Double.parseDouble(coords.split(" ")[1]), radius);
            getMapComposer().removeLayer(label);
            mapLayer = getMapComposer().addWKTLayer(wkt, label, label);
        }

        if (mapLayer != null) {  //might be a duplicate layer making mapLayer == null            
            String bbox = obj.getString("bbox");
            String fid = obj.getString("fid");
            String spid = Util.getStringValue("\"id\":\"" + fid + "\"", "spid", Util.readUrl(CommonData.layersServer + "/fields"));

            MapLayerMetadata md = mapLayer.getMapLayerMetadata();
            try {
                double[][] bb = null;
                if (point) {
                    bb = SimpleShapeFile.parseWKT(mapLayer.getWKT()).getBoundingBox();
                } else {
                    bb = SimpleShapeFile.parseWKT(bbox).getBoundingBox();
                }
                ArrayList<Double> dbb = new ArrayList<Double>();
                dbb.add(bb[0][0]);
                dbb.add(bb[0][1]);
                dbb.add(bb[1][0]);
                dbb.add(bb[1][1]);
                md.setBbox(dbb);
            } catch (Exception e) {
                logger.debug("failed to parse: " + mapLayer.getWKT(), e);
            }
            md.setMoreInfo(CommonData.layersServer + "/layers/view/more/" + spid);

            Facet facet = null;
            if (!point && mapLayer.getFacets() == null) {
                facet = Util.getFacetForObject(jo.getString("pid"), label);
                if (facet != null) {
                    ArrayList<Facet> facets = new ArrayList<Facet>();
                    facets.add(facet);
                    mapLayer.setFacets(facets);
                }
            }
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

        if (ci == null) {
            btnOk.setDisabled(true);
        } else {
            btnOk.setDisabled(false);

            //specifically for the gazetteer layer point detection
            boolean point = false;
            if (ci.getDescription() != null && ci.getDescription().contains("Gazetteer")) {
                String[] s = ci.getDescription().split(",");
                try {
                    double lat = Double.parseDouble(s[2].trim());
                    double lng = Double.parseDouble(s[2].trim());
                    point = true;
                } catch (Exception e) {
                }
            }

            if (point) {
                hbRadius.setVisible(true);
            } else {
                hbRadius.setVisible(false);
            }
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
