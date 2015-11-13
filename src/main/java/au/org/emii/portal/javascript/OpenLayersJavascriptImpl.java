package au.org.emii.portal.javascript;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.BiocacheQuery;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Query;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import au.org.emii.portal.util.Validate;
import au.org.emii.portal.value.BoundingBox;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.util.Clients;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Support for generating javascript for use with an openlayers map held within
 * an iframe.
 * <p/>
 * Assumptions: 1) Map iframe is referenced via window.mapFrame 2) Map INSTANCE
 * is referenced via window.mapFrame.map 3) OpenLayers API is referenced via
 * window.mapFrame.OpenLayers 4) All non-base layers to be displayed on the map
 * exist in an associative array referenced via window.mapFrame.mapLayers
 *
 * @author geoff
 */
public class OpenLayersJavascriptImpl implements OpenLayersJavascript {

    public static final String SAFE_TO_PROCEED_CLOSE = "} ";
    public static final String SAFE_TO_PROCEED_OPEN = "if (typeof safeToProceed !== 'undefined' && safeToProceed) { ";
    private static final Logger LOGGER = Logger.getLogger(OpenLayersJavascriptImpl.class);
    private LayerUtilities layerUtilities = null;
    private String additionalScript = "";

    @Override
    public String getIFrameReferences() {
        return "var safeToProceed=true; " + "if (mapLayers == null) {" + "\tmapLayers = window.mapFrame.mapLayers; " + "} " + "if (map == null) {" + "\tmap = window.mapFrame.map; " + "} " + "if (OpenLayers == null) {" + "\tOpenLayers = window.mapFrame.OpenLayers; " + "} " + "if (baseLayers == null) {" + "\tbaseLayers = window.mapFrame.baseLayers; " + "} " + "if (currentBaseLayer == null) {" + "\tcurrentBaseLayer = window.mapFrame.currentBaseLayer; " + "} " + "if (currentBaseLayer == null) {" + "\tcurrentBaseLayer = window.mapFrame.currentBaseLayer; " + "} " + "if (registerLayer == null) {" + "\tregisterLayer = window.mapFrame.registerLayer; " + "} " + "if (\t(mapLayers == null) || " + "\t\t(map == null) || " + "\t\t(OpenLayers == null) || " + "\t\t(baseLayers == null)) { " + "\tsafeToProceed=false;" + "\talert(\'map subsystem is not fully loaded yet - this operation will fail\');" + "} ";
    }

    private String wrapWithSafeToProceed(String script) {
        return SAFE_TO_PROCEED_OPEN + script + SAFE_TO_PROCEED_CLOSE;
    }

    /*
     * Remove whitespace and carriage returns
     */
    @Override
    public String minify(String fragment) {
        return fragment.replaceAll("\\s+", " ");
    }

    @Override
    public String initialiseMap(BoundingBox boundingBox) {
        return "window.mapFrame.buildMapReal("
                + Math.max(-180, boundingBox.getMinLongitude()) + ", "
                + Math.max(-85, boundingBox.getMinLatitude()) + ", "
                + Math.min(180, boundingBox.getMaxLongitude()) + ", "
                + Math.min(85, boundingBox.getMaxLatitude()) + "); ";
    }

    @Override
    public String addPolygonDrawingTool() {
        return "window.mapFrame.addPolygonDrawingTool()";
    }

    @Override
    public String addRadiusDrawingTool() {
        return "window.mapFrame.addRadiusDrawingTool()";
    }

    @Override
    public String addFeatureSelectionTool() {
        return "window.mapFrame.addFeatureSelectionTool()";
    }

    @Override
    public String addBoxDrawingTool() {
        return "window.mapFrame.addBoxDrawingTool()";
    }

    @Override
    public void zoomGeoJsonExtentNow(MapLayer ml) {
        execute(zoomGeoJsonExtent(ml));

    }

    @Override
    public String zoomGeoJsonExtent(MapLayer ml) {
        String script;
        if (ml.getMapLayerMetadata().getBboxString() != null) {
            // cluster
            script = "window.mapFrame.map.zoomToExtent(new OpenLayers.Bounds.fromString('" + ml.getMapLayerMetadata().getBboxString()
                    + "').transform(new OpenLayers.Projection('EPSG:4326'),map.getProjectionObject()))";
        } else {
            script = "window.mapFrame.zoomBoundsGeoJSON('" + ml.getName().replaceAll("'", "\\'") + "')";
        }

        return script;

    }

