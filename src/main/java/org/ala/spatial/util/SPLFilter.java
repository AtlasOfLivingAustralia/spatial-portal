package org.ala.spatial.util;

import net.sf.json.JSONObject;

/**
 *
 * @author ajay
 */
public class SPLFilter {

    public SPLFilter() {
    }

    public SPLFilter(JSONObject _layer,
            int[] _catagories, String[] _catagory_names,
            double _minimum, double _maximum) {
        layer = _layer;
        if (layer != null) {
            layername = _layer.getString("name");
        }
        catagories = _catagories;
        catagory_names = _catagory_names;
        minimum_value = _minimum;
        maximum_value = _maximum;

        minimum_initial = _minimum;
        maximum_initial = _maximum;
    }
    public int count = 0;
    public JSONObject layer;
    public String layername = "";
    public int[] catagories = null;		//use to maintain Set validity
    public String[] catagory_names = null;
    public double minimum_value = 0;
    public boolean changed;
    public String filterString;

    public JSONObject getLayer() {
        return layer;
    }

    public String getLayername() {
        return layername;
    }

    public int[] getCatagories() {
        return catagories;
    }

    public String[] getCatagory_names() {
        return catagory_names;
    }

    public double getMinimum_value() {
        return minimum_value;
    }

    public double getMaximum_value() {
        return maximum_value;
    }

    public double getMinimum_initial() {
        return minimum_initial;
    }

    public double getMaximum_initial() {
        return maximum_initial;
    }
    public double maximum_value = 0;
    public double minimum_initial = 0;
    public double maximum_initial = 0;

    /* only works if created with constructor and 'catagories' Set validity is maintained */
    public boolean isChanged() {
        if (minimum_value != minimum_initial
                || maximum_value != maximum_initial
                || (catagories != null && catagory_names != null
                && catagories.length != catagory_names.length)) {
            return true;
        }
        return false;
    }

    public boolean equals(SPLFilter f) {
        if (layername != f.layername
                || (catagories != null && f.catagories != null
                && catagories.length != f.catagories.length)
                || minimum_value != f.minimum_value
                || maximum_value != f.maximum_value) {
            return false;
        }
        return true;
    }

    public String getFilterString() {
        /* TODO finish */

        /*
         * cases:
         *
         * catagorical
         * - include all
         * - include none
         * - only; comma separated list of names
         *
         * continous
         * - include all
         * - include none
         * - between x and y
         *
         */

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
            /*if(minimum_value <= minimum_initial
            && maximum_value >= maximum_initial){
            return "include all";
            }else if(minimum_value > maximum_value
            || maximum_value < minimum_initial
            || minimum_value > maximum_initial){
            return "include none";
            }else*/            {
                return "between " + String.format("%.4f", ((float) minimum_value))
                        + " and " + String.format("%.4f", ((float) maximum_value));
            }
        }

    }

    public void setCatagories(int[] catagories) {
        this.catagories = catagories;
    }

    public void setCatagory_names(String[] catagory_names) {
        this.catagory_names = catagory_names;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setLayer(JSONObject layer) {
        this.layer = layer;
    }

    public void setLayername(String layername) {
        this.layername = layername;
    }

    public void setMaximum_initial(double maximum_initial) {
        this.maximum_initial = maximum_initial;
    }

    public void setMaximum_value(double maximum_value) {
        this.maximum_value = maximum_value;
    }

    public void setMinimum_initial(double minimum_initial) {
        this.minimum_initial = minimum_initial;
    }

    public void setMinimum_value(double minimum_value) {
        this.minimum_value = minimum_value;
    }

    public void setChanged(boolean isChanged) {
        this.changed = isChanged;
    }

    public void setFilterString(String filterString) {
        this.filterString = filterString;
    }
}
