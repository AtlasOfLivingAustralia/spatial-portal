/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.data;

import java.io.Serializable;
import java.util.*;

/**
 * @author Adam
 */
public class QueryField implements Serializable {
    /**
     * The sub group that the query field belongs to.  Allows items to be group under common headings
     */
    GroupType group = GroupType.CUSTOM;
    String name;
    String displayName;
    boolean store;
    ArrayList<String> tmpData = new ArrayList<String>();

    public enum FieldType {

        LONG, INT, STRING, FLOAT, DOUBLE, AUTO
    }

    public enum GroupType {
        TAXONOMIC("Taxonomic", 1),
        GEOSPATIAL("Geospatial", 2),
        TEMPORAL("Temporal", 3),
        RECORD_DETAILS("Record Details", 4),
        ATTRIBUTION("Attribution", 5),
        RECORD_ASSERTIONS("Record Assertions", 6),
        CUSTOM("Custom", 0);
        private static final Map<String, GroupType> nameLookup = new HashMap<String, GroupType>();

        static {
            for (GroupType mt : EnumSet.allOf(GroupType.class)) {
                nameLookup.put(mt.name, mt);
            }
        }

        private String name;
        private Integer order;

        GroupType(String name, Integer order) {
            this.name = name;
            this.order = order;
        }

        public Integer getOrder() {
            return order;
        }

        public String getName() {
            return name;
        }

        public static GroupType getGroupType(String group) {
            return nameLookup.get(group);
        }

    }

    FieldType fieldType = FieldType.AUTO;
    long[] longData = null;
    int[] intData = null;
    String[] stringData = null;
    float[] floatData = null;
    double[] doubleData = null;
    int[] stringCounts = null;
    LegendObject legend;

    public QueryField(String name) {
        this.name = name;
        this.displayName = name;
        store = false;
        this.fieldType = FieldType.AUTO;
    }

    public QueryField(String name, FieldType fieldType) {
        this.name = name;
        this.displayName = name;
        store = false;
        this.fieldType = fieldType;
    }

    public QueryField(String name, String displayName, FieldType fieldType) {
        this.name = name;
        this.displayName = displayName;
        store = false;
        this.fieldType = fieldType;
    }

    public QueryField(String name, String displayName, GroupType group, FieldType fieldType) {
        this.name = name;
        this.displayName = displayName;
        this.group = group;
        store = false;
        this.fieldType = fieldType;
    }

    public QueryField(String name, String displayName, GroupType group, FieldType fieldType, boolean store) {
        this.name = name;
        this.displayName = displayName;
        this.group = group;
        this.store = store;
        this.fieldType = fieldType;
    }