    @Override
    public void zoomLayerExtent(MapLayer ml) {
        String script = "";
        /* if map boundingbox is defined use it to zoom */
        if (ml.getMapLayerMetadata().getBboxString() != null) {
            script = "map.zoomToExtent(new OpenLayers.Bounds(" + ml.getMapLayerMetadata().getBboxString() + ")" + ".transform(" + "  new OpenLayers.Projection('EPSG:4326'),"
                    + "  map.getProjectionObject()));";
            execute(script);
        }
    }

    @Override
    public void removeLayer(MapLayer ml) {

        String script = "window.mapFrame.removeItFromTheList('" + ml.getName() + "')";
        execute(script);

    }

    @Override
    public String removeMapLayer(MapLayer layer) {
        return removeMapLayer(layer, true);
    }

    /**
     * Generate the code to remove a layer from the map and the array of layers
     * - don't forget to scope your iFrameReferences first - see
     * removeLayerNow()
     *
     * @return
     */
    private String removeMapLayer(MapLayer layer, boolean recursive) {
        /*
         * Safe to assume always dealing with the mapLayers associative array
         * here because the user doesn't have control over the base layers (?)
         *
         * It is possible that a user has already removed a layer by unchecking
         * the display layer checkbox - in this case we must do nothing here or
         * the user will get a script error because the layer has already been
         * removed from openlayers
         */
        StringBuilder script = new StringBuilder();

        // if we aren't displaying the layer atm, we don't need to remove it...
        // the isDisplayed() flag can
        if (layer.isDisplayed()) {
            script.append("if (mapLayers['").append(layer.getUniqueIdJS()).append("'] != null) { ").append(" window.mapFrame.removeFromSelectControl('").append(layer.getNameJS()).append("'); ").append(" map.removeLayer(mapLayers['").append(layer.getUniqueIdJS()).append("']); ").append(" mapLayers['").append(layer.getUniqueIdJS()).append("'] = null; ").append("} ");
            layer.setDisplayed(false);
        }
        if (recursive) {
            for (MapLayer child : layer.getChildren()) {
                script.append(removeMapLayer(child, true));
            }
        }

        return wrapWithSafeToProceed(script.toString());
    }

    /**
     * As removeLayer but execute immediately without returning any code
     */
    @Override
    public void removeMapLayerNow(MapLayer mapLayer) {
        execute(getIFrameReferences() + removeMapLayer(mapLayer) + getAdditionalScript());
    }

    /**
     * Ask OpenLayers to re-order the layers displayed in the associative array
     * in the order of the passed in ArrayList of Layer. Usually you will want
     * to call this method after reordering the ArrayList (e.g., via
     * drag-n-drop) to update the map display.
     * <p/>
     * Higher values for the layer index will display above those with lower
     * values, whereas the items in the activeLayers list are the oposite with
     * low indexed values assumed to be 'above' those with higher values. This
     * is to allow the active layers list box to be displayed logically.
     *
     * @return
     */
    @Override
    public String updateMapLayerIndexes(List<MapLayer> activeLayers) {
        // safe to assume fixed order baselayers (?)
        int order = 0;
        StringBuilder script = new StringBuilder();
        for (int i = activeLayers.size() - 1; i > -1; i--) {
            if (activeLayers.get(i).isDisplayed()) {
                script.append("map.setLayerIndex(mapLayers['").append(activeLayers.get(i).getUniqueIdJS()).append("'], ").append(order).append("); ");
                order++;
            }
        }
        return wrapWithSafeToProceed(script.toString());
    }

    @Override
    public void updateMapLayerIndexesNow(List<MapLayer> activeLayers) {
        execute(getIFrameReferences() + updateMapLayerIndexes(activeLayers));
    }

    public String zoomToBoundingBox(BoundingBox boundingBox) {
        String script = "var mapObj = window.frames.mapFrame.map;" + "map.zoomToExtent(" + " (new OpenLayers.Bounds(" + Math.max(-180, boundingBox.getMinLongitude()) + ", "
                + Math.max(-85, boundingBox.getMinLatitude()) + ", " + Math.min(180, boundingBox.getMaxLongitude()) + ", " + Math.min(85, boundingBox.getMaxLatitude())
                + ")).transform(mapObj.displayProjection, mapObj.projection) " + "); ";
        return wrapWithSafeToProceed(script);
    }

