/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.javascript;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.value.BoundingBox;

import java.util.List;

/**
 * @author geoff
 */
public interface OpenLayersJavascript {

    /**
     * JavaScript objects in the iframe are referenced as variables in the
     * parent document (us!) to simplify the generated javascript
     * <p/>
     * This string will get inlined into all classes that reference it -
     * findbugs reports this but I'm willing to sacrifice a small amount of
     * memory rather than refactor this code to use getters.
     */
    public static final String iFrameReferences = "var safeToProceed=true; " + "if (mapLayers == null) {" + "\tmapLayers = window.mapFrame.mapLayers; " + "} " + "if (map == null) {" + "\tmap = window.mapFrame.map; " + "} " + "if (OpenLayers == null) {" + "\tOpenLayers = window.mapFrame.OpenLayers; " + "} " + "if (baseLayers == null) {" + "\tbaseLayers = window.mapFrame.baseLayers; " + "} " + "if (currentBaseLayer == null) {" + "\tcurrentBaseLayer = window.mapFrame.currentBaseLayer; " + "} " + "if (currentBaseLayer == null) {" + "\tcurrentBaseLayer = window.mapFrame.currentBaseLayer; " + "} " + "if (registerLayer == null) {" + "\tregisterLayer = window.mapFrame.registerLayer; " + "} " + "if (\t(mapLayers == null) || " + "\t\t(map == null) || " + "\t\t(OpenLayers == null) || " + "\t\t(baseLayers == null)) { " + "\tsafeToProceed=false;" + "\talert(\'map subsystem is not fully loaded yet - this operation will fail\');" + "} ";
    public static final String safeToProceedClose = "} ";
    public static final String safeToProceedOpen = "if (safeToProceed) { ";

    /**
     * appends any additional scripts to the execute function
     *
     * @param additionalScript
     * @return
     */
    public void setAdditionalScript(String additionalScript);

    public String getAdditionalScript();

    public void useAdditionalScript();

    public String activateMapLayer(MapLayer mapLayer);

    /**
     * Activate a map layer described by the passed in Layer instance. If the id
     * already exists in the associative array of layers, nothing will happen
     * when you execute the script
     *
     * @param mapLayer
     * @return
     */
    public String activateMapLayer(MapLayer mapLayer, boolean recursive, boolean alternativeScript);

    /**
     * As activeateMapLayer but executes immediately
     *
     * @param layer
     */
    public void activateMapLayerNow(MapLayer layer);

    /**
     * Activate all layers in the passed in list. As each layer will be
     * sequentially added to openlayers' layer array, we need to iterate the
     * list in reverse order
     *
     * @return
     */
    public String activateMapLayers(List<MapLayer> layers);

    /**
     * As activateateMapLayers but with immediate execution
     *
     * @param layers
     */
    public void activateMapLayersNow(List<MapLayer> layers);

    public void removeLayer(MapLayer ml);

    public void redrawFeatures(MapLayer selectedLayer);

    public void redrawWKTFeatures(MapLayer selectedLayer);

    public String defineImageMapLayer(MapLayer mapLayer);

    public String defineKMLMapLayer(MapLayer layer);

    public String defineGeoJSONMapLayer(MapLayer layer);

    public String defineWKTMapLayer(MapLayer layer);

    public void zoomGeoJsonExtentNow(MapLayer layer);

    public String zoomGeoJsonExtent(MapLayer layer);

    public void addFeatureSelection();

    public void zoomLayerExtent(MapLayer ml);

    /**
     * create an instance of OpenLayers.Layer.WMS.
     * <p/>
     * Base layers will be rendered differently and stored in the baseLayers
     * associative array instead of the mapLayers associative array
     *
     * @param layer
     * @return
     */
    public String defineWMSMapLayer(MapLayer layer);

    /**
     * Convenience wrapper around ZKs JavaScript execution system
     *
     * @param script
     */
    public void execute(String script);

    public String initialiseMap();

    public void initialiseMapNow();

    public void initialiseTransectDrawing(MapLayer mapLayer);

    public boolean mapLoaded();

    public String minify(String fragment);

    /**
     * Create a popup window
     *
     * @param uri
     * @param title
     * @return
     */
    public String popupWindow(String uri, String title);

    /**
     * Create a popup window immediately
     *
     * @param uri
     * @param title
     * @return
     */
    public void popupWindowNow(String uri, String title);

    /**
     * Convenience method to reload a map layer by removing it and then adding
     * it
     *
     * @param mapLayer
     * @return
     */
    public String reloadMapLayer(MapLayer mapLayer);

    /**
     * Immediate execution of reloadMapLayer
     *
     * @param mapLayer
     */
    public void reloadMapLayerNow(MapLayer mapLayer);

    public String removeMapLayer(MapLayer layer);

    /**
     * Generate the code to remove a layer from the map and the array of layers
     * - don't forget to scope your iFrameReferences first - see
     * removeLayerNow()
     *
     * @param id
     * @return
     */
    public String removeMapLayer(MapLayer layer, boolean recursive);

    /**
     * As removeLayer but execute immediately without returning any code
     *
     * @param id
     */
    public void removeMapLayerNow(MapLayer mapLayer);

    /**
     * Set the opacity for the layer at the position key in the associative
     * array of layers
     *
     * @param key
     * @param percentage
     * @return
     */
    public String setMapLayerOpacity(MapLayer mapLayer, float percentage);

    public void setMapLayerOpacityNow(MapLayer mapLayer, float percentage);

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
    public String updateMapLayerIndexes(List<MapLayer> activeLayers);

    public void updateMapLayerIndexesNow(List<MapLayer> activeLayers);

    public String wrapWithSafeToProceed(String script);

    public String zoomToBoundingBox(BoundingBox boundingBox);

    public void zoomToBoundingBoxNow(BoundingBox boundingBox);

    public String zoomToBoundingBox(BoundingBox boundingBox, boolean closest);

    public void zoomToBoundingBoxNow(BoundingBox boundingBox, boolean closest);

    /**
     * Adds the openlayers polygon drawing tool to the map
     */
    public String addPolygonDrawingTool();

    /**
     * Adds the radius drawing tool to the map
     */
    public String addRadiusDrawingTool();

    /**
     * Adds the feature selection tool (for area) to the map
     */
    public String addFeatureSelectionTool();

    /**
     * Copy for Sampling, ALOC, Filtering, Adds the openlayers polygon drawing
     * tool to the map
     */
    public void addPolygonDrawingToolSampling();

    public void addPolygonDrawingToolALOC();

    public void addPolygonDrawingToolFiltering();

    public void removePolygonSampling();

    public void removePolygonALOC();

    public void removePolygonFiltering();

    public void removeAreaSelection();

    /**
     * Adds the openlayers box drawing tool to the map
     */
    public String addBoxDrawingTool();

    /**
     * Adds a geojson layer
     *
     * @param url the url of the json feature
     */
    public void addGeoJsonLayer(String url);

    public String setBaseLayer(String baseLayer);

}
