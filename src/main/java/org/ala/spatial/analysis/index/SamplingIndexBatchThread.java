/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author Adam
 */
public class SamplingIndexBatchThread implements Runnable {

    Thread t;
    LinkedBlockingQueue<String> lbq;
    String output_path;
    double[][] points;
    String [] ids;

    public SamplingIndexBatchThread(LinkedBlockingQueue<String> lbq_, String output_path, double[][] points_, String [] ids) {
        t = new Thread(this);
        lbq = lbq_;
        this.output_path = output_path;
        points = points_;
        this.ids = ids;
        t.start();
    }

    public void run() {

        /* get next batch */
        String next;
        try {
            synchronized (lbq) {
                if (lbq.size() > 0) {
                    next = lbq.take();
                } else {
                    next = null;
                }
            }

            SamplingIndexBatch si = new SamplingIndexBatch(output_path, points, ids);

            while (next != null) {
                /* update for this layer */
                si.layersUpdate(next);

                /* get next available */
                synchronized (lbq) {
                    if (lbq.size() > 0) {
                        next = lbq.take();
                    } else {
                        next = null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isAlive() {
        return t.isAlive();
    }
}
