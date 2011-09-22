package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.ala.spatial.gazetteer.GazetteerPointSearch;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.LayersUtil;
import org.ala.spatial.util.Util;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

/**
 *
 * @author Adam
 */
public class AreaMapPolygon extends AreaToolComposer {

    SettingsSupplementary settingsSupplementary;
    private Textbox displayGeom;
    //String layerName;
    Textbox txtLayerName;
    Button btnOk;
    Button btnClear;
    Button btnAddLayer;
//    ContextualLayersAutoComplete autoCompleteLayers;
//    String treeName, treePath, treeMetadata;
//    int treeSubType;
    Radio rAddLayer;
    Vbox vbxLayerList;
    Radiogroup rgPolygonLayers;
 //   List<Radio> radioItems;
    @Override
    public void afterCompose() {
        super.afterCompose();
        loadLayerSelection();
        txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));
         btnOk.setDisabled(true);
        btnClear.setDisabled(true);
        Clients.evalJavaScript("mapFrame.toggleClickHandler(false);");
    }

    public void onClick$btnOk(Event event) {
        ok = true;
        Clients.evalJavaScript("mapFrame.toggleClickHandler(true);");
        this.detach();
    }

    public void onClick$btnClear(Event event) {
        MapComposer mc = getThisMapComposer();
        if(layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        String script = mc.getOpenLayersJavascript().addFeatureSelectionTool();
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
        displayGeom.setValue("");
        btnOk.setDisabled(true);
        btnClear.setDisabled(true);
    }

    public void onClick$btnCancel(Event event) {
        MapComposer mc = getThisMapComposer();
        if(layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        Clients.evalJavaScript("mapFrame.toggleClickHandler(true);");
        this.detach();
    }

    public void onCheck$rgPolygonLayers(Event event) {
        
          Radio selectedItem = rgPolygonLayers.getSelectedItem();
         
          //Add and remove layer to set as top layer
          String layerName = selectedItem.getValue();
          MapComposer mc = getThisMapComposer();
          MapLayer ml = mc.getMapLayer(layerName);
          mc.removeLayer(layerName);
          mc.activateLayer(ml, true);
          
    }


      public void loadLayerSelection() {
        try {

            Radio rSelectedLayer = (Radio) getFellowIfAny("rSelectedLayer");

            List<MapLayer> layers = getMapComposer().getContextualLayers();
            
            if (!layers.isEmpty()) {
            
                for (int i = 0; i < layers.size(); i++) {

                    MapLayer lyr = layers.get(i);
                    Radio rAr = new Radio(lyr.getDisplayName());
                    rAr.setId(lyr.getDisplayName().replaceAll(" ", ""));
                    rAr.setValue(lyr.getDisplayName());
                    rAr.setParent(rgPolygonLayers);

                    if (i == 0) {
                        rAr.setSelected(true);
                    }
                    rgPolygonLayers.insertBefore(rAr, rSelectedLayer);
                }
                rSelectedLayer.setSelected(true);
            }
        } catch (Exception e) {
            
        }
    }


    /**
     * Searches the gazetter at a given point and then maps the polygon feature
     * found at the location (for the current top contextual layer).
     * @param event 
     */
    public void onSearchPoint(Event event) {
        String searchPoint = (String) event.getData();
        String lon = searchPoint.split(",")[0];
        String lat = searchPoint.split(",")[1];


        System.out.println("*************************************");
        System.out.println("CommonData.getLayerList");
        //System.out.println(CommonData.getLayerList());
        System.out.println("*************************************");


        Object llist = CommonData.getLayerListJSONArray();
        JSONArray layerlist = JSONArray.fromObject(llist);
        //JSONArray layerlist = JSONArray.fromObject(CommonData.getLayerList());
        MapComposer mc = getThisMapComposer();

        List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
        Boolean searchComplete = false;
        for (int i = 0; i < activeLayers.size(); i++) {
            MapLayer ml = activeLayers.get(i);

            String activeLayerName = "none";
            if (ml.getUri() != null)
                activeLayerName = ml.getUri().replaceAll("^.*ALA:", "").replaceAll("&.*", "");
            System.out.println("ACTIVE LAYER: " + activeLayerName);
            if (ml.isDisplayed()) {
                for (int j = 0; j < layerlist.size(); j++) {
                    if (searchComplete) {
                        break;
                    }

                    JSONObject jo = layerlist.getJSONObject(j);
                    // System.out.println("********" + jo.getString("name"));
                    if (ml != null && jo.getString("type") != null
                            && jo.getString("type").length() > 0
                            && jo.getString("type").equalsIgnoreCase("contextual")
                            && jo.getString("name").equalsIgnoreCase(activeLayerName)) {

                        
                        System.out.println(ml.getName());
                        String featureURI = GazetteerPointSearch.PointSearch(lon, lat, activeLayerName, CommonData.geoServer);
                        System.out.println(featureURI);
                        if (featureURI.equalsIgnoreCase("none")) { // featureURI == null
                            continue;
                        }
                        //if it is a filtered layer, expect the filter as part of the new uri.
//                        boolean passedFilterCheck = true;
//                        try {
//                            String filter = ml.getUri().replaceAll("^.*cql_filter=", "").replaceFirst("^.*='", "").replaceAll("'.*", "");
//                            if (filter != null && filter.length() > 0) {
//                                System.out.println(filter.toLowerCase());
//                                passedFilterCheck = featureURI.toLowerCase().contains(filter.toLowerCase().replace(" ", "_"));
//                            }
//                        } catch (Exception e) {
//                        }
//                        if (!passedFilterCheck) {
//                            continue;
//                        }


                        //add feature to the map as a new layer
                        String feature_text = Util.wktFromJSON(readGeoJSON(featureURI));

                        String json = readGeoJSON(featureURI);
                        String wkt = Util.wktFromJSON(json);
                        if (wkt.contentEquals("none")) {
                            continue;
                          //  break;
                        } else {
                            searchComplete = true;
                            displayGeom.setValue(feature_text);
                            //mc.removeFromList(mc.getMapLayer("Active Area"));
                            layerName = (mc.getMapLayer(txtLayerName.getValue()) == null)?txtLayerName.getValue():mc.getNextAreaLayerName(txtLayerName.getValue());
                            MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, txtLayerName.getValue());
                            MapLayerMetadata md = mapLayer.getMapLayerMetadata();
                            if(md == null) {
                                md = new MapLayerMetadata();
                                mapLayer.setMapLayerMetadata(md);
                            }
                            md.setMoreInfo(LayersUtil.getMetadataForWKT("User selected map polygon", wkt));
                       
                            btnOk.setDisabled(false);
                            btnClear.setDisabled(false);
                            break;

                        }
                    }
                }
            }
        }
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
