/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import antlr.debug.Event;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.ala.spatial.gazetteer.AutoComplete;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Page;
import org.zkoss.zul.Comboitem;

/**
 *
 * @author angus
 */
public class AreaRegionSelection extends UtilityComposer {

    String satServer;
    String geoServer;
    private AutoComplete gazetteerAuto;
    //private SettingsSupplementary settingsSupplementary = null;

    @Override
    public void afterCompose() {
        super.afterCompose();

        // if (settingsSupplementary != null) {

        //   satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        // }
        satServer = "http://spatial-dev.ala.org.au";
        geoServer = "http://spatial-dev.ala.org.au";
    }

    public void onClick$btnOk(Event event) {
        try { 
            Messagebox.show("Working??");
        }
        catch (Exception e) {}
        this.detach();
    }

    public void onClick$btnClear(Event event) {
        MapComposer mc = getThisMapComposer();
        mc.removeLayer("Active Area");
        
       
    }

    public void onClick$btnCancel(Event event) {
        MapComposer mc = getThisMapComposer();
        mc.removeLayer("Active Area");
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

        //exit if no match found
        if (ci == null) {
            return;
        }

        String link = (String) ci.getValue();
        String label = ci.getLabel();
//        if (settingsSupplementary != null) {
//            geoServer = settingsSupplementary.getValue(CommonData.GEOSERVER_URL);
//            logger.debug(geoServer + link);
//        } else {
//            return;
//        }


        //add feature to the map as active area

        String json = readGeoJSON(geoServer + link);
        String wkt = wktFromJSON(json);
        MapComposer mc = getThisMapComposer();
        MapLayer mapLayer = mc.addWKTLayer(wkt, "Active Area");
        //  updateSpeciesList(false);



    }
   
    /**
     * updates species list analysis tab with refreshCount
     */
    void updateSpeciesList(boolean populateSpeciesList) {
        try {
            FilteringResultsWCController win =
                    (FilteringResultsWCController) getMapComposer().getFellow("leftMenuAnalysis").getFellow("analysiswindow").getFellow("sf").getFellow("selectionwindow").getFellow("speciesListForm").getFellow("popup_results");
            //if (!populateSpeciesList) {
            win.refreshCount();
            //} else {
            //    win.onClick$refreshButton2();
            // }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // updateAreaLabel();
    }

    /**
     * Gets the main pages controller so we can add a
     * drawing tool to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    /**
     * get Active Area as WKT string, from a layer name
     *
     * @param layer name of layer as String
     * @param register_shape true to register the shape with alaspatial shape register
     * @return
     */
    String getLayerGeoJsonAsWkt(String layer, boolean register_shape) {
        String wkt = ""; //DEFAULT_AREA;

        if (!register_shape) {
            return wktFromJSON(getMapComposer().getMapLayer(layer).getGeoJSON());
        }

        try {
            //try to get table name from uri like gazetteer/aus1/Queensland.json
            String uri = getMapComposer().getMapLayer(layer).getUri();
            String gaz = "gazetteer/";
            int i1 = uri.indexOf(gaz);
            int i2 = uri.indexOf("/", i1 + gaz.length() + 1);
            int i3 = uri.lastIndexOf(".json");
            String table = uri.substring(i1 + gaz.length(), i2);
            String value = uri.substring(i2 + 1, i3);
            //test if available in alaspatial
            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(satServer + "/alaspatial/species/shape/lookup");
            get.addParameter("table", table);
            get.addParameter("value", value);
            get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("register table and value with alaspatial: " + slist);

            if (slist != null && result == 200) {
                wkt = "LAYER(" + layer + "," + slist + ")";

                return wkt;
            }
        } catch (Exception e) {
            System.out.println("no alaspatial shape for layer: " + layer);
            e.printStackTrace();
        }
        try {
            //class_name is same as layer name
            wkt = wktFromJSON(getMapComposer().getMapLayer(layer).getGeoJSON());

            if (!register_shape) {
                return wkt;
            }

            //register wkt with alaspatial and use LAYER(layer name, id)
            HttpClient client = new HttpClient();
            //GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            PostMethod get = new PostMethod(satServer + "/alaspatial/species/shape/register");
            get.addParameter("area", wkt);
            get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("register wkt shape with alaspatial: " + slist);

            wkt = "LAYER(" + layer + "," + slist + ")";
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("SelectionController.getLayerGeoJsonAsWkt(" + layer + "): " + wkt);
        return wkt;
    }

    /**
     * transform json string with geometries into wkt.
     *
     * extracts 'shape_area' if available and assigns it to storedSize.
     *
     * @param json
     * @return
     */
    private String wktFromJSON(String json) {
        try {
            JSONObject obj = JSONObject.fromObject(json);
            JSONArray geometries = obj.getJSONArray("geometries");
            String wkt = "";
            for (int i = 0; i < geometries.size(); i++) {
                String coords = geometries.getJSONObject(i).getString("coordinates");

                if (geometries.getJSONObject(i).getString("type").equalsIgnoreCase("multipolygon")) {
                    wkt += coords.replace("]]],[[[", "))*((").replace("]],[[", "))*((").replace("],[", "*").replace(",", " ").replace("*", ",").replace("[[[[", "MULTIPOLYGON(((").replace("]]]]", ")))");

                } else {
                    wkt += coords.replace("],[", "*").replace(",", " ").replace("*", ",").replace("[[[", "POLYGON((").replace("]]]", "))").replace("],[", "),(");
                }

                wkt = wkt.replace(")))MULTIPOLYGON(", ")),");
            }
            return wkt;
        } catch (JSONException e) {
            return "none";
        }
    }

       /**
     * get Active Area as WKT string, from a layer name and feature class
     *
     * @param layer name of layer as String
     * @param classification value of feature classification
     * @param register_shape true to register the shape with alaspatial shape register
     * @return
     */
    String getWktFromURI(String layer, boolean register_shape) {
        String feature_text = "";//DEFAULT_AREA;

        if (!register_shape) {
            String json = readGeoJSON(layer);
            return feature_text = wktFromJSON(json);
        }

        try {
            String uri = layer;
            String gaz = "gazetteer/";
            int i1 = uri.indexOf(gaz);
            int i2 = uri.indexOf("/", i1 + gaz.length() + 1);
            int i3 = uri.lastIndexOf(".json");
            String table = uri.substring(i1 + gaz.length(), i2);
            String value = uri.substring(i2 + 1, i3);
            //test if available in alaspatial
            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(satServer + "/alaspatial/species/shape/lookup");
            get.addParameter("table", table);
            get.addParameter("value", value);
            get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("register table and value with alaspatial: " + slist);

            if (slist != null && result == 200) {
                feature_text = "LAYER(" + layer + "," + slist + ")";

                return feature_text;
            }
        } catch (Exception e) {
            System.out.println("no alaspatial shape for layer: " + layer);
            e.printStackTrace();
        }
        try {
            //class_name is same as layer name
            String json = readGeoJSON(layer);
            feature_text = wktFromJSON(json);

            if (!register_shape) {
                return feature_text;
            }

            //register wkt with alaspatial and use LAYER(layer name, id)
            HttpClient client = new HttpClient();
            //GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            PostMethod get = new PostMethod(satServer + "/alaspatial/species/shape/register");
            get.addParameter("area", feature_text);
            get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("register wkt shape with alaspatial: " + slist);

            feature_text = "LAYER(" + layer + "," + slist + ")";
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("SelectionController.getLayerGeoJsonAsWkt(" + layer + "): " + feature_text);
        return feature_text;
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


