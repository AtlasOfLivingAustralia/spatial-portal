/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.composer.gazetteer.GazetteerAutoComplete;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import net.sf.json.JSONObject;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.ala.layers.legend.Facet;
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
        //TODO: why is "cl" needed for grid class layers?  fix layers-store/layers-service
        JSONObject obj;
        if (jo.getString("pid").contains(":")) {
            obj = JSONObject.fromObject(Util.readUrl(CommonData.layersServer + "/object/" + "cl" + jo.getString("pid")));
        } else {
            obj = JSONObject.fromObject(Util.readUrl(CommonData.layersServer + "/object/" + jo.getString("pid")));
        }
        String label = ci.getLabel();

        //add feature to the map as a new layer
        MapLayer mapLayer;
        //   if (displayAsWms.isChecked()) {
        logger.debug(label + " | " + obj.getString("wmsurl"));
        mapLayer = getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(label), label, obj.getString("wmsurl"), 0.6f, /*metadata url*/ null,
                null, LayerUtilities.WKT, null, null);
        if (mapLayer == null) {
            return;
        }


        mapLayer.setPolygonLayer(true);

        this.layerName = mapLayer.getName();


        SimpleRegion sr = SimpleShapeFile.parseWKT(obj.getString("bbox"));
        double[][] bb = sr.getBoundingBox();
        ArrayList<Double> dbb = new ArrayList<Double>();
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
            mapLayer.setWKT("ENVELOPE(" + obj.getString("fid") + "," + obj.getString("pid") + ")");
        }

        String fid = obj.getString("fid");
        String spid = Util.getStringValue("\"id\":\"" + fid + "\"", "spid", Util.readUrl(CommonData.layersServer + "/fields"));

        MapLayerMetadata md = mapLayer.getMapLayerMetadata();
        md.setBbox(dbb);

        md.setMoreInfo(CommonData.layersServer + "/layers/view/more/" + spid);

        Facet facet = null;
        if (!point && mapLayer.getFacets() == null) {
            facet = Util.getFacetForObject(jo, label);
            if (facet != null) {
                ArrayList<Facet> facets = new ArrayList<Facet>();
                facets.add(facet);
                mapLayer.setFacets(facets);
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
