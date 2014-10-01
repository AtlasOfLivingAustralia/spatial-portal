package au.org.ala.spatial.util;

import au.org.ala.spatial.StringConstants;
import net.sf.json.JSONObject;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * @author ajay
 */
public class SPLFilter {

    private int count = 0;
    private JSONObject layer;
    private String layername = "";
    private int[] catagories = null;
    private String[] catagoryNames = null;
    private double minimumValue = 0;
    private boolean changed;
    private String filterString;
    private double maximumValue = 0;
    private double minimumInitial = 0;
    private double maximumInitial = 0;

    public SPLFilter() {
    }

    public SPLFilter(JSONObject layer,
                     int[] catagories, String[] catagoryNames,
                     double minimum, double maximum) {
        this.layer = layer;
        if (layer != null) {
            layername = layer.getString(StringConstants.NAME);
        }
        this.catagories = catagories == null ? null : catagories.clone();
        this.catagoryNames = catagoryNames == null ? null : catagoryNames.clone();
        minimumValue = minimum;
        maximumValue = maximum;

        minimumInitial = minimum;
        maximumInitial = maximum;
    }

    public JSONObject getLayer() {
        return layer;
    }

    public void setLayer(JSONObject layer) {
        this.layer = layer;
    }

    public String getLayername() {
        return layername;
    }

    public void setLayername(String layername) {
        this.layername = layername;
    }

    public int[] getCatagories() {
        return catagories;
    }

    public void setCatagories(int[] catagories) {
        this.catagories = catagories == null ? null : catagories.clone();
    }

    public String[] getCatagoryNames() {
        return catagoryNames;
    }

    public void setCatagoryNames(String[] catagoryNames) {
        this.catagoryNames = catagoryNames == null ? null : catagoryNames.clone();
    }

    public double getMinimumValue() {
        return minimumValue;
    }

    public void setMinimumValue(double minimumValue) {
        this.minimumValue = minimumValue;
    }

    public double getMaximumValue() {
        return maximumValue;
    }

    public void setMaximumValue(double maximumValue) {
        this.maximumValue = maximumValue;
    }

    public double getMinimumInitial() {
        return minimumInitial;
    }

    public void setMinimumInitial(double minimumInitial) {
        this.minimumInitial = minimumInitial;
    }

    public double getMaximumInitial() {
        return maximumInitial;
    }

    public void setMaximumInitial(double maximumInitial) {
        this.maximumInitial = maximumInitial;
    }

    /* only works if created with constructor and 'catagories' Set validity is maintained */
    public boolean isChanged() {
        return changed
                || minimumValue != minimumInitial
                || maximumValue != maximumInitial
                || (catagories != null && catagoryNames != null
                && catagories.length != catagoryNames.length);
    }

    public void setChanged(boolean isChanged) {
        this.changed = isChanged;
    }

    public boolean equals(Object o) {
        if (o instanceof SPLFilter) {
            SPLFilter f = (SPLFilter) o;
            return !(!layername.equals(f.layername)
                    || (catagories != null && f.catagories != null
                    && catagories.length != f.catagories.length)
                    || minimumValue != f.minimumValue
                    || maximumValue != f.maximumValue);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return new HashCodeBuilder(71, 13).
                append(layername).
                append(catagories == null ? 0 : catagories.length).
                append(minimumValue).
                append(maximumValue).
                toHashCode();
    }

    public String getFilterString() {

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

        if (catagoryNames != null) {
            if (catagories == null || catagories.length == 0) {
                filterString = "include none";
            } else if (catagories.length == catagoryNames.length) {
                filterString = "include all";
            } else {
                StringBuilder string = new StringBuilder();
                for (int i : catagories) {
                    if (string.length() == 0) {
                        string.append("only; ");
                        string.append(catagoryNames[i]);
                    } else {
                        string.append(", ");
                        string.append(catagoryNames[i]);
                    }
                }
                filterString = string.toString();
            }
        } else {
            filterString = "between " + String.format("%.4f", (float) minimumValue)
                    + " and " + String.format("%.4f", (float) maximumValue);
        }

        return filterString;
    }

    public void setFilterString(String filterString) {
        this.filterString = filterString;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
