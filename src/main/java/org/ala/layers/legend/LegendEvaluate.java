/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.layers.legend;

/**
 * @author Adam
 */
public class LegendEvaluate {

    public static Legend buildFrom(double[] d) {
        float[] dsorted = new float[d.length];
        for (int i = 0; i < d.length; i++) {
            dsorted[i] = (float) d[i];
        }
        java.util.Arrays.sort(dsorted);

        Legend[] legends = new Legend[3];
        //legends[3] = new LegendEqualSize();
        legends[1] = new LegendEvenInterval();
        legends[2] = new LegendEvenIntervalLog();
        legends[0] = new LegendEvenIntervalLog10();

        double bestE = Double.MAX_VALUE;
        int bestI = -1;
        for (int i = 0; i < legends.length; i++) {
            legends[i].generate(dsorted);
            legends[i].determineGroupSizes(dsorted);
            double e2 = legends[i].evaluateStdDev(dsorted);
            if (i == 0 && !Double.isNaN(e2)) {
                bestI = i;
                bestE = e2;
            } else if (e2 < bestE) {
                bestI = i;
                bestE = e2;
            }
        }
        if (bestI == -1) {
            return null;
        } else {
            return legends[bestI];
        }
    }

    public static Legend buildFrom(int[] intArray) {
        double[] d = new double[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            if (intArray[i] == Integer.MIN_VALUE) {
                d[i] = Double.NaN;
            } else {
                d[i] = intArray[i];
            }
        }
        return LegendEvaluate.buildFrom(d);
    }

}
