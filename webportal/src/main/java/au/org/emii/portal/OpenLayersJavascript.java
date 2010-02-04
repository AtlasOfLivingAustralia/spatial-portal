package au.org.emii.portal;

import au.org.emii.portal.config.Config;
import java.util.List;
import org.apache.log4j.Logger;
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
public class OpenLayersJavascript {

        protected final static Logger logger = Logger.getLogger(OpenLayersJavascript.class);
        /**
         * JavaScript objects in the iframe are referenced as variables in
         * the parent document (us!) to simplify the generated javascript
         *
         * This string will get inlined into all classes that reference it -
         * findbugs reports this but I'm willing to sacrifice a small amount of
         * memory rather than refactor this code to use getters.
         */
        public static final String iFrameReferences =
                "var safeToProceed=true; "
                + "if (mapLayers == null) {"
                + "	mapLayers = window.mapFrame.mapLayers; "
                + "} "
                + "if (map == null) {"
                + "	map = window.mapFrame.map; "
                + "} "
                + "if (OpenLayers == null) {"
                + "	OpenLayers = window.mapFrame.OpenLayers; "
                + "} "
                + "if (baseLayers == null) {"
                + "	baseLayers = window.mapFrame.baseLayers; "
                + "} "
                + "if (currentBaseLayer == null) {"
                + "	currentBaseLayer = window.mapFrame.currentBaseLayer; "
                + "} "
                + "if (currentBaseLayer == null) {"
                + "	currentBaseLayer = window.mapFrame.currentBaseLayer; "
                + "} "
                + "if (registerLayer == null) {"
                + "	registerLayer = window.mapFrame.registerLayer; "
                + "} "
                +
                /* warning - map is not loaded properly! - only currentBaseLayer
                 * is allowed to be null because it gets loaded in javascript
                 */
                "if (	(mapLayers == null) || "
                + "		(map == null) || "
                + "		(OpenLayers == null) || "
                + "		(baseLayers == null)) { "
                + "	safeToProceed=false;"
                + "	alert('map subsystem is not fully loaded yet - this operation will fail');"
                + "} ";
        public static final String safeToProceedOpen =
                "if (safeToProceed) { ";
        public static final String safeToProceedClose =
                "} ";

        public static String wrapWithSafeToProceed(String script) {
                return safeToProceedOpen
                        + script
                        + safeToProceedClose;
        }

        /*
         * Remove whitespace and carriage returns
         */
        public static String minify(String fragment) {
                return fragment.replaceAll("\\s+", " ");
        }

        public static String initialiseMap() {
                return "window.mapFrame.buildMapReal(); ";
        }

        public static void initialiseMapNow() {
                execute(
                        initialiseMap());
        }


        public static void initialiseTransectDrawing(MapLayer mapLayer) {
            String script = "window.mapFrame.addLineDrawingLayer('" +
                                        mapLayer.getNameJS() + "','" +
                                        mapLayer.getLayer()    + "','" +
                                        mapLayer.getUriJS() +  "')";
            execute(script) ; // Safe to proceed - map loaded way beforehand

        }


