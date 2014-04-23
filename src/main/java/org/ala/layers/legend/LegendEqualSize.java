/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.layers.legend;

/**
 * generates legend using equal size of unique values in
 * each catagory.
 *
 * @author Adam
 */
public class LegendEqualSize extends Legend {

    @Override
    public void generate(float[] d, int divisions) {
        init(d, divisions);
        if (Float.isNaN(max)) {
            return;
        }
        cutoffs = new float[divisions];

        //calculate step based on remaining info
        int step = (int) Math.ceil(numberOfUniqueValues / (double) divisions);

        int uniqueCount = 0;
        int uniqueSum = 0;
        int pos = 0;
        for (int i = 0; i < lastValue; i++) {
            if (uniqueCount >= step) {
                uniqueSum += uniqueCount;
                uniqueCount = 0;

                while (i + 1 < lastValue && d[i] == d[i + 1]) {
                    i++;
                }
                if (i < lastValue) {
                    cutoffs[pos] = d[i - 1];
                } else {
                    while (pos < cutoffs.length) {
                        cutoffs[pos] = max;
                        pos++;
                    }
                    break;
                }

                pos++;

                //update step based on remaining info
                step = (int) Math.ceil((numberOfUniqueValues - uniqueSum) / (double) (divisions - pos));
            }

            if (i == 0 || d[i - 1] != d[i]) {
                uniqueCount++;
            }
        }

        //force top
        cutoffs[cutoffs.length - 1] = max;
    }

    @Override
    public String getTypeName() {
        return "Equal Size";
    }
}