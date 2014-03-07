package au.org.emii.portal.javascript;

import au.org.ala.spatial.data.BiocacheQuery;
import au.org.ala.spatial.data.Query;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.util.Validate;
import au.org.emii.portal.value.BoundingBox;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.util.Clients;

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

    private LayerUtilities layerUtilities = null;
    private SettingsSupplementary settingsSupplementary = null;
    protected final static Logger logger = Logger.getLogger(OpenLayersJavascriptImpl.class);

    @Override
    public String wrapWithSafeToProceed(String script) {
        return safeToProceedOpen + script + safeToProceedClose;
    }

    /*
     * Remove whitespace and carriage returns
     */
    @Override
    public String minify(String fragment) {
        return fragment.replaceAll("\\s+", " ");
    }

    @Override
    public String initialiseMap() {
        return "window.mapFrame.buildMapReal(); ";
    }

    @Override
    public void initialiseMapNow() {
        execute(initialiseMap());
    }

    @Override
    public void initialiseTransectDrawing(MapLayer mapLayer) {
        String script = "window.mapFrame.addLineDrawingLayer('" + mapLayer.getNameJS() + "','" + mapLayer.getLayer() + "','" + mapLayer.getUriJS() + "')";
        execute(script); // Safe to proceed - map loaded way beforehand

    }

    @Override
    public String addPolygonDrawingTool() {
        String script = "window.mapFrame.addPolygonDrawingTool()";
        return script;
    }

    @Override
    public String addRadiusDrawingTool() {
        String script = "window.mapFrame.addRadiusDrawingTool()";
        return script;
    }

    @Override
    public String addFeatureSelectionTool() {
        String script = "window.mapFrame.addFeatureSelectionTool()";
        return script;
    }

    @Override
    public void addPolygonDrawingToolSampling() {
        String script = "window.mapFrame.addPolygonDrawingToolSampling()";
        execute(script);
    }

    @Override
    public void addPolygonDrawingToolALOC() {
        String script = "window.mapFrame.addPolygonDrawingToolALOC()";
        execute(script);
    }

    @Override
    public void addPolygonDrawingToolFiltering() {
        String script = "window.mapFrame.addPolygonDrawingToolFiltering()";
        execute(script);
    }

    @Override
    public void removePolygonSampling() {
        String script = "window.mapFrame.removePolygonSampling()";
        execute(script);
    }

    @Override
    public void removePolygonALOC() {
        String script = "window.mapFrame.removePolygonALOC()";
        execute(script);
    }

    @Override
    public void removePolygonFiltering() {
        String script = "window.mapFrame.removePolygonFiltering()";
        execute(script);
    }

    @Override
    public void removeAreaSelection() {
        String script = "window.mapFrame.removeAreaSelection()";
        execute(script);
    }

    @Override
    public String addBoxDrawingTool() {
        String script = "window.mapFrame.addBoxDrawingTool()";
        return script;
    }

    @Override
    public void addGeoJsonLayer(String url) {
        String script = "window.mapFrame.addJsonFeatureToMap('" + url + "')";
        execute(script);
    }

    @Override
    public void addFeatureSelection() {
        String script = "window.mapFrame.setFeatureSelect();";
        execute(script);
    }

    @Override
    public void zoomGeoJsonExtentNow(MapLayer ml) {
        execute(zoomGeoJsonExtent(ml));

    }

    @Override
    public String zoomGeoJsonExtent(MapLayer ml) {
        String script = "";
        if (ml.getMapLayerMetadata() != null && ml.getMapLayerMetadata().getBboxString() != null) {
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
        String script;
        /* if map boundingbox is defined use it to zoom */
        if (ml.getMapLayerMetadata() != null) {
            if (ml.getMapLayerMetadata().getBboxString() != null) {
                script = "map.zoomToExtent(new OpenLayers.Bounds(" + ml.getMapLayerMetadata().getBboxString() + ")" + ".transform(" + "  new OpenLayers.Projection('EPSG:4326'),"
                        + "  map.getProjectionObject()));";
            } else {

                script = "window.mapFrame.loadBaseMap();";
            }
        } else {
            script = "window.mapFrame.zoomBoundsLayer('" + ml.getName() + "')";
        }
        execute(script);
    }

    @Override
    public String defineImageMapLayer(MapLayer mapLayer) {
        List<Double> bbox = mapLayer.getMapLayerMetadata().getBbox();

        String script = "	mapLayers['" + mapLayer.getUniqueIdJS() + "'] = new OpenLayers.Layer.Image(" + "		'" + mapLayer.getNameJS() + "', " + "		'" + mapLayer.getUriJS() + "', "
                + " 		new OpenLayers.Bounds(";

        if (mapLayer.getSubType() == LayerUtilities.ENVIRONMENTAL_ENVELOPE) {
            script += "112" + "," + "-44" + "," + "154" + "," + "-9";
        } else {
            script += bbox.get(0) + "," + bbox.get(1) + "," + bbox.get(2) + "," + bbox.get(3);
        }

        script += "		).transform(map.displayProjection, map.projection), "
                // + "             map.baseLayer.getExtent(),       "
                + " 		new OpenLayers.Size(" + settingsSupplementary.getValue("animation_width") + "," + settingsSupplementary.getValue("animation_height") + "), " + "		{" + "			format: 'image/png', "
                + "			opacity:" + mapLayer.getOpacity() + ", " + "			isBaseLayer : false, " + "			maxResolution: map.baseLayer.maxResolution, "
                + "           minResolution: map.baseLayer.minResolution, " + "           projection: new OpenLayers.Projection('EPSG:900913'), " + "			resolutions: map.baseLayer.resolutions "
                + "		} " + "	); " + // register for loading images...
                "registerLayer(mapLayers['" + mapLayer.getUniqueIdJS() + "']);";

        return wrapWithSafeToProceed(script);

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
     * @param id
     * @return
     */
    @Override
    public String removeMapLayer(MapLayer layer, boolean recursive) {
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
            script.append("if (mapLayers['").append(layer.getUniqueIdJS()).append("'] != null) { ").append("	window.mapFrame.removeFromSelectControl('").append(layer.getNameJS()).append("'); ").append("	map.removeLayer(mapLayers['").append(layer.getUniqueIdJS()).append("']); ").append("	mapLayers['").append(layer.getUniqueIdJS()).append("'] = null; ").append("} ");
            layer.setDisplayed(false);
        }
        if (recursive) {
            for (MapLayer child : layer.getChildren()) {
                script.append(removeMapLayer(child, recursive));
            }
        }

        return wrapWithSafeToProceed(script.toString());
    }

    /**
     * As removeLayer but execute immediately without returning any code
     *
     * @param id
     */
    @Override
    public void removeMapLayerNow(MapLayer mapLayer) {
        execute(iFrameReferences + removeMapLayer(mapLayer) + getAdditionalScript());
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
        execute(iFrameReferences + updateMapLayerIndexes(activeLayers));
    }

    @Override
    public void zoomToBoundingBoxNow(BoundingBox boundingBox) {
        execute(iFrameReferences + zoomToBoundingBox(boundingBox));
    }

    @Override
    public String zoomToBoundingBox(BoundingBox boundingBox) {
        String script = "var mapObj = window.frames.mapFrame.map;" + "map.zoomToExtent(" + "	(new OpenLayers.Bounds(" + Math.max(-180, boundingBox.getMinLongitude()) + ", "
                + Math.max(-85, boundingBox.getMinLatitude()) + ", " + Math.min(180, boundingBox.getMaxLongitude()) + ", " + Math.min(85, boundingBox.getMaxLatitude())
                + ")).transform(mapObj.displayProjection, mapObj.projection) " + "); ";
        return wrapWithSafeToProceed(script);
    }

    @Override
    public void zoomToBoundingBoxNow(BoundingBox boundingBox, boolean closest) {
        execute(iFrameReferences + zoomToBoundingBox(boundingBox, closest));
    }

    @Override
    public String zoomToBoundingBox(BoundingBox boundingBox, boolean closest) {
        String script = "var mapObj = window.frames.mapFrame.map;" + "map.zoomToExtent(" + "	(new OpenLayers.Bounds(" + Math.max(-180, boundingBox.getMinLongitude()) + ", "
                + Math.max(-85, boundingBox.getMinLatitude()) + ", " + Math.min(180, boundingBox.getMaxLongitude()) + ", " + Math.min(85, boundingBox.getMaxLatitude())
                + ")).transform(mapObj.displayProjection, mapObj.projection) " + ", " + closest + "); ";
        return wrapWithSafeToProceed(script);
    }

    @Override
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
        boolean okToAddLayer;
        if (mapLayer.isBaseLayer()) {
            associativeArray = "baseLayers";
        } else {
            associativeArray = "mapLayers";
        }

        StringBuilder script = new StringBuilder("if (" + associativeArray + "['" + mapLayer.getUniqueIdJS() + "'] == null) { ");

        switch (mapLayer.getType()) {

            case LayerUtilities.KML:
                script.append(defineKMLMapLayer(mapLayer));
                okToAddLayer = true;
                break;
            case LayerUtilities.GEOJSON:
                script.append(defineGeoJSONMapLayer(mapLayer));
                okToAddLayer = true;
                break;
            case LayerUtilities.WKT:
                script.append(defineWKTMapLayer(mapLayer));
                okToAddLayer = true;
                break;
            case LayerUtilities.IMAGELAYER:
                script.append(defineImageMapLayer(mapLayer));
                okToAddLayer = true;

                break;
            default:
                script.append(defineWMSMapLayer(mapLayer));
                okToAddLayer = true;
        }

        // close off the if statement
        script.append("} ");

        // only attempt to add a layer to the map if the type is supported
        if (okToAddLayer) {
            script.append("	if(").append(associativeArray).append("['").append(mapLayer.getUniqueIdJS()).append("'] != undefined) map.addLayer(").append(associativeArray).append("['").append(mapLayer.getUniqueIdJS()).append("']); ");

            /*
             * for base layers, we must also call setBaseLayer() now the map
             * knows about the layer
             */
            if (mapLayer.isBaseLayer()) {
                // remove previous baselayer (if any) to prevent supurious
                // requests
                script.append("if (currentBaseLayer != null) { " + "	map.removeLayer( ").append(associativeArray).append("[currentBaseLayer]); ").append(associativeArray).append("[currentBaseLayer] = null; ").append("} ").append("currentBaseLayer='").append(mapLayer.getUniqueIdJS()).append("'; ").append("map.setBaseLayer(").append(associativeArray).append("[currentBaseLayer]); ");
            }

            // add all the vector layers to be the selectable list
            // script.append("window.mapFrame.setVectorLayersSelectable();");
            mapLayer.setDisplayed(true);
        }

        if (recursive) {
            for (MapLayer child : mapLayer.getChildren()) {
                if (child.getHighlightState() == null || child.getHighlightState().equals("show")) {
                    script.append(activateMapLayer(child, recursive, alternativeScript));
                }
            }
        }

        return wrapWithSafeToProceed(getAdditionalScript() + script.toString());
    }

    @Override
    public String defineKMLMapLayer(MapLayer layer) {
        /*
         * can't have a GeoRSS baselayer so we don't need to decide where to
         * store the layer definition
         */
        String script = "	mapLayers['" + layer.getUniqueIdJS() + "'] = window.mapFrame.loadKmlFile('" + layer.getNameJS() + "','" + layer.getUriJS() + "');" + // register
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

    @Override
    public String defineWKTMapLayer(MapLayer layer) {
        String script = "" + "var vector_layer = window.mapFrame.addWKTFeatureToMap('" + layer.getWKT() + "','" + layer.getNameJS() + "','" + layer.getEnvColour() + "', " + layer.getOpacity() + ");"
                + "mapLayers['" + layer.getUniqueIdJS() + "'] = vector_layer;";

        if (layer.getPointsOfInterestWS() != null) {
            script += "mapLayers['" + layer.getUniqueIdJS() + "']" + ".pointsOfInterestWS = '" + layer.getPointsOfInterestWS() + "';";
        }

        script += "registerLayer(mapLayers['" + layer.getUniqueIdJS() + "']);";

        return wrapWithSafeToProceed(script);
    }

    @Override
    public String defineGeoJSONMapLayer(MapLayer layer) {
        /*
         * can't have a GeoJSON baselayer so we don't need to decide where to
         * store the layer definition
         */

        String script;
        if (layer.getGeoJSON() != null && layer.getGeoJSON().length() > 0) {
            script = "" + "var vector_layer = window.mapFrame.addJsonFeatureToMap('" + layer.getGeoJSON() + "','" + layer.getNameJS() + "','" + layer.getEnvColour() + "'," + layer.getSizeVal() + ", "
                    + layer.getOpacity() + "," + layer.getSizeUncertain() + ");" + "mapLayers['" + layer.getUniqueIdJS() + "'] = vector_layer;" + "registerLayer(mapLayers['" + layer.getUniqueIdJS()
                    + "']);";
        } else {
            script = "window.mapFrame.addJsonUrlToMap('" + layer.getUri() + "','" + layer.getNameJS() + "','" + layer.getEnvColour() + "'," + layer.getSizeVal() + ", " + layer.getOpacity() + ","
                    + layer.getSizeUncertain() + ");";
        }

        logger.debug("defineGeoJSONMapLayer: " + script);

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
            if (version.equals("1.3.0")) {
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
    @Override
    public String defineWMSMapLayer(MapLayer layer) {
        String associativeArray = null;
        String gutter = "0";
        String params = "";
        if (layer.isBaseLayer()) {
            associativeArray = "baseLayers";
        } else {
            associativeArray = "mapLayers";
            gutter = settingsSupplementary.getValue("openlayers_tile_gutter");
        }
        if (!Validate.empty(layer.getCql())) {
            params = "CQL_FILTER: '" + layer.getCqlJS() + "' ";
            params += ", ";
        }
        if (!Validate.empty(layer.getEnvParams())) {
            params += "env: '" + layer.getEnvParams().replace("'", "\\'") + "', ";
        }

        String script = "	" + associativeArray + "['" + layer.getUniqueIdJS() + "'] = new OpenLayers.Layer.WMS(" + "		'"
                + layer.getNameJS() + "', " + "		'"
                + layer.getUriJS().replace("wms?service=WMS&version=1.1.0&request=GetMap&", "wms\\/reflect?") + "', " + "		{"
                + ((layer.getSelectedStyleNameJS().equals("Default")) ? "" : "			styles: '" + layer.getSelectedStyleNameJS() + "', ") + "			layers: '" + layer.getLayerJS() + "', " + "			format: '"
                + layer.getImageFormat() + "', " + "         srs: 'epsg:900913', " + "			transparent: " + (!layer.isBaseLayer()) + ", " +
                "			" + params + wmsVersionDeclaration(layer) + // ","
                "		}, " + "		{ "
                // + "             " + "maxExtent: (new OpenLayers.Bounds(" +
                // bbox.get(0) + "," + bbox.get(1) + "," + bbox.get(2) + "," +
                // bbox.get(3) +
                // ")).transform(new OpenLayers.Projection('EPSG:4326'),map.getProjectionObject()),"
                + "			isBaseLayer: " + layer.isBaseLayer() + ", " + "			opacity: " + layer.getOpacity() + ", " + "			queryable: true, "
                // + "			buffer: " +
                // settingsSupplementary.getValue("openlayers_tile_buffer") +
                // ", "
                + "			gutter: " + gutter + ", " + "			wrapDateLine: true" // +
                + "		}  " + "	); " + // decorate with getFeatureInfoBuffer field
                // - do not set buffer
                // it's used to set a margin of cached
                // tiles around the viewport!!
                associativeArray + "['" + layer.getUniqueIdJS() + "']" + ".getFeatureInfoBuffer =" + settingsSupplementary.getValue("get_feature_info_buffer") + "; ";
        // add ws and bs urls for species layers
        if (layer.getSpeciesQuery() != null) {
            Query q = layer.getSpeciesQuery();
            if (q instanceof BiocacheQuery) {
                BiocacheQuery bq = (BiocacheQuery) q;
                try {
                    script += associativeArray + "['" + layer.getUniqueIdJS() + "']" + ".ws ='" + StringEscapeUtils.escapeJavaScript(bq.getWS()) + "'; " + associativeArray + "['"
                            + layer.getUniqueIdJS() + "']" + ".bs ='" + StringEscapeUtils.escapeJavaScript(bq.getBS()) + "'; ";
                } catch (Exception e) {
                    logger.error("error escaping for JS: " + bq.getBS(), e);
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
        execute(iFrameReferences + getAdditionalScript() + activateMapLayer(layer));
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
        return wrapWithSafeToProceed(script.toString());
    }

    /**
     * As activateateMapLayers but with immediate execution
     *
     * @param layers
     */
    @Override
    public void activateMapLayersNow(List<MapLayer> layers) {
        execute(iFrameReferences + activateMapLayers(layers));
    }

    /**
     * Set the opacity for the layer at the position key in the associative
     * array of layers
     *
     * @param key
     * @param percentage
     * @return
     */
    @Override
    public String setMapLayerOpacity(MapLayer mapLayer, float percentage) {

        /*
         * safe to force the associative array to be mapLayers here because the
         * user can't control the base layer (?)
         */
        String script = "mapLayers['" + mapLayer.getUniqueIdJS() + "'].setOpacity(" + percentage + "); mapLayers['" + mapLayer.getUniqueIdJS() + "'].redraw(); ";

        return wrapWithSafeToProceed(script);
    }

    @Override
    public void setMapLayerOpacityNow(MapLayer mapLayer, float percentage) {
        execute(iFrameReferences + setMapLayerOpacity(mapLayer, percentage));
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
        if (mapLayer.getGeoJSON() == null && mapLayer.getType() == LayerUtilities.GEOJSON) {
            return removeMapLayer(mapLayer) + activateMapLayer(mapLayer, false, true);
        } else {
            return removeMapLayer(mapLayer) + activateMapLayer(mapLayer);
        }

    }

    /**
     * Immediate execution of reloadMapLayer
     *
     * @param mapLayer
     */
    @Override
    public void reloadMapLayerNow(MapLayer mapLayer) {
        execute(iFrameReferences + reloadMapLayer(mapLayer));
    }

    /**
     * Convenience wrapper around ZKs JavaScript execution system
     *
     * @param script
     */
    @Override
    public void execute(String script) {
        if (mapLoaded()) {
            script = minify(script);
            logger.debug("exec javascript: " + script);
            Clients.evalJavaScript(script);
        } else {
            logger.info("refused to execute javascript - map not loaded");
        }
    }

    @Override
    public boolean mapLoaded() {
        PortalSession portalSession = (PortalSession) Sessions.getCurrent().getAttribute("portalSession");

        return ((portalSession != null) && portalSession.isMapLoaded());
    }

    /**
     * Create a popup window
     *
     * @param uri
     * @param title
     * @return
     */
    @Override
    public String popupWindow(String uri, String title) {
        String script = "window.open(\"" + uri + "\",\"" + title + "\");";
        return script;
    }

    /**
     * Create a popup window immediately
     *
     * @param uri
     * @param title
     * @return
     */
    @Override
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

    public SettingsSupplementary getSettingsSupplementary() {
        return settingsSupplementary;
    }

    @Required
    public void setSettingsSupplementary(SettingsSupplementary settingsSupplementary) {
        this.settingsSupplementary = settingsSupplementary;
    }

    private String additionalScript = "";

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
    public String getAdditionalScript() {
        String aS = additionalScript;
        additionalScript = ""; // reset after use
        return aS;
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
