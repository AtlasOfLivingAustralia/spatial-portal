package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.composer.gazetteer.GazetteerPointSearch;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.javascript.OpenLayersJavascript;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.layers.legend.Facet;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Adam
 */
public class AreaMapPolygon extends AreaToolComposer {

    private static Logger logger = Logger.getLogger(AreaMapPolygon.class);
    SettingsSupplementary settingsSupplementary;
    private Textbox displayGeom;
    Textbox txtLayerName;
    Button btnOk;
    Button btnClear;
    Button btnAddLayer;
    Radio rAddLayer;
    Vbox vbxLayerList;
    Radiogroup rgPolygonLayers;
    Checkbox displayAsWms;

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
        MapLayer ml = getMapComposer().getMapLayer(layerName);
        ml.setDisplayName(txtLayerName.getValue());
        getMapComposer().redrawLayersList();
        ok = true;
        Clients.evalJavaScript("mapFrame.toggleClickHandler(true);");

        String activeLayerName = "none";
        if (ml.getUri() != null) {
            activeLayerName = ml.getUri().replaceAll("^.*ALA:", "").replaceAll("&.*", "");
        }
        getMapComposer().setAttribute("activeLayerName", activeLayerName);
        getMapComposer().setAttribute("mappolygonlayer", rgPolygonLayers.getSelectedItem().getValue());

