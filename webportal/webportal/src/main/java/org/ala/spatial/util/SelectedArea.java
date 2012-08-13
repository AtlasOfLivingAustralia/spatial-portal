/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import au.org.emii.portal.menu.MapLayer;
import java.io.Serializable;

/**
 *
 * @author Adam
 */
public class SelectedArea implements Serializable {
    
    private static final long serialVersionUID = 1L;

    MapLayer mapLayer;
    String wkt;

    public SelectedArea(MapLayer mapLayer, String wkt) {
        this.mapLayer = mapLayer;
        this.wkt = wkt;
    }

    public String getWkt() {
        if (mapLayer != null) {
            return mapLayer.getWKT();
        } else {
            return wkt;
        }
    }

    public MapLayer getMapLayer() {
        return mapLayer;
    }
}
