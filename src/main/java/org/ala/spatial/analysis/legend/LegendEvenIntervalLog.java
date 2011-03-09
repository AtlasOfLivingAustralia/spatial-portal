/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.analysis.legend;

/**
 * generates legend with even interval cutoff's.
 *
 * @author Adam
 */
public class LegendEvenIntervalLog extends Legend {

    @Override
    public void generate(float[] d) {
        init(d);
        if(Float.isNaN(max)) {
            return;
        }
        int divisions = 10;

        cutoffs = new float[divisions];

        for(int i=0;i<divisions;i++){
            cutoffs[i] = (float) Math.pow(Math.E,Math.log(max) * ((i + 1) / (double) (divisions)));
        }

        //fix max
        cutoffs[divisions - 1] = max;
    }

    @Override
    public String getTypeName() {
        return "Even Interval Log";
    }
}