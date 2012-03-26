package org.ala.spatial.analysis.index;

import java.io.Serializable;

/**
 * container for layer filter;
 * 
 * - includes minimum and maximum for environmental layers
 * - includes catagory names and indexes of selected for contextual layers
 * 
 * @author adam
 *
 */
public class LayerFilter extends Object implements Serializable {

    static final long serialVersionUID = -2733856402542621244L;
    /**
     * layer name
     */
    String layername = "";
    /**
     * for environmental layers
     *
     * filter minimum
     */
    double minimum_value = 0;
    /**
     * for environmental layers
     *
     * filter maximum
     */
    double maximum_value = 0;

    public LayerFilter(String layername, double min, double max) {
        this.layername = layername;
        this.minimum_value = min;
        this.maximum_value = max;
    }

    /**
     * gets layer name
     * @return String
     */
    public String getLayername() {
        return layername;
    }

    /**
     * for environmental
     *
     * gets minimum applied
     * @return
     */
    public double getMinimum_value() {
        return minimum_value;
    }

    /**
     * for environmental
     *
     * gets maximum applied
     * @return
     */
    public double getMaximum_value() {
        return maximum_value;
    }

    public boolean isValid(double value) {
        return !Double.isNaN(value) && value >= minimum_value && value <= maximum_value;
    }

    public static LayerFilter[] parseLayerFilters(String s) {
        String[] terms = s.split(":");

        LayerFilter[] lf = new LayerFilter[terms.length];

        int i = 0;
        for (String t : terms) {
            lf[i] = parseLayerFilter(t);
        }

        return lf;
    }

    public static LayerFilter parseLayerFilter(String s) {
        String[] tokens = s.split(",");

        return new LayerFilter(tokens[0], Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]));
    }
}