    public void zoomToBoundingBoxNow(BoundingBox boundingBox, boolean closest) {
        execute(getIFrameReferences() + zoomToBoundingBox(boundingBox, closest));
    }

    @Override
    public String zoomToBoundingBox(BoundingBox boundingBox, boolean closest) {
        String script = "var mapObj = window.frames.mapFrame.map;" + "map.zoomToExtent(" + " (new OpenLayers.Bounds(" + Math.max(-180, boundingBox.getMinLongitude()) + ", "
                + Math.max(-85, boundingBox.getMinLatitude()) + ", " + Math.min(180, boundingBox.getMaxLongitude()) + ", " + Math.min(85, boundingBox.getMaxLatitude())
                + ")).transform(mapObj.displayProjection, mapObj.projection) " + ", " + closest + "); ";
        return wrapWithSafeToProceed(script);
    }

    public String activateMapLayer(MapLayer mapLayer) {
        return activateMapLayer(mapLayer, true, false);
    }

    /**
     * Activate a map layer described by the passed in Layer instance. If the id
     * already exists in the associative array of layers, nothing will happen
     * when you execute the script
     *
     * @param mapLayer
     * @return
     */
    @Override
    public String activateMapLayer(MapLayer mapLayer, boolean recursive, boolean alternativeScript) {
        String associativeArray;

        if (mapLayer.isBaseLayer()) {
            associativeArray = StringConstants.BASE_LAYERS;
        } else {
            associativeArray = StringConstants.MAP_LAYERS;
        }

        StringBuilder script = new StringBuilder("if (" + associativeArray + "['" + mapLayer.getUniqueIdJS() + "'] == null) { ");

        switch (mapLayer.getType()) {

            case LayerUtilitiesImpl.WKT:
                script.append(defineWKTMapLayer(mapLayer));
                break;
            default:
                script.append(defineWMSMapLayer(mapLayer));
        }

        // close off the if statement
        script.append("} ");

        // only attempt to add a layer to the map if the type is supported

        script.append(" if(").append(associativeArray).append("['").append(mapLayer.getUniqueIdJS()).append("'] != undefined) map.addLayer(").append(associativeArray).append("['").append(mapLayer.getUniqueIdJS()).append("']); ");

        /*
         * for base layers, we must also call setBaseLayer() now the map
         * knows about the layer
         */
        if (mapLayer.isBaseLayer()) {
            // remove previous baselayer (if any) to prevent supurious
            // requests
            script.append("if (currentBaseLayer != null) { ")
                    .append(" map.removeLayer( ").append(associativeArray).append("[currentBaseLayer]); ")
                    .append(associativeArray).append("[currentBaseLayer] = null; ").append("} ")
                    .append("currentBaseLayer='").append(mapLayer.getUniqueIdJS()).append("'; ")
                    .append("map.setBaseLayer(").append(associativeArray).append("[currentBaseLayer]); ");
        }

        // add all the vector layers to be the selectable list

        mapLayer.setDisplayed(true);

        if (recursive) {
            for (MapLayer child : mapLayer.getChildren()) {
                if (child.getHighlightState() == null || "show".equals(child.getHighlightState())) {
                    script.append(activateMapLayer(child, true, alternativeScript));
                }
            }
        }

        return wrapWithSafeToProceed(getAdditionalScript() + script.toString());
    }

    public String defineKMLMapLayer(MapLayer layer) {
        /*
         * can't have a GeoRSS baselayer so we don't need to decide where to
         * store the layer definition
         */
        String script = " mapLayers['" + layer.getUniqueIdJS() + "'] = window.mapFrame.loadKmlFile('" + layer.getNameJS() + "','" + layer.getUriJS() + "');" +
                // register
                // for
                // loading
                // images...
                "registerLayer(mapLayers['" + layer.getUniqueIdJS() + "']);";

        return wrapWithSafeToProceed(script);
    }

    @Override
    public void redrawFeatures(MapLayer selectedLayer) {
        String script = "window.mapFrame.redrawFeatures('" + selectedLayer.getName() + "','" + selectedLayer.getEnvColour() + "', " + selectedLayer.getOpacity() + "," + selectedLayer.getSizeVal()
                + "," + selectedLayer.getSizeUncertain() + ")";
        execute(script);
    }

