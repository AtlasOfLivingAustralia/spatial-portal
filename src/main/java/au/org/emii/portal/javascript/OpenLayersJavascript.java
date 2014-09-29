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
    String getIFrameReferences();

    /**
     * appends any additional scripts to the execute function
     *
     * @param additionalScript
     * @return
     */
    void setAdditionalScript(String additionalScript);

    void useAdditionalScript();

    /**
     * Activate a map layer described by the passed in Layer instance. If the id
     * already exists in the associative array of layers, nothing will happen
     * when you execute the script
     *
     * @param mapLayer
     * @return
     */
    String activateMapLayer(MapLayer mapLayer, boolean recursive, boolean alternativeScript);

    /**
     * As activeateMapLayer but executes immediately
     *
     * @param layer
     */
    void activateMapLayerNow(MapLayer layer);

    /**
     * Activate all layers in the passed in list. As each layer will be
     * sequentially added to openlayers' layer array, we need to iterate the
     * list in reverse order
     *
     * @return
     */
    String activateMapLayers(List<MapLayer> layers);

    void removeLayer(MapLayer ml);

    void redrawFeatures(MapLayer selectedLayer);

    void redrawWKTFeatures(MapLayer selectedLayer);

    String addBoxDrawingTool();

    void zoomGeoJsonExtentNow(MapLayer ml);

    String zoomGeoJsonExtent(MapLayer ml);

    void zoomLayerExtent(MapLayer ml);

    /**
     * Convenience wrapper around ZKs JavaScript execution system
     *
     * @param script
     */
    void execute(String script);

    String initialiseMap(BoundingBox boundingBox);

    boolean mapLoaded();

    String minify(String fragment);

    /**
     * Convenience method to reload a map layer by removing it and then adding
     * it
     *
     * @param mapLayer
     * @return
     */
    String reloadMapLayer(MapLayer mapLayer);

    /**
     * Immediate execution of reloadMapLayer
     *
     * @param mapLayer
     */
    void reloadMapLayerNow(MapLayer mapLayer);

    String removeMapLayer(MapLayer layer);

    /**
     * As removeLayer but execute immediately without returning any code
     */
    void removeMapLayerNow(MapLayer mapLayer);

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
    String updateMapLayerIndexes(List<MapLayer> activeLayers);

    void updateMapLayerIndexesNow(List<MapLayer> activeLayers);

    String zoomToBoundingBox(BoundingBox boundingBox, boolean closest);

    /**
     * Adds the openlayers polygon drawing tool to the map
     */
    String addPolygonDrawingTool();

    /**
     * Adds the radius drawing tool to the map
     */
    String addRadiusDrawingTool();

    /**
     * Adds the feature selection tool (for area) to the map
     */
    String addFeatureSelectionTool();

    String setBaseLayer(String baseLayer);
}
