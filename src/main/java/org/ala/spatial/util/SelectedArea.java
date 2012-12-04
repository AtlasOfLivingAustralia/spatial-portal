/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import au.org.emii.portal.menu.MapLayer;
import java.io.Serializable;
import java.text.DecimalFormat;

/**
 *
 * @author Adam
 */
public class SelectedArea implements Serializable {
    
    private static final long serialVersionUID = 1L;

    MapLayer mapLayer;
    String wkt;
    String area;

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
    /**
     * Calculates the area in km2 and returns a "pretty" string version
     * @return
     */
    public String getKm2Area(){
        if(area == null){
          try {
            double totalarea = Util
                .calculateArea(getWkt());
            DecimalFormat df = new DecimalFormat("###,###.##");
            area = df.format(totalarea / 1000 / 1000);
            
    
        } catch (Exception e) {
           area = ""; 
        }
        }
        return area;
    }
}
