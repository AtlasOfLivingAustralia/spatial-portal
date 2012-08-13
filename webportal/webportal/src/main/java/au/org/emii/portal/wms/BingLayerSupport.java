
/**
 *
 * examples of the javascript used to add these layers
 *
 *  var shaded = new OpenLayers.Layer.VirtualEarth("Shaded", {
        type: VEMapStyle.Shaded
    });
    var hybrid = new OpenLayers.Layer.VirtualEarth("Hybrid", {
        type: VEMapStyle.Hybrid
    });
    var aerial = new OpenLayers.Layer.VirtualEarth("Aerial", {
        type: VEMapStyle.Aerial
    });
 *
 *
 * 
 */

package au.org.emii.portal.wms;

/**
 *
 * @author brendon
 */
public class BingLayerSupport {
    private String type;
    private String layerName;

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    
}
