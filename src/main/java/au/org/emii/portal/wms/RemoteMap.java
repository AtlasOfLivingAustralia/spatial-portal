/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.wms;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.config.xmlbeans.BaseLayer;
import au.org.emii.portal.config.xmlbeans.Discovery;
import au.org.emii.portal.config.xmlbeans.Service;
import au.org.emii.portal.lang.LanguagePack;
import java.util.Hashtable;

/**
 *
 * @author geoff
 */
public interface RemoteMap {

    /**
     * Create a MapLayer instance and test that an image can be read from
     * the URI.
     *
     * Image format, and wms layer name and wms type are automatically obtained from the
     * URI.
     *
     * If there is no version parameter in the uri, we use a sensible
     * default (v1.1.1)
     *
     * @param label Label to use for the menu system
     * @param uri URI to read the map layer from (a GetMap request)
     * @param opacity Opacity value for this layer
     */
    MapLayer createAndTestWMSLayer(String label, String uri, float opacity);

    MapLayer createAndTestWMSLayer(String label, String uri, float opacity, boolean queryable);

    MapLayer createGeoJSONLayer(String label, String uri, boolean points_type);
    MapLayer createGeoJSONLayer(String label, String uri, boolean points_type, Hashtable properties);

    String getJson(String uri);

    MapLayer createGeoJSONLayerWithGeoJSON(String label, String uri, String json);

    MapLayer createKMLLayer(String label, String name, String uri); 

    MapLayer createWKTLayer(String wkt, String label);

    MapLayer createLocalLayer(int type, String label);
}
