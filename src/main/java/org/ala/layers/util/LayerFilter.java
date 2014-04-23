package org.ala.layers.util;

import java.io.Serializable;

/**
 * container for layer filter;
 * <p/>
 * - includes minimum and maximum for environmental layers
 * - includes catagory names and indexes of selected for contextual layers
 *
 * @author adam
 */
public class LayerFilter extends Object implements Serializable {

    static final long serialVersionUID = -2733856402542621244L;
    /**
     * layer name
     */
    String layername = "";
    /**
     * for environmental layers
     * <p/>
     * filter minimum
     */
    double minimum_value = 0;
    /**
     * for environmental layers
     * <p/>
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
     *
     * @return String
     */
    public String getLayername() {
        return layername;
    }

    /**
     * for environmental
     * <p/>
     * gets minimum applied
     *
     * @return
     */
    public double getMinimum_value() {
        return minimum_value;
    }

    /**
     * for environmental
     * <p/>
     * gets maximum applied
     *
     * @return
     */
    public double getMaximum_value() {
        return maximum_value;
    }

    public boolean isValid(double value) {
        return !Double.isNaN(value) && value >= minimum_value && value <= maximum_value;
    }

    public static LayerFilter[] parseLayerFilters(String s) {
        if (s.toUpperCase().startsWith("ENVELOPE(")) {
            s = s.substring("ENVELOPE(".length(), s.length() - 1); //remove 'envelope(..)' wrapper
        }

        String[] terms = s.split(":");

        LayerFilter[] lf = new LayerFilter[terms.length];

        int i = 0;
        for (String t : terms) {
            System.out.println("parsing filter term: " + t);
            lf[i] = parseLayerFilter(t);
            i++;
        }

        return lf;
    }

    public static LayerFilter parseLayerFilter(String s) {
        if (s.toUpperCase().startsWith("ENVELOPE(")) {
            s = s.substring("ENVELOPE(".length(), s.length() - 1); //remove 'envelope(..)' wrapper
        }

        String[] tokens = s.split(",");

        return new LayerFilter(tokens[0], Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]));
    }
}