    public boolean isStored() {
        return store;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setStored(boolean store) {
        this.store = store;
    }

    public void ensureCapacity(int size) {
        if (tmpData != null) {
            tmpData.ensureCapacity(size);
        }
    }

    public void add(String s) {
        if (tmpData != null) {
            if (s == null) {
                s = "";
            } else {
                s = s.trim();
            }
            tmpData.add(s);
        }
    }

    public void store() {
        if (tmpData == null) {
            return;
        }
        if (fieldType == FieldType.AUTO) {
            updateFieldType();
        }

        switch (fieldType) {
            case INT:
                storeAsInt();
                break;
            case LONG:
                storeAsLong();
                break;
            case FLOAT:
                storeAsFloat();
                break;
            case DOUBLE:
                storeAsDouble();
                break;
            default: //string
                storeAsString();
                break;
        }

        tmpData = null;
    }

    void storeAsInt() {
        intData = new int[tmpData.size()];
        for (int i = 0; i < intData.length; i++) {
            try {
                intData[i] = Integer.parseInt(tmpData.get(i));
            } catch (Exception e) {
                try {
                    intData[i] = (int) Double.parseDouble(tmpData.get(i));
                } catch (Exception ex) {
                    intData[i] = Integer.MIN_VALUE;
                }
            }
        }
    }

    void storeAsLong() {
        longData = new long[tmpData.size()];
        for (int i = 0; i < longData.length; i++) {
            try {
                longData[i] = Long.parseLong(tmpData.get(i));
            } catch (Exception e) {
                try {
                    longData[i] = (long) Double.parseDouble(tmpData.get(i));
                } catch (Exception ex) {
                    longData[i] = Long.MIN_VALUE;
                }
            }
        }
    }

    void storeAsFloat() {
        floatData = new float[tmpData.size()];
        for (int i = 0; i < floatData.length; i++) {
            try {
                floatData[i] = Float.parseFloat(tmpData.get(i));
            } catch (Exception e) {
                floatData[i] = Float.NaN;
            }
        }
    }

    void storeAsDouble() {
        doubleData = new double[tmpData.size()];
        for (int i = 0; i < doubleData.length; i++) {
            try {
                doubleData[i] = Double.parseDouble(tmpData.get(i));
            } catch (Exception e) {
                doubleData[i] = Double.NaN;
            }
        }
    }

    void storeAsString() {
        TreeSet<String> uniqueStrings = new TreeSet<String>();
        for (int i = 0; i < tmpData.size(); i++) {
            uniqueStrings.add(tmpData.get(i));
        }
        stringData = new String[uniqueStrings.size()];
        uniqueStrings.toArray(stringData);
        java.util.Arrays.sort(stringData);

        stringCounts = new int[stringData.length];
        intData = new int[tmpData.size()];
        for (int i = 0; i < tmpData.size(); i++) {
            int pos = java.util.Arrays.binarySearch(stringData, tmpData.get(i));
            intData[i] = pos;
            stringCounts[pos]++;
        }
    }

    private void updateFieldType() {
        if (tmpData == null) {
            return;
        }
        int intCount = 0;
        int longCount = 0;
        int floatCount = 0;
        int doubleCount = 0;
        int stringCount = 0;
        for (int i = 0; i < tmpData.size(); i++) {
            String s = tmpData.get(i);

            //substitution for sampling nulls
            if (s.equals("n/a")) {
                s = null;
            }

            if (s != null && s.length() > 0) {
                try {
                    Long.parseLong(s);
                    longCount++;

                    Integer.parseInt(s);
                    intCount++;
                } catch (Exception e) {
                }

                try {
                    Double.parseDouble(s);
                    doubleCount++;

                    Float.parseFloat(s);
                    floatCount++;
                } catch (Exception e) {
                }

                stringCount++;
            }
        }

        FieldType determinedType;

        if (stringCount <= 1 || (stringCount > longCount && stringCount > doubleCount)) {
            determinedType = FieldType.STRING;
        } else if (doubleCount > longCount) {
            if (floatCount == doubleCount) {
                determinedType = FieldType.FLOAT;
            } else {
                determinedType = FieldType.DOUBLE;
            }
        } else {
            if (intCount == longCount) {
                determinedType = FieldType.INT;
            } else {
                determinedType = FieldType.LONG;
            }
        }

        fieldType = determinedType;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "QueryField [group=" + group + ", name=" + name + ", displayName=" + displayName + ", store=" + store + ", fieldType=" + fieldType + ", legend=" + legend + "]";
    }

    /**
     * @return the group
     */
    public GroupType getGroup() {
        return group;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public int getInt(int pos) {
        return intData[pos];
    }

    public long getLong(int pos) {
        return longData[pos];
    }

    public double getDouble(int pos) {
        return doubleData[pos];
    }

    public float getFloat(int pos) {
        return floatData[pos];
    }

    public String getString(int pos) {
        return stringData[intData[pos]];
    }

    public String getAsString(int pos) {
        switch (fieldType) {
            case INT:
                return String.valueOf((intData[pos] == Integer.MIN_VALUE) ? "n/a" : intData[pos]);
            case LONG:
                return String.valueOf((longData[pos] == Long.MIN_VALUE) ? "n/a" : longData[pos]);
            case FLOAT:
                return String.valueOf((Float.isNaN(floatData[pos])) ? "n/a" : floatData[pos]);
            case DOUBLE:
                return String.valueOf((Double.isNaN(doubleData[pos])) ? "n/a" : doubleData[pos]);
            case STRING:
                return stringData[intData[pos]];
            default:
                if (tmpData != null) {
                    return tmpData.get(pos);
                } else {
                    return null;
                }
        }
    }

    void copyData(QueryField src) {
        fieldType = src.fieldType;
        longData = src.longData;
        intData = src.intData;
        floatData = src.floatData;
        doubleData = src.doubleData;
        stringData = src.stringData;
        stringCounts = src.stringCounts;
        legend = src.legend;
    }

    public LegendObject getLegend() {
        if (legend == null) {
            legend = LegendBuilder.build(this);
        }

        return legend;
    }

    public int getColour(int i) {
        getLegend(); //builds legend if not yet built

        switch (fieldType) {
            case INT:
                return legend.getColour(intData[i] == Integer.MIN_VALUE ? Double.NaN : intData[i]);
            case LONG:
                return legend.getColour(longData[i] == Long.MIN_VALUE ? Double.NaN : longData[i]);
            case FLOAT:
                return legend.getColour(Float.isNaN(floatData[i]) ? Double.NaN : floatData[i]);
            case DOUBLE:
                return legend.getColour(doubleData[i]);
            case STRING:
                return legend.getColour(stringData[intData[i]]);
            default:
                return LegendObject.DEFAULT_COLOUR;
        }
    }

    public int getColourForValue(double value) {
        if (fieldType == FieldType.STRING) {
            return legend.getColour(String.valueOf(value));
        } else if (legend.numericLegend != null) {
            return legend.getColour(value);
        } else {
            return LegendObject.DEFAULT_COLOUR;
        }
    }

    /**
     * Orders the Query field based on the group and then supplied order.
     *
     * @author Natasha Carter (natasha.carter@csiro.au)
     */
    public static class QueryFieldComparator implements Comparator<QueryField> {

        @Override
        public int compare(QueryField qf1, QueryField qf2) {
            if (qf1.group == null || qf2.group == null) {
                return 0;
            }
            return qf1.group.getOrder().compareTo(qf2.group.getOrder());
        }

    }
}