        this.detach();
    }

    public void onClick$btnClear(Event event) {
        MapComposer mc = getMapComposer();
        if (layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        String script = mc.getOpenLayersJavascript().addFeatureSelectionTool();
        mc.getOpenLayersJavascript().execute(OpenLayersJavascript.iFrameReferences + script);
        displayGeom.setValue("");
        btnOk.setDisabled(true);
        btnClear.setDisabled(true);
    }

    public void onClick$btnCancel(Event event) {
        MapComposer mc = getMapComposer();
        if (layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        Clients.evalJavaScript("mapFrame.toggleClickHandler(true);");
        this.detach();
    }

    public void onCheck$rgPolygonLayers(Event event) {

        Radio selectedItem = rgPolygonLayers.getSelectedItem();

        //Add and remove layer to set as top layer
        String layerName = selectedItem.getValue();
        MapComposer mc = getMapComposer();
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
     *
     * @param event
     */
    public void onSearchPoint(Event event) {
        String searchPoint = (String) event.getData();
        String lon = searchPoint.split(",")[0];
        String lat = searchPoint.split(",")[1];

        logger.debug("*************************************");
        logger.debug("CommonData.getLayerList");
        //logger.debug(CommonData.getLayerList());
        logger.debug("*************************************");

        Object llist = CommonData.getLayerListJSONArray();
        JSONArray layerlist = JSONArray.fromObject(llist);
        //JSONArray layerlist = JSONArray.fromObject(CommonData.getLayerList());
        MapComposer mc = getMapComposer();

        List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
        Boolean searchComplete = false;
        for (int i = 0; i < activeLayers.size(); i++) {
            MapLayer ml = activeLayers.get(i);

            String activeLayerName = "none";
            if (ml.getUri() != null) {
                activeLayerName = ml.getUri().replaceAll("^.*ALA:", "").replaceAll("&.*", "");
            }
            logger.debug("ACTIVE LAYER: " + activeLayerName);
            if (ml.isDisplayed()) {
                for (int j = 0; j < layerlist.size(); j++) {
                    if (searchComplete) {
                        break;
                    }

                    JSONObject jo = layerlist.getJSONObject(j);
                    // logger.debug("********" + jo.getString("name"));
                    if (ml != null && jo.getString("type") != null
                            && jo.getString("type").length() > 0
                            && jo.getString("type").equalsIgnoreCase("contextual")
                            && jo.getString("name").equalsIgnoreCase(activeLayerName)) {

                        logger.debug(ml.getName());
                        Map<String, String> feature = GazetteerPointSearch.PointSearch(lon, lat, activeLayerName, CommonData.geoServer);
                        if (feature == null || !feature.containsKey("pid")) { // featureURI == null
                            continue;
                        }

                        //***
                        // avoid using WKT because it can be extremely large for some layers
                        // instead use the contextual envelope identifier
                        //***

                        //add feature to the map as a new layer
                        //TODO: why is "cl" needed for grid class layers?  fix layers-store/layers-service
                        JSONObject obj;
                        if (feature.get("pid").contains(":")) {
                            //TODO: as with "cl", this is not needed after layers-store/layers-service fix
                            String pid = feature.get("pid");
                            if (pid.indexOf(":") != pid.lastIndexOf(":")) {
                                pid = pid.substring(0, pid.lastIndexOf(":"));
                                feature.put("pid", pid);
                            }
                            obj = JSONObject.fromObject(readUrl(CommonData.layersServer + "/object/" + "cl" + feature.get("pid")));
                        } else {
                            obj = JSONObject.fromObject(readUrl(CommonData.layersServer + "/object/" + feature.get("pid")));
                        }

                        searchComplete = true;
                        displayGeom.setValue("layer: " + jo.getString("displayname") + "\r\n"
                                + "area: " + obj.getString("name"));

                        logger.debug("setting layerName from " + layerName);
                        layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
                        logger.debug("to " + layerName);
                        MapLayer mapLayer;

                        String url = obj.getString("wmsurl");
                        mapLayer = getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(txtLayerName.getValue()), txtLayerName.getValue(), url, 0.6f, /*metadata url*/ null,
                                null, LayerUtilities.WKT, null, null);

                        //add colour!
                        ml.setRedVal(255);
                        ml.setGreenVal(0);
                        ml.setBlueVal(0);

                        mapLayer.setPolygonLayer(true);

                        JSONObject objJson = JSONObject.fromObject(readUrl(CommonData.layersServer + "/object/" + feature.get("pid")));

                        Facet facet = Util.getFacetForObject(objJson, feature.get("value"));

                        if (facet != null) {
                            ArrayList<Facet> facets = new ArrayList<Facet>();
                            facets.add(facet);
                            mapLayer.setFacets(facets);

                            mapLayer.setWKT("ENVELOPE(" + objJson.getString("fid") + "," + feature.get("pid") + ")");
                        } else {
                            //no facet = not in Biocache, must use WKT
                            mapLayer.setWKT(readUrl(CommonData.layersServer + "/shape/wkt/" + feature.get("pid")));
                        }
                        MapLayerMetadata md = mapLayer.getMapLayerMetadata();
                        String bbString = "";
                        try {
                            bbString = objJson.getString("bbox");
                            bbString = bbString.replace("POLYGON((", "").replace("))", "").replace(",", " ");
                            String[] split = bbString.split(" ");
                            ArrayList<Double> bbox = new ArrayList<Double>();

                            bbox.add(Double.parseDouble(split[0]));
                            bbox.add(Double.parseDouble(split[1]));
                            bbox.add(Double.parseDouble(split[2]));
                            bbox.add(Double.parseDouble(split[3]));

                            md.setBbox(bbox);
                        } catch (Exception e) {
                            logger.debug("failed to parse: " + bbString, e);
                        }
                        try {
                            md.setMoreInfo(CommonData.layersServer + "/layers/view/more/" + jo.getString("spid"));
                        } catch (Exception e) {
                            logger.error("error setting map layer moreInfo", e);
                        }

                        //found the object on the layer
                        btnOk.setDisabled(false);
                        btnClear.setDisabled(false);


                        mc.updateLayerControls();
                        break;
                    }
                }
            }
        }
    }

    String getStringValue(String startAt, String tag, String json) {
        String typeStart = "\"" + tag + "\":\"";
        String typeEnd = "\"";
        int beginning = startAt == null ? 0 : json.indexOf(startAt) + startAt.length();
        int start = json.indexOf(typeStart, beginning) + typeStart.length();
        int end = json.indexOf(typeEnd, start);
        return json.substring(start, end);
    }

    private String readUrl(String feature) {
        StringBuilder content = new StringBuilder();

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