        public static String defineAnimatedLayer(MapLayer mapLayer) {
                List<Double> bbox = mapLayer.getMapLayerMetadata().getBbox();

                String script =
                        "	mapLayers['" + mapLayer.getUniqueIdJS() + "'] = new OpenLayers.Layer.Image("
                        + "		'" + mapLayer.getNameJS() + "', "
                        + "		'" + LayerUtilities.getAnimationUriJS(mapLayer) + "', "
                        + " 		new OpenLayers.Bounds("
                        + bbox.get(0) + ","
                        + bbox.get(1) + ","
                        + bbox.get(2) + ","
                        + bbox.get(3)
                        + "		), "
                        + " 		new OpenLayers.Size(" + LayerUtilities.ANIMATION_WIDTH + "," + LayerUtilities.ANIMATION_HEIGHT + "), "
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
                        + ".timeSeriesPlotUri='" + LayerUtilities.getAnimationTimeSeriesPlotUriJS(mapLayer) + "'; "
                        + "mapLayers['" + mapLayer.getUniqueIdJS() + "']"
                        + ".queryable=true; "
                        + "mapLayers['" + mapLayer.getUniqueIdJS() + "']"
                        + ".animatedNcwmsLayer=true; "
                        + "mapLayers['" + mapLayer.getUniqueIdJS() + "']"
                        + ".featureInfoResponseType=" + mapLayer.getType() + "; "
                        + "mapLayers['" + mapLayer.getUniqueIdJS() + "']"
                        + ".baseUri='" + LayerUtilities.getAnimationFeatureInfoUriJS(mapLayer) + "'; "
                        + decorateWithMetadata("mapLayers", mapLayer)
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
        public static String animate(MapLayer mapLayer) {

                String script =
                        "if (mapLayers['" + mapLayer.getUniqueIdJS() + "'] == null) { "
                        + defineAnimatedLayer(mapLayer)
                        + "	map.addLayer(mapLayers['" + mapLayer.getUniqueIdJS() + "']); "
                        + "} ";


                // 'http://obsidian:8080/ncWMS/wms?LAYERS=67%2Ftemp&ELEVATION=-5&TIME=2006-09-01T12:00:00.000Z/2006-09-19T12:00:00.000Z&TRANSPARENT=true&STYLES=BOXFILL%2Frainbow&CRS=EPSG%3A4326&COLORSCALERANGE=9.405405%2C29.66159&NUMCOLORBANDS=254&LOGSCALE=false&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&EXCEPTIONS=XML&FORMAT=image/gif&BBOX=-180,-90,180,90&WIDTH=512&HEIGHT=400', // URL to the image
                mapLayer.setDisplayed(true);
                return wrapWithSafeToProceed(script);
        }

        public static String removeMapLayer(MapLayer layer) {
                return removeMapLayer(layer, false);
        }

        /**
         * Generate the code to remove a layer from the map and the
         * array of layers - don't forget to scope your iFrameReferences
         * first - see removeLayerNow()
         * @param id
         * @return
         */
        public static String removeMapLayer(MapLayer layer, boolean recursive) {
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
        public static void removeMapLayerNow(MapLayer mapLayer) {
                execute(
                        iFrameReferences
                        + removeMapLayer(mapLayer));
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
        public static String updateMapLayerIndexes(List<MapLayer> activeLayers) {
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

        public static void updateMapLayerIndexesNow(List<MapLayer> activeLayers) {
                execute(
                        iFrameReferences
                        + updateMapLayerIndexes(activeLayers));
        }

        public static void zoomToBoundingBoxNow(BoundingBox boundingBox) {
                execute(
                        iFrameReferences
                        + zoomToBoundingBox(boundingBox));
        }

        public static String zoomToBoundingBox(BoundingBox boundingBox) {
                String script =
                        "map.zoomToExtent("
                        + "	new OpenLayers.Bounds("
                        + boundingBox.getMinLongitude() + ", "
                        + boundingBox.getMinLatitude() + ", "
                        + boundingBox.getMaxLongitude() + ", "
                        + boundingBox.getMaxLatitude()
                        + ") "
                        + "); ";
                return wrapWithSafeToProceed(script);
        }

        public static String activateMapLayer(MapLayer mapLayer) {
                return activateMapLayer(mapLayer, false);
        }

        /**
         * Activate a map layer described by the passed in Layer instance.  If the id already
         * exists in the associative array of layers, nothing will happen when you execute
         * the script
         * @param mapLayer
         * @return
         */
        public static String activateMapLayer(MapLayer mapLayer, boolean recursive) {
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
                        case LayerUtilities.WMS_1_0_0:
                        case LayerUtilities.WMS_1_1_0:
                        case LayerUtilities.WMS_1_1_1:
                        case LayerUtilities.WMS_1_3_0:
                        case LayerUtilities.NCWMS:
                        case LayerUtilities.THREDDS:
                                if (mapLayer.isCurrentlyAnimated()) {
                                        script.append(defineAnimatedLayer(mapLayer));
                                        okToAddLayer = true;
                                } else {
                                        script.append(defineWMSMapLayer(mapLayer));
                                        okToAddLayer = true;
                                }
                                break;
                        case LayerUtilities.GEORSS:
                                script.append(defineGeoRSSMapLayer(mapLayer));
                                okToAddLayer = true;
                                break;
                        case LayerUtilities.KML:
                                script.append(defineKMLMapLayer(mapLayer));
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
                                "	map.addLayer(" + associativeArray + "['" + mapLayer.getUniqueIdJS() + "']); ");

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
                        mapLayer.setDisplayed(true);
                }

                if (recursive) {
                        for (MapLayer child : mapLayer.getChildren()) {
                                script.append(activateMapLayer(child, recursive));
                        }
                }

                return wrapWithSafeToProceed(script.toString());
        }

        public static String defineKMLMapLayer(MapLayer layer) {
                /* can't have a GeoRSS baselayer so we don't need to decide where to store
                 * the layer definition
                 */
                String script =
                        "	mapLayers['" + layer.getUniqueIdJS() + "'] = new OpenLayers.Layer.GML("
                        + "		'" + layer.getNameJS() + "', "
                        + "		'" + layer.getUriJS() + "', "
                        + "		{"
                        + "			transparent: true, "
                        + //"			projection: new OpenLayers.Projection('EPSG:3032'), " +
                        //"			projection: new OpenLayers.Projection('EPSG:6326'), " +
                        "			internalProjection: new OpenLayers.Projection('ESPG:4326'), "
                        + "			externalProjection: new OpenLayers.Projection('ESPG:3032'), "
                        + "			format: OpenLayers.Format.KML, "
                        + "			formatOptions: { "
                        + "				extractStyles: true, "
                        + "				extractAttributes: true"
                        + "			} "
                        + "		}, "
                        + "		{ "
                        + "			opacity:" + layer.getOpacity() + ","
                        + "			wrapDateLine: true "
                        + "		}  "
                        + "	);"
                        + // register for loading images...
                        "registerLayer(mapLayers['" + layer.getUniqueIdJS() + "']);";

                return wrapWithSafeToProceed(script);
        }

        public static String defineGeoRSSMapLayer(MapLayer layer) {
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
        private static String wmsVersionDeclaration(MapLayer layer) {
                String version = layer.getWmsVersion();
                String versionJS = "";
                if (version != null) {
                        if (layer.getWmsVersion().equals("1.3.0")) {
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
        public static String defineWMSMapLayer(MapLayer layer) {
                String associativeArray = null;
                String gutter = "0";
                String params = "";
                if (layer.isBaseLayer()) {
                        associativeArray = "baseLayers";
                } else {
                        associativeArray = "mapLayers";
                        gutter = Config.getValue("openlayers_tile_gutter");
                }
                if (!Validate.empty(layer.getSld())) {
                        params = "SLD: '" + layer.getSldJS() + "' ";
                        params += ", ";
                }
                if (!Validate.empty(layer.getCql())) {
                        params = "CQL_FILTER: '" + layer.getCqlJS() + "' ";
                        params += ", ";
                }

                //extend to add ogc filter


                String script =
                        "	" + associativeArray + "['" + layer.getUniqueIdJS() + "'] = new OpenLayers.Layer.WMS("
                        + "		'" + layer.getNameJS() + "', "
                        + "		'" + layer.getUriJS() + "', "
                        + "		{"
                        + "			styles: '" + layer.getSelectedStyleNameJS() + "', "
                        + "			layers: '" + layer.getLayerJS() + "', "
                        + "			format: '" + layer.getImageFormat() + "', "
                        + "			transparent: " + (! layer.isBaseLayer()) + ", "
                        + "			" + params
                        +                       wmsVersionDeclaration(layer) + //","
                        "		}, "
                        + "		{ "
                        + "			isBaseLayer: " + layer.isBaseLayer() + ", "
                        + "			opacity: " + layer.getOpacity() + ", "
                        + "			queryable: " + layer.isQueryable() + ", "
                        + "			buffer: " + Config.getValue("openlayers_tile_buffer") + ", "
                        + "			gutter: " + gutter + ", "
                        + "			wrapDateLine: true "
                        + "		}  "
                        + "	); "
                        + // decorate with getFeatureInfoBuffer field - do not set buffer
                        // it's used to set a margin of cached tiles around the viewport!!
                        associativeArray + "['" + layer.getUniqueIdJS() + "']"
                        + ".getFeatureInfoBuffer =" + Config.getValue("get_feature_info_buffer") + "; ";


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
        private static String decorateWithMetadata(String associativeArray, MapLayer layer) {
                // ncwms/thredds - decorate with units parameter
                String script;
                if (LayerUtilities.supportsMetadata(layer.getType())) {
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
        public static void activateMapLayerNow(MapLayer layer) {
                execute(
                        iFrameReferences
                        + activateMapLayer(layer));
        }

        /**
         * Activate all layers in the passed in list.  As each layer will be sequentially
         * added to openlayers' layer array, we need to iterate the list in reverse order
         * @return
         */
        public static String activateMapLayers(List<MapLayer> layers) {
                StringBuffer script = new StringBuffer();
                if (layers != null) {
                        for (int i = layers.size() - 1; i >= 0; i--) {
                                MapLayer layer = (MapLayer) layers.get(i);
                                // skip any layers that are not marked for display
                                if (layer.isDisplayed()) {
                                        script.append(activateMapLayer(layer));
                                }
                        }
                }
                return wrapWithSafeToProceed(script.toString());
        }

        /**
         * As activateateMapLayers but with immediate execution
         * @param layers
         */
        public static void activateMapLayersNow(List<MapLayer> layers) {
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
        public static String setMapLayerOpacity(MapLayer mapLayer, float percentage) {

                /* safe to force the associative array to be mapLayers here because
                 * the user can't control the base layer (?)
                 */
                String script =
                        "mapLayers['" + mapLayer.getUniqueIdJS() + "'].setOpacity(" + percentage + ") ";

                return wrapWithSafeToProceed(script);
        }

        public static void setMapLayerOpacityNow(MapLayer mapLayer, float percentage) {
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
        public static String reloadMapLayer(MapLayer mapLayer) {
                String script =
                        removeMapLayer(mapLayer)
                        + activateMapLayer(mapLayer);

                return script;

        }

        /**
         * Immediate execution of reloadMapLayer
         * @param mapLayer
         */
        public static void reloadMapLayerNow(MapLayer mapLayer) {
                execute(
                        iFrameReferences
                        + reloadMapLayer(mapLayer));
        }

        /**
         * Convenience wrapper around ZKs JavaScript execution system
         * @param script
         */
        public static void execute(String script) {
                if (mapLoaded()) {
                        script = minify(script);
                        logger.debug("exec javascript: " + script);
                        Clients.evalJavaScript(script);
                } else {
                        logger.info("refused to execute javascript - map not loaded");
                }
        }

        public static boolean mapLoaded() {
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
        public static String popupWindow(String uri, String title) {
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
        public static void popupWindowNow(String uri, String title) {
                execute(popupWindow(uri, title));
        }
}