    @Override
    public void redrawWKTFeatures(MapLayer selectedLayer) {
        String script = "window.mapFrame.redrawWKTFeatures('" + selectedLayer.getWKT() + "', '" + selectedLayer.getName() + "','" + selectedLayer.getEnvColour() + "', " + selectedLayer.getOpacity()
                + ")";
        execute(script);
    }

    public String defineWKTMapLayer(MapLayer layer) {
        String script = "" + "var vector_layer = window.mapFrame.addWKTFeatureToMap('" + layer.getWKT() + "','" + layer.getNameJS() + "','" + layer.getEnvColour() + "', " + layer.getOpacity() + ");"
                + "mapLayers['" + layer.getUniqueIdJS() + "'] = vector_layer;";

        if (layer.getPointsOfInterestWS() != null) {
            script += "mapLayers['" + layer.getUniqueIdJS() + "']" + ".pointsOfInterestWS = '" + layer.getPointsOfInterestWS() + "';";
        }

        script += "registerLayer(mapLayers['" + layer.getUniqueIdJS() + "']);";

        return wrapWithSafeToProceed(script);
    }

    /**
     * Requesting a WMS 1.3.0 layer means you have to request a CRS as well or
     * the map server (ncwms) won't return a map - force epsg:4326 for now
     *
     * @param layer
     * @return
     */
    private String wmsVersionDeclaration(MapLayer layer) {
        String version = layerUtilities.getWmsVersion(layer);
        String versionJS = "";
        if (version != null) {
            if ("1.3.0".equals(version)) {
                versionJS = "version: '1.3.0', " + "crs: 'epsg:4326' ";
            } else {
                versionJS = "version: '" + version + "' ";
            }
        }
        return versionJS;

    }

