/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.add.area;

import au.org.ala.legend.Facet;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.gazetteer.GazetteerAutoComplete;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.*;

import java.util.ArrayList;
import java.util.List;

;

/**
 * @author angus
 */
public class AreaRegionSelection extends AreaToolComposer {

    private static final Logger LOGGER = Logger.getLogger(AreaRegionSelection.class);
    private Button btnOk;
    private Hbox hbRadius;
    private Doublebox dRadius;
    private Checkbox displayAsWms;
    private GazetteerAutoComplete gazetteerAuto;

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
        JSONParser jp = new JSONParser();
        JSONObject obj = null;
        try {
            obj = (JSONObject) jp.parse(Util.readUrl(CommonData.getLayersServer() + "/object/" + jo.get(StringConstants.PID)));
        } catch (ParseException e) {
            LOGGER.error("failed to parse for object: " + jo.get(StringConstants.PID));
        }

        String label = ci.getLabel();

        //add feature to the map as a new layer
        MapLayer mapLayer;
        LOGGER.debug(label + " | " + obj.get(StringConstants.WMSURL));
        mapLayer = getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(label), label, obj.get(StringConstants.WMSURL).toString(), 0.6f, /*metadata url*/ null,
                null, LayerUtilitiesImpl.WKT, null, null);
        if (mapLayer == null) {
            return;
        }


        mapLayer.setPolygonLayer(true);

        this.layerName = mapLayer.getName();

        List<Double> dbb = Util.getBoundingBox(obj.get(StringConstants.BBOX).toString());

        //if the layer is a point create a radius
        boolean point = false;
        if (dbb.get(0).floatValue() == dbb.get(2).floatValue() && (float) dbb.get(1).floatValue() == dbb.get(3).floatValue()) {
            point = true;

            mapLayer.setWKT("POINT(" + dbb.get(0).floatValue() + " " + dbb.get(1).floatValue() + ")");

            double radius = dRadius.getValue() * 1000.0;

            String wkt = Util.createCircleJs(dbb.get(0).floatValue(), dbb.get(1).floatValue(), radius);
            getMapComposer().removeLayer(label);
            mapLayer = getMapComposer().addWKTLayer(wkt, label, label);

            //redo bounding box
            dbb = Util.getBoundingBox(wkt);
        } else {
            mapLayer.setWKT(Util.readUrl(CommonData.getLayersServer() + "/shape/wkt/" + obj.get(StringConstants.PID)));
        }

        String fid = obj.get(StringConstants.FID).toString();

        MapLayerMetadata md = mapLayer.getMapLayerMetadata();
        md.setBbox(dbb);


        Facet facet = null;
        if (!point && mapLayer.getFacets() == null) {
            //only get field data if it is an intersected layer (to exclude layers containing points)
            if (CommonData.getLayer(fid) != null) {
                JSONObject fieldJson = null;
                try {
                    fieldJson = (JSONObject) jp.parse(Util.readUrl(CommonData.getLayersServer() + "/field/" + fid + "?pageSize=0"));
                } catch (ParseException e) {
                    LOGGER.error("failed to parse for field: " + fid);
                }

                md.setMoreInfo(CommonData.getLayersServer() + "/layers/view/more/" + fieldJson.get("spid"));

                facet = Util.getFacetForObject(label, fid);
            }

            if (facet != null) {
                List<Facet> facets = new ArrayList<Facet>();
                facets.add(facet);
                mapLayer.setFacets(facets);
            }
        }

        int colour = Util.nextColour();
        int r = (colour >> 16) & 0x000000ff;
        int g = (colour >> 8) & 0x000000ff;
        int b = (colour) & 0x000000ff;

        mapLayer.setRedVal(r);
        mapLayer.setGreenVal(g);
        mapLayer.setBlueVal(b);
        mapLayer.setDynamicStyle(true);
        getMapComposer().applyChange(mapLayer);
        getMapComposer().updateLayerControls();

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
                    point = !Double.isNaN(lat);
                } catch (NumberFormatException e) {
                    //not a double value
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
            sb.append("\n").append(CommonData.lang(StringConstants.ERROR_INVALID_RADIUS));
        }

        if (sb.length() > 0) {
            getMapComposer().showMessage(sb.toString());
        }

        return sb.length() == 0;
    }
}
