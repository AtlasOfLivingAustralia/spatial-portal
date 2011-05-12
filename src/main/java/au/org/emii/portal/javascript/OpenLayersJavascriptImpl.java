package au.org.emii.portal.javascript;

import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.util.Validate;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.util.Clients;

/**
 * Support for generating javascript for use with an openlayers map held within an
 * iframe.
 *
 * Assumptions:
 * 1) 	Map iframe is referenced via window.mapFrame
 * 2) 	Map INSTANCE is referenced via window.mapFrame.map
 * 3)	OpenLayers API is referenced via window.mapFrame.OpenLayers
 * 4) 	All non-base layers to be displayed on the map exist in an associative array referenced
 * 		via window.mapFrame.mapLayers
 *
 * @author geoff
 *
 */
public class OpenLayersJavascriptImpl implements OpenLayersJavascript {

    private LayerUtilities layerUtilities = null;
    private SettingsSupplementary settingsSupplementary = null;
    protected final static Logger logger = Logger.getLogger(OpenLayersJavascriptImpl.class);

    @Override
    public String wrapWithSafeToProceed(String script) {
        return safeToProceedOpen
                + script
                + safeToProceedClose;
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
        execute(
                initialiseMap());
    }

    @Override
    public void initialiseTransectDrawing(MapLayer mapLayer) {
        String script = "window.mapFrame.addLineDrawingLayer('"
                + mapLayer.getNameJS() + "','"
                + mapLayer.getLayer() + "','"
                + mapLayer.getUriJS() + "')";
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
            //cluster
            script = "window.mapFrame.map.zoomToExtent(new OpenLayers.Bounds.fromString('"
                    + ml.getMapLayerMetadata().getBboxString()
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
                script = "map.zoomToExtent(new OpenLayers.Bounds("
                        + ml.getMapLayerMetadata().getBboxString() + ")"
                        + ".transform("
                        + "  new OpenLayers.Projection('EPSG:4326'),"
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
    public String defineAnimatedLayer(MapLayer mapLayer) {
        List<Double> bbox = mapLayer.getMapLayerMetadata().getBbox();

        String script =
                "	mapLayers['" + mapLayer.getUniqueIdJS() + "'] = new OpenLayers.Layer.Image("
                + "		'" + mapLayer.getNameJS() + "', "
                + "		'" + layerUtilities.getAnimationUriJS(mapLayer) + "', "
                + " 		new OpenLayers.Bounds("
                + bbox.get(0) + ","
                + bbox.get(1) + ","
                + bbox.get(2) + ","
                + bbox.get(3)
                + "		), "
                + " 		new OpenLayers.Size("
                + settingsSupplementary.getValue("animation_width") + ","
                + settingsSupplementary.getValue("animation_height") + "), "
                + "		{"
                + "			format: 'image/gif', "
                + "			opacity:" + mapLayer.getOpacity() + ", "
                + "			isBaseLayer : false, "
                + "			maxResolution: map.baseLayer.maxResolution, "
                + "           minResolution: map.baseLayer.minResolution, "
                + "			resolutions: map.baseLayer.resolutions "
                + "		} "
                + "	); "
                + // decorate with extra fields =D by assignment
                "mapLayers['" + mapLayer.getUniqueIdJS() + "']"
                + ".timeSeriesPlotUri='" + layerUtilities.getAnimationTimeSeriesPlotUriJS(mapLayer) + "'; "
                + "mapLayers['" + mapLayer.getUniqueIdJS() + "']"
                + ".queryable=true; "
                + "mapLayers['" + mapLayer.getUniqueIdJS() + "']"
                + ".animatedNcwmsLayer=true; "
                + "mapLayers['" + mapLayer.getUniqueIdJS() + "']"
                + ".featureInfoResponseType=" + mapLayer.getType() + "; "
                + "mapLayers['" + mapLayer.getUniqueIdJS() + "']"
                + ".baseUri='" + layerUtilities.getAnimationFeatureInfoUriJS(mapLayer) + "'; "
                + decorateWithMetadata("mapLayers", mapLayer)
                + // register for loading images...
                "registerLayer(mapLayers['" + mapLayer.getUniqueIdJS() + "']);";


        return wrapWithSafeToProceed(script);

    }

    @Override
    public String defineImageMapLayer(MapLayer mapLayer) {
        List<Double> bbox = mapLayer.getMapLayerMetadata().getBbox();

        String script =
                "	mapLayers['" + mapLayer.getUniqueIdJS() + "'] = new OpenLayers.Layer.Image("
                + "		'" + mapLayer.getNameJS() + "', "
                + "		'" + mapLayer.getUriJS() + "', "
                + " 		new OpenLayers.Bounds(";

        if (mapLayer.getSubType() == LayerUtilities.ENVIRONMENTAL_ENVELOPE) {
            script +=  "112.9" + ","
                + "-43.8" + ","
                + "153.64" + ","
                + "-9";
        } else {
            script += bbox.get(0) + ","
                + bbox.get(1) + ","
                + bbox.get(2) + ","
                + bbox.get(3);
        }
        
        script += "		).transform(map.displayProjection, map.projection), "
                //+ "             map.baseLayer.getExtent(),       "
                + " 		new OpenLayers.Size("
                + settingsSupplementary.getValue("animation_width") + ","
                + settingsSupplementary.getValue("animation_height") + "), "
                + "		{"
                + "			format: 'image/png', "
                + "			opacity:" + mapLayer.getOpacity() + ", "
                + "			isBaseLayer : false, "
                + "			maxResolution: map.baseLayer.maxResolution, "
                + "           minResolution: map.baseLayer.minResolution, "
                + "           projection: new OpenLayers.Projection('EPSG:900913'), "
                + "			resolutions: map.baseLayer.resolutions "
                + "		} "
                + "	); "
                + // register for loading images...
                "registerLayer(mapLayers['" + mapLayer.getUniqueIdJS() + "']);";



        return wrapWithSafeToProceed(script);

    }

    /**
     * Animate the layer - display a big ass animated
     * GIF
     * @param mapLayer
     * @return
     */
    @Override
    public String animate(MapLayer mapLayer) {

        String script =
                "if (mapLayers['" + mapLayer.getUniqueIdJS() + "'] == null) { "
                + defineAnimatedLayer(mapLayer)
                + "	map.addLayer(mapLayers['" + mapLayer.getUniqueIdJS() + "']); "
                + "} ";


        // 'http://obsidian:8080/ncWMS/wms?LAYERS=67%2Ftemp&ELEVATION=-5&TIME=2006-09-01T12:00:00.000Z/2006-09-19T12:00:00.000Z&TRANSPARENT=true&STYLES=BOXFILL%2Frainbow&CRS=EPSG%3A4326&COLORSCALERANGE=9.405405%2C29.66159&NUMCOLORBANDS=254&LOGSCALE=false&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&EXCEPTIONS=XML&FORMAT=image/gif&BBOX=-180,-90,180,90&WIDTH=512&HEIGHT=400', // URL to the image
        mapLayer.setDisplayed(true);
        return wrapWithSafeToProceed(script);
    }

    @Override
    public void removeLayer(MapLayer ml) {


        String script = "window.mapFrame.removeItFromTheList('" + ml.getName() + "')";
        execute(script);

    }

    @Override
    public String removeMapLayer(MapLayer layer) {
        return removeMapLayer(layer, false);
    }

    /**
     * Generate the code to remove a layer from the map and the
     * array of layers - don't forget to scope your iFrameReferences
     * first - see removeLayerNow()
     * @param id
     * @return
     */
    @Override
    public String removeMapLayer(MapLayer layer, boolean recursive) {
        /*
         * Safe to assume always dealing with the mapLayers associative array
         * here because the user doesn't have control over the base layers (?)
         *
         * It is possible that a user has already removed a layer by
         * unchecking the display layer checkbox - in this case we must
         * do nothing here or the user will get a script error because the
         * layer has already been removed from openlayers
         */
        StringBuffer script = new StringBuffer();

        // if we aren't displaying the layer atm, we don't need to remove it...
        // the isDisplayed() flag can
        if (layer.isDisplayed()) {
            script.append(
                    "if (mapLayers['" + layer.getUniqueIdJS() + "'] != null) { "
                    + "	window.mapFrame.removeFromSelectControl('" + layer.getNameJS() + "'); "
                    + "	map.removeLayer(mapLayers['" + layer.getUniqueIdJS() + "']); "
                    + "	mapLayers['" + layer.getUniqueIdJS() + "'] = null; "
                    + "} ");
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
     * As removeLayer but execute immediately without returning
     * any code
     * @param id
     */
    @Override
    public void removeMapLayerNow(MapLayer mapLayer) {
        execute(
                iFrameReferences
                + removeMapLayer(mapLayer)
                + getAdditionalScript());
    }

    /**
     * Ask OpenLayers to re-order the layers displayed in the associative
     * array in the order of the passed in ArrayList of Layer.  Usually
     * you will want to call this method after reordering the ArrayList
     * (e.g., via drag-n-drop) to update the map display.
     *
     * Higher values for the layer index will display above those with
     * lower values, whereas the items in the activeLayers list are the
     * oposite with low indexed values assumed to be 'above' those with
     * higher values.  This is to allow the active layers list box to
     * be displayed logically.
     *
     * @return
     */
    @Override
    public String updateMapLayerIndexes(List<MapLayer> activeLayers) {
        // safe to assume fixed order baselayers (?)
        int order = 0;
        StringBuffer script = new StringBuffer();
        for (int i = activeLayers.size() - 1; i > -1; i--) {
            if (activeLayers.get(i).isDisplayed()) {
                script.append(
                        "map.setLayerIndex(mapLayers['"
                        + activeLayers.get(i).getUniqueIdJS()
                        + "'], " + order + "); ");
                order++;
            }
        }
        return wrapWithSafeToProceed(script.toString());
    }

    @Override
    public void updateMapLayerIndexesNow(List<MapLayer> activeLayers) {
        execute(
                iFrameReferences
                + updateMapLayerIndexes(activeLayers));
    }

    @Override
    public void zoomToBoundingBoxNow(BoundingBox boundingBox) {
        execute(
                iFrameReferences
                + zoomToBoundingBox(boundingBox));
    }

    @Override
    public String zoomToBoundingBox(BoundingBox boundingBox) {
        String script =
                "var mapObj = window.frames.mapFrame.map;"
                + "map.zoomToExtent("
                + "	(new OpenLayers.Bounds("
                + boundingBox.getMinLongitude() + ", "
                + boundingBox.getMinLatitude() + ", "
                + boundingBox.getMaxLongitude() + ", "
                + boundingBox.getMaxLatitude()
                + ")).transform(mapObj.displayProjection, mapObj.projection) "
                + "); ";
        return wrapWithSafeToProceed(script);
    }

    @Override
    public String activateMapLayer(MapLayer mapLayer) {
        return activateMapLayer(mapLayer, false, false);
    }

    /**
     * Activate a map layer described by the passed in Layer instance.  If the id already
     * exists in the associative array of layers, nothing will happen when you execute
     * the script
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

        StringBuffer script = new StringBuffer(
                "if (" + associativeArray + "['" + mapLayer.getUniqueIdJS() + "'] == null) { ");


        switch (mapLayer.getType()) {
            case LayerUtilitiesImpl.WMS_1_0_0:
            case LayerUtilitiesImpl.WMS_1_1_0:
            case LayerUtilitiesImpl.WMS_1_1_1:
            case LayerUtilitiesImpl.WMS_1_3_0:
            case LayerUtilitiesImpl.NCWMS:
            case LayerUtilitiesImpl.THREDDS:
                if (mapLayer.isCurrentlyAnimated()) {
                    script.append(defineAnimatedLayer(mapLayer));
                    okToAddLayer = true;
                } else {
                    script.append(defineWMSMapLayer(mapLayer));
                    okToAddLayer = true;
                }
                break;
            case LayerUtilitiesImpl.GEORSS:
                script.append(defineGeoRSSMapLayer(mapLayer));
                okToAddLayer = true;
                break;
            case LayerUtilitiesImpl.KML:
                script.append(defineKMLMapLayer(mapLayer));
                okToAddLayer = true;
                break;
            case LayerUtilitiesImpl.GEOJSON:
                //script.append("window.mapFrame.addJsonFeatureToMap('" + mapLayer.getGeoJSON() + "', '" + mapLayer.getName() + "')");
                if (alternativeScript && mapLayer.getMapLayerMetadata().getPartsCount() > 0) {
                    script.append("window.mapFrame.drawFeaturesGeoJsonUrl('" + mapLayer.getUri() + "'," + mapLayer.getMapLayerMetadata().getPartsCount() + ", '" + mapLayer.getName() + "','" + mapLayer.getEnvColour() + "', " + mapLayer.getOpacity() + "," + mapLayer.getSizeVal() + "," + mapLayer.getSizeUncertain() + ")");
                } else {
                    script.append(defineGeoJSONMapLayer(mapLayer));
                }
                okToAddLayer = true;
                break;
            case LayerUtilitiesImpl.WKT:
                script.append(defineWKTMapLayer(mapLayer));
                okToAddLayer = true;
                break;
            case LayerUtilitiesImpl.IMAGELAYER:
                script.append(defineImageMapLayer(mapLayer));
                okToAddLayer = true;

                break;
            default:
                okToAddLayer = false;
                logger.error(
                        "unsupported type " + mapLayer.getType() + " reached in activateMapLayer "
                        + "for layer: " + mapLayer.getUniqueIdJS() + " at " + mapLayer.getUriJS());

        }

        // close off the if statement
        script.append("} ");

        // only attempt to add a layer to the map if the type is supported
        if (okToAddLayer) {
            script.append(
                    "	if(" + associativeArray + "['" + mapLayer.getUniqueIdJS() + "'] != undefined) map.addLayer(" + associativeArray + "['" + mapLayer.getUniqueIdJS() + "']); ");

            /* for base layers, we must also call setBaseLayer() now the map knows about
             * the layer
             */
            if (mapLayer.isBaseLayer()) {
                // remove previous baselayer (if any) to prevent supurious requests
                script.append(
                        "if (currentBaseLayer != null) { "
                        + "	map.removeLayer( " + associativeArray + "[currentBaseLayer]); "
                        + associativeArray + "[currentBaseLayer] = null; "
                        + "} "
                        + "currentBaseLayer='" + mapLayer.getUniqueIdJS() + "'; "
                        + "map.setBaseLayer(" + associativeArray + "[currentBaseLayer]); ");
            }

            //add all the vector layers to be the selectable list

            //script.append("window.mapFrame.setVectorLayersSelectable();");
            mapLayer.setDisplayed(true);
        }

        if (recursive) {
            for (MapLayer child : mapLayer.getChildren()) {
                script.append(activateMapLayer(child, recursive, alternativeScript));
            }
        }

        String test = wrapWithSafeToProceed(getAdditionalScript() + script.toString());
        //   System.out.println("EXECUTING - " + test);

        return wrapWithSafeToProceed(getAdditionalScript() + script.toString());
    }

    @Override
    public String defineKMLMapLayer(MapLayer layer) {
        /* can't have a GeoRSS baselayer so we don't need to decide where to store
         * the layer definition
         */
        String script =
                "	mapLayers['" + layer.getUniqueIdJS() + "'] = window.mapFrame.loadKmlFile('" + layer.getNameJS() + "','" + layer.getUriJS() + "');"
                + // register for loading images...
                "registerLayer(mapLayers['" + layer.getUniqueIdJS() + "']);";

        return wrapWithSafeToProceed(script);
    }

    @Override
    public void redrawFeatures(MapLayer selectedLayer) {
        String script;
        if (selectedLayer.getGeoJSON() == null || selectedLayer.getGeoJSON().length() == 0) {
            script = "window.mapFrame.redrawFeaturesGeoJsonUrl('" + selectedLayer.getUri() + "'," + selectedLayer.getMapLayerMetadata().getPartsCount() + ", '" + selectedLayer.getName() + "','" + selectedLayer.getEnvColour() + "', " + selectedLayer.getOpacity() + "," + selectedLayer.getSizeVal() + "," + selectedLayer.getSizeUncertain() + ")";
        } else {
            script = "window.mapFrame.redrawFeatures('" + selectedLayer.getGeoJSON() + "', '" + selectedLayer.getName() + "','" + selectedLayer.getEnvColour() + "', " + selectedLayer.getOpacity() + "," + selectedLayer.getSizeVal() + "," + selectedLayer.getSizeUncertain() + ")";
        }
        //String script = "window.mapFrame.redrawUrlFeatures('" + selectedLayer.getUri() + "', '" + selectedLayer.getName() + "','" + selectedLayer.getEnvColour() + "', " + selectedLayer.getOpacity() + "," + selectedLayer.getSizeVal() + ")";
        execute(script);


    }

    @Override
    public void redrawWKTFeatures(MapLayer selectedLayer) {
        String script = "window.mapFrame.redrawWKTFeatures('" + selectedLayer.getWKT() + "', '" + selectedLayer.getName() + "','" + selectedLayer.getEnvColour() + "', " + selectedLayer.getOpacity() + ")";
        execute(script);
    }

    @Override
    public String defineWKTMapLayer(MapLayer layer) {
        String script = ""
                + "var vector_layer = window.mapFrame.addWKTFeatureToMap('" + layer.getWKT() + "','" + layer.getNameJS() + "','" + layer.getEnvColour() + "', " + layer.getOpacity() + ");"
                + "mapLayers['" + layer.getUniqueIdJS() + "'] = vector_layer;"
                + "registerLayer(mapLayers['" + layer.getUniqueIdJS() + "']);";

        return wrapWithSafeToProceed(script);
    }

    @Override
    public String defineGeoJSONMapLayer(MapLayer layer) {
        /* can't have a GeoJSON baselayer so we don't need to decide where to store
         * the layer definition
         */

        String script;
        if (layer.getGeoJSON() != null && layer.getGeoJSON().length() > 0) {
            script = ""
                    + "var vector_layer = window.mapFrame.addJsonFeatureToMap('" + layer.getGeoJSON() + "','" + layer.getNameJS() + "','" + layer.getEnvColour() + "'," + layer.getSizeVal() + ", " + layer.getOpacity() + "," + layer.getSizeUncertain() + ");"
                    + "mapLayers['" + layer.getUniqueIdJS() + "'] = vector_layer;"
                    + "registerLayer(mapLayers['" + layer.getUniqueIdJS() + "']);";
        } else {
            script = "window.mapFrame.addJsonUrlToMap('" + layer.getUri() + "','" + layer.getNameJS() + "','" + layer.getEnvColour() + "'," + layer.getSizeVal() + ", " + layer.getOpacity() + "," + layer.getSizeUncertain() + ");";
        }

        System.out.println("defineGeoJSONMapLayer: " + script);

        return wrapWithSafeToProceed(script);
    }

    @Override
    public String defineGeoRSSMapLayer(MapLayer layer) {
        /* can't have a GeoRSS baselayer so we don't need to decide where to store
         * the layer definition
         */
        String script =
                "	mapLayers['" + layer.getUniqueIdJS() + "'] = new OpenLayers.Layer.GeoRSS("
                + "		'" + layer.getNameJS() + "', "
                + "		'" + layer.getUriJS() + "', "
                + "		{"
                + "			transparent: true "
                + "		}, "
                + "		{"
                + "			opacity:" + layer.getOpacity()
                + "		}  "
                + "	); "
                + // register for loading images...
                "registerLayer(mapLayers['" + layer.getUniqueIdJS() + "']);";

        return wrapWithSafeToProceed(script);
    }

    /**
     * Requesting a WMS 1.3.0 layer means you have to request a
     * CRS as well or the map server (ncwms) won't return a map
     * - force epsg:4326 for now
     * @param layer
     * @return
     */
    private String wmsVersionDeclaration(MapLayer layer) {
        String version = layerUtilities.getWmsVersion(layer);
        String versionJS = "";
        if (version != null) {
            if (version.equals("1.3.0")) {
                versionJS =
                        "version: '1.3.0', "
                        + "crs: 'epsg:4326' ";
            } else {
                versionJS =
                        "version: '" + version + "' ";
            }
        }
        return versionJS;

    }

    /**
     * create an instance of OpenLayers.Layer.WMS.
     *
     * Base layers will be rendered differently and stored in the baseLayers
     * associative array instead of the mapLayers associative array
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
            params += "env: '" + layer.getEnvParams() + "', ";
        }

        //extend to add ogc filter
        if (layer.getMapLayerMetadata() != null && layer.getMapLayerMetadata().getBbox() == null) {
            List<Double> bbox = layerUtilities.getBBoxIndex(layer.getUri());
            if (bbox == null) {
                bbox = new ArrayList(4);
                double[] box = layer.getMapLayerMetadata().getLayerExtent();
                bbox.add(box[0]);
                bbox.add(box[1]);
                bbox.add(box[2]);
                bbox.add(box[3]);
            }
            if (bbox != null && layer.getMapLayerMetadata() != null) {
                layer.getMapLayerMetadata().setBbox(bbox);
            }
        }

        String script =
                "	" + associativeArray + "['" + layer.getUniqueIdJS() + "'] = new OpenLayers.Layer.WMS("
                + "		'" + layer.getNameJS() + "', "
                + "		'" + layer.getUriJS().replace("wms?service=WMS&version=1.1.0&request=GetMap&", "wms\\/reflect?") + "', "
                + "		{"
                + "			styles: '" + layer.getSelectedStyleNameJS() + "', "
                + "			layers: '" + layer.getLayerJS() + "', "
                + "			format: '" + layer.getImageFormat() + "', "
                + "                     srs: 'epsg:900913', "
                + "			transparent: " + (!layer.isBaseLayer()) + ", "
                + "			" + params
                + wmsVersionDeclaration(layer) + //","
                "		}, "
                + "		{ "
                //      + "             " + "maxExtent: (new OpenLayers.Bounds(" + bbox.get(0) + "," + bbox.get(1) + "," + bbox.get(2) + "," + bbox.get(3) + ")).transform(new OpenLayers.Projection('EPSG:4326'),map.getProjectionObject()),"
                + "			isBaseLayer: " + layer.isBaseLayer() + ", "
                + "			opacity: " + layer.getOpacity() + ", "
                + "			queryable: " + layer.isQueryable() + ", "
                //                + "			buffer: " + settingsSupplementary.getValue("openlayers_tile_buffer") + ", "
                + "			gutter: " + gutter + ", "
                + "			wrapDateLine: true" //+ (layer.getName().contains("WorldClim") || layer.getName().contains("_cars")) //set cars layers wrap=true FIXME: this is hack - should actually check the extents
                + "		}  "
                + "	); "
                + // decorate with getFeatureInfoBuffer field - do not set buffer
                // it's used to set a margin of cached tiles around the viewport!!
                associativeArray + "['" + layer.getUniqueIdJS() + "']"
                + ".getFeatureInfoBuffer =" + settingsSupplementary.getValue("get_feature_info_buffer") + "; ";


        if (!layer.isBaseLayer()) {
            script += " " + associativeArray + "['" + layer.getUniqueIdJS() + "']"
                    + ".featureInfoResponseType=" + layer.getType() + "; ";

        }
        // register for loading images...
        script +=
                "registerLayer(" + associativeArray + "['" + layer.getUniqueIdJS() + "']);";

        // ncwms/thredds - decorate with units parameter
        script += decorateWithMetadata(associativeArray, layer);


        return wrapWithSafeToProceed(script);
    }

    /**
     * Return a String to decorate a layer declaration with
     * metadata fields if the layer supports it.  If the layer
     * doesn't support metadata, return an empty string rather
     * than null
     */
    private String decorateWithMetadata(String associativeArray, MapLayer layer) {
        // ncwms/thredds - decorate with units parameter
        String script;
        if (layerUtilities.supportsMetadata(layer.getType())) {
            script =
                    " " + associativeArray + "['" + layer.getUniqueIdJS() + "']"
                    + ".ncWMSMetaData = new Object(); "
                    + " " + associativeArray + "['" + layer.getUniqueIdJS() + "']"
                    + ".ncWMSMetaData.unit='" + layer.getMapLayerMetadata().getUnitsJS() + "'; ";
        } else {
            script = "";
        }
        return script;
    }

    /**
     * As activeateMapLayer but executes immediately
     * @param layer
     */
    @Override
    public void activateMapLayerNow(MapLayer layer) {
        execute(
                iFrameReferences
                + getAdditionalScript()
                + activateMapLayer(layer));
    }

    /**
     * Activate all layers in the passed in list.  As each layer will be sequentially
     * added to openlayers' layer array, we need to iterate the list in reverse order
     * @return
     */
    @Override
    public String activateMapLayers(List<MapLayer> layers) {
        StringBuffer script = new StringBuffer();
        if (layers != null) {
            for (int i = layers.size() - 1; i >= 0; i--) {
                MapLayer layer = (MapLayer) layers.get(i);
                // skip any layers that are not marked for display
                if (layer.isDisplayed()) {
                    script.append(activateMapLayer(layer, false, true));
                }
            }
        }
        return wrapWithSafeToProceed(script.toString());
    }

    /**
     * As activateateMapLayers but with immediate execution
     * @param layers
     */
    @Override
    public void activateMapLayersNow(List<MapLayer> layers) {
        execute(
                iFrameReferences
                + activateMapLayers(layers));
    }

    /**
     * Set the opacity for the layer at the position key in the associative
     * array of layers
     * @param key
     * @param percentage
     * @return
     */
    @Override
    public String setMapLayerOpacity(MapLayer mapLayer, float percentage) {

        /* safe to force the associative array to be mapLayers here because
         * the user can't control the base layer (?)
         */
        String script =
                "mapLayers['" + mapLayer.getUniqueIdJS() + "'].setOpacity(" + percentage + "); mapLayers['" + mapLayer.getUniqueIdJS() + "'].redraw(); ";

        return wrapWithSafeToProceed(script);
    }

    @Override
    public void setMapLayerOpacityNow(MapLayer mapLayer, float percentage) {
        execute(
                iFrameReferences
                + setMapLayerOpacity(mapLayer, percentage));
    }

    /**
     * Convenience method to reload a map layer by removing it
     * and then adding it
     * @param mapLayer
     * @return
     */
    @Override
    public String reloadMapLayer(MapLayer mapLayer) {
        if (mapLayer.getGeoJSON() == null && mapLayer.getType() == LayerUtilitiesImpl.GEOJSON) {
            return removeMapLayer(mapLayer)
                    + activateMapLayer(mapLayer, false, true);
        } else {
            return removeMapLayer(mapLayer)
                    + activateMapLayer(mapLayer);
        }

    }

    /**
     * Immediate execution of reloadMapLayer
     * @param mapLayer
     */
    @Override
    public void reloadMapLayerNow(MapLayer mapLayer) {
        execute(
                iFrameReferences
                + reloadMapLayer(mapLayer));
    }

    /**
     * Convenience wrapper around ZKs JavaScript execution system
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

        return ((portalSession != null)
                && portalSession.isMapLoaded());
    }

    /**
     * Create a popup window
     * @param uri
     * @param title
     * @return
     */
    @Override
    public String popupWindow(String uri, String title) {
        String script =
                "window.open(\"" + uri + "\",\"" + title + "\");";
        return script;
    }

    /**
     * Create a popup window immediately
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
        additionalScript = "";  //reset after use
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
