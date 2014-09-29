/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import au.org.emii.portal.menu.MapLayer;

import java.util.List;

/**
 * @author geoff
 */
public interface LayerUtilities {

    String getVersionValue(String uri);

    String getLayer(String wms);

    String getLayers(String url);

    /**
     * Convert a string WMS version eg (1.3.0) to its integer
     * representation within the portal eg WMS_1_3_0
     *
     * @param requestedType
     * @return Integer constant representing string representation
     * of WMS version.  Returns UNSUPPORTED if nothing matches
     */
    int internalVersion(String requestedType);

    String getWmsVersion(MapLayer mapLayer);

    List<Double> getBBox(String uri);

    String getImageFormat(String uri);
}
