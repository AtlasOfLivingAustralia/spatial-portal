package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Textbox;


/**
 *
 * @author Adam
 */
public class AreaPolygon extends UtilityComposer {

    String satServer;
    private Textbox displayGeom;
    private static final String DEFAULT_AREA = "CURRENTVIEW()";
    //private SettingsSupplementary settingsSupplementary = null;
    String layerName;

    @Override
    public void afterCompose() {
        super.afterCompose();

	// if (settingsSupplementary != null) {

         //   satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
       // }
        satServer = "http://spatial-dev.ala.org.au";
    }

    public void onClick$btnNext(Event event) {
        this.detach();
    }

    public void onClick$btnClear(Event event) {
        MapComposer mc = getThisMapComposer();
        if(layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        String script = mc.getOpenLayersJavascript().addPolygonDrawingTool();
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
        displayGeom.setValue(DEFAULT_AREA);
    }

    public void onClick$btnCancel(Event event) {
        MapComposer mc = getThisMapComposer();
        if(layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        this.detach();
    }

    /**
     *
     * @param event
     */
    public void onSelectionGeom(Event event) {
        String selectionGeom = (String) event.getData();

        try {
	
            String wkt = "";
            if (selectionGeom.contains("NaN NaN")) {
                displayGeom.setValue(DEFAULT_AREA);
              //  lastTool = null;
            } else if (selectionGeom.startsWith("LAYER(")) {
                //reset stored size
               // storedSize = null;
                //get WKT from this feature
                String v = selectionGeom.replace("LAYER(", "");
                //FEATURE(table name if known, class name)
                v = v.substring(0, v.length() - 1);
                wkt = getLayerGeoJsonAsWkt(v, true);
                displayGeom.setValue(wkt);

                //for display
                wkt = getLayerGeoJsonAsWkt(v, false);

                //calculate area is not populated
//                if (storedSize == null) {
//                    storedSize = getAreaOfWKT(wkt);
//                }
            } else {
                wkt = selectionGeom;
                displayGeom.setValue(wkt);
            }
      //      updateComboBoxText();
            updateSpeciesList(false); // true

            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

            //add feature to the map as a new layer
            if (wkt.length() > 0) {
                layerName = mc.getNextAreaLayerName("My polygon");
                MapLayer mapLayer = mc.addWKTLayer(wkt, layerName);
            }
         //   rgAreaSelection.getSelectedItem().setChecked(false);

        } catch (Exception e) {//FIXME
        }
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
}
