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
package org.ala.spatial.analysis.method;

import Jama.Matrix;
import org.six11.util.math.PCA;

/**
 * PCA to get colours
 */
public class Pca {

    /**
     * uses PCA to make colours (0-255, RGB) of data
     *
     * @param data
     * @return for each data record, one r, one g and one b component as 0-255
     * integer
     */
    public static int[][] getColours(double[][] cdata) {

        if (cdata == null || cdata.length == 0) {
            return null;
        }

        int i, j, k;

        //clone data
        double[][] data = cdata.clone();
        int[][] output = new int[data.length][3];
        int nvars = data[0].length;

        //scale data
        double min;
        double max;
        double sum;
        for (i = 0; i < data[0].length; i++) {
            //get min/max
            min = data[0][i];
            max = data[0][i];
            for (j = 1; j < data.length; j++) {
                if (data[j][i] < min) {
                    min = data[j][i];
                }
                if (data[j][i] > max) {
                    max = data[j][i];
                }
            }

            //scale to 0-1
            double range = max - min;
            if (range > 0) {
                for (j = 0; j < data.length; j++) {
                    data[j][i] = (data[j][i] - min) / range;
                }
            }

            //-mean
            sum = 0;
            for (j = 0; j < data.length; j++) {
                sum += data[j][i];
            }
            for (j = 0; j < data.length; j++) {
                data[j][i] -= sum / (double) data.length;
            }
        }


        /*
         * to bring more colour variation, when 2 columns, double columns (add
         * small randomness) when 3 columns, continue when 1 column, triple it
         */
        if (data[0].length == 1) {
            double[][] datamultiply = new double[data.length][3];
            for (i = 0; i < data.length; i++) {
                datamultiply[i][0] = data[i][0];
                datamultiply[i][1] = data[i][0];
                datamultiply[i][2] = data[i][0];
            }
            data = datamultiply;
            nvars = 3;
        }
        if (data[0].length == 2) {
            double[][] datamultiply = new double[data.length][4];
            for (i = 0; i < data.length; i++) {
                datamultiply[i][0] = data[i][0];
                datamultiply[i][1] = data[i][1];
                datamultiply[i][2] = data[i][0];
                datamultiply[i][3] = data[i][1];
            }
            data = datamultiply;
            nvars = 4;
        }
        {
            PCA pca = new PCA(data);
            k = 3;
            Matrix features = PCA.getDominantComponentsMatrix(pca.getDominantComponents(k));
            Matrix featuresXpose = features.transpose();
            Matrix adjustedInput = new Matrix(PCA.getMeanAdjusted(data, pca.getMeans()));
            Matrix xformedData = featuresXpose.times(adjustedInput.transpose());

            double[][] values = xformedData.transpose().getArray();

            /*
             * get min/max of values
             */
            double[] minval = new double[3];
            double[] maxval = new double[3];
            double[] range = new double[3];
            for (j = 0; j < 3; j++) {
                for (i = 0; i < values.length; i++) {
                    if (i == 0 || minval[j] > values[i][j]) {
                        minval[j] = values[i][j];
                    }
                    if (i == 0 || maxval[j] < values[i][j]) {
                        maxval[j] = values[i][j];
                    }
                }
            }
            for (j = 0; j < 3; j++) {
                range[j] = maxval[j] - minval[j];
                System.out.println("min/max for j=" + j + " " + minval[j] + " " + maxval[j]);
            }


            /*
             * transfer as colours
             */
            for (i = 0; i < data.length; i++) {
                for (j = 0; j < 3; j++) {
                    output[i][j] = (int) ((values[i][j] - minval[j]) / ((double) range[j]) * 255);
                }
            }
        }

        return output;
    }
}
