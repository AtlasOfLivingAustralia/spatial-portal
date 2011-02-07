package org.ala.spatial.analysis.index;

import java.io.Serializable;

import org.ala.spatial.util.*;

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
     * maintenance of Species List results
     *
     * TODO: remove, used in UI only
     */
    public int count = 0;
    /**
     * layer that filter applies to
     */
    public Layer layer;
    /**
     * layer name
     *
     * TODO: duplicated, remove
     */
    public String layername = "";
    /**
     * for catagorical layers
     *
     * sorted set of selected catagories, see catagory_names	 *
     *
     * only works if created with constructor and 'catagories'
     * Set validity is maintained manually
     */
    public int[] catagories = null;		//use to maintain Set validity manually
    /**
     * for catagorical layers
     *
     * sorted set of valid catagories
     */
    public String[] catagory_names = null;
    /**
     * for environmental layers
     *
     * filter minimum
     */
    public double minimum_value = 0;
    /**
     * for environmental layers
     *
     * filter maximum
     */
    public double maximum_value = 0;
    /**
     * for environmental layers
     *
     * filter minimum extent
     */
    public double minimum_initial = 0;
    /**
     * for environmental layers
     *
     * filter maximum extent
     */
    public double maximum_initial = 0;

    /**
     * constructor for new LayerFilter with initialization
     *
     * for both environmental and contextual layers
     *
     * @param _layer layer that is referenced by LayerFilter, not null
     * @param _catagories for contextual layers, array of selected catagories
     * by index as int []
     * @param _catagory_names for contextual layers, array of catagory names
     * as String []
     * @param _minimum for environmental layers, bounding minimum extent as double
     * @param _maximum for environmental layers, bounding maximum extent as double
     */
    public LayerFilter(Layer _layer,
            int[] _catagories, String[] _catagory_names,
            double _minimum, double _maximum) {
        layer = _layer;
        if (layer != null) {
            layername = _layer.name;
        }
        catagories = _catagories;
        catagory_names = _catagory_names;
        minimum_value = _minimum;
        maximum_value = _maximum;

        minimum_initial = _minimum;
        maximum_initial = _maximum;
    }

    /**
     * gets layer
     * @return String
     */
    public Layer getLayer() {
        return layer;
    }

    /**
     * gets layer name
     * @return String
     */
    public String getLayername() {
        return layername;
    }

    /**
     * gets selected catagories
     * @return int[] if selected catagory indexes
     */
    public int[] getCatagories() {
        return catagories;
    }

    /**
     * for catagorical
     *
     * gets list of catagory names
     * @return
     */
    public String[] getCatagory_names() {
        return catagory_names;
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

    /**
     * for environmental
     *
     * gets minimum extent
     * @return
     */
    public double getMinimum_initial() {
        return minimum_initial;
    }

    /**
     * for environmental
     *
     * gets maximum extent
     * @return
     */
    public double getMaximum_initial() {
        return maximum_initial;
    }

    /**
     * gets filter state, true if a filter is applied.
     *
     * @return true iff filter does not include full range
     */
    public boolean isChanged() {
        if (minimum_value != minimum_initial
                || maximum_value != maximum_initial
                || (catagories != null && catagory_names != null
                && catagories.length != catagory_names.length)) {
            return true;
        }
        return false;
    }

    /**
     * format selected filter values for return
     *
     * cases:
     *
     * catagorical/contextual
     * - include all
     * - include none
     * - other selection; comma separated list of name
     *
     * continous/environmental
     * - between x and y
     *
     * @return String
     */
    @Override
    public String toString() {
        if (catagory_names != null) {
            if (catagories == null || catagories.length == 0) {
                return "include none";
            } else if (catagories.length == catagory_names.length) {
                return "include all";
            } else {
                StringBuffer string = new StringBuffer();
                for (int i : catagories) {
                    if (string.length() == 0) {
                        string.append("only; ");
                        string.append(catagory_names[i]);
                    } else {
                        string.append(", ");
                        string.append(catagory_names[i]);
                    }
                }
                return string.toString();
            }
        } else {
            return "between " + String.format("%.4f", ((float) minimum_value))
                    + " and " + String.format("%.4f", ((float) maximum_value));
        }

    }

    /**
     * creates a copy of another LayerFilter
     *
     * @param copy LayerFilter to copy
     */
    public LayerFilter copy() {
        LayerFilter lf = new LayerFilter(
                this.layer,
                (this.catagories != null) ? this.catagories.clone() : null,
                (this.catagory_names != null) ? this.catagory_names.clone() : null,
                this.minimum_initial,
                this.maximum_initial);

        /* copy values not initialized */
        lf.count = this.count;
        lf.minimum_value = this.minimum_value;
        lf.maximum_value = this.maximum_value;

        return lf;
    }
}
