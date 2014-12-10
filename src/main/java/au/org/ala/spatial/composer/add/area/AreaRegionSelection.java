/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.gazetteer.GazetteerAutoComplete;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import net.sf.json.JSONObject;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.ala.layers.legend.Facet;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.*;

import java.util.ArrayList;
import java.util.List;

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
        JSONObject obj = JSONObject.fromObject(Util.readUrl(CommonData.getLayersServer() + "/object/" + jo.getString(StringConstants.PID)));

        String label = ci.getLabel();

        //add feature to the map as a new layer
        MapLayer mapLayer;
        LOGGER.debug(label + " | " + obj.getString(StringConstants.WMSURL));
        mapLayer = getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(label), label, obj.getString(StringConstants.WMSURL), 0.6f, /*metadata url*/ null,
                null, LayerUtilitiesImpl.WKT, null, null);
        if (mapLayer == null) {
            return;
        }


        mapLayer.setPolygonLayer(true);

        this.layerName = mapLayer.getName();

        SimpleRegion sr = SimpleShapeFile.parseWKT(obj.getString(StringConstants.BBOX));
        double[][] bb = sr.getBoundingBox();
        List<Double> dbb = new ArrayList<Double>();
        dbb.add(bb[0][0]);
        dbb.add(bb[0][1]);
        dbb.add(bb[1][0]);
        dbb.add(bb[1][1]);

        //if the layer is a point create a radius
        boolean point = false;
        if ((float) bb[0][0] == (float) bb[1][0] && (float) bb[0][1] == (float) bb[1][1]) {
            point = true;

            mapLayer.setWKT("POINT(" + bb[0][0] + " " + bb[0][1] + ")");

            double radius = dRadius.getValue() * 1000.0;

            String wkt = Util.createCircleJs(bb[0][0], bb[0][1], radius);
            getMapComposer().removeLayer(label);
            mapLayer = getMapComposer().addWKTLayer(wkt, label, label);

            //redo bounding box
            sr = SimpleShapeFile.parseWKT(wkt);
            bb = sr.getBoundingBox();
            dbb = new ArrayList<Double>();
            dbb.add(bb[0][0]);
            dbb.add(bb[0][1]);
            dbb.add(bb[1][0]);
            dbb.add(bb[1][1]);
        } else {
            mapLayer.setWKT(Util.readUrl(CommonData.getLayersServer() + "/shape/wkt/" + obj.getString(StringConstants.PID)));
        }

        String fid = obj.getString(StringConstants.FID);

        MapLayerMetadata md = mapLayer.getMapLayerMetadata();
        md.setBbox(dbb);


        Facet facet = null;
        if (!point && mapLayer.getFacets() == null) {
            //only get field data if it is an intersected layer (to exclude layers containing points)
            if (CommonData.getLayer(fid) != null) {
                JSONObject fieldJson = JSONObject.fromObject(Util.readUrl(CommonData.getLayersServer() + "/field/" + fid + "?pageSize=0"));

                md.setMoreInfo(CommonData.getLayersServer() + "/layers/view/more/" + fieldJson.getString("spid"));

                facet = Util.getFacetForObject(label, fid);
            }

            if (facet != null) {
                List<Facet> facets = new ArrayList<Facet>();
                facets.add(facet);
                mapLayer.setFacets(facets);
            }
        }

        mapLayer.setRedVal(255);
        mapLayer.setGreenVal(0);
        mapLayer.setBlueVal(0);
        mapLayer.setDynamicStyle(true);
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
