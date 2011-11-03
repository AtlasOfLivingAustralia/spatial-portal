/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.analysis.index;

import java.util.concurrent.LinkedBlockingQueue;
import org.ala.spatial.util.Grid;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.TabulationSettings;

/**
 *
 * @author Adam
 */
public class CalculateDistanceThread implements Runnable {


    Thread t;
    double[][] range;
    double[][] min;
    LinkedBlockingQueue<int[]> lbq;
    double[][] measures;
    int[] next;
    Layer[] layers;

    CalculateDistanceThread(LinkedBlockingQueue<int[]> lbq_, double[][] range_, double[][] min_, double[][] measures_, Layer[] layers_) {
        lbq = lbq_;
        range = range_;
        min = min_;
        measures = measures_;
        layers = layers_;

        t = new Thread(this);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    @Override
    public void run() {
        try {
            next = new int[1];
            while (next != null) {
                synchronized (lbq) {
                    if (lbq.size() > 0) {
                        next = lbq.take();
                    } else {
                        next = null;
                    }
                }
                if (next != null) {
                    measures[next[0]][next[1]] = getDistance(layers[next[0]].name, layers[next[1]].name);
                    measures[next[1]][next[0]] = measures[next[0]][next[1]];
                    System.out.print(".");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    double getDistance(String layer1, String layer2) {
        Grid g1 = Grid.getGridStandardized(TabulationSettings.getPath(layer1));
        Grid g2 = Grid.getGridStandardized(TabulationSettings.getPath(layer2));

        float[] d1 = g1.getGrid();
        float[] d2 = g2.getGrid();

        int count = 0;
        double sum = 0;

        double longitude;
        double latitude;
        int p1, p2;
        double v1, v2;

        for (int i = 0; i < TabulationSettings.grd_nrows; i++) {
            for (int j = 0; j < TabulationSettings.grd_ncols; j++) {
                longitude = TabulationSettings.grd_xmin + TabulationSettings.grd_xdiv * j;
                latitude = TabulationSettings.grd_ymin + TabulationSettings.grd_ydiv * i;

                p1 = g1.getcellnumber(longitude, latitude);
                p2 = g2.getcellnumber(longitude, latitude);

//                if (p1 >= 0 && p1 < d1.length && range[i][j] > 0) {
                if (p1 >= 0 && p1 < d1.length) {
                    v1 = d1[p1];//(d1[p1]-g1.minval) / (g1.maxval - g1.minval);
                } else {
                    continue;
                }

                if (p2 >= 0 && p2 < d2.length) {
                    v2 = d2[p2];//(d2[p2]-g2.minval) / (g2.maxval - g2.minval);
                } else {
                    continue;
                }

                if (!Double.isNaN(v1) && !Double.isNaN(v2)) {
                    count++;
//                    sum += java.lang.Math.abs(v1 - v2) / range[i][j];
                    sum += java.lang.Math.abs(v1 - v2);
                }
            }
        }

        return sum / count;
    }

    boolean isAlive() {
        return t.isAlive();
    }
}
