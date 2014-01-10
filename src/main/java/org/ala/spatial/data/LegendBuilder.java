/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.data;

/**
 *
 * @author Adam
 */
public class LegendBuilder {

    /**
     * Build a legend from a QueryField
     * 
     * @param qf
     * @return
     */
    static public LegendObject build(QueryField qf) {
        switch (qf.getFieldType()) {
            case INT:
                return intLegend(qf);
            case LONG:
                return longLegend(qf);
            case FLOAT:
                return floatLegend(qf);
            case DOUBLE:
                return doubleLegend(qf);
            default:
                return stringLegend(qf);
        }
    }

    private static LegendObject intLegend(QueryField qf) {
        int[] raw = qf.intData;
        double[] d = new double[raw.length];
        for (int i = 0; i < raw.length; i++) {
            if (raw[i] == Integer.MIN_VALUE) {
                d[i] = Double.NaN;
            } else {
                d[i] = raw[i];
            }
        }
        return legendFromDoubles(d, qf);
    }

    static LegendObject legendFromDoubles(double[] d, QueryField qf) {
        java.util.Arrays.sort(d);
        Legend legend = new LegendEqualArea();
        legend.generate(d);

        legend.determineGroupSizes(d);

        return new LegendObject(legend, qf.getFieldType());
    }
    
    static LegendObject legendForDecades(double[] d, QueryField qf) {
        Legend legend = new LegendDecade();
        java.util.Arrays.sort(d);
        legend.generate(d);
        return new LegendObject(legend, qf.getFieldType());        
    }

    private static LegendObject longLegend(QueryField qf) {
        long[] raw = qf.longData;
        double[] d = new double[raw.length];
        for (int i = 0; i < raw.length; i++) {
            if (raw[i] == Long.MIN_VALUE) {
                d[i] = Double.NaN;
            } else {
                d[i] = raw[i];
            }
        }
        return legendFromDoubles(d, qf);
    }

    private static LegendObject floatLegend(QueryField qf) {
        float[] raw = qf.floatData;
        double[] d = new double[raw.length];
        for (int i = 0; i < raw.length; i++) {
            d[i] = raw[i];
        }
        return legendFromDoubles(d, qf);
    }

    private static LegendObject doubleLegend(QueryField qf) {
        double[] raw = qf.doubleData;
        double[] d = new double[raw.length];
        for (int i = 0; i < raw.length; i++) {
            d[i] = raw[i];
        }
        return legendFromDoubles(d, qf);
    }

    private static LegendObject stringLegend(QueryField qf) {
        return new LegendObject(qf.stringData, qf.stringCounts);
    }
}
