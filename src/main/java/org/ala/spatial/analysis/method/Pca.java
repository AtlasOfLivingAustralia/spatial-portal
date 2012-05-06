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
     * @return	for each data record, one r, one g and one b component as 0-255
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
        /*
         * treat columns as colours if(data[0].length <= 3){	//no need for pca
         *
         * // max/min per data column double [] minval = new double[3]; double
         * [] maxval = new double[3]; double [] range = new double[3];
         * for(i=0;i<data.length;i++){ for(j=0;j<nvars;j++){ if(i == 0 ||
         * minval[j] > data[i][j]){ minval[j] = data[i][j]; } if(i == 0 ||
         * maxval[j] < data[i][j]){ maxval[j] = data[i][j]; } } }
         * for(j=0;j<nvars;j++){ range[j] = maxval[j] - minval[j]; }
         *
         * // transfer as colours for(i=0;i<data.length;i++){
         * for(j=0;j<nvars;j++){ output[i][j] = (int)((data[i][j] -
         * minval[j])/((double)range[j])*255); } for(;j<3;j++){	//continue
         * //TODU: make missing value variable //output[i][j] = missing_value; }
         * } }else
         */
        {
//            /*
//             * use PCA to get top 3 eigenvectors for colours
//             */
//
//            /*
//             * column means, assumption: no missing values
//             */
//            double[] sum = new double[nvars];
//            double[] mean = new double[nvars];
//            for (i = 0; i < data.length; i++) {
//                for (j = 0; j < nvars; j++) {
//                    sum[j] += data[i][j];
//                }
//            }
//            for (j = 0; j < nvars; j++) {
//                mean[j] = sum[j] / ((double) data.length);
//            }
//
//            /*
//             * make covarient matrix
//             */
//            double[][] covar = new double[nvars][nvars];
//            double[][] covar_copy = new double[nvars][nvars];
//
//            for (i = 0; i < nvars; i++) {
//                for (j = 0; j < nvars; j++) {
//                    for (k = 0; k < data.length; k++) {
//                        covar[i][j] +=
//                                (data[k][i] - mean[i])
//                                * (data[k][j] - mean[j]);
//                    }
//                    covar[i][j] /= (double) data.length;
//                    covar_copy[i][j] = covar[i][j];
//                }
//            }
//
//            /*
//             * use pca()
//             */
//            double[] work = new double[covar.length];
//            double[] eigval = new double[covar.length];
//
//            pca(covar, work, eigval);
//
//            /*
//             * determine columns for top 3 eigval's
//             */
//            double[] topeigval = new double[3];
//            int[] topeigcol = new int[3];
//            for (j = 0; j < 3; j++) {
//                topeigval[j] = Double.MAX_VALUE * -1;
//                /*
//                 * get first valid point
//                 */
//                for (i = 0; i < eigval.length; i++) {
//                    if ((j == 0 || eigval[i] < topeigval[j - 1])) {
//                        topeigval[j] = eigval[i];
//                        topeigcol[j] = i;
//                        break;
//                    }
//                }
//                /*
//                 * do test this pass
//                 */
//                for (i = 0; i < eigval.length; i++) {
//                    if ((j == 0 || eigval[i] < topeigval[j - 1]) && (topeigval[j] < eigval[i])) {
//
//                        topeigval[j] = eigval[i];
//                        topeigcol[j] = i;
//                    }
//                }
//            }
//
//            //dump eigval for inspection
//            System.out.println("eigval=");
//            for (i = 0; i < eigval.length; i++) {
//                System.out.print(eigval[i] + ",");
//            }
//            System.out.println("");
//
//            //dump covar [0][1][2] for inspection
//            for (j = 0; j < 3; j++) {
//                System.out.println("covar(" + j + ")=" + topeigcol[j]);
//                for (i = 0; i < covar.length; i++) {
//                    System.out.print(covar[i][topeigcol[j]] + ",");
//                }
//            }
//
//            /*
//             * convert top 3 covar to values
//             */
//            double[][] values = new double[data.length][3];
//            for (j = 0; j < 3; j++) {
//                for (i = 0; i < covar.length; i++) {
//                    for (k = 0; k < data.length; k++) {
//                        values[k][j] += data[k][i] * covar[i][topeigcol[j]];
//                    }
//                }
//            }

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

    /*
     * SUBROUTINE PCA1 (ASSVEC,WORK,EIGVAL,LAB)
     */
    static void pca(double[][] assvec, double[] work, double[] eigval) {
        double eta = 1.0e-8;
        double tol = 1.0e-30;

        int nvec = 3;	//number of vectors;

        //make assvec		
        //for(int i=1;i<n;i++){
        //assvec[i][j] = 0;
        //}


        //*--------------------GET EIGEN SOLUTION
        int n, j, i;
        double ev, vij;
        n = eigval.length;
        if (!tdiag(n, tol, eigval, work, assvec)
                || !lrvt(n, eta, eigval, work, assvec)) {
            //error
            System.out.println("PCA error");
        }

        //*-----------------GET TOTAL EIGENVALUES FOR VAR
        double sumev = 0;
        for (i = 0; i < n; i++) {
            sumev += eigval[i];
        }
        //*----------------STANDARDIZE EIGENVECTORS

        for (j = 0; j < nvec; j++) {
            ev = Math.abs(eigval[j]);
            for (i = 0; i < n; i++) {
                vij = assvec[i][j];
                assvec[i][j] = Math.sqrt(vij * vij * ev);
                if (vij < 0) {
                    assvec[i][j] *= -1;
                }
            }
        }

        //*----------------------------------------WRITE EIGENVALUES/VECTORS
        //eigen vectors = assvec;
        //eigen values = eigval;
    }

    /*
     * SUBROUTINE LRVT (N,PRECIS,EIGVAL,WORK,ASEVEC,IFAULT,IUTRMO)
     *
     * PCA: Q-REIGENVALUES/VECTORS
     *
     * ALGORITHM AS 60.2 APPL.STATIST.(1973) VOL.22, NO.2
     *
     * FINDS LATENT VECTORS AND ROOTS OF TRIDIAGONAL MATRIX
     *
     * LANGUAGE: FTN 5 CALLED BY: CALL LRVT(N,PRECIS,EIGVAL,WORK,ASEVEC,IFAULT)
     * PARAMETERS: PRECIS ..... EIGVAL(N) ....... WORK(N) ....... ASEVEC(N,N)
     * ..... IFAULT ..... ERROR RETURN IF IFAULT.NE.0
     *
     ********************************************************************
     *
     *
     * returns true if successful
     */
    static boolean lrvt(int n, double precis, double[] eigval, double[] work, double[][] asevec) {
        double mits = 30.0;
        double zero = 0;
        double one = 1;
        double two = 2;

        int m;
        int i;

        for (i = 1; i < n; i++) {
            work[i - 1] = work[i];
        }
        work[n - 1] = 0;

        double b = 0;
        double f = 0;
        double p, r, g, c, s;
        int ip1, j, k;

        int l;
        double h;

        for (l = 0; l < n; l++) {
            System.out.println("iutrmo:" + (n - l + 1));

            j = 0;
            h = precis * (Math.abs(eigval[l]) + Math.abs(work[l]));
            if (b < h) {
                b = h;
            }
            //*-------------LOOK FOR SMALL SUB-DIAGONAL ELEMENT
            for (m = l; m < n; m++) {
                if (Math.abs(work[m]) <= b) {
                    break;
                }
            }
            if (m != l) {
                while (true) {
                    if (j == mits) {
                        return false;
                    }

                    j++;
                    //*---------------FORM SHIFT
                    g = eigval[l];
                    p = (eigval[l + 1] = g) / (two * work[l]);
                    r = Math.sqrt(p * p + one);
                    if (p < 0) {
                        eigval[l] = work[l] / (p - r);
                    } else {
                        eigval[l] = work[l] / (p + r);
                    }
                    h = g - eigval[l];
                    for (i = l + 1; i < n; i++) {
                        eigval[i] = eigval[i] - h;
                    }
                    f += h;

                    //*-----------------QL TRANSFORMATION
                    p = eigval[m];
                    c = one;
                    s = zero;
                    for (i = m - 1; i >= l; i--) {
                        ip1 = i + 1;
                        g = c * work[i];
                        h = c * p;
                        if (Math.abs(p) >= Math.abs(work[i])) {
                            c = work[i] / p;
                            r = Math.sqrt(c * c + one);
                            work[ip1] = s * p * r;
                            s = c / r;
                            c = one / r;
                        } else {
                            c = p / work[i];
                            r = Math.sqrt(c * c + one);
                            work[ip1] = s * work[i] * r;
                            s = one / r;
                            c = c / r;
                        }
                        p = c * eigval[i] - s * g;
                        eigval[ip1] = h + s * (c * g + s * eigval[i]);
                        //*-----------------------FORM VECTOR
                        for (k = 0; k < n; k++) {
                            h = asevec[k][ip1];
                            asevec[k][ip1] = s * asevec[k][i] + c * h;
                            asevec[k][i] = c * asevec[k][i] - s * h;
                        }
                    }
                    work[l] = s * p;
                    eigval[l] = c * p;
                    if (Math.abs(work[l]) > b) {
                        continue;
                    } else {
                        break;
                    }
                }
                eigval[l] = eigval[l] + f;
            }
        }

        //*-----------------ORDER EIGENVALUES AND VECTORS	
	   /*
         * want to retain original order for(i=0;i<nm1;i++){ k = i; p =
         * eigval[i]; for(j=i+1;j<n;j++){ if(eigval[j] > p){ k = j; p =
         * eigval[j]; } } if(k != i){ eigval[k] = eigval[i]; eigval[i] = p;
         * for(j=0;j<n;j++){ p = asevec[j][i]; asevec[j][i] = asevec[j][k];
         * asevec[j][k] = p; } } }
         */

        return true;

    }

    static boolean tdiag(int n, double tol, double[] eigval, double[] work, double[][] asevec) {
        double zero = 0;
        double one = 1;

        int i, j, l, k;
        double f, g, h, hh;

        for (i = n - 1; i >= 1; i--) {
            l = i - 2;
            f = asevec[i][i - 1];
            g = zero;
            if (l >= 0) {
                for (k = 0; k <=0; k++) {
                    g += asevec[i][k] * asevec[i][k];
                }
            }
            h = g + f * f;

            //*------------IF G TOO SMALL FOR ORTHOGONALITY-TRANSFORMATION SKIPPED
            if (g <= tol) {
                work[i] = f;
                eigval[i] = zero;
                break;
            }
            l++;
            if (f < zero) {
                g = Math.sqrt(h);
            } else {
                g = -Math.sqrt(h);
            }
            work[i] = g;
            h = h - f * g;
            asevec[i][i - 1] = f - g;
            f = zero;
            for (j = 0; j < l; j++) {
                asevec[j][i] = asevec[i][j] / h;
                g = zero;
                //*-------------------FORM ELEMENT OF A*U
                for (k = 0; k < j; k++) {
                    g += asevec[j][k] * asevec[i][k];
                }
                if (j < l) {
                    for (k = j + 1; k < l; k++) {
                        g += asevec[k][j] * asevec[i][k];
                    }
                }
                work[j] = g / h;
                f += g * asevec[j][i];
            }
            //*-------------------FORM K
            hh = f / (h + h);
            //*-------------------FORM REDUCED A
            for (j = 0; j < l; j++) {
                f = asevec[i][j];
                g = work[j] - hh * f;
                work[j] = g;
                for (k = 0; k < j; k++) {
                    asevec[j][k] = asevec[j][k] - f * work[k] - g * asevec[i][k];
                }
            }
            eigval[i] = h;
        }
        eigval[0] = zero;
        work[0] = zero;

        //*-----------------ACCUMULATION OF TRANSFORM MATRICES
        l = 0;
        for (i = 0; i < n; i++) {
            l = i - 1;
            if (eigval[i] != zero && l != 0) {
            }
            for (j = 0; j < l; j++) {
                g = zero;
                for (k = 0; k < l; k++) {
                    g += asevec[i][k] * asevec[k][j];
                }
                for (k = 0; k < l; k++) {
                    asevec[k][j] = asevec[k][j] - g * asevec[k][i];
                }
            }

            eigval[i] = asevec[i][i];
            asevec[i][i] = one;
            if (l != 0) {
                for (j = 1; j < l; j++) {
                    asevec[i][j] = zero;
                    asevec[j][i] = zero;
                }
            }
        }

        return true;
    }
}