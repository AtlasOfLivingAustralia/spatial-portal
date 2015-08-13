/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.menu;

import au.org.ala.legend.Facet;
import au.org.ala.spatial.util.Util;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.List;

/**
 * @author Adam
 */
public class SelectedArea implements Serializable {

    private static final long serialVersionUID = 1L;

    private MapLayer mapLayer;
    private String wkt;
    private String area;

    private String reducedWkt = null;

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

    public String getReducedWkt() {
        if (reducedWkt == null) {
            reducedWkt = Util.reduceWKT(getWkt()).getReducedWKT();
        }
        return reducedWkt;
    }

    public MapLayer getMapLayer() {
        return mapLayer;
    }

    /**
     * Calculates the area in km2 and returns a "pretty" string version
     *
     * @return
     */
    public String getKm2Area() {
        if (area == null) {
            if (mapLayer != null && mapLayer.getAreaSqKm() != null && mapLayer.getAreaSqKm().length() > 0) {
                area = mapLayer.getAreaSqKm();
            } else {
                try {
                    double totalarea = Util
                            .calculateArea(getWkt());
                    DecimalFormat df = new DecimalFormat("###,###.##");
                    area = df.format(totalarea / 1000.0 / 1000.0);


                } catch (Exception e) {
                    area = "";
                }
            }
        }
        return area;
    }

    public List<Facet> getFacets() {
        return (mapLayer != null) ? mapLayer.getFacets() : null;
    }
}