    /**
     * create an instance of OpenLayers.Layer.WMS.
     * <p/>
     * Base layers will be rendered differently and stored in the baseLayers
     * associative array instead of the mapLayers associative array
     *
     * @param layer
     * @return
     */
    public String defineWMSMapLayer(MapLayer layer) {
        String associativeArray;
        String gutter = "0";
        String params = "";
        if (layer.isBaseLayer()) {
            associativeArray = StringConstants.BASE_LAYERS;
        } else {
            associativeArray = StringConstants.MAP_LAYERS;
        }
        if (!Validate.empty(layer.getCql())) {
            params = "CQL_FILTER: '" + layer.getCqlJS() + "' ";
            params += ", ";
        }
        if (!Validate.empty(layer.getEnvParams())) {
            try {
                params += "env: '" + URLEncoder.encode(layer.getEnvParams(), StringConstants.UTF_8) + "', ";
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("failed to encode env params : " + layer.getEnvParams().replace("'", "\\'"), e);
            }
        }

        String dynamicStyle = "";
        if (layer.isPolygonLayer()) {
            String colour = Integer.toHexString((0xFF0000 & (layer.getRedVal() << 16)) | (0x00FF00 & layer.getGreenVal() << 8) | (0x0000FF & layer.getBlueVal()));
            while (colour.length() < 6) {
                colour = "0" + colour;
            }
            String filter;
            /*
                two types of areas are displayed as WMS.
                1. environmental envelopes. these are backed by a grid file.
                2. layerdb, objects table, areas referenced by a pid.  these are geometries.
             */
            if (layer.getUri().contains("ALA:envelope")) {
                filter = "";
                if (!layer.getUri().contains("sld_body")) {
                    filter = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\">"
                            + "<NamedLayer><Name>" + layerUtilities.getLayer(layer.getUri()) + "</Name>"
                            + "<UserStyle><FeatureTypeStyle><Rule><RasterSymbolizer><Geometry></Geometry>"
                            + "<ColorMap>"
                            + "<ColorMapEntry color=\"#ffffff\" opacity=\"0\" quantity=\"0\"/>"
                            + "<ColorMapEntry color=\"#" + colour + "\" opacity=\"1\" quantity=\"1\" />"
                            + "</ColorMap></RasterSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";
                }

            } else if (layer.getUri() != null && layer.getUri().contains("ColorMapEntry")) {
                //area from grid as contextual layer
                String uri = layer.getUri();

                //replace with current colour
                String str = "ColorMapEntry+color%3D%220x";
                int p = uri.indexOf(str);
                while (p > 0 && p + str.length() + 6 < uri.length()) {
                    uri = uri.substring(0, p + str.length()) + colour + uri.substring(p + str.length() + 6);
                    p = uri.indexOf(str, p + 1);
                }
                layer.setUri(uri);
                filter = "";
            } else {
                filter = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><StyledLayerDescriptor version=\"1.0.0\" xmlns=\"http://www.opengis.net/sld\">"
                        + "<NamedLayer><Name>" + layerUtilities.getLayer(layer.getUri()) + "</Name>"
                        + "<UserStyle><FeatureTypeStyle><Rule><Title>Polygon</Title><PolygonSymbolizer><Fill>"
                        + "<CssParameter name=\"fill\">#" + colour + "</CssParameter></Fill>"
                        + "</PolygonSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";
            }
            try {
                if (filter.length() == 0) {
                    dynamicStyle = "";
                } else {
                    dynamicStyle = "&sld_body=" + URLEncoder.encode(filter, StringConstants.UTF_8);
                }
            } catch (Exception e) {
                LOGGER.debug("invalid filter sld", e);
            }
        }

        String script = " " + associativeArray + "['" + layer.getUniqueIdJS() + "'] = new OpenLayers.Layer.WMS(" + "  '"
                + layer.getNameJS() + "', " + "  '"
                + layer.getUriJS().replace("wms?service=WMS&version=1.1.0&request=GetMap&", "wms\\/reflect?") + dynamicStyle + "', " + "  {"
                + ((StringConstants.DEFAULT.equals(layer.getSelectedStyleNameJS())) ? "" : "   styles: '" + layer.getSelectedStyleNameJS() + "', ")
                + "   layers: '" + layer.getLayerJS() + "', " + "   format: '"
                + layer.getImageFormat() + "', " + "         srs: 'epsg:3857', " + "   transparent: " + (!layer.isBaseLayer()) + ", " +
                "   " + params + wmsVersionDeclaration(layer) +
                "  }, " + "  { "
                + "   isBaseLayer: " + layer.isBaseLayer() + ", " + "   opacity: " + layer.getOpacity() + ", " + "   queryable: true, "
                + "   gutter: " + gutter + ", " + "   wrapDateLine: true"
                + "  }  " + " ); " +
                // decorate with getFeatureInfoBuffer field
                // - do not set buffer
                // it's used to set a margin of cached
                // tiles around the viewport!!
                associativeArray + "['" + layer.getUniqueIdJS() + "']" + ".getFeatureInfoBuffer =" + CommonData.getSettings().getProperty("get_feature_info_buffer") + "; ";
        // add ws and bs urls for species layers
        if (layer.getSpeciesQuery() != null) {
            Query q = layer.getSpeciesQuery();
            if (q instanceof BiocacheQuery) {
                BiocacheQuery bq = (BiocacheQuery) q;
                try {
                    script += associativeArray + "['" + layer.getUniqueIdJS() + "']" + ".ws ='" + StringEscapeUtils.escapeJavaScript(bq.getWS()) + "'; " + associativeArray + "['"
                            + layer.getUniqueIdJS() + "']" + ".bs ='" + StringEscapeUtils.escapeJavaScript(bq.getBS()) + "'; ";
                } catch (Exception e) {
                    LOGGER.error("error escaping for JS: " + bq.getBS(), e);
                }
            }
            if (q.flagRecordCount() > 0) {
                script += "parent.addFlaggedRecords('" + layer.getNameJS() + "','" + StringEscapeUtils.escapeJavaScript(q.getFlaggedRecords()) + "'); ";
            }
        }

        if (!layer.isBaseLayer()) {
            script += " " + associativeArray + "['" + layer.getUniqueIdJS() + "']" + ".featureInfoResponseType=" + layer.getType() + "; ";

        }
        // register for loading images...
        script += "registerLayer(" + associativeArray + "['" + layer.getUniqueIdJS() + "']);";

        return wrapWithSafeToProceed(script);
    }

    /**
     * As activeateMapLayer but executes immediately
     *
     * @param layer
     */
    @Override
    public void activateMapLayerNow(MapLayer layer) {
        execute(getIFrameReferences() + getAdditionalScript() + activateMapLayer(layer));
    }

