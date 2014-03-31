/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package org.ala.spatial.analysis.index;

import java.io.Serializable;

/**
 * container for layer filter;
 *
 * - includes minimum and maximum for environmental layers
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
    /**
     * for contextual layers
     *
     * list of layer pids
     */
    String [] ids = null;

    /**
     * Construct a new filter for environmental layers
     *
     * @param layername name of this layer as String.  See layersdb.fields.id
     * @param min minimum bound as double.
     * @param max maximum bound as double.
     */
    public LayerFilter(String layername, double min, double max) {
        this.layername = layername;
        this.minimum_value = min;
        this.maximum_value = max;
    }

    /**
     * Construct a new filter for contextual layers
     *
     * @param layername name of this layer as String.  See layersdb.fields.id
     * @param ids String array containing layersdb.objects.id values for shapes in this layer
     */
    public LayerFilter(String layername, String [] ids) {
        this.layername = layername;
        this.ids = ids;
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
     *
     * gets minimum applied
     *
     * @return
     */
    public double getMinimum_value() {
        return minimum_value;
    }

    /**
     * for environmental
     *
     * gets maximum applied
     *
     * @return
     */
    public double getMaximum_value() {
        return maximum_value;
    }

    /**
     * for contextual
     *
     * get layer objects pids.  see layersdb.objects.id
     *
     * @return
     */
    public String [] getIds() {
        return ids;
    }

    /**
     * Environmental ONLY
     *
     * test if a layer value is within the LayerFilter range.
     *
     * @param value number to test as double.
     * @return true if the value is in the filter range.
     */
    public boolean isValid(double value) {
        return !Double.isNaN(value) && value >= minimum_value && value <= maximum_value;
    }

    /**
     * parse a string into an array of LayerFilters.
     *
     * e.g. "ENVELOPE(el600,1,4:el801,9.6,20.3)" or "el600,1,4:el801,9.6,20.3"
     * and for contextual layers "ENVELOPE(cl904,pid1,pid2,pid3)"
     *
     * @param s String to parse.
     * @return array of filters as LayerFilter[].
     */
    public static LayerFilter[] parseLayerFilters(String s) {
        if (s.toUpperCase().startsWith("ENVELOPE(")) {
            s = s.substring("ENVELOPE(".length(), s.length() - 1); //remove 'envelope(..)' wrapper
        }

        String[] terms = s.split(":");

        LayerFilter[] lf = new LayerFilter[terms.length];

        int i = 0;
        for (String t : terms) {
            lf[i] = parseLayerFilter(t);
            i++;
        }

        return lf;
    }

    /**
     * Parse a single filter term into a new LayerFilter.
     *
     * e.g. "el600,1,4" or "ENVELOPE(el600,1,4)"
     *
     * @param s term to parse.
     * @return filter as LayerFilter.
     */
    public static LayerFilter parseLayerFilter(String s) {
        if (s.toUpperCase().startsWith("ENVELOPE(")) {
            s = s.substring("ENVELOPE(".length(), s.length() - 1); //remove 'envelope(..)' wrapper
        }

        String[] tokens = s.split(",");

        if (tokens[0].startsWith("el")) {
            return new LayerFilter(tokens[0], Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]));
        } else {
            //contextual, get ids
            String [] ids = new String[tokens.length-1];
            System.arraycopy(tokens,1,ids,0,ids.length);
            return new LayerFilter(tokens[0], ids);
        }
    }

    public boolean isContextual() {
        return layername.startsWith("cl");
    }
}
