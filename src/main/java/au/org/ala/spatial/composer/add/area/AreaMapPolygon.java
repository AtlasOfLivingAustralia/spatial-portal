package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.gazetteer.GazetteerPointSearch;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.layers.legend.Facet;
import org.apache.log4j.Logger;
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
        loadLayerSelection();
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
        getMapComposer().setAttribute("mappolygonlayer", rgPolygonLayers.getSelectedItem().getValue());

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

    public void loadLayerSelection() {
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

        Object llist = CommonData.getLayerListJSONArray();
        JSONArray layerlist = JSONArray.fromObject(llist);
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
                    JSONObject jo = layerlist.getJSONObject(j);
                    if (jo.getString(StringConstants.TYPE) != null
                            && jo.getString(StringConstants.TYPE).length() > 0
                            && StringConstants.CONTEXTUAL.equalsIgnoreCase(jo.getString(StringConstants.TYPE))
                            && jo.getString(StringConstants.NAME).equalsIgnoreCase(activeLayerName)) {

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
                        JSONObject obj;
                        obj = JSONObject.fromObject(readUrl(CommonData.getLayersServer() + "/object/" + feature.get(StringConstants.PID)));

                        searchComplete = true;
                        displayGeom.setValue("layer: " + jo.getString(StringConstants.DISPLAYNAME) + "\r\n"
                                + "area: " + obj.getString(StringConstants.NAME));

                        LOGGER.debug("setting layerName from " + layerName);
                        layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
                        LOGGER.debug("to " + layerName);
                        MapLayer mapLayer;

                        String url = obj.getString(StringConstants.WMSURL);
                        mapLayer = getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(txtLayerName.getValue()), txtLayerName.getValue(), url, 0.6f, /*metadata url*/ null,
                                null, LayerUtilitiesImpl.WKT, null, null);

                        //add colour!
                        ml.setRedVal(255);
                        ml.setGreenVal(0);
                        ml.setBlueVal(0);
                        ml.setDynamicStyle(true);
                        getMapComposer().updateLayerControls();

                        mapLayer.setPolygonLayer(true);

                        JSONObject objJson = JSONObject.fromObject(readUrl(CommonData.getLayersServer() + "/object/" + feature.get(StringConstants.PID)));

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
                            bbString = objJson.getString(StringConstants.BBOX);
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
                            md.setMoreInfo(CommonData.getLayersServer() + "/layers/view/more/" + jo.getString("spid"));
                        } catch (Exception e) {
                            LOGGER.error("error setting map layer moreInfo", e);
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