    /**
     * Activate all layers in the passed in list. As each layer will be
     * sequentially added to openlayers' layer array, we need to iterate the
     * list in reverse order
     *
     * @return
     */
    @Override
    public String activateMapLayers(List<MapLayer> layers) {
        StringBuilder script = new StringBuilder();
        if (layers != null) {
            for (int i = layers.size() - 1; i >= 0; i--) {
                MapLayer layer = layers.get(i);
                // skip any layers that are not marked for display
                if (layer.isDisplayed()) {
                    script.append(activateMapLayer(layer, true, true));
                }
            }
        }
        if (script.toString().length() > 0) {
            return wrapWithSafeToProceed(script.toString());
        } else {
            return "";
        }
    }

    /**
     * As activateateMapLayers but with immediate execution
     *
     * @param layers
     */
    public void activateMapLayersNow(List<MapLayer> layers) {
        execute(getIFrameReferences() + activateMapLayers(layers));
    }

    /**
     * Set the opacity for the layer at the position key in the associative
     * array of layers
     *
     * @param percentage
     * @return
     */
    public String setMapLayerOpacity(MapLayer mapLayer, float percentage) {

        /*
         * safe to force the associative array to be mapLayers here because the
         * user can't control the base layer (?)
         */
        String script = "mapLayers['" + mapLayer.getUniqueIdJS() + "'].setOpacity(" + percentage + "); mapLayers['" + mapLayer.getUniqueIdJS() + "'].redraw(); ";

        return wrapWithSafeToProceed(script);
    }

    public void setMapLayerOpacityNow(MapLayer mapLayer, float percentage) {
        execute(getIFrameReferences() + setMapLayerOpacity(mapLayer, percentage));
    }

    /**
     * Convenience method to reload a map layer by removing it and then adding
     * it
     *
     * @param mapLayer
     * @return
     */
    @Override
    public String reloadMapLayer(MapLayer mapLayer) {
        return removeMapLayer(mapLayer) + activateMapLayer(mapLayer);
    }

    /**
     * Immediate execution of reloadMapLayer
     *
     * @param mapLayer
     */
    @Override
    public void reloadMapLayerNow(MapLayer mapLayer) {
        execute(getIFrameReferences() + reloadMapLayer(mapLayer));
    }

    /**
     * Convenience wrapper around ZKs JavaScript execution system
     *
     * @param script
     */
    @Override
    public void execute(String script) {
        if (mapLoaded()) {
            String minScript = minify(script);
            LOGGER.debug("exec javascript: " + minScript);
            Clients.evalJavaScript(minScript);
        } else {
            LOGGER.debug("refused to execute javascript - map not loaded");
        }
    }

    @Override
    public boolean mapLoaded() {
        PortalSession portalSession = (PortalSession) Sessions.getCurrent().getAttribute(StringConstants.PORTAL_SESSION);

        return portalSession != null && portalSession.isMapLoaded();
    }

    /**
     * Create a popup window
     *
     * @param uri
     * @param title
     * @return
     */
    public String popupWindow(String uri, String title) {
        return "window.open(\"" + uri + "\",\"" + title + "\");";
    }

    /**
     * Create a popup window immediately
     *
     * @param uri
     * @param title
     * @return
     */
    public void popupWindowNow(String uri, String title) {
        execute(popupWindow(uri, title));
    }

    public LayerUtilities getLayerUtilities() {
        return layerUtilities;
    }

    @Required
    public void setLayerUtilities(LayerUtilities layerUtilities) {
        this.layerUtilities = layerUtilities;
    }

    public String getAdditionalScript() {
        String aS = additionalScript;
        // reset after use
        additionalScript = "";
        return aS;
    }

    @Override
    public void setAdditionalScript(String additionalScript) {
        if (this.additionalScript == null) {
            this.additionalScript = "";
        }
        if (additionalScript != null) {
            this.additionalScript += additionalScript;
        }
    }

    @Override
    public void useAdditionalScript() {
        if (additionalScript != null && additionalScript.length() > 0) {
            this.execute(getAdditionalScript());
        }
    }

    @Override
    public String setBaseLayer(String baseLayer) {
        return "changeBaseLayer('" + baseLayer + "');";
    }
}
