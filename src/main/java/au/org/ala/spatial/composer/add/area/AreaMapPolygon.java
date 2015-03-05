package au.org.ala.spatial.composer.add.area;

import au.org.ala.legend.Facet;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.gazetteer.GazetteerPointSearch;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;

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

    private static final Logger LOGGER = Logger.getLogger(AreaMapPolygon.class);
    private Textbox txtLayerName;
    private Button btnOk;
    private Button btnClear;
    private Radiogroup rgPolygonLayers;
    private Textbox displayGeom;

    @Override
    public void afterCompose() {
        super.afterCompose();
        txtLayerName.setValue(getMapComposer().getNextAreaLayerName(CommonData.lang(StringConstants.DEFAULT_AREA_LAYER_NAME)));
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

        String activeLayerName = StringConstants.NONE;
        if (ml.getUri() != null) {
            activeLayerName = ml.getUri().replaceAll("^.*ALA:", "").replaceAll("&.*", "");
        }
        getMapComposer().setAttribute("activeLayerName", activeLayerName);

        try {
            if (rgPolygonLayers.getSelectedItem() == null) {
                if (rgPolygonLayers.getItemCount() > 0) {
                    getMapComposer().setAttribute("mappolygonlayer", rgPolygonLayers.getItemAtIndex(0).getValue());
                }
            } else {
                getMapComposer().setAttribute("mappolygonlayer", rgPolygonLayers.getSelectedItem().getValue());
            }
        } catch (Exception e) {
            LOGGER.error("failed to set map area polygon selected by the radio button selection", e);
        }

        this.detach();
    }

    public void onClick$btnClear(Event event) {
        MapComposer mc = getMapComposer();
        if (layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        String script = mc.getOpenLayersJavascript().addFeatureSelectionTool();
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().getIFrameReferences() + script);
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

        LOGGER.debug("*************************************");
        LOGGER.debug("CommonData.getLayerList");
        LOGGER.debug("*************************************");

        JSONArray layerlist = CommonData.getLayerListJSONArray();
        MapComposer mc = getMapComposer();

        List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
        Boolean searchComplete = false;
        for (int i = 0; i < activeLayers.size(); i++) {
            MapLayer ml = activeLayers.get(i);

            String activeLayerName = StringConstants.NONE;
            if (ml.getUri() != null) {
                activeLayerName = ml.getUri().replaceAll("^.*ALA:", "").replaceAll("&.*", "");
            }
            LOGGER.debug("ACTIVE LAYER: " + activeLayerName);
            if (ml.isDisplayed()) {
                for (int j = 0; j < layerlist.size() && !searchComplete; j++) {
                    JSONObject jo = (JSONObject) layerlist.get(j);
                    if (jo.get(StringConstants.TYPE) != null
                            && jo.get(StringConstants.TYPE).toString().length() > 0
                            && StringConstants.CONTEXTUAL.equalsIgnoreCase(jo.get(StringConstants.TYPE).toString())
                            && jo.get(StringConstants.NAME).toString().equalsIgnoreCase(activeLayerName)) {

                        LOGGER.debug(ml.getName());
                        Map<String, String> feature = GazetteerPointSearch.pointSearch(lon, lat, activeLayerName, CommonData.getGeoServer());
                        if (feature == null || !feature.containsKey(StringConstants.PID)) {
                            continue;
                        }

                        //***
                        // avoid using WKT because it can be extremely large for some layers
                        // instead use the contextual envelope identifier
                        //***

                        //add feature to the map as a new layer
                        JSONObject obj = null;
                        JSONParser jp = new JSONParser();
                        try {
                            obj = (JSONObject) jp.parse(readUrl(CommonData.getLayersServer() + "/object/" + feature.get(StringConstants.PID)));
                        } catch (ParseException e) {
                            LOGGER.error("failed to parse object: " + feature.get(StringConstants.PID));
                        }

                        searchComplete = true;
                        displayGeom.setValue("layer: " + jo.get(StringConstants.DISPLAYNAME) + "\r\n"
                                + "area: " + obj.get(StringConstants.NAME));

                        LOGGER.debug("setting layerName from " + layerName);
                        layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
                        LOGGER.debug("to " + layerName);
                        MapLayer mapLayer;

                        String url = obj.get(StringConstants.WMSURL).toString();
                        mapLayer = getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(txtLayerName.getValue()), txtLayerName.getValue(), url, 0.6f, /*metadata url*/ null,
                                null, LayerUtilitiesImpl.WKT, null, null);

                        layerName = mapLayer.getName();

                        //add colour!
                        int colour = Util.nextColour();
                        int r = (colour >> 16) & 0x000000ff;
                        int g = (colour >> 8) & 0x000000ff;
                        int b = (colour) & 0x000000ff;

                        ml.setRedVal(r);
                        ml.setGreenVal(g);
                        ml.setBlueVal(b);
                        ml.setDynamicStyle(true);
                        getMapComposer().applyChange(ml);
                        getMapComposer().updateLayerControls();

                        mapLayer.setPolygonLayer(true);

                        JSONObject objJson = null;
                        try {
                            objJson = (JSONObject) jp.parse(readUrl(CommonData.getLayersServer() + "/object/" + feature.get(StringConstants.PID)));
                        } catch (ParseException e) {
                            LOGGER.error("failed to parse for object: " + feature.get(StringConstants.PID));
                        }

                        Facet facet = null;
                        //only get field data if it is an intersected layer (to exclude layers containing points)
                        if (CommonData.getLayer((String) objJson.get(StringConstants.FID)) != null) {
                            facet = Util.getFacetForObject(feature.get(StringConstants.VALUE), (String) objJson.get(StringConstants.FID));
                        }

                        if (facet != null) {
                            List<Facet> facets = new ArrayList<Facet>();
                            facets.add(facet);
                            mapLayer.setFacets(facets);

                            mapLayer.setWKT(readUrl(CommonData.getLayersServer() + "/shape/wkt/" + feature.get(StringConstants.PID)));
                        } else {
                            //no facet = not in Biocache, must use WKT
                            mapLayer.setWKT(readUrl(CommonData.getLayersServer() + "/shape/wkt/" + feature.get(StringConstants.PID)));
                        }
                        MapLayerMetadata md = mapLayer.getMapLayerMetadata();
                        String bbString = "";
                        try {
                            bbString = objJson.get(StringConstants.BBOX).toString();
                            bbString = bbString.replace(StringConstants.POLYGON + "((", "").replace("))", "").replace(",", " ");
                            String[] split = bbString.split(" ");
                            List<Double> bbox = new ArrayList<Double>();

                            bbox.add(Double.parseDouble(split[0]));
                            bbox.add(Double.parseDouble(split[1]));
                            bbox.add(Double.parseDouble(split[2]));
                            bbox.add(Double.parseDouble(split[3]));

                            md.setBbox(bbox);
                        } catch (NumberFormatException e) {
                            LOGGER.debug("failed to parse: " + bbString, e);
                        }
                        try {
                            md.setMoreInfo(CommonData.getLayersServer() + "/layers/view/more/" + jo.get("spid"));
                        } catch (Exception e) {
                            LOGGER.error("error setting map layer moreInfo: " + (jo != null ? jo.toString() : "jo is null"), e);
                        }

                        //found the object on the layer
                        btnOk.setDisabled(false);
                        btnClear.setDisabled(false);

                        mc.updateLayerControls();
                    }
                }
            }
        }
    }

    private String readUrl(String feature) {
        StringBuilder content = new StringBuilder();

        HttpURLConnection conn = null;
        try {
            // Construct data

            // Send data
            URL url = new URL(feature);
            conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                content.append(line);
            }

        } catch (Exception e) {
            LOGGER.error("failed to read URL: " + feature, e);
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    LOGGER.error("failed to close url: " + feature, e);
                }
            }
        }
        return content.toString();
    }
}
