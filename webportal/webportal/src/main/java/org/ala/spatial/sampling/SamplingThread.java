/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.sampling;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author Adam
 */
public class SamplingThread extends Thread {

    LinkedBlockingQueue<Integer> lbq;
    CountDownLatch cdl;
    double[][] points;
    ArrayList<String> layers;
    ArrayList<String[]> output;

    public SamplingThread(LinkedBlockingQueue<Integer> lbq, CountDownLatch cdl, ArrayList<String> layers, double[][] points, ArrayList<String[]> output) {
        this.lbq = lbq;
        this.cdl = cdl;
        this.points = points;
        this.layers = layers;
        this.output = output;

        setPriority(MIN_PRIORITY);
    }

    public void run() {
        try {
            while (true) {
                int pos = lbq.take();
                try {                    
                    Sampling.sample(points, layers.get(pos), output.get(pos));
                } catch (Exception e) {
                }

                cdl.countDown();
            }
        } catch (Exception e) {
        }
    }
}
