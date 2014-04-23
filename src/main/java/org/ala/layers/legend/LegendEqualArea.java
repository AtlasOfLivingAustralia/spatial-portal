/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.layers.legend;

import java.util.ArrayList;

/**
 * generates legend using equal size of unique values in
 * each catagory.
 *
 * @author Adam
 */
public class LegendEqualArea extends Legend {

    @Override
    public void generate(float[] d, int divisions) {
        init(d, divisions);
        if (Float.isNaN(max)) {
            return;
        }
        cutoffs = new float[divisions];

        double scaling = 1;
        int direction = 0;
        int pos = 0;
        while (true) {
            //calculate step based on remaining info
            int step = (int) (Math.ceil(numberOfRecords / (double) divisions) * scaling);

            //determine if any 'unique values' require their own division
            ArrayList<Float> highFrequencyValues = getValuesByFrequency(d, step);

            int count = 0;
            pos = 0;
            int i = 0;
            for (i = 0; i < lastValue && pos < divisions; i++) {
                if (count >= step
                        || (i + 1 < lastValue && highFrequencyValues.contains(d[i + 1]))
                        || highFrequencyValues.contains(d[i])
                        || i == lastValue - 1) {
                    while (i + 1 < lastValue && d[i] == d[i + 1]) {
                        i++;
                        count++;
                    }
                    if (i < lastValue) {
                        cutoffs[pos] = d[i];
                    } else {
                        break;
                    }

                    pos++;

                    //update step based on remaining info
                    step = (int) (Math.ceil((numberOfRecords - i) / (double) (divisions - pos)) * scaling);

                    count = 0;
                }

                count++;
            }

            if (i != lastValue && direction >= 0) {
                //too many divisions, make step larger
                scaling *= 1.2;
                direction = 1;
            } else if (pos < divisions && numberOfUniqueValues > divisions
                    && direction <= 0) {
                //too few divisions, make step smaller
                scaling /= 1.2;
                direction = -1;
            } else {
                break;
            }
        }

        stretch(cutoffs, divisions, pos);

        //force top
        cutoffs[cutoffs.length - 1] = max;
    }

    @Override
    public String getTypeName() {
        return "Equal Area";
    }

    private void stretch(float[] cutoffs, int divisions, int pos) {
        if (pos < divisions && pos > 0) {
            //add spacing
            double step = divisions / (divisions - pos + 1.0);
            for (int i = 1; i < divisions - pos + 1; i++) {
                int min = (int) Math.round(i * step);
                for (int j = divisions - 1; j >= min; j--) {
                    cutoffs[j] = cutoffs[j - 1];
                }
            }

            //compensate for '<' method by halving the size of cutoff spans of the same value
            int start = 1;
            for (int i = 1; i < divisions; i++) {
                if (cutoffs[i] != cutoffs[i - 1] && (i - start) > 1) {
                    int mid = (i - start) / 2 + start;

                    //fill up with the lower value
                    for (int j = start; j < mid; j++) {
                        cutoffs[j] = cutoffs[j - 1];
                    }

                    start = i;
                }
            }

            //set unqiue end
            if (cutoffs[divisions - 1] == cutoffs[divisions - 2]) {
                for (int i = divisions - 1; i > 0; i--) {
                    if (cutoffs[i] != cutoffs[i - 1]) {
                        for (int j = i; j < divisions - 1; j++) {
                            cutoffs[j] = cutoffs[j - 1];
                        }
                        break;
                    }
                }
            }
        }
    }

    private ArrayList<Float> getValuesByFrequency(float[] d, int step) {
        int[] uniqueValueDistribution = new int[numberOfUniqueValues];
        float[] uniqueValues = new float[numberOfUniqueValues];
        int p = 0;
        uniqueValues[0] = d[0];
        for (int i = 0; i < lastValue; i++) {
            if (i > 0 && d[i] != d[i - 1]) {
                p++;
                uniqueValues[p] = d[i];
            }
            uniqueValueDistribution[p]++;
        }

        ArrayList<Float> list = new ArrayList<Float>();
        for (int i = 0; i < numberOfUniqueValues; i++) {
            if (uniqueValueDistribution[i] >= step) {
                list.add(uniqueValues[i]);
            }
        }

        return list;
    }
}