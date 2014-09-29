/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import au.org.emii.portal.menu.MapLayer;

/**
 * @author geoff
 */
public interface RemoteMap {

    /**
     * Create a MapLayer instance and test that an image can be read from
     * the URI.
     * <p/>
     * Image format, and wms layer name and wms type are automatically obtained from the
     * URI.
     * <p/>
     * If there is no version parameter in the uri, we use a sensible
     * default (v1.1.1)
     *
     * @param label   Label to use for the menu system
     * @param uri     URI to read the map layer from (a GetMap request)
     * @param opacity Opacity value for this layer
     */
    MapLayer createAndTestWMSLayer(String label, String uri, float opacity);

    MapLayer createWKTLayer(String wkt, String label);

    MapLayer createLocalLayer(int type, String label);

}
