/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import org.zkoss.zk.ui.event.Event;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import net.sf.json.JSONObject;
import org.ala.spatial.data.Facet;
import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.gazetteer.AutoComplete;
import org.ala.spatial.sampling.SimpleShapeFile;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.Util;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Hbox;

/**
 *
 * @author angus
 */
public class AreaRegionSelection extends AreaToolComposer {

    Button btnOk;
    Hbox hbRadius;
    private AutoComplete gazetteerAuto;
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

        String link = (String) ci.getValue();
        String label = ci.getLabel();

        //add feature to the map as a new layer
        MapLayer mapLayer;
        if (displayAsWms.isChecked()) {
            String url = CommonData.geoServer
                    + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:Objects&format=image/png&viewparams=s:"
                    + link.substring(link.lastIndexOf('/') + 1);
            logger.info(label + " | " + url);
            mapLayer = getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(label), label, url, 0.6f, /*metadata url*/ null,
                    null, LayerUtilities.WKT, null, null);
            if(mapLayer == null) {
                return;
            }
            mapLayer.setWKT(Util.readUrl(CommonData.layersServer + link.replace("/geojson/", "/wkt/")));
            mapLayer.setPolygonLayer(true);
        } else {
            mapLayer = getMapComposer().addGeoJSON(label, CommonData.layersServer + link);
            if(mapLayer == null) {
                return;
            }
        }
        this.layerName = mapLayer.getName();

        //if the layer is a point create a radius
        boolean point = false;
        if (mapLayer.getWKT().startsWith("POINT")) {
            point = true;
            String coords = mapLayer.getWKT().replace("POINT(", "").replace(")","");

            double radius = dRadius.getValue() * 1000.0;

            String wkt = Util.createCircleJs(Double.parseDouble(coords.split(" ")[0]), Double.parseDouble(coords.split(" ")[1]), radius);
            getMapComposer().removeLayer(label);
            mapLayer = getMapComposer().addWKTLayer(wkt, label, label);
        }

        if (mapLayer != null) {  //might be a duplicate layer making mapLayer == null
            String object = Util.readUrl(CommonData.layersServer + "/object/" + link.substring(link.lastIndexOf('/') + 1));
            String bbox = Util.getStringValue(null, "bbox", object);
            String fid = Util.getStringValue(null, "fid", object);
            String spid = Util.getStringValue("\"id\":\"" + fid + "\"", "spid", Util.readUrl(CommonData.layersServer + "/fields"));

            MapLayerMetadata md = mapLayer.getMapLayerMetadata();
            if (md == null) {
                md = new MapLayerMetadata();
                mapLayer.setMapLayerMetadata(md);
            }
            try {
                double[][] bb = null;
                if(point) {
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
                System.out.println("failed to parse: " + mapLayer.getWKT());
                e.printStackTrace();
            }
            md.setMoreInfo(CommonData.layersServer + "/layers/" + spid);

            Facet facet = null;
            if(!point) {
                facet = Util.getFacetForObject(link.substring(link.lastIndexOf('/') + 1), label);
            }
            if (facet != null) {
                ArrayList<Facet> facets = new ArrayList<Facet>();
                facets.add(facet);
                mapLayer.setData("facets", facets);
            }

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
