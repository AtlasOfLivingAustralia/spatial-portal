/*
 * javascript to add map
 *
 * layer = new OpenLayers.Layer.OSM( "Simple OSM Map");
 */

package au.org.emii.portal.wms;

/**
 *
 * @author brendon
 */
public class OpenStreetMapSupport {
    private String layerName;

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

}
